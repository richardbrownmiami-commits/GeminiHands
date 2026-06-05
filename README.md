# Gemini Hands

**Give Gemini AI hands to control your Android device.**

Gemini Hands is a native Android app that wraps Google Gemini in a WebView and uses Android's Accessibility Service, WorkManager, and Device Admin to let Gemini execute real actions on your phone — no API key needed.

## How It Works

1. **You chat with Gemini** through the built-in browser (loads gemini.google.com directly)
2. **The app monitors** Gemini's responses in real-time via JavaScript injection
3. **When Gemini says** something actionable (like "Opening WhatsApp for you"), the app **actually does it**
4. **Scheduled wake-ups** can auto-send messages to Gemini and activate voice mode

## Features

### Embedded Gemini Browser
- Opens Google Gemini directly — no API key required
- Login with your Google account
- Full browser functionality with download support

### Full Device Control via Accessibility Service
- Tap, swipe, scroll anywhere on screen
- Type text into any app
- Read screen content
- Press Back, Home, Recent Apps
- Open/close any app by name
- Find and click UI elements by text

### System Controls
- Toggle WiFi, Bluetooth, Flashlight, Airplane Mode
- Adjust volume and brightness
- Set alarms and timers
- Make phone calls and send SMS
- Send WhatsApp messages
- Take photos/videos
- Play/pause music
- Read contacts and calendar
- Check battery level
- Get GPS location
- Read/set clipboard
- Change wallpaper
- Toggle Do Not Disturb
- Lock device

### Notification Management
- Reads all incoming notifications
- Feeds notification summaries to Gemini
- Can dismiss notifications

### WorkManager (Background Tasks)
- Scheduled Gemini wake-ups (survives app kill and reboot)
- Periodic notification checks
- Queued action execution
- Battery-friendly background processing

### Device Admin
- Lock/unlock device
- Screen timeout control
- Security policy enforcement

### Action History
- Logs every action Gemini triggers
- Shows what was said and what was executed
- Timestamps for all activities

### Foreground Service
- Keeps app alive in background
- Persistent notification for quick access
- Survives battery optimization

## Setup Instructions

### 1. Install the APK
Download the APK from GitHub Actions artifacts or build it yourself.

### 2. Enable Permissions
The app will guide you through enabling:

1. **Accessibility Service**: Settings > Accessibility > Gemini Hands > Enable
2. **Notification Access**: Settings > Notifications > Notification Access > Gemini Hands
3. **Device Admin**: Follow the in-app prompt
4. **Do Not Disturb Access**: Settings > Sound > Do Not Disturb Access
5. **Display Over Other Apps**: Settings > Apps > Special Access > Display Over Other Apps
6. **Modify System Settings**: Settings > Apps > Special Access > Modify System Settings

### 3. Grant Runtime Permissions
The app will request permissions for Phone, SMS, Contacts, Calendar, Camera, Location, and Storage.

### 4. Disable Battery Optimization
Settings > Battery > Battery Optimization > Gemini Hands > Don't Optimize

## Building from Source

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 21
- Android SDK 34+

### Build Steps
```bash
git clone https://github.com/YOUR_USERNAME/GeminiHands.git
cd GeminiHands
./gradlew assembleDebug
```

The APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

### GitHub Actions
Every push to `main` automatically builds the APK. Download it from the Actions tab > Latest workflow run > Artifacts.

## Architecture

```
+------------------------------------------+
|           Gemini WebView                  |
|    (gemini.google.com loaded here)        |
+------------------------------------------+
|         JavaScript Injection              |
|   (monitors chat, detects responses)      |
+------------------------------------------+
|         Chat Command Parser               |
|  (extracts actions from Gemini text)      |
+------------------------------------------+
|         Command Executor                  |
|   (routes commands to controllers)        |
+------------+------------+-----------------+
|Accessibility| Device    |   WorkManager   |
|  Service   | Controller|  (Background)   |
|(tap,swipe, |(WiFi,BT,  |  (Scheduling)   |
| type,read) |calls,SMS) |                 |
+------------+------------+-----------------+
```

## Command Detection Examples

When Gemini responds with:
- "I'll open WhatsApp for you" -> Opens WhatsApp
- "Setting an alarm for 7 AM" -> Creates alarm at 7:00
- "Turning on the flashlight" -> Toggles flashlight on
- "Calling John" -> Looks up John in contacts and calls
- "Scrolling down" -> Scrolls the current screen down
- "Taking a screenshot" -> Captures screenshot

## Safety and Privacy

- All processing happens locally on your device
- No data is sent to external servers (except to Google Gemini via WebView)
- You control what permissions are granted
- Action history is stored locally only
- You can disable the service at any time

## Based On

This project is built on top of [Gemini-Pro](https://github.com/RxNaison/Gemini-Pro) by RxNaison, with extensive additions for device automation.

## License

MIT License
