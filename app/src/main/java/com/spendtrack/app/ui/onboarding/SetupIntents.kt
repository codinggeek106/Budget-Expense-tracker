package com.spendtrack.app.ui.onboarding

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/** Snapshot of the permissions/settings the app can detect by itself. */
data class SetupStatus(
    val listenerEnabled: Boolean,
    val notificationsAllowed: Boolean,
    val batteryUnrestricted: Boolean,
) {
    companion object {
        fun read(context: Context): SetupStatus {
            val pm = context.getSystemService(PowerManager::class.java)
            return SetupStatus(
                listenerEnabled = context.packageName in
                    NotificationManagerCompat.getEnabledListenerPackages(context),
                notificationsAllowed = NotificationManagerCompat.from(context).areNotificationsEnabled(),
                batteryUnrestricted = pm?.isIgnoringBatteryOptimizations(context.packageName) == true,
            )
        }
    }
}

/**
 * Settings screens. HyperOS/MIUI-specific screens are tried first and fall back to the
 * standard App info page when the OEM activity is missing or not exported.
 */
object SetupIntents {

    fun notificationListenerSettings() = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

    fun appDetails(context: Context) =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))

    fun appNotificationSettings(context: Context) =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

    fun miuiAutostart() = Intent().setComponent(
        ComponentName(
            "com.miui.securitycenter",
            "com.miui.permcenter.autostart.AutoStartManagementActivity",
        )
    )

    fun miuiPermissionEditor(context: Context) = Intent("miui.intent.action.APP_PERM_EDITOR")
        .setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")
        .putExtra("extra_pkgname", context.packageName)

    /** Starts the first intent that resolves. Returns false if none could be opened. */
    fun launchFirst(context: Context, vararg intents: Intent): Boolean {
        for (intent in intents) {
            try {
                context.startActivity(intent)
                return true
            } catch (_: ActivityNotFoundException) {
            } catch (_: SecurityException) {
            }
        }
        return false
    }
}
