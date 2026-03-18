package com.example.myapplication.service.detector

import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.myapplication.utils.NotificationHelper

class UnconsciousScrollingDetector(
    private val context: Context
) {

    /* ---------------- State ---------------- */

    private var sessionStartTime = 0L
    private var lastInteractionTime = 0L

    private var scrollCount = 0
    private var breakCount = 0

    /* ---------------- Thresholds ---------------- */

    private val MIN_SESSION_TIME = 15_000L       // 15 sec
    private val NO_INTERACTION_TIME = 10_000L    // 10 sec
    private val BREAK_THRESHOLD = 2_000L         // 2 sec

    /* ---------------- Notification ---------------- */

    private val COOLDOWN = 45_000L
    private var lastNotificationTime = 0L
    private var triggered = false

    /* ================================================= */

    fun onAccessibilityEvent(
        event: AccessibilityEvent,
        now: Long,
        prevTime: Long
    ) {

        val isScroll =
            event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED

        val isInteraction =
            event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED ||
                    event.eventType == AccessibilityEvent.TYPE_VIEW_LONG_CLICKED ||
                    event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED

        /* ---------------- Session start ---------------- */

        if (sessionStartTime == 0L) {
            sessionStartTime = now
            lastInteractionTime = now
        }

        val timeDelta = now - prevTime

        /* ---------------- Break detection ---------------- */

        if (timeDelta > BREAK_THRESHOLD) {
            breakCount++
        }

        /* ---------------- Interaction ---------------- */

        if (isInteraction) {
            lastInteractionTime = now
            triggered = false
        }

        /* ---------------- Scroll ---------------- */

        if (isScroll) {
            scrollCount++
        }

        /* =================================================
           🔥 UNCONSCIOUS DETECTION
           ================================================= */

        val sessionDuration = now - sessionStartTime
        val timeSinceInteraction = now - lastInteractionTime

        val isLongSession = sessionDuration > MIN_SESSION_TIME
        val noInteraction = timeSinceInteraction > NO_INTERACTION_TIME
        val lowBreaks = breakCount <= 1

        Log.d("UnconsciousDebug", """
Session: $sessionDuration
NoInteraction: $timeSinceInteraction
Breaks: $breakCount
Scrolls: $scrollCount
""".trimIndent())

        if (
            isLongSession &&
            noInteraction &&
            lowBreaks &&
            !triggered &&
            now - lastNotificationTime > COOLDOWN
        ) {

            triggered = true
            lastNotificationTime = now

            Log.d("UnconsciousDebug", "🔥 UNCONSCIOUS TRIGGERED")

            NotificationHelper.showUnconsciousScrollingNotification(
                context,
                UnconsciousType.DEEP_DIVE
            )
        }
    }

    /* ================================================= */

    fun resetSession() {
        sessionStartTime = 0L
        lastInteractionTime = 0L
        scrollCount = 0
        breakCount = 0
        triggered = false
    }

    fun hasDetectedUnconsciousScrolling(): Boolean {
        return triggered
    }
}