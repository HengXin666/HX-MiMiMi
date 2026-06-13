package com.hxmimimi.alarmcalendar.controller

enum class ReliabilityRequirementId {
    NOTIFICATIONS,
    LOCATION,
    EXACT_ALARM,
    FULL_SCREEN_INTENT,
    BATTERY_OPTIMIZATION,
    OEM_AUTOSTART,
    OEM_LOCK_SCREEN,
    OEM_BACKGROUND_POPUP,
}

enum class ReliabilityStatus {
    READY,
    NEEDS_ACTION,
    REVIEW,
}

enum class ReliabilitySeverity {
    CRITICAL,
    IMPORTANT,
    RECOMMENDED,
}

enum class ReliabilityAction {
    OPEN_NOTIFICATION_SETTINGS,
    OPEN_LOCATION_SETTINGS,
    OPEN_EXACT_ALARM_SETTINGS,
    OPEN_FULL_SCREEN_INTENT_SETTINGS,
    REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
    OPEN_OEM_AUTOSTART_SETTINGS,
    OPEN_OEM_LOCK_SCREEN_SETTINGS,
    OPEN_OEM_BACKGROUND_POPUP_SETTINGS,
}

data class ReliabilitySignals(
    val sdkInt: Int,
    val manufacturer: String,
    val notificationsGranted: Boolean,
    val locationGranted: Boolean,
    val exactAlarmAllowed: Boolean,
    val fullScreenIntentAllowed: Boolean,
    val ignoringBatteryOptimizations: Boolean,
)

data class ReliabilityRequirement(
    val id: ReliabilityRequirementId,
    val title: String,
    val detail: String,
    val status: ReliabilityStatus,
    val severity: ReliabilitySeverity,
    val action: ReliabilityAction?,
)

class ReliabilityAdvisor {
    fun assess(signals: ReliabilitySignals): List<ReliabilityRequirement> = buildList {
        add(
            ReliabilityRequirement(
                id = ReliabilityRequirementId.NOTIFICATIONS,
                title = "通知栏与锁屏提醒",
                detail = if (signals.notificationsGranted) {
                    "响铃和日历提醒可以显示在通知栏。"
                } else {
                    "需要允许通知，否则闹钟只能响铃，锁屏提醒和日历提示会不稳定。"
                },
                status = if (signals.notificationsGranted) ReliabilityStatus.READY else ReliabilityStatus.NEEDS_ACTION,
                severity = ReliabilitySeverity.CRITICAL,
                action = if (signals.notificationsGranted) null else ReliabilityAction.OPEN_NOTIFICATION_SETTINGS,
            ),
        )
        add(
            ReliabilityRequirement(
                id = ReliabilityRequirementId.LOCATION,
                title = "定位运动证据",
                detail = if (signals.locationGranted) {
                    "防赖床会结合定位位移与加速度判断是否真的起身。"
                } else {
                    "建议允许定位，用位移补充加速度证据；未允许时只能依赖加速度。"
                },
                status = if (signals.locationGranted) ReliabilityStatus.READY else ReliabilityStatus.NEEDS_ACTION,
                severity = ReliabilitySeverity.IMPORTANT,
                action = if (signals.locationGranted) null else ReliabilityAction.OPEN_LOCATION_SETTINGS,
            ),
        )
        add(
            ReliabilityRequirement(
                id = ReliabilityRequirementId.EXACT_ALARM,
                title = "精确闹钟",
                detail = if (signals.exactAlarmAllowed) {
                    "系统允许按用户设定时间精确触发。"
                } else {
                    "需要允许“闹钟和提醒”，否则 Android 12+ 可能拒绝精确调度。"
                },
                status = if (signals.exactAlarmAllowed) ReliabilityStatus.READY else ReliabilityStatus.NEEDS_ACTION,
                severity = ReliabilitySeverity.CRITICAL,
                action = if (signals.exactAlarmAllowed) null else ReliabilityAction.OPEN_EXACT_ALARM_SETTINGS,
            ),
        )
        add(
            ReliabilityRequirement(
                id = ReliabilityRequirementId.FULL_SCREEN_INTENT,
                title = "锁屏全屏响铃",
                detail = if (signals.fullScreenIntentAllowed) {
                    "来闹钟时可以直接唤起锁屏响铃界面。"
                } else {
                    "Android 14+ 需要单独允许全屏通知，否则锁屏上可能只显示横幅。"
                },
                status = if (signals.fullScreenIntentAllowed) ReliabilityStatus.READY else ReliabilityStatus.NEEDS_ACTION,
                severity = ReliabilitySeverity.CRITICAL,
                action = if (signals.fullScreenIntentAllowed) null else ReliabilityAction.OPEN_FULL_SCREEN_INTENT_SETTINGS,
            ),
        )

        val oem = ChineseReliabilityOem.from(signals.manufacturer)
        val isChineseOem = oem != null
        add(
            ReliabilityRequirement(
                id = ReliabilityRequirementId.BATTERY_OPTIMIZATION,
                title = "省电策略",
                detail = if (signals.ignoringBatteryOptimizations) {
                    "应用已不受系统电池优化限制。"
                } else if (isChineseOem) {
                    "建议设为“不限制”，避免省电策略影响防赖床检测和日历提醒。"
                } else {
                    "建议允许忽略电池优化，降低后台回收对提醒链路的影响。"
                },
                status = if (signals.ignoringBatteryOptimizations) ReliabilityStatus.READY else ReliabilityStatus.NEEDS_ACTION,
                severity = if (isChineseOem) ReliabilitySeverity.IMPORTANT else ReliabilitySeverity.RECOMMENDED,
                action = if (signals.ignoringBatteryOptimizations) null else ReliabilityAction.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            ),
        )

        if (isChineseOem) {
            add(
                ReliabilityRequirement(
                    id = ReliabilityRequirementId.OEM_AUTOSTART,
                    title = "自启动",
                    detail = oem?.autostartDetail ?: "中国定制系统通常需要在手机管家里允许应用自启动，否则重启后提醒链路可能被拦截。",
                    status = ReliabilityStatus.REVIEW,
                    severity = ReliabilitySeverity.IMPORTANT,
                    action = ReliabilityAction.OPEN_OEM_AUTOSTART_SETTINGS,
                ),
            )
            add(
                ReliabilityRequirement(
                    id = ReliabilityRequirementId.OEM_LOCK_SCREEN,
                    title = "锁屏显示",
                    detail = oem?.lockScreenDetail ?: "请在系统权限或通知管理中允许锁屏显示，确保闹钟能在熄屏状态直接出现。",
                    status = ReliabilityStatus.REVIEW,
                    severity = ReliabilitySeverity.IMPORTANT,
                    action = ReliabilityAction.OPEN_OEM_LOCK_SCREEN_SETTINGS,
                ),
            )
            add(
                ReliabilityRequirement(
                    id = ReliabilityRequirementId.OEM_BACKGROUND_POPUP,
                    title = "后台弹出界面",
                    detail = oem?.backgroundPopupDetail ?: "请允许后台弹出界面或悬浮到前台，避免闹钟响铃时无法打开全屏页面。",
                    status = ReliabilityStatus.REVIEW,
                    severity = ReliabilitySeverity.IMPORTANT,
                    action = ReliabilityAction.OPEN_OEM_BACKGROUND_POPUP_SETTINGS,
                ),
            )
        }
    }
}

private data class ChineseReliabilityOem(
    val names: Set<String>,
    val autostartDetail: String,
    val lockScreenDetail: String,
    val backgroundPopupDetail: String,
) {
    companion object {
        private val Xiaomi = ChineseReliabilityOem(
            names = setOf("xiaomi", "redmi", "poco"),
            autostartDetail = "在 MIUI/HyperOS 手机管家中允许自启动，避免重启后闹钟和日历提醒链路被拦截。",
            lockScreenDetail = "在通知管理中允许锁屏通知，并在应用权限里确认可在锁屏显示闹钟页面。",
            backgroundPopupDetail = "在权限管理中允许后台弹出界面，确保锁屏或后台时能打开全屏响铃页面。",
        )
        private val Huawei = ChineseReliabilityOem(
            names = setOf("huawei", "honor"),
            autostartDetail = "在启动管理中关闭自动管理，并允许自启动、关联启动和后台活动。",
            lockScreenDetail = "在通知设置中允许锁屏通知，保证闹钟响铃时可见。",
            backgroundPopupDetail = "在应用权限中确认可从后台唤起或显示在其他应用上层。",
        )
        private val Oppo = ChineseReliabilityOem(
            names = setOf("oppo", "realme", "oneplus"),
            autostartDetail = "在手机管家或权限隐私中允许自启动，并将后台耗电设为不限制。",
            lockScreenDetail = "在通知管理中允许锁屏通知和横幅提醒。",
            backgroundPopupDetail = "在权限管理中允许后台弹出界面，避免闹钟只能停留在通知栏。",
        )
        private val Vivo = ChineseReliabilityOem(
            names = setOf("vivo", "iqoo"),
            autostartDetail = "在权限管理中开启自启动，并允许后台高耗电。",
            lockScreenDetail = "在通知管理中允许锁屏通知。",
            backgroundPopupDetail = "在权限管理中允许后台弹出界面或悬浮窗相关权限。",
        )

        fun from(manufacturer: String): ChineseReliabilityOem? {
            val normalized = manufacturer.lowercase()
            return listOf(Xiaomi, Huawei, Oppo, Vivo).firstOrNull { normalized in it.names }
        }
    }
}
