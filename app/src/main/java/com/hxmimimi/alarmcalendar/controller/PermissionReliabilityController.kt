package com.hxmimimi.alarmcalendar.controller

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class PermissionReliabilityController(private val context: Context) {
    private val advisor = ReliabilityAdvisor()
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun requirements(): List<ReliabilityRequirement> = advisor.assess(signals())

    fun signals(): ReliabilitySignals = ReliabilitySignals(
        sdkInt = Build.VERSION.SDK_INT,
        manufacturer = Build.MANUFACTURER.orEmpty(),
        notificationsGranted = notificationsGranted(),
        locationGranted = locationGranted(),
        exactAlarmAllowed = canScheduleExactAlarms(),
        fullScreenIntentAllowed = canUseFullScreenIntent(),
        ignoringBatteryOptimizations = ignoringBatteryOptimizations(),
    )

    fun intentFor(action: ReliabilityAction): Intent = resolvedOrAppDetails(when (action) {
        ReliabilityAction.OPEN_NOTIFICATION_SETTINGS -> notificationSettingsIntent()
        ReliabilityAction.OPEN_LOCATION_SETTINGS -> appDetailsIntent()
        ReliabilityAction.OPEN_EXACT_ALARM_SETTINGS -> Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            .setData(packageUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ReliabilityAction.OPEN_FULL_SCREEN_INTENT_SETTINGS -> fullScreenIntentSettingsIntent()
        ReliabilityAction.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS -> Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            .setData(packageUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ReliabilityAction.OPEN_OEM_AUTOSTART_SETTINGS -> oemAutostartIntent()
        ReliabilityAction.OPEN_OEM_LOCK_SCREEN_SETTINGS -> oemNotificationIntent()
        ReliabilityAction.OPEN_OEM_BACKGROUND_POPUP_SETTINGS -> oemBackgroundPopupIntent()
    })

    private fun notificationsGranted(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun locationGranted(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun canUseFullScreenIntent(): Boolean {
        if (Build.VERSION.SDK_INT < 34) return true
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        return notificationManager.canUseFullScreenIntent()
    }

    private fun ignoringBatteryOptimizations(): Boolean {
        val powerManager = context.getSystemService(PowerManager::class.java)
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    private fun notificationSettingsIntent(): Intent {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(packageUri())
        }
        return intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun fullScreenIntentSettingsIntent(): Intent {
        val intent = if (Build.VERSION.SDK_INT >= 34) {
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).setData(packageUri())
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(packageUri())
        }
        return intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun oemAutostartIntent(): Intent {
        val manufacturer = Build.MANUFACTURER.orEmpty().lowercase()
        val component = when (manufacturer) {
            "xiaomi", "redmi", "poco" -> ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
            "huawei", "honor" -> ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")
            "oppo", "realme", "oneplus" -> ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")
            "vivo", "iqoo" -> ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
            else -> null
        }
        return Intent().apply {
            component?.let { setComponent(it) }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }.takeIf { context.packageManager.resolveActivity(it, PackageManager.MATCH_DEFAULT_ONLY) != null }
            ?: appDetailsIntent()
    }

    private fun oemNotificationIntent(): Intent {
        val manufacturer = Build.MANUFACTURER.orEmpty().lowercase()
        val component = when (manufacturer) {
            "xiaomi", "redmi", "poco" -> ComponentName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")
            "huawei", "honor" -> ComponentName("com.huawei.systemmanager", "com.huawei.notificationmanager.ui.NotificationManagmentActivity")
            "oppo", "realme", "oneplus" -> ComponentName("com.coloros.notificationmanager", "com.coloros.notificationmanager.AppDetailPreferenceActivity")
            "vivo", "iqoo" -> ComponentName("com.vivo.notificationmanager", "com.vivo.notificationmanager.activity.NotifySettingsActivity")
            else -> null
        }
        return componentIntentOrNotificationSettings(component)
    }

    private fun oemBackgroundPopupIntent(): Intent {
        val manufacturer = Build.MANUFACTURER.orEmpty().lowercase()
        val component = when (manufacturer) {
            "xiaomi", "redmi", "poco" -> ComponentName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")
            "huawei", "honor" -> ComponentName("com.huawei.systemmanager", "com.huawei.permissionmanager.ui.MainActivity")
            "oppo", "realme", "oneplus" -> ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.PermissionManagerActivity")
            "vivo", "iqoo" -> ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.SoftPermissionDetailActivity")
            else -> null
        }
        return componentIntentOrAppDetails(component)
    }

    private fun componentIntentOrNotificationSettings(component: ComponentName?): Intent =
        componentIntent(component) ?: notificationSettingsIntent()

    private fun componentIntentOrAppDetails(component: ComponentName?): Intent =
        componentIntent(component) ?: appDetailsIntent()

    private fun componentIntent(component: ComponentName?): Intent? =
        component?.let {
            Intent().setComponent(it).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }?.takeIf { context.packageManager.resolveActivity(it, PackageManager.MATCH_DEFAULT_ONLY) != null }

    private fun packageUri(): Uri = Uri.parse("package:${context.packageName}")

    private fun resolvedOrAppDetails(intent: Intent): Intent =
        intent.takeIf { context.packageManager.resolveActivity(it, PackageManager.MATCH_DEFAULT_ONLY) != null }
            ?: appDetailsIntent()

    private fun appDetailsIntent(): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(packageUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
