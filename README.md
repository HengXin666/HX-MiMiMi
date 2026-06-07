# HX MiMiMi

简洁粉色主题的 Android 闹钟 + 日历应用，背景色 `#3c3c3c`，支持手机和平板。

## 功能

- 闹钟：精确响铃、重复规则、按中国工作日/节假日重复、自定义本地铃声、防赖床检测、醒来提示。
- 日历：倒数日、事项日、间隔日；支持提前 N 天、15 分钟前、2 分钟前提醒；可配置重复规则。
- 统计：记录响铃、关闭时间、运动分数、防赖床提醒次数。
- 导出：支持 JSON 配置导出和 SQLite 数据库导出。
- 发布：push 到 `main` 后 GitHub Actions 自动构建并更新 `rolling-release`，不需要手动创建 tag。

## 可靠性策略

应用使用 `AlarmManager.setAlarmClock` 调度闹钟，响铃时启动前台服务并显示全屏/锁屏高优先级通知。系统重启、应用升级、时间或时区变化后会重新调度已启用闹钟和日历提醒。

Android 的系统限制：如果用户在系统设置里对应用执行“强行停止”，第三方应用无法接收广播或自动恢复闹钟，必须由用户重新打开应用。普通划掉后台、进程被系统回收不属于强行停止，闹钟仍按系统闹钟调度能力触发。

## 权限

- `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`：精确闹钟。
- `POST_NOTIFICATIONS`：通知栏、锁屏提醒。
- `RECEIVE_BOOT_COMPLETED`：重启后重排闹钟。
- `WAKE_LOCK`、`VIBRATE`、`FOREGROUND_SERVICE`、`USE_FULL_SCREEN_INTENT`：响铃、震动、前台服务和锁屏全屏提醒。

自定义铃声通过系统文件选择器获取持久 URI，不额外申请媒体库读取权限。

## 本地构建

```bash
./gradlew test assembleRelease
```

当前 release 工作流未配置签名密钥，产物是 unsigned APK。正式发布时应在 GitHub Secrets 中加入 keystore，并在 workflow 中启用签名步骤。
