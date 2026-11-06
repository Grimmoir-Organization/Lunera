# Lunera 1.8.3 rollback

This archive restores the regular Lunera 1.8.3 application source from the original release. Runtime code and resources are unchanged from that release. APKs, build caches, old report exports, design exports, private configuration and signing keys are excluded.

The restored signed APK is version 1.8.3, versionCode 10803. Version 1.8.4 uses versionCode 10804, so Android normally rejects installing this older APK directly over it. Export any settings/playlists you need before considering an uninstall; uninstalling removes private app data. Music stored outside the app's private storage is separate.

Known limitation: 1.8.3 contains the previously reported notification-X crash on Android 15. This rollback restores the old baseline and is not a fix for that crash or for a lingering media card.

Expected close behavior for a future fix: after an explicit X/Close, playback and its active media notification should stay closed even when the Activity is subsequently removed from Recents. Removing the Activity from Recents during normal active playback is a different case and previously was intended to preserve music playback.

Build requirements remain JDK 21, Gradle 9.8.2, SDK Platform 37.0 / Build Tools 37.0.0, and Platform 36 / Build Tools 36.0.0 for shared modules. Use your existing signing identity for release builds.

Android version-code guidance: https://developer.android.com/studio/publish/versioning
