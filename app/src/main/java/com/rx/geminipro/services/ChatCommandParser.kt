package com.rx.geminipro.services

import android.util.Log

data class ParsedCommand(
    val action: String,
    val target: String = "",
    val parameters: Map<String, String> = emptyMap(),
    val rawText: String = ""
)

class ChatCommandParser {

    companion object {
        private const val TAG = "ChatCommandParser"

        // Action patterns that Gemini might use in responses
        private val ACTION_PATTERNS = mapOf(
            // App control
            "open_app" to listOf(
                Regex("(?i)opening\\s+(.+?)(?:\\s+for you|\\s+now|\\.|\$)"),
                Regex("(?i)i'll open\\s+(.+?)(?:\\s+for you|\\s+now|\\.|\$)"),
                Regex("(?i)launching\\s+(.+?)(?:\\s+for you|\\s+now|\\.|\$)")
            ),
            // Alarm/Timer
            "set_alarm" to listOf(
                Regex("(?i)setting (?:an )?alarm (?:for |at )(.+?)(?:\\.|\$)"),
                Regex("(?i)i'll set (?:an )?alarm (?:for |at )(.+?)(?:\\.|\$)"),
                Regex("(?i)alarm set (?:for |at )(.+?)(?:\\.|\$)")
            ),
            "set_timer" to listOf(
                Regex("(?i)setting (?:a )?timer (?:for )(.+?)(?:\\.|\$)"),
                Regex("(?i)i'll set (?:a )?timer (?:for )(.+?)(?:\\.|\$)")
            ),
            // Communication
            "make_call" to listOf(
                Regex("(?i)calling\\s+(.+?)(?:\\s+now|\\.|\$)"),
                Regex("(?i)i'll call\\s+(.+?)(?:\\s+now|\\.|\$)"),
                Regex("(?i)making a call to\\s+(.+?)(?:\\.|\$)")
            ),
            "send_sms" to listOf(
                Regex("(?i)sending (?:a )?(?:text|message|sms) to\\s+(.+?)(?:\\s+saying|\\.|\$)"),
                Regex("(?i)i'll (?:text|message|send)\\s+(.+?)(?:\\.|\$)")
            ),
            // Settings
            "toggle_wifi" to listOf(
                Regex("(?i)turning (on|off) (?:the )?wi-?fi"),
                Regex("(?i)(enabling|disabling) (?:the )?wi-?fi")
            ),
            "toggle_bluetooth" to listOf(
                Regex("(?i)turning (on|off) (?:the )?bluetooth"),
                Regex("(?i)(enabling|disabling) (?:the )?bluetooth")
            ),
            "toggle_flashlight" to listOf(
                Regex("(?i)turning (on|off) (?:the )?(?:flashlight|torch)"),
                Regex("(?i)(enabling|disabling) (?:the )?(?:flashlight|torch)")
            ),
            "toggle_airplane" to listOf(
                Regex("(?i)turning (on|off) (?:the )?airplane mode"),
                Regex("(?i)(enabling|disabling) (?:the )?airplane mode")
            ),
            "toggle_dnd" to listOf(
                Regex("(?i)turning (on|off) (?:the )?do not disturb"),
                Regex("(?i)(enabling|disabling) (?:the )?(?:do not disturb|dnd)")
            ),
            // Volume/Brightness
            "set_volume" to listOf(
                Regex("(?i)setting (?:the )?volume to\\s+(\\d+)"),
                Regex("(?i)(?:turning|setting) (?:the )?volume (up|down)"),
                Regex("(?i)(?:increasing|decreasing) (?:the )?volume")
            ),
            "set_brightness" to listOf(
                Regex("(?i)setting (?:the )?brightness to\\s+(\\d+)"),
                Regex("(?i)(?:turning|setting) (?:the )?brightness (up|down)")
            ),
            // Media
            "play_music" to listOf(
                Regex("(?i)playing\\s+(.+?)(?:\\.|\$)"),
                Regex("(?i)i'll play\\s+(.+?)(?:\\.|\$)")
            ),
            "pause_music" to listOf(
                Regex("(?i)pausing (?:the )?music"),
                Regex("(?i)i'll pause")
            ),
            // Camera
            "take_photo" to listOf(
                Regex("(?i)taking (?:a )?photo"),
                Regex("(?i)opening (?:the )?camera"),
                Regex("(?i)i'll take (?:a )?(?:photo|picture)")
            ),
            // Navigation
            "press_back" to listOf(
                Regex("(?i)(?:pressing|going) back"),
                Regex("(?i)i'll go back")
            ),
            "press_home" to listOf(
                Regex("(?i)going (?:to )?home"),
                Regex("(?i)pressing home")
            ),
            // Tap/Click
            "tap" to listOf(
                Regex("(?i)tapping (?:on )?(.+?)(?:\\.|\$)"),
                Regex("(?i)clicking (?:on )?(.+?)(?:\\.|\$)"),
                Regex("(?i)i'll tap (?:on )?(.+?)(?:\\.|\$)")
            ),
            // Type
            "type_text" to listOf(
                Regex("(?i)typing\\s+[\"'](.+?)[\"']"),
                Regex("(?i)i'll type\\s+[\"'](.+?)[\"']"),
                Regex("(?i)entering\\s+[\"'](.+?)[\"']")
            ),
            // Scroll
            "scroll_down" to listOf(
                Regex("(?i)scrolling down"),
                Regex("(?i)i'll scroll down")
            ),
            "scroll_up" to listOf(
                Regex("(?i)scrolling up"),
                Regex("(?i)i'll scroll up")
            ),
            // Screenshot
            "screenshot" to listOf(
                Regex("(?i)taking (?:a )?screenshot"),
                Regex("(?i)i'll (?:take (?:a )?)?screenshot")
            ),
            // Reminder
            "set_reminder" to listOf(
                Regex("(?i)setting (?:a )?reminder (?:for |to |about )(.+?)(?:\\.|\$)"),
                Regex("(?i)i'll remind you (?:to |about )(.+?)(?:\\.|\$)"),
                Regex("(?i)reminder set (?:for |to )(.+?)(?:\\.|\$)")
            ),
            // Send WhatsApp
            "send_whatsapp" to listOf(
                Regex("(?i)sending (?:a )?whatsapp (?:message )?to\\s+(.+?)(?:\\s+saying|\\.|\$)"),
                Regex("(?i)i'll whatsapp\\s+(.+?)(?:\\.|\$)")
            ),
            // Lock device
            "lock_device" to listOf(
                Regex("(?i)locking (?:the )?(?:device|phone|screen)"),
                Regex("(?i)i'll lock (?:the )?(?:device|phone|screen)")
            ),
            // Read notifications
            "read_notifications" to listOf(
                Regex("(?i)reading (?:your )?notifications"),
                Regex("(?i)checking (?:your )?notifications"),
                Regex("(?i)i'll check (?:your )?notifications")
            )
        )

        // Common app name to package name mapping
        val APP_PACKAGES = mapOf(
            "whatsapp" to "com.whatsapp",
            "instagram" to "com.instagram.android",
            "youtube" to "com.google.android.youtube",
            "chrome" to "com.android.chrome",
            "gmail" to "com.google.android.gm",
            "maps" to "com.google.android.apps.maps",
            "google maps" to "com.google.android.apps.maps",
            "camera" to "com.android.camera",
            "phone" to "com.android.dialer",
            "messages" to "com.google.android.apps.messaging",
            "settings" to "com.android.settings",
            "calendar" to "com.google.android.calendar",
            "clock" to "com.google.android.deskclock",
            "calculator" to "com.google.android.calculator",
            "files" to "com.google.android.apps.nbu.files",
            "spotify" to "com.spotify.music",
            "twitter" to "com.twitter.android",
            "x" to "com.twitter.android",
            "facebook" to "com.facebook.katana",
            "telegram" to "org.telegram.messenger",
            "netflix" to "com.netflix.mediaclient",
            "amazon" to "com.amazon.mShop.android.shopping",
            "play store" to "com.android.vending",
            "photos" to "com.google.android.apps.photos",
            "google photos" to "com.google.android.apps.photos",
            "contacts" to "com.google.android.contacts",
            "notes" to "com.google.android.keep",
            "keep" to "com.google.android.keep"
        )
    }

    fun parseResponse(geminiResponse: String): List<ParsedCommand> {
        val commands = mutableListOf<ParsedCommand>()

        for ((action, patterns) in ACTION_PATTERNS) {
            for (pattern in patterns) {
                val match = pattern.find(geminiResponse)
                if (match != null) {
                    val target = if (match.groupValues.size > 1) match.groupValues[1].trim() else ""
                    commands.add(ParsedCommand(
                        action = action,
                        target = target,
                        rawText = geminiResponse
                    ))
                    Log.d(TAG, "Parsed command: $action, target: $target")
                    break // Only match first pattern per action
                }
            }
        }

        return commands
    }

    fun getPackageForApp(appName: String): String? {
        val normalized = appName.lowercase().trim()
        return APP_PACKAGES[normalized]
            ?: APP_PACKAGES.entries.find { normalized.contains(it.key) }?.value
    }
}
