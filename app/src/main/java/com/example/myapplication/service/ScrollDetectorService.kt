package com.example.myapplication.service

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
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
    private val MIN_WATCH_TIME_MS = 2000L 

    private var deepScrollRecordedThisSession = false
    private var sessionStartTime = 0L
    private var lastEventTime = 0L
    private var accumulatedSessionTime = 0L
    private var lastPersistedMs = 0L

    private val SESSION_RESET_MS = 5 * 60_000L
    private val MIN_SESSION_MS = 2_000L

    private var notifyAfterReels = 10
    private var reelNotifyInterval = 10
    private var nextNotifyReel = 10
    private var reelsInSession = 0

    private var rapidScrollStreak = 0
    private var lastRapidScrollTime = 0L
    private val RAPID_SCROLL_THRESHOLD_MS = 500L
    private val RAPID_SCROLL_STREAK_LIMIT = 3
    private var lastUnconsciousNotificationTime = 0L
    private val UNCONSCIOUS_COOLDOWN_MS = 45_000L

    private val handler = Handler(Looper.getMainLooper())
    private var endSessionRunnable: Runnable? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        val database = AppDatabase.getDatabase(applicationContext)
        val apiService = RetrofitClient.apiService
        registrationRepository = RegistrationRepository(applicationContext, apiService)
        statsRepository = StatsRepository(database.statsDao(), apiService, registrationRepository)
        syncManager = SyncManager(applicationContext)
        unconsciousDetector = UnconsciousScrollingDetector(applicationContext)

        serviceScope.launch {
            registrationRepository.registerDeviceIfNeeded()
        }
        syncManager.startPeriodicSync()

        serviceScope.launch {
            NotificationSettingsStore.notifyAfterReelsFlow(applicationContext).collect {
                notifyAfterReels = it.coerceAtLeast(1)
                nextNotifyReel = notifyAfterReels
            }
        }
        serviceScope.launch {
            ReelSettingsStore.intervalFlow(applicationContext).collect {
                reelNotifyInterval = it.coerceAtLeast(1)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        if (pkg != "com.instagram.android") {
            endSession()
            return
        }

        val now = System.currentTimeMillis()
        val isScrollEvent = event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED

        if (isReelsScroll(event)) {
            unconsciousDetector.updateLastEventTime(now)
            unconsciousDetector.onAccessibilityEvent(event, now)

            val currentIndex = event.fromIndex
            if (currentIndex != currentWatchingReelIndex && currentIndex != -1) {
                // Check if previous reel was watched long enough
                if (currentWatchingReelIndex != -1 && now - lastWatchStartTime >= MIN_WATCH_TIME_MS) {
                    recordReelView()
                }
                currentWatchingReelIndex = currentIndex
                lastWatchStartTime = now
            }
        }

        if (unconsciousDetector.hasDetectedUnconsciousScrolling() && !deepScrollRecordedThisSession) {
            deepScrollRecordedThisSession = true
            serviceScope.launch {
                statsRepository.incrementDeepScroll()
            }
        }

        if (isScrollEvent && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (abs(event.scrollDeltaX) > abs(event.scrollDeltaY)) return
            if (event.scrollDeltaY == 0 && event.maxScrollY == 0) return
        }

        if (lastEventTime != 0L && now - lastEventTime > SESSION_RESET_MS) {
            endSession()
        }

        if (sessionStartTime == 0L) {
            startSession(now)
            return
        }

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
        return pkg == "com.instagram.android" && (className.contains("ViewPager") || className.contains("RecyclerView"))
    }

    private fun recordReelView() {
        reelsInSession++
        serviceScope.launch {
            statsRepository.incrementReels()
            if (reelsInSession >= nextNotifyReel) {
                nextNotifyReel += notifyAfterReels
                NotificationHelper.showMindfulNotification(applicationContext, reelsInSession, true)
            }
        }
    }

    private fun handleRapidScroll(now: Long) {
        val sinceLast = now - lastRapidScrollTime
        rapidScrollStreak = if (sinceLast < RAPID_SCROLL_THRESHOLD_MS) rapidScrollStreak + 1 else 1
        lastRapidScrollTime = now

        if (accumulatedSessionTime >= 2 * 60_000L &&
            rapidScrollStreak >= RAPID_SCROLL_STREAK_LIMIT &&
            now - lastUnconsciousNotificationTime > UNCONSCIOUS_COOLDOWN_MS) {
            lastUnconsciousNotificationTime = now
            NotificationHelper.showUnconsciousScrollingNotification(applicationContext, UnconsciousType.RAPID_SWIPING)
        }
    }

    private fun startSession(now: Long) {
        sessionStartTime = now
        lastEventTime = now
        accumulatedSessionTime = 0L
        lastPersistedMs = 0L
        nextNotifyReel = notifyAfterReels
        reelsInSession = 0
        lastReelIndex = -1 // Fix: variable from previous version
        currentWatchingReelIndex = -1
        lastWatchStartTime = now
        
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
        syncJob?.cancel()
        
        // Final watch check
        if (currentWatchingReelIndex != -1 && System.currentTimeMillis() - lastWatchStartTime >= MIN_WATCH_TIME_MS) {
            recordReelView()
        }

        unconsciousDetector.resetSession()
        deepScrollRecordedThisSession = false
        sessionStartTime = 0L
        lastEventTime = 0L
        syncManager.triggerImmediateSync()
    }

    private fun scheduleSessionTimeout() {
        endSessionRunnable?.let { handler.removeCallbacks(it) }
        endSessionRunnable = Runnable { endSession() }
        handler.postDelayed(endSessionRunnable!!, SESSION_RESET_MS)
    }

    override fun onInterrupt() = endSession()

    override fun onDestroy() {
        endSession()
        serviceScope.cancel()
        super.onDestroy()
    }

    private var lastReelIndex = -1 // For backward compatibility in code above
}
