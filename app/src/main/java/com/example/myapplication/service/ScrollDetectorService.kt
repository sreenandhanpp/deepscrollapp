package com.example.myapplication.service

import android.accessibilityservice.AccessibilityService
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.ActivityCompat
import com.example.myapplication.data.NotificationSettingsStore
import com.example.myapplication.data.ReelSettingsStore
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.remote.RetrofitClient
import com.example.myapplication.data.repository.*
import com.example.myapplication.service.detector.UnconsciousScrollingDetector
import com.example.myapplication.service.detector.UnconsciousType
import com.example.myapplication.utils.NotificationHelper
import kotlinx.coroutines.*
import kotlin.math.abs

class ScrollDetectorService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var statsRepository: StatsRepository
    private lateinit var registrationRepository: RegistrationRepository
    private lateinit var syncManager: SyncManager
    private lateinit var unconsciousDetector: UnconsciousScrollingDetector

    private var syncJob: Job? = null
    private var lastWatchStartTime = 0L
    private var currentWatchingReelIndex = -1
    private val MIN_WATCH_TIME_MS = 500L // Reduced to 500ms for easier testing

    private var deepScrollRecordedThisSession = false
    private var sessionStartTime = 0L
    private var lastEventTime = 0L
    private var accumulatedSessionTime = 0L
    private var lastPersistedMs = 0L

    private val SESSION_RESET_MS = 10 * 60_000L // Increased to 10 minutes
    private val MIN_SESSION_MS = 2_000L

    private var notifyAfterReels = 1
    private var reelNotifyInterval = 1
    private var nextNotifyReel = 1
    private var reelsInSession = 0

    // Notification toggles — kept in sync from DataStore
    private var rapidSwipeNotifyEnabled = true
    private var zoneOutNotifyEnabled = true
    private var reelCountNotifyEnabled = true

    private var rapidScrollStreak = 0
    private var lastRapidScrollTime = 0L
    private val RAPID_SCROLL_THRESHOLD_MS = 500L
    private val RAPID_SCROLL_STREAK_LIMIT = 3
    private var lastUnconsciousNotificationTime = 0L
    private val UNCONSCIOUS_COOLDOWN_MS = 45_000L

    private val handler = Handler(Looper.getMainLooper())
    private var endSessionRunnable: Runnable? = null

    companion object {
        private const val TAG = "ScrollDetector"
    }

    // ─────────────────────────────────────────────────────────────

    override fun onServiceConnected() {
        super.onServiceConnected()
        val database = AppDatabase.getDatabase(applicationContext)
        val apiService = RetrofitClient.apiService
        val deepScrollApi = RetrofitClient.deepScrollApi
        registrationRepository = RegistrationRepository(deepScrollApi, applicationContext)
        statsRepository = StatsRepository(database.statsDao(), apiService, registrationRepository)
        syncManager = SyncManager(applicationContext)
        unconsciousDetector = UnconsciousScrollingDetector(applicationContext)

        serviceScope.launch {
            registrationRepository.registerDeviceIfNeeded()
        }
        syncManager.startPeriodicSync()

        // Reel interval — recalculate nextNotifyReel relative to current count on change
        serviceScope.launch {
            NotificationSettingsStore.notifyAfterReelsFlow(applicationContext).collect { newVal ->
                val clamped = newVal.coerceAtLeast(1)
                notifyAfterReels = clamped
                reelNotifyInterval = clamped
                nextNotifyReel = reelsInSession + clamped
                Log.d(TAG, "Notification interval updated: notifyAfterReels=$notifyAfterReels, nextNotifyReel=$nextNotifyReel")
            }
        }
        serviceScope.launch {
            ReelSettingsStore.intervalFlow(applicationContext).collect {
                reelNotifyInterval = it.coerceAtLeast(1)
                Log.d(TAG, "Reel interval updated: $reelNotifyInterval")
            }
        }

        // Notification toggles
        serviceScope.launch {
            NotificationSettingsStore.rapidSwipeEnabled(applicationContext).collect {
                rapidSwipeNotifyEnabled = it
                Log.d(TAG, "Rapid swipe notify enabled: $rapidSwipeNotifyEnabled")
            }
        }
        serviceScope.launch {
            NotificationSettingsStore.zoneOutEnabled(applicationContext).collect {
                zoneOutNotifyEnabled = it
                Log.d(TAG, "Zone out notify enabled: $zoneOutNotifyEnabled")
            }
        }
        serviceScope.launch {
            NotificationSettingsStore.mindlessEnabled(applicationContext).collect {
                reelCountNotifyEnabled = it
                Log.d(TAG, "Reel count notify enabled: $reelCountNotifyEnabled")
            }
        }

        Log.d(TAG, "ScrollDetectorService connected successfully")
        handler.postDelayed({
            NotificationHelper.checkNotificationStatus(applicationContext)
            NotificationHelper.testNotification(applicationContext)
            Log.d(TAG, "Test notification sent - check if you see it!")
        }, 3000)
    }

    // ─────────────────────────────────────────────────────────────

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return

        // Log basic event info for debugging
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            Log.v(TAG, "Scroll event from: $pkg | class: ${event.className}")
        }

        if (pkg != "com.instagram.android") {
            // 🛑 STOP: Do not call endSession() immediately here.
            // System events or transient notifications can have different package names.
            // We let the SESSION_RESET_MS timeout handle session termination naturally.
            return
        }

        val now = System.currentTimeMillis()
        val isScrollEvent = event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED

        // ── Session start ────────────────────────────────────────
        if (sessionStartTime == 0L) {
            startSession(now)
        } else {
            // Check if we need to resume or if it's been too long
            if (lastEventTime != 0L && now - lastEventTime > SESSION_RESET_MS) {
                endSession()
                startSession(now)
            }
        }

        // ── Unconscious detection updates ──
        unconsciousDetector.updateLastEventTime(now)
        unconsciousDetector.onAccessibilityEvent(event, now)

        // ── Reel-specific index counting ────────────────────────
        if (isReelsScroll(event)) {
            val currentIndex = event.fromIndex
            Log.d(TAG, "Reel scroll event: index=$currentIndex, currentIdx=$currentWatchingReelIndex")

            if (currentIndex != currentWatchingReelIndex && currentIndex != -1) {
                val watchTime = now - lastWatchStartTime
                Log.d(TAG, "Index changed! Previous watch time: $watchTime ms")

                if (currentWatchingReelIndex != -1 && watchTime >= MIN_WATCH_TIME_MS) {
                    recordReelView()
                } else if (currentWatchingReelIndex != -1) {
                    Log.d(TAG, "Watch time too short ($watchTime < $MIN_WATCH_TIME_MS), not counting.")
                }
                currentWatchingReelIndex = currentIndex
                lastWatchStartTime = now
            }
        }

        // ── Deep scroll ─────────────────────────────────────────
        if (unconsciousDetector.hasDetectedUnconsciousScrolling() && !deepScrollRecordedThisSession) {
            deepScrollRecordedThisSession = true
            serviceScope.launch {
                statsRepository.incrementDeepScroll()
            }
        }

        // ── Filter horizontal / fake scrolls ────────────────────
        if (isScrollEvent && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (abs(event.scrollDeltaX) > abs(event.scrollDeltaY)) return
            if (event.scrollDeltaY == 0 && event.maxScrollY == 0) return
        }

        // ── Accumulate time ──────────────────────────────────────
        val delta = now - lastEventTime
        accumulatedSessionTime += delta
        lastEventTime = now

        val deltaSincePersist = accumulatedSessionTime - lastPersistedMs
        if (deltaSincePersist >= 60_000L) {
            serviceScope.launch {
                statsRepository.addUsageMinutes(1)
            }
            lastPersistedMs += 60_000L
        }

        if (isScrollEvent) {
            handleRapidScroll(now)
        }

        scheduleSessionTimeout()
    }

    // ─────────────────────────────────────────────────────────────
    // REEL HELPERS
    // ─────────────────────────────────────────────────────────────

    private fun isReelsScroll(event: AccessibilityEvent): Boolean {
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED) return false
        val pkg = event.packageName?.toString() ?: ""
        val className = event.className?.toString() ?: ""

        // Loosened check: any scroll in Instagram that's not clearly a list/grid of photos
        // or just detecting the main package.
        val matched = pkg == "com.instagram.android" &&
                (className.contains("ViewPager") ||
                        className.contains("RecyclerView") ||
                        className.contains("LayoutManager") ||
                        className.contains("FrameLayout")) // Some reels are wrapped in FrameLayouts

        if (matched) {
            Log.d(TAG, "Reel scroll detected | class=$className")
        }
        return matched
    }

    private fun recordReelView() {
        reelsInSession++

        Log.d(TAG, "=== REEL VIEW RECORDED ===")
        Log.d(TAG, "reelsInSession: $reelsInSession")
        Log.d(TAG, "nextNotifyReel: $nextNotifyReel")
        Log.d(TAG, "notifyAfterReels: $notifyAfterReels")
        Log.d(TAG, "reelCountNotifyEnabled: $reelCountNotifyEnabled")

        val shouldNotify = reelCountNotifyEnabled && reelsInSession >= nextNotifyReel

        if (shouldNotify) {
            Log.d(TAG, "*** SHOULD NOTIFY! Showing notification for reel #$reelsInSession ***")
            nextNotifyReel += notifyAfterReels
            Log.d(TAG, "Next notification at reel #$nextNotifyReel")

            // Check notification permission on Android 13+
            val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.checkSelfPermission(
                    applicationContext,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

            if (hasPermission) {
                serviceScope.launch {
                    try {
                        Log.d(TAG, "Calling NotificationHelper.showMindfulNotification")
                        NotificationHelper.showMindfulNotification(
                            applicationContext,
                            reelsInSession,
                            true
                        )
                        Log.d(TAG, "NotificationHelper call completed successfully")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error showing notification", e)
                    }
                }
            } else {
                Log.e(TAG, "Notification permission not granted for Android 13+")
            }
        } else {
            Log.d(TAG, "No notification - conditions not met")
            if (!reelCountNotifyEnabled) {
                Log.d(TAG, "  - reelCountNotifyEnabled is false")
            }
            if (reelsInSession < nextNotifyReel) {
                Log.d(TAG, "  - need ${nextNotifyReel - reelsInSession} more reels until next notification")
            }
        }

        serviceScope.launch {
            statsRepository.incrementReels()
        }
    }

    // ─────────────────────────────────────────────────────────────
    // RAPID SCROLL
    // ─────────────────────────────────────────────────────────────

    private fun handleRapidScroll(now: Long) {
        val sinceLast = now - lastRapidScrollTime
        rapidScrollStreak = if (sinceLast < RAPID_SCROLL_THRESHOLD_MS) rapidScrollStreak + 1 else 1
        lastRapidScrollTime = now

        if (rapidSwipeNotifyEnabled &&
            accumulatedSessionTime >= 10_000L && // Reduced from 2 mins to 10s for testing
            rapidScrollStreak >= RAPID_SCROLL_STREAK_LIMIT &&
            now - lastUnconsciousNotificationTime > UNCONSCIOUS_COOLDOWN_MS
        ) {
            lastUnconsciousNotificationTime = now
            Log.d(TAG, "Rapid scroll detected! Showing notification")

            // Check notification permission
            val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.checkSelfPermission(
                    applicationContext,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

            if (hasPermission) {
                NotificationHelper.showUnconsciousScrollingNotification(
                    applicationContext,
                    UnconsciousType.RAPID_SWIPING
                )
            } else {
                Log.e(TAG, "Notification permission not granted for rapid scroll notification")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // SESSION MANAGEMENT
    // ─────────────────────────────────────────────────────────────

    private fun startSession(now: Long) {
        Log.d(TAG, "Starting new session at $now")
        sessionStartTime = now
        lastEventTime = now
        accumulatedSessionTime = 0L
        lastPersistedMs = 0L
        nextNotifyReel = notifyAfterReels
        reelsInSession = 0
        currentWatchingReelIndex = -1
        lastWatchStartTime = now
        rapidScrollStreak = 0

        Log.d(TAG, "Session initialized: nextNotifyReel=$nextNotifyReel, notifyAfterReels=$notifyAfterReels")

        serviceScope.launch {
            statsRepository.incrementSessions()
        }
        startSyncLoop()
    }

    private fun startSyncLoop() {
        syncJob?.cancel()
        syncJob = serviceScope.launch {
            while (isActive) {
                delay(1000)
                statsRepository.syncWithBackend()
            }
        }
    }

    private fun endSession() {
        if (sessionStartTime == 0L) return
        Log.d(TAG, "Ending session. Total reels viewed: $reelsInSession")
        syncJob?.cancel()

        // Count the last reel if it was watched long enough
        if (currentWatchingReelIndex != -1 &&
            System.currentTimeMillis() - lastWatchStartTime >= MIN_WATCH_TIME_MS
        ) {
            recordReelView()
        }

        unconsciousDetector.resetSession()
        deepScrollRecordedThisSession = false
        sessionStartTime = 0L
        lastEventTime = 0L
        currentWatchingReelIndex = -1
        syncManager.triggerImmediateSync()
    }

    private fun scheduleSessionTimeout() {
        endSessionRunnable?.let { handler.removeCallbacks(it) }
        endSessionRunnable = Runnable { endSession() }
        handler.postDelayed(endSessionRunnable!!, SESSION_RESET_MS)
    }

    override fun onInterrupt() = endSession()

    override fun onDestroy() {
        Log.d(TAG, "Service destroying")
        endSession()
        serviceScope.cancel()
        super.onDestroy()
    }
}