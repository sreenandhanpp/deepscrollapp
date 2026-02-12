package com.example.myapplication.service.detector

import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.os.Build

object ReelsDebugLogger {

    private const val TAG = "Unscroll-Reels"
    private const val INSTAGRAM_PKG = "com.instagram.android"
    private const val VIEWPAGER_CLASS = "androidx.viewpager.widget.ViewPager"

    fun log(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED) return

        val className = event.className?.toString() ?: "null"
        val pkg = event.packageName?.toString() ?: "null"

        if (pkg != INSTAGRAM_PKG || className != VIEWPAGER_CLASS) return  // Filter to Instagram ViewPager scrolls

        val deltaY = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) event.scrollDeltaY else 0
        val maxY = event.maxScrollY

        Log.d(TAG, """
            📦 Package: $pkg
            🏷 Class: $className
            🔄 deltaY: $deltaY
            📏 maxScrollY: $maxY
        """.trimIndent())

        // Dive deeper: Get source node and dump hierarchy
        event.source?.let { rootNode ->
            Log.d(TAG, "Starting hierarchy dump for potential Reels...")
            dumpNodeHierarchy(rootNode, 0, 5)  // Start recursion with depth 0, max depth 5
        } ?: Log.d(TAG, "No source node available")
    }

    private fun dumpNodeHierarchy(node: AccessibilityNodeInfo, depth: Int, maxDepth: Int) {
        if (depth > maxDepth) return

        val indent = "  ".repeat(depth)
        val className = node.className?.toString() ?: "null"
        val resId = node.viewIdResourceName?.toString() ?: "null"
        val text = node.text?.toString() ?: "null"
        val desc = node.contentDescription?.toString() ?: "null"
        val isClickable = node.isClickable
        val childCount = node.childCount

        Log.d(TAG, """
            $indent🔍 Node Depth: $depth
            $indent🏷 Class: $className
            $indent🆔 ID: $resId
            $indent📝 Text: $text
            $indent📄 Desc: $desc
            $indent👆 Clickable: $isClickable
            $indent👶 Children: $childCount
        """.trimIndent())

        // Recurse into children
        for (i in 0 until childCount) {
            node.getChild(i)?.let { child ->
                dumpNodeHierarchy(child, depth + 1, maxDepth)
            }
        }

        // Clean up to avoid memory leaks
        node.recycle()
    }
}