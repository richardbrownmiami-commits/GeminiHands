package com.rx.geminipro.services

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log

class GeminiDeviceAdmin : DeviceAdminReceiver() {

    companion object {
        private const val TAG = "GeminiDeviceAdmin"

        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context, GeminiDeviceAdmin::class.java)
        }

        fun isAdminActive(context: Context): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            return dpm.isAdminActive(getComponentName(context))
        }

        fun lockDevice(context: Context) {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            if (isAdminActive(context)) {
                dpm.lockNow()
                Log.d(TAG, "Device locked via Device Admin")
            }
        }

        fun requestAdminPermission(context: Context) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, getComponentName(context))
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Gemini Hands needs Device Admin access to lock/unlock your device and control screen timeout.")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d(TAG, "Device Admin enabled")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.d(TAG, "Device Admin disabled")
    }

    override fun onPasswordChanged(context: Context, intent: Intent, user: android.os.UserHandle) {
        super.onPasswordChanged(context, intent, user)
    }
}
