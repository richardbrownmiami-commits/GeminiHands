package com.rx.geminipro.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.*

class GeminiAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "GeminiAccessibility"
        var instance: GeminiAccessibilityService? = null
            private set
        var isRunning = false
            private set
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        isRunning = true
        Log.d(TAG, "Accessibility Service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            when (it.eventType) {
                AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                    val text = it.text?.joinToString(" ") ?: ""
                    val packageName = it.packageName?.toString() ?: ""
                    ActionLogger.getInstance()?.logAction(
                        "notification_received",
                        "From: $packageName - $text",
                        "logged"
                    )
                }
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    Log.d(TAG, "Window changed: ${it.packageName}")
                }
                else -> {}
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        isRunning = false
        serviceScope.cancel()
    }

    // Tap at specific coordinates
    fun tapAt(x: Float, y: Float) {
        val path = Path()
        path.moveTo(x, y)
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        dispatchGesture(gesture, null, null)
        Log.d(TAG, "Tapped at ($x, $y)")
    }

    // Long press at coordinates
    fun longPressAt(x: Float, y: Float) {
        val path = Path()
        path.moveTo(x, y)
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 1000))
            .build()
        dispatchGesture(gesture, null, null)
        Log.d(TAG, "Long pressed at ($x, $y)")
    }

    // Swipe from one point to another
    fun swipe(startX: Float, startY: Float, endX: Float, endY: Float, duration: Long = 300) {
        val path = Path()
        path.moveTo(startX, startY)
        path.lineTo(endX, endY)
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()
        dispatchGesture(gesture, null, null)
        Log.d(TAG, "Swiped from ($startX, $startY) to ($endX, $endY)")
    }

    // Scroll up
    fun scrollUp() {
        val displayMetrics = resources.displayMetrics
        val centerX = displayMetrics.widthPixels / 2f
        val startY = displayMetrics.heightPixels * 0.7f
        val endY = displayMetrics.heightPixels * 0.3f
        swipe(centerX, startY, centerX, endY, 500)
    }

    // Scroll down
    fun scrollDown() {
        val displayMetrics = resources.displayMetrics
        val centerX = displayMetrics.widthPixels / 2f
        val startY = displayMetrics.heightPixels * 0.3f
        val endY = displayMetrics.heightPixels * 0.7f
        swipe(centerX, startY, centerX, endY, 500)
    }

    // Press Back button
    fun pressBack() {
        performGlobalAction(GLOBAL_ACTION_BACK)
        Log.d(TAG, "Pressed Back")
    }

    // Press Home button
    fun pressHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
        Log.d(TAG, "Pressed Home")
    }

    // Press Recent Apps
    fun pressRecents() {
        performGlobalAction(GLOBAL_ACTION_RECENTS)
        Log.d(TAG, "Pressed Recents")
    }

    // Open notifications panel
    fun openNotifications() {
        performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    }

    // Open quick settings
    fun openQuickSettings() {
        performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
    }

    // Take screenshot
    fun takeScreenshot() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
            Log.d(TAG, "Screenshot taken")
        }
    }

    // Lock screen
    fun lockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            Log.d(TAG, "Screen locked")
        }
    }

    // Power dialog
    fun openPowerDialog() {
        performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
    }

    // Type text into focused field
    fun typeText(text: String) {
        val rootNode = rootInActiveWindow ?: return
        val focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        focusedNode?.let {
            val arguments = Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            it.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            Log.d(TAG, "Typed text: $text")
        }
    }

    // Find and click element by text
    fun clickByText(text: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val nodes = rootNode.findAccessibilityNodeInfosByText(text)
        if (nodes.isNotEmpty()) {
            for (node in nodes) {
                if (node.isClickable) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    Log.d(TAG, "Clicked element with text: $text")
                    return true
                }
                // Try clicking parent if node isn't clickable
                var parent = node.parent
                while (parent != null) {
                    if (parent.isClickable) {
                        parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        Log.d(TAG, "Clicked parent of element with text: $text")
                        return true
                    }
                    parent = parent.parent
                }
            }
        }
        return false
    }

    // Find and click element by view ID
    fun clickById(viewId: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val nodes = rootNode.findAccessibilityNodeInfosByViewId(viewId)
        if (nodes.isNotEmpty()) {
            nodes[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Log.d(TAG, "Clicked element with ID: $viewId")
            return true
        }
        return false
    }

    // Read all text on screen
    fun readScreen(): String {
        val rootNode = rootInActiveWindow ?: return ""
        val textBuilder = StringBuilder()
        readNodeText(rootNode, textBuilder)
        return textBuilder.toString()
    }

    private fun readNodeText(node: AccessibilityNodeInfo, builder: StringBuilder) {
        node.text?.let {
            builder.append(it).append("\n")
        }
        node.contentDescription?.let {
            builder.append("[").append(it).append("]\n")
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            readNodeText(child, builder)
        }
    }

    // Get all clickable elements on screen
    fun getClickableElements(): List<Map<String, Any>> {
        val rootNode = rootInActiveWindow ?: return emptyList()
        val elements = mutableListOf<Map<String, Any>>()
        findClickableNodes(rootNode, elements)
        return elements
    }

    private fun findClickableNodes(node: AccessibilityNodeInfo, elements: MutableList<Map<String, Any>>) {
        if (node.isClickable) {
            val rect = Rect()
            node.getBoundsInScreen(rect)
            elements.add(mapOf(
                "text" to (node.text?.toString() ?: ""),
                "description" to (node.contentDescription?.toString() ?: ""),
                "id" to (node.viewIdResourceName ?: ""),
                "bounds" to "${rect.left},${rect.top},${rect.right},${rect.bottom}",
                "className" to (node.className?.toString() ?: "")
            ))
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findClickableNodes(child, elements)
        }
    }

    // Open an app by package name
    fun openApp(packageName: String) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                Log.d(TAG, "Opened app: $packageName")
            } else {
                Log.e(TAG, "App not found: $packageName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening app: ${e.message}")
        }
    }

    // Paste text from clipboard to focused field
    fun pasteText() {
        val rootNode = rootInActiveWindow ?: return
        val focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        focusedNode?.performAction(AccessibilityNodeInfo.ACTION_PASTE)
    }

    // Select all text in focused field
    fun selectAll() {
        val rootNode = rootInActiveWindow ?: return
        val focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        focusedNode?.performAction(AccessibilityNodeInfo.ACTION_SELECT_ALL)
    }

    // Copy selected text
    fun copyText() {
        val rootNode = rootInActiveWindow ?: return
        val focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        focusedNode?.performAction(AccessibilityNodeInfo.ACTION_COPY)
    }
}
