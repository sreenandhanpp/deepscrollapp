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
import com.example.myapplication.data.repository.RegistrationRepository
import com.example.myapplication.data.repository.StatsRepository
import com.example.myapplication.data.repository.SyncManager
import com.example.myapplication.service.detector.UnconsciousScrollingDetector
import com.example.myapplication.service.detector.UnconsciousType
import com.example.myapplication.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
    private val MIN_WATCH_TIME_MS = 500L

    private var deepScrollRecordedThisSession = false
    private var sessionStartTime = 0L
    private var lastEventTime = 0L
    private var accumulatedSessionTime = 0L
    private var lastPersistedMs = 0L

    private val SESSION_RESET_MS = 10 * 60_000L
    private val MIN_SESSION_MS = 2_000L

    // Reel notification tracking
    private var notifyAfterReels = 5  // Default to 5 reels
    private var reelNotifyInterval = 5
    private var nextNotifyReel = 5
    private var reelsInSession = 0
    private var lastReelNotificationTime = 0L
    private val MIN_NOTIFICATION_INTERVAL_MS =
        30_000L // Don't notify more than once every 30 seconds

    // Notification toggles
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

    override fun onServiceConnected() {
        super.onServiceConnected()
        val database = AppDatabase.getDatabase(applicationContext)
        val deepScrollApi = RetrofitClient.deepScrollApi
        
        registrationRepository = RegistrationRepository(
            apiService = deepScrollApi,
            context = applicationContext
        )
        statsRepository = StatsRepository(
            statsDao = database.statsDao(),
            apiService = deepScrollApi,
            registrationRepository = registrationRepository
        )
        syncManager = SyncManager(applicationContext)
        unconsciousDetector = UnconsciousScrollingDetector(applicationContext)

        serviceScope.launch {
            registrationRepository.registerDeviceIfNeeded()
        }
        syncManager.startPeriodicSync()

        // Load reel notification settings
        serviceScope.launch {
            NotificationSettingsStore.notifyAfterReelsFlow(applicationContext).collect { newVal ->
                val clamped = newVal.coerceAtLeast(1)
                notifyAfterReels = clamped
                reelNotifyInterval = clamped
                // Reset next notification threshold when settings change
                nextNotifyReel = reelsInSession + clamped
                Log.d(
                    TAG,
                    "Notification interval updated: notifyAfterReels=$notifyAfterReels, nextNotifyReel=$nextNotifyReel"
                )
            }
        }

        serviceScope.launch {
            ReelSettingsStore.intervalFlow(applicationContext).collect {
                reelNotifyInterval = it.coerceAtLeast(1)
                Log.d(TAG, "Reel interval updated: $reelNotifyInterval")
            }
        }

        // Load notification toggles
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

        // Test notification after delay
        handler.postDelayed({
            NotificationHelper.checkNotificationStatus(applicationContext)
            testReelCountNotification()
        }, 3000)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            Log.v(TAG, "Scroll event from: $pkg | class: ${event.className}")
        }

        if (pkg != "com.instagram.android") {
            return
        }

        val now = System.currentTimeMillis()
        val isScrollEvent = event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED

        // Session management
        if (sessionStartTime == 0L) {
            startSession(now)
        } else {
            if (lastEventTime != 0L && now - lastEventTime > SESSION_RESET_MS) {
                endSession()
                startSession(now)
            }
        }

        // Unconscious detection
        unconsciousDetector.updateLastEventTime(now)
        unconsciousDetector.onAccessibilityEvent(event, now)

        // Reel tracking with improved detection
        if (isReelsScroll(event)) {
            val currentIndex = event.fromIndex
            Log.d(
                TAG,
                "Reel scroll event: index=$currentIndex, currentIdx=$currentWatchingReelIndex"
            )

            if (currentIndex != currentWatchingReelIndex && currentIndex != -1) {
                val watchTime = now - lastWatchStartTime
                Log.d(TAG, "Index changed! Previous watch time: $watchTime ms")

                if (currentWatchingReelIndex != -1 && watchTime >= MIN_WATCH_TIME_MS) {
                    recordReelView()
                } else if (currentWatchingReelIndex != -1) {
                    Log.d(
                        TAG,
                        "Watch time too short ($watchTime < $MIN_WATCH_TIME_MS), not counting."
                    )
                }
                currentWatchingReelIndex = currentIndex
                lastWatchStartTime = now
            }
        }

        // Deep scroll tracking
        if (unconsciousDetector.hasDetectedUnconsciousScrolling() && !deepScrollRecordedThisSession) {
            deepScrollRecordedThisSession = true
            serviceScope.launch {
                statsRepository.incrementDeepScroll()
            }
        }

        // Filter horizontal scrolls
        if (isScrollEvent && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (abs(event.scrollDeltaX) > abs(event.scrollDeltaY)) return
            if (event.scrollDeltaY == 0 && event.maxScrollY == 0) return
        }

        // Time accumulation
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

    private fun isReelsScroll(event: AccessibilityEvent): Boolean {
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED) return false
        val pkg = event.packageName?.toString() ?: ""
        val className = event.className?.toString() ?: ""

        val matched = pkg == "com.instagram.android" &&
                (className.contains("ViewPager") ||
                        className.contains("RecyclerView") ||
                        className.contains("LayoutManager") ||
                        className.contains("FrameLayout"))

        if (matched) {
            Log.d(TAG, "Reel scroll detected | class=$className")
        }
        return matched
    }

    private fun recordReelView() {
        reelsInSession++

        Log.d(TAG, "=== REEL VIEW RECORDED ===")
        Log.d(TAG, "Total reels in session: $reelsInSession")
        Log.d(TAG, "Next notification at: $nextNotifyReel")
        Log.d(TAG, "Notify after reels: $notifyAfterReels")
        Log.d(TAG, "Reel count notify enabled: $reelCountNotifyEnabled")

        // Check if we should show notification
        val now = System.currentTimeMillis()
        val shouldNotify = reelCountNotifyEnabled &&
                reelsInSession >= nextNotifyReel &&
                (now - lastReelNotificationTime) >= MIN_NOTIFICATION_INTERVAL_MS

        if (shouldNotify) {
            showReelCountNotification()
            nextNotifyReel += notifyAfterReels
            lastReelNotificationTime = now
            Log.d(TAG, "Next notification scheduled at reel #$nextNotifyReel")
        } else {
            Log.d(TAG, "No notification - conditions not met")
            if (!reelCountNotifyEnabled) {
                Log.d(TAG, "  - reelCountNotifyEnabled is false")
            }
            if (reelsInSession < nextNotifyReel) {
                val remaining = nextNotifyReel - reelsInSession
                Log.d(TAG, "  - Need $remaining more reels until next notification")
            }
            if ((now - lastReelNotificationTime) < MIN_NOTIFICATION_INTERVAL_MS) {
                val waitTime =
                    (MIN_NOTIFICATION_INTERVAL_MS - (now - lastReelNotificationTime)) / 1000
                Log.d(TAG, "  - Need to wait ${waitTime}s before next notification")
            }
        }

        // Save to repository
        serviceScope.launch {
            statsRepository.incrementReels()
            // Also save current reel count to shared preferences for persistence
            saveReelCountToPreferences(reelsInSession)
        }
    }

    private fun showReelCountNotification() {
        Log.d(TAG, "*** SHOWING REEL COUNT NOTIFICATION #$reelsInSession ***")

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
            try {
                // Show notification with reel count
                NotificationHelper.showMindfulNotification(
                    applicationContext,
                    reelsInSession,
                    true
                )

                // Also show a summary notification for debugging
                Log.d(TAG, "Notification shown successfully for $reelsInSession reels")

            } catch (e: Exception) {
                Log.e(TAG, "Error showing reel count notification", e)
            }
        } else {
            Log.e(TAG, "Notification permission not granted for Android 13+")
        }
    }

    private fun testReelCountNotification() {
        // Test function to verify notification system works
        Log.d(TAG, "Testing reel count notification system")
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (hasPermission && reelCountNotifyEnabled) {
            NotificationHelper.showMindfulNotification(
                applicationContext,
                10, // Test with 10 reels
                true
            )
            Log.d(TAG, "Test notification sent")
        }
    }

    private fun saveReelCountToPreferences(count: Int) {
        // Save current session reel count to be able to restore if needed
        applicationContext.getSharedPreferences("reel_stats", MODE_PRIVATE)
            .edit()
            .putInt("last_session_reels", count)
            .apply()
    }

    private fun handleRapidScroll(now: Long) {
        val sinceLast = now - lastRapidScrollTime
        rapidScrollStreak = if (sinceLast < RAPID_SCROLL_THRESHOLD_MS) rapidScrollStreak + 1 else 1
        lastRapidScrollTime = now

        if (rapidSwipeNotifyEnabled &&
            accumulatedSessionTime >= 10_000L &&
            rapidScrollStreak >= RAPID_SCROLL_STREAK_LIMIT &&
            now - lastUnconsciousNotificationTime > UNCONSCIOUS_COOLDOWN_MS
        ) {
            lastUnconsciousNotificationTime = now
            Log.d(TAG, "Rapid scroll detected! Showing notification")

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
            }
        }
    }

    private fun startSession(now: Long) {
        Log.d(TAG, "Starting new session at $now")
        sessionStartTime = now
        lastEventTime = now
        accumulatedSessionTime = 0L
        lastPersistedMs = 0L

        // Reset reel tracking for new session
        nextNotifyReel = notifyAfterReels
        reelsInSession = 0
        currentWatchingReelIndex = -1
        lastWatchStartTime = now
        rapidScrollStreak = 0
        lastReelNotificationTime = 0L

        Log.d(
            TAG,
            "Session initialized: notifyAfterReels=$notifyAfterReels, nextNotifyReel=$nextNotifyReel"
        )

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

        // Count the last reel if watched long enough
        if (currentWatchingReelIndex != -1 &&
            System.currentTimeMillis() - lastWatchStartTime >= MIN_WATCH_TIME_MS
        ) {
            recordReelView()
        }

        // Show final session summary if more than 10 reels watched
        if (reelsInSession >= 10 && reelCountNotifyEnabled) {
            NotificationHelper.showMindfulNotification(
                applicationContext,
                reelsInSession,
                false
            )
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