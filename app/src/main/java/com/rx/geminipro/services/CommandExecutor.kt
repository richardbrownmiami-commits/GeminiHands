package com.rx.geminipro.services

import android.content.Context
import android.util.Log

class CommandExecutor(private val context: Context) {

    companion object {
        private const val TAG = "CommandExecutor"
    }

    private val deviceController = DeviceController(context)
    private val chatParser = ChatCommandParser()

    fun processGeminiResponse(response: String) {
        val commands = chatParser.parseResponse(response)
        if (commands.isEmpty()) {
            Log.d(TAG, "No actionable commands found in response")
            return
        }

        for (command in commands) {
            executeCommand(command)
        }
    }

    private fun executeCommand(command: ParsedCommand) {
        Log.d(TAG, "Executing: ${command.action} -> ${command.target}")

        try {
            when (command.action) {
                "open_app" -> {
                    val packageName = chatParser.getPackageForApp(command.target)
                    if (packageName != null) {
                        deviceController.openApp(packageName)
                        logSuccess(command, "Opened ${command.target}")
                    } else {
                        // Try accessibility service to open via search
                        GeminiAccessibilityService.instance?.openApp(command.target)
                        logSuccess(command, "Attempted to open ${command.target}")
                    }
                }

                "set_alarm" -> {
                    val time = parseTime(command.target)
                    if (time != null) {
                        deviceController.setAlarm(time.first, time.second, "Gemini Reminder")
                        logSuccess(command, "Alarm set for ${time.first}:${time.second}")
                    }
                }

                "set_timer" -> {
                    val seconds = parseTimerDuration(command.target)
                    if (seconds > 0) {
                        deviceController.setTimer(seconds, "Gemini Timer")
                        logSuccess(command, "Timer set for $seconds seconds")
                    }
                }

                "make_call" -> {
                    val number = findContactNumber(command.target)
                    if (number != null) {
                        deviceController.makeCall(number)
                        logSuccess(command, "Calling ${command.target}")
                    }
                }

                "send_sms" -> {
                    val number = findContactNumber(command.target)
                    val message = command.parameters["message"] ?: ""
                    if (number != null) {
                        deviceController.sendSms(number, message)
                        logSuccess(command, "SMS sent to ${command.target}")
                    }
                }

                "send_whatsapp" -> {
                    val number = findContactNumber(command.target)
                    val message = command.parameters["message"] ?: ""
                    if (number != null) {
                        deviceController.sendWhatsApp(number, message)
                        logSuccess(command, "WhatsApp sent to ${command.target}")
                    }
                }

                "toggle_wifi" -> {
                    val enable = command.target.lowercase().let {
                        it.contains("on") || it.contains("enabl")
                    }
                    deviceController.toggleWifi(enable)
                    logSuccess(command, "WiFi ${if (enable) "enabled" else "disabled"}")
                }

                "toggle_bluetooth" -> {
                    val enable = command.target.lowercase().let {
                        it.contains("on") || it.contains("enabl")
                    }
                    deviceController.toggleBluetooth(enable)
                    logSuccess(command, "Bluetooth ${if (enable) "enabled" else "disabled"}")
                }

                "toggle_flashlight" -> {
                    val enable = command.target.lowercase().let {
                        it.contains("on") || it.contains("enabl")
                    }
                    deviceController.toggleFlashlight(enable)
                    logSuccess(command, "Flashlight ${if (enable) "on" else "off"}")
                }

                "toggle_airplane" -> {
                    deviceController.openAirplaneModeSettings()
                    logSuccess(command, "Opened airplane mode settings")
                }

                "toggle_dnd" -> {
                    val enable = command.target.lowercase().let {
                        it.contains("on") || it.contains("enabl")
                    }
                    deviceController.toggleDoNotDisturb(enable)
                    logSuccess(command, "DND ${if (enable) "enabled" else "disabled"}")
                }

                "set_volume" -> {
                    val level = command.target.filter { it.isDigit() }.toIntOrNull()
                    if (level != null) {
                        deviceController.setVolume(level)
                        logSuccess(command, "Volume set to $level%")
                    } else if (command.target.contains("up")) {
                        deviceController.volumeUp()
                        logSuccess(command, "Volume up")
                    } else if (command.target.contains("down")) {
                        deviceController.volumeDown()
                        logSuccess(command, "Volume down")
                    }
                }

                "set_brightness" -> {
                    val level = command.target.filter { it.isDigit() }.toIntOrNull()
                    if (level != null) {
                        deviceController.setBrightness(level)
                        logSuccess(command, "Brightness set to $level%")
                    }
                }

                "take_photo" -> {
                    deviceController.openCamera()
                    logSuccess(command, "Camera opened")
                }

                "play_music" -> {
                    deviceController.playPauseMedia()
                    logSuccess(command, "Media play/pause")
                }

                "pause_music" -> {
                    deviceController.playPauseMedia()
                    logSuccess(command, "Media paused")
                }

                "press_back" -> {
                    GeminiAccessibilityService.instance?.pressBack()
                    logSuccess(command, "Pressed back")
                }

                "press_home" -> {
                    GeminiAccessibilityService.instance?.pressHome()
                    logSuccess(command, "Pressed home")
                }

                "tap" -> {
                    GeminiAccessibilityService.instance?.clickByText(command.target)
                    logSuccess(command, "Tapped on ${command.target}")
                }

                "type_text" -> {
                    GeminiAccessibilityService.instance?.typeText(command.target)
                    logSuccess(command, "Typed: ${command.target}")
                }

                "scroll_down" -> {
                    GeminiAccessibilityService.instance?.scrollDown()
                    logSuccess(command, "Scrolled down")
                }

                "scroll_up" -> {
                    GeminiAccessibilityService.instance?.scrollUp()
                    logSuccess(command, "Scrolled up")
                }

                "screenshot" -> {
                    GeminiAccessibilityService.instance?.takeScreenshot()
                    logSuccess(command, "Screenshot taken")
                }

                "lock_device" -> {
                    GeminiAccessibilityService.instance?.lockScreen()
                    logSuccess(command, "Device locked")
                }

                "read_notifications" -> {
                    val summary = GeminiNotificationListener.instance?.getNotificationSummary()
                        ?: "Notification listener not active"
                    logSuccess(command, summary)
                }

                "set_reminder" -> {
                    // Use alarm as reminder
                    val time = parseTime(command.target)
                    if (time != null) {
                        deviceController.setAlarm(time.first, time.second, command.target)
                        logSuccess(command, "Reminder set: ${command.target}")
                    }
                }

                else -> {
                    Log.w(TAG, "Unknown command: ${command.action}")
                    ActionLogger.getInstance()?.logAction(
                        command.action, command.target, "unknown_command"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing ${command.action}: ${e.message}")
            ActionLogger.getInstance()?.logAction(
                command.action, command.target, "error: ${e.message}"
            )
        }
    }

    private fun logSuccess(command: ParsedCommand, result: String) {
        ActionLogger.getInstance()?.logAction(command.action, command.target, result)
    }

    private fun parseTime(timeStr: String): Pair<Int, Int>? {
        // Parse patterns like "5 PM", "5:30 PM", "17:30", "5 o'clock"
        val patterns = listOf(
            Regex("(\\d{1,2}):(\\d{2})\\s*(am|pm)?", RegexOption.IGNORE_CASE),
            Regex("(\\d{1,2})\\s*(am|pm)", RegexOption.IGNORE_CASE),
            Regex("(\\d{1,2})\\s*o'?clock", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val match = pattern.find(timeStr)
            if (match != null) {
                var hour = match.groupValues[1].toIntOrNull() ?: continue
                val minute = if (match.groupValues.size > 2 && match.groupValues[2].all { it.isDigit() })
                    match.groupValues[2].toIntOrNull() ?: 0 else 0
                val amPm = match.groupValues.lastOrNull { it.lowercase() in listOf("am", "pm") }

                if (amPm?.lowercase() == "pm" && hour < 12) hour += 12
                if (amPm?.lowercase() == "am" && hour == 12) hour = 0

                return Pair(hour, minute)
            }
        }
        return null
    }

    private fun parseTimerDuration(durationStr: String): Int {
        var totalSeconds = 0
        val hourMatch = Regex("(\\d+)\\s*hour").find(durationStr)
        val minMatch = Regex("(\\d+)\\s*min").find(durationStr)
        val secMatch = Regex("(\\d+)\\s*sec").find(durationStr)

        hourMatch?.let { totalSeconds += (it.groupValues[1].toIntOrNull() ?: 0) * 3600 }
        minMatch?.let { totalSeconds += (it.groupValues[1].toIntOrNull() ?: 0) * 60 }
        secMatch?.let { totalSeconds += (it.groupValues[1].toIntOrNull() ?: 0) }

        // If just a number, assume minutes
        if (totalSeconds == 0) {
            val justNumber = durationStr.filter { it.isDigit() }.toIntOrNull()
            if (justNumber != null) totalSeconds = justNumber * 60
        }

        return totalSeconds
    }

    private fun findContactNumber(nameOrNumber: String): String? {
        // If it's already a phone number
        if (nameOrNumber.any { it.isDigit() } && nameOrNumber.count { it.isDigit() } >= 7) {
            return nameOrNumber.filter { it.isDigit() || it == '+' }
        }
        // Try to find in contacts
        val contacts = deviceController.getContacts()
        val match = contacts.find {
            it["name"]?.lowercase()?.contains(nameOrNumber.lowercase()) == true
        }
        return match?.get("number")
    }
}
