package com.example.myapplication.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.example.myapplication.data.ScrollDataStore
import com.example.myapplication.data.ReelSettingsStore
import com.example.myapplication.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScrollDetectorService : AccessibilityService() {

    /* =========================================================
       ================= SESSION STATE =========================
       ========================================================= */

    private var sessionStartTime = 0L
    private var lastAnyScrollTime = 0L

    private val SESSION_RESET_MS = 45_000L   // inactivity resets session
    private val MIN_SESSION_MS = 3_000L      // ignore first 3s (testing)

    private var sessionStartScrollCount = 0
    private var lastAwarenessTriggerAt = 0

    /* =========================================================
       ================= SCROLL COUNT (CANONICAL) ===============
       ========================================================= */

    private var cachedScrollCount = 0

    // 🔒 DEBOUNCE (THIS FIXES DOUBLE COUNT)
    private var lastCountedScrollTime = 0L
    private val SCROLL_DEBOUNCE_MS = 400L

    /* =========================================================
       ================= USER CONFIG ============================
       ========================================================= */

    private var reelInterval = 10

    /* =========================================================
       ================= MINDLESS DETECTION =====================
       ========================================================= */

    private var lastScrollTime = 0L
    private var continuousScrollTime = 0L

    private val PAUSE_THRESHOLD_MS = 800L
    private val MIN_CONTINUOUS_SCROLL_MS = 5_000L   // testing
    private val NOTIFICATION_COOLDOWN_MS = 15_000L  // testing

    private var lastMindlessNotificationTime = 0L

    /* =========================================================
       ================= SERVICE SETUP ==========================
       ========================================================= */

    override fun onServiceConnected() {
        super.onServiceConnected()

        // Load user-defined reel interval
        CoroutineScope(Dispatchers.IO).launch {
            ReelSettingsStore.intervalFlow(applicationContext).collect {
                reelInterval = it
            }
        }
    }

    /* =========================================================
       ================= MAIN EVENT HANDLER =====================
       ========================================================= */

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED) return

        val now = System.currentTimeMillis()

        /* -------- Reset session on inactivity -------- */

        if (now - lastAnyScrollTime > SESSION_RESET_MS) {
            sessionStartTime = 0L
            sessionStartScrollCount = 0
            lastAwarenessTriggerAt = 0
            continuousScrollTime = 0L
        }
        lastAnyScrollTime = now

        /* -------- Start session -------- */

        if (sessionStartTime == 0L) {
            sessionStartTime = now
            sessionStartScrollCount = cachedScrollCount
            lastScrollTime = now
            return
        }

        /* -------- Ignore early exploration -------- */

        if (now - sessionStartTime < MIN_SESSION_MS) {
            lastScrollTime = now
            return
        }

        /* =====================================================
           ===== CANONICAL SCROLL COUNT (DEBOUNCED) =============
           ===================================================== */

        var didCountScroll = false

        if (now - lastCountedScrollTime > SCROLL_DEBOUNCE_MS) {
            lastCountedScrollTime = now
            didCountScroll = true

            cachedScrollCount++

            CoroutineScope(Dispatchers.IO).launch {
                ScrollDataStore.incrementScroll(applicationContext)
            }
        }

        /* =====================================================
           ============ REEL AWARENESS (SESSION) =================
           ===================================================== */

        if (didCountScroll) {
            val reelsThisSession = cachedScrollCount - sessionStartScrollCount

            if (
                reelInterval > 0 &&
                reelsThisSession > 0 &&
                reelsThisSession % reelInterval == 0 &&
                reelsThisSession != lastAwarenessTriggerAt
            ) {
                lastAwarenessTriggerAt = reelsThisSession

                NotificationHelper.showReelAwarenessNotification(
                    context = applicationContext,
                    count = reelsThisSession
                )
            }
        }

        /* =====================================================
           ============ MINDLESS SCROLL DETECTION ===============
           ===================================================== */

        if (now - lastScrollTime < PAUSE_THRESHOLD_MS) {
            continuousScrollTime += (now - lastScrollTime)
        } else {
            continuousScrollTime = 0L
        }

        lastScrollTime = now

        if (
            continuousScrollTime >= MIN_CONTINUOUS_SCROLL_MS &&
            now - lastMindlessNotificationTime > NOTIFICATION_COOLDOWN_MS
        ) {
            lastMindlessNotificationTime = now
            continuousScrollTime = 0L
            sessionStartTime = now

            // Prevent overlap with awareness notifications
            lastAwarenessTriggerAt =
                cachedScrollCount - sessionStartScrollCount

            NotificationHelper.showMindfulNotification(applicationContext)
        }
    }

    override fun onInterrupt() {}
}
