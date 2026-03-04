# Android test APK build

This repository is now wired as an Android app module that can produce an installable **debug APK** for device testing.

## Build command

```bash
./scripts/build-test-apk.sh
```

or directly:

```bash
gradle :app:assembleDebug
```

## Output APK

After a successful build, the installable APK is:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Install to connected device

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Notes

- Java 17 is recommended/required for the Android Gradle Plugin.
- First build needs access to Maven repositories (Google + Maven Central) to download Android/Compose dependencies.
