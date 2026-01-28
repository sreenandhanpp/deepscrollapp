package com.example.myapplication.service.detector

import android.content.Context
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import com.example.myapplication.utils.NotificationHelper
import kotlin.math.abs
import kotlin.math.log2
import java.util.Calendar

class UnconsciousScrollingDetector(
    private val context: Context
) {

    private var lastDetectedType: UnconsciousType? = null


    /* ---------------- Input analysis ---------------- */

    private val scrollHistory = mutableListOf<ScrollEvent>()
    private val recentGestures = CircularBuffer<GestureType>(100)

    /* ---------------- Engagement metrics ---------------- */

    private var lastInteractionTime = 0L
    private var contentConsumptionTime = 0L
    private var scrollReversals = 0

    /* ---------------- Session analysis ---------------- */

    private var sessionStartTime = 0L
    private var totalSessionTime = 0L
    private var naturalBreakCount = 0

    /* ---------------- Thresholds ---------------- */

    private val VELOCITY_THRESHOLD = 800f
    private val PAUSE_THRESHOLD_MS = 3_000L
    private val INTERACTION_WINDOW_MS = 10_000L
    private val UNCONSCIOUS_THRESHOLD = 0.75f

    /* ---------------- NEW SAFETY GUARDS ---------------- */

    private val WARMUP_TIME_MS = 60_000L
    private val MIN_SCROLL_EVENTS = 8
    private val MIN_UNCONSCIOUS_DURATION_MS = 20_000L


    private var unconsciousStartTime = 0L

    /* ---------------- Notification safety ---------------- */

    private val UNCONSCIOUS_COOLDOWN_MS = 45_000L
    private var lastUnconsciousNotificationTime = 0L
    private var unconsciousTriggeredThisSession = false

    /* ---------------- Shared state ---------------- */

    private var lastEventTime = 0L

    fun updateLastEventTime(time: Long) {
        lastEventTime = time
    }

    /* ==========================================================
       MAIN ENTRY
       ========================================================== */

    fun onAccessibilityEvent(event: AccessibilityEvent, now: Long) {

        val isScrollEvent =
            event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED

        val isInteractionEvent = isMeaningfulInteraction(event)

        /* ---- Session time ---- */
        if (sessionStartTime == 0L) sessionStartTime = now
        totalSessionTime = now - sessionStartTime

        val timeDelta = now - lastEventTime
        if (timeDelta > PAUSE_THRESHOLD_MS) naturalBreakCount++

        /* ---- Interaction ---- */
        if (isInteractionEvent) {
            lastInteractionTime = now
            recentGestures.add(GestureType.INTERACTION)
            contentConsumptionTime += timeDelta

            // Reset unconscious candidate if user interacts
            unconsciousStartTime = 0L
        }

        /* ---- Scroll ---- */
        if (isScrollEvent) {
            val deltaY =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                    event.scrollDeltaY else 0

            val velocity =
                if (timeDelta > 0)
                    abs(deltaY).toFloat() / (timeDelta / 1000f)
                else 0f

            val gestureType = classifyGesture(deltaY, velocity, timeDelta)

            recentGestures.add(gestureType)
            scrollHistory.add(
                ScrollEvent(now, 0, deltaY, velocity, gestureType)
            )
        }

        /* ======================================================
           🛑 EARLY EXIT: Warm-up / Intent phase
           ====================================================== */

        if (
            totalSessionTime < WARMUP_TIME_MS ||
            scrollHistory.size < MIN_SCROLL_EVENTS
        ) return


        /* ---- Detection ---- */
        val result = detectUnconsciousScrolling(now) ?: run {
            unconsciousStartTime = 0L
            return
        }
        if (
            result.type == UnconsciousType.RAPID_SWIPING &&
            totalSessionTime < 2 * WARMUP_TIME_MS
        ) {
            return
        }

        /* ---- Sustained unconscious check ---- */
        if (unconsciousStartTime == 0L) {
            unconsciousStartTime = now
            return
        }

        if (now - unconsciousStartTime < MIN_UNCONSCIOUS_DURATION_MS) return

        /* ---- Cooldown + once-per-session ---- */
        if (
            unconsciousTriggeredThisSession ||
            now - lastUnconsciousNotificationTime < UNCONSCIOUS_COOLDOWN_MS
        ) return



        if (result.type != UnconsciousType.RAPID_SWIPING) {
            unconsciousTriggeredThisSession = true
        }
        lastUnconsciousNotificationTime = now
        lastDetectedType = result.type


        NotificationHelper.showUnconsciousScrollingNotification(
            context,
            result.type
        )
    }

    /* ==========================================================
       RESET (called by service)
       ========================================================== */

    fun resetSession() {
        scrollHistory.clear()
        recentGestures.clear()

        lastInteractionTime = 0L
        contentConsumptionTime = 0L
        scrollReversals = 0

        sessionStartTime = 0L
        totalSessionTime = 0L
        naturalBreakCount = 0

        lastEventTime = 0L
        unconsciousStartTime = 0L
        unconsciousTriggeredThisSession = false
        lastDetectedType = null   // ✅ ADD THIS

    }

    /* ==========================================================
       Detection logic (UNCHANGED)
       ========================================================== */

    private fun classifyGesture(
        deltaY: Int,
        velocity: Float,
        timeDelta: Long
    ): GestureType =
        when {
            velocity > VELOCITY_THRESHOLD && timeDelta < 500L ->
                GestureType.QUICK_SWIPE
            abs(deltaY) > 100 && timeDelta > 1000L ->
                GestureType.SLOW_SCROLL
            timeDelta > PAUSE_THRESHOLD_MS ->
                GestureType.PAUSE
            else ->
                GestureType.SLOW_SCROLL
        }

    private fun detectUnconsciousScrolling(now: Long): UnconsciousScore? {
        val score =
            analyzeInputPatterns(now) * 0.35f +
                    analyzeEngagementAbsence(now) * 0.30f +
                    analyzeSessionBehavior(now) * 0.20f +
                    analyzeContextualFactors(now) * 0.15f

        return if (score > UNCONSCIOUS_THRESHOLD)
            UnconsciousScore(score, detectUnconsciousType(now))
        else null
    }

    private fun analyzeInputPatterns(now: Long): Float {
        if (scrollHistory.size < 5) return 0f

        val avgVelocity =
            scrollHistory.takeLast(10).map { it.velocity }.average().toFloat()

        val velocityScore = (avgVelocity / 1200f).coerceIn(0f, 1f)

        val windowStart = now - 10_000L
        val eventsPerSecond =
            scrollHistory.count { it.timestamp >= windowStart } / 10f

        val frequencyScore = (eventsPerSecond / 3f).coerceIn(0f, 1f)
        val entropy = calculateGestureEntropy()

        return velocityScore * 0.4f +
                frequencyScore * 0.3f +
                (1f - entropy) * 0.3f
    }

    private fun calculateGestureEntropy(): Float {
        val counts = recentGestures.groupBy { it }
        val total = counts.values.sumOf { it.size }.toDouble()
        if (total == 0.0) return 1f

        var entropy = 0.0
        counts.values.forEach {
            val p = it.size / total
            entropy -= p * log2(p)
        }
        return entropy.toFloat()
    }

    private fun analyzeEngagementAbsence(now: Long): Float {
        val timeSinceInteraction = now - lastInteractionTime
        val interactionScore =
            (timeSinceInteraction.toFloat() / INTERACTION_WINDOW_MS).coerceIn(0f, 1f)

        val consumptionRatio =
            contentConsumptionTime.toFloat() / totalSessionTime.coerceAtLeast(1)

        val consumptionScore = 1f - consumptionRatio.coerceIn(0f, 1f)
        val breakScore = 1f - (naturalBreakCount / 5f).coerceIn(0f, 1f)

        return interactionScore * 0.4f +
                consumptionScore * 0.3f +
                breakScore * 0.3f
    }

    private fun analyzeSessionBehavior(now: Long): Float {
        val duration = now - sessionStartTime
        if (duration <= 0) return 0f

        val breaksPerMinute =
            naturalBreakCount / (duration / 60000f).coerceAtLeast(1f)

        return if (breaksPerMinute < 0.5f) 1f else 0f
    }

    private fun analyzeContextualFactors(now: Long): Float {
        val hour = Calendar.getInstance().apply {
            timeInMillis = now
        }.get(Calendar.HOUR_OF_DAY)

        return when {
            hour in 22..23 || hour in 0..6 -> 1f
            hour in 18..21 -> 0.7f
            else -> 0.3f
        }
    }

    private fun detectUnconsciousType(now: Long): UnconsciousType =
        when {
            // 🧠 Deep, sustained scrolling first
            totalSessionTime > 60_000L && naturalBreakCount == 0 ->
                UnconsciousType.DEEP_DIVE

            // 💤 Zone-out (no interaction)
            now - lastInteractionTime > 30_000L ->
                UnconsciousType.ZONE_OUT

            // 🤖 Pattern entropy collapse
            calculateGestureEntropy() < 0.2f ->
                UnconsciousType.ROBOTIC

            // ⚡ Rapid swipe LAST (least severe)
            recentGestures.count { it == GestureType.QUICK_SWIPE } >= 3 ->
                UnconsciousType.RAPID_SWIPING

            else ->
                UnconsciousType.MINDLESS_BROWSING
        }


    private fun isMeaningfulInteraction(event: AccessibilityEvent): Boolean =
        event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED ||
                event.eventType == AccessibilityEvent.TYPE_VIEW_LONG_CLICKED ||
                event.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED ||
                event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED

    fun hasDetectedUnconsciousScrolling(): Boolean {
        // ❌ Exclude rapid swiping from deep scroll definition
        return unconsciousTriggeredThisSession &&
                lastDetectedType != UnconsciousType.RAPID_SWIPING
    }

}
