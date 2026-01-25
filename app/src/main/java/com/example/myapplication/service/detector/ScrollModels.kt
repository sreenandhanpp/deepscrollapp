package com.example.myapplication.service.detector

data class ScrollEvent(
    val timestamp: Long,
    val deltaX: Int,
    val deltaY: Int,
    val velocity: Float,
    val gestureType: GestureType
)

enum class GestureType {
    QUICK_SWIPE,
    SLOW_SCROLL,
    PAUSE,
    INTERACTION
}

enum class UnconsciousType {
    RAPID_SWIPING, ZONE_OUT, ROBOTIC, DEEP_DIVE, MINDLESS_BROWSING
}

data class UnconsciousScore(
    val score: Float,
    val type: UnconsciousType
)

enum class ScrollContext {
    REELS,
    COMMENTS,
    CHAT,
    FEED,
    UNKNOWN
}
