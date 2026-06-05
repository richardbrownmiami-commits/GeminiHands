package com.rx.geminipro.screens

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rx.geminipro.services.GeminiAccessibilityService
import com.rx.geminipro.services.GeminiDeviceAdmin
import com.rx.geminipro.services.GeminiNotificationListener

@Composable
fun PermissionScreen(
    onAllGranted: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Gemini Hands Setup",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Enable these permissions to give Gemini full control of your device",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Accessibility Service
        PermissionCard(
            title = "Accessibility Service",
            description = "Required for tapping, swiping, typing, and controlling other apps",
            isEnabled = isAccessibilityEnabled(context),
            onClick = {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Notification Listener
        PermissionCard(
            title = "Notification Access",
            description = "Required to read and manage your notifications",
            isEnabled = isNotificationListenerEnabled(context),
            onClick = {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Device Admin
        PermissionCard(
            title = "Device Administrator",
            description = "Required to lock/unlock device and control screen",
            isEnabled = GeminiDeviceAdmin.isAdminActive(context),
            onClick = {
                GeminiDeviceAdmin.requestAdminPermission(context)
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Do Not Disturb
        PermissionCard(
            title = "Do Not Disturb Access",
            description = "Required to toggle Do Not Disturb mode",
            isEnabled = isDndAccessGranted(context),
            onClick = {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Display over other apps
        PermissionCard(
            title = "Display Over Other Apps",
            description = "Required for overlay controls and floating widgets",
            isEnabled = Settings.canDrawOverlays(context),
            onClick = {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Write Settings
        PermissionCard(
            title = "Modify System Settings",
            description = "Required to change brightness and other system settings",
            isEnabled = Settings.System.canWrite(context),
            onClick = {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onAllGranted,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Continue to Gemini", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "You can enable permissions later from Settings",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isEnabled) {
                Text(
                    text = "✓ Enabled",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Button(onClick = onClick) {
                    Text("Enable")
                }
            }
        }
    }
}

fun isAccessibilityEnabled(context: Context): Boolean {
    val service = "${context.packageName}/${GeminiAccessibilityService::class.java.canonicalName}"
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    val colonSplitter = TextUtils.SimpleStringSplitter(':')
    colonSplitter.setString(enabledServices)
    while (colonSplitter.hasNext()) {
        val componentName = colonSplitter.next()
        if (componentName.equals(service, ignoreCase = true)) return true
    }
    return false
}

fun isNotificationListenerEnabled(context: Context): Boolean {
    val cn = ComponentName(context, GeminiNotificationListener::class.java)
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat != null && flat.contains(cn.flattenToString())
}

fun isDndAccessGranted(context: Context): Boolean {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    return notificationManager.isNotificationPolicyAccessGranted
}
