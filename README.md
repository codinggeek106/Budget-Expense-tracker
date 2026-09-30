# SpendTrack

A personal, fully offline Android expense tracker. It reads UPI payment notifications from GPay,
PhonePe and Paytm, asks what each payment was for, and keeps everything in a local Room database.
The app declares no `INTERNET` permission and no SMS permissions.

See [`SPENDTRACK_SPEC.md`](SPENDTRACK_SPEC.md) for the full spec.

## Status

| Phase | Scope | State |
|---|---|---|
| 1 | Skeleton, notification listener, raw sample capture, setup screen | Built; needs on-device check |
| 2 | Parsers | Waiting for real notification samples |
| 3 | Data + dedupe | Not started |
| 4 | Prompting | Not started |
| 5 | Reports and budgets | Not started |
| 6 | Stretch | Not started |

## Build

Requirements: JDK 17+ (Android Studio's bundled JBR works) and the Android SDK. `local.properties`
must point at the SDK (Android Studio writes this automatically), e.g.
`sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk`.

```bash
# Windows Git Bash, using Android Studio's JDK
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
./gradlew assembleDebug      # APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew test               # JVM + Robolectric unit tests
```

Install on the phone (USB debugging on):

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or copy the APK to the phone and open it (allow "Install unknown apps" for your file manager).

Toolchain: Gradle 9.8.0, AGP 9.4.1 (built-in Kotlin), Kotlin 2.4.20, KSP 2.3.12, Compose BOM
2026.09.00, Room 2.8.5. compileSdk/targetSdk 37, minSdk 29. Versions live in
`gradle/libs.versions.toml`.

## Phone setup (HyperOS / MIUI)

HyperOS kills background apps aggressively and blocks notification access for sideloaded apps.
The in-app **Setup** screen links to each of these and shows live status for the ones it can detect.

1. **Allow restricted settings** (Android 13+, sideloaded APKs): Settings → Apps → SpendTrack →
   ⋮ (top right) → *Allow restricted settings*. Without this the notification-access toggle is greyed out.
2. **Notification access**: Setup → *Grant notification access* → enable SpendTrack.
   Setup should then show *Access granted: Yes* and *Detector connected: Yes*.
3. **Notifications** (Android 13+): allow when prompted, for the category prompts.
4. **Autostart**: Security → Permissions → Autostart → enable SpendTrack.
5. **Battery saver**: App info → Battery saver → *No restrictions*.
6. **Lock in recents**: open recents, long-press the SpendTrack card, tap the lock.
7. **Floating notifications**: App info → Notifications → allow floating notifications.
8. **Display pop-up windows while running in background**: App info → Other permissions → allow.

If *Detector connected* shows **No** while access is granted, tap *Reconnect detector*. If it
stays disconnected, toggle notification access off and on.

## Collecting parser samples (Phase 1 → Phase 2)

Parsers are written only against real notification text, never guessed formats.

1. With setup done, pay someone with GPay, PhonePe or Paytm. Also try to capture non-payments:
   money received, a failed or pending payment, a refund or cashback, a payment request, a promo.
2. Open Setup → **Captured notifications (debug)**. Each notification from those three apps
   is listed (latest 500 kept).
3. Tap **Copy all** or the share icon, paste into
   `app/src/test/resources/samples/{gpay,phonepe,paytm}.txt`, and fill in each `expect:` line.
   The format is described in `app/src/test/resources/samples/README.md`.

## Privacy and data

- No `INTERNET` permission. The manifest also strips it (`tools:node="remove"`) in case a library
  ever tries to merge it in.
- `android:allowBackup="false"`, and data-extraction rules exclude all data from cloud backup and
  device transfer.
- Copied samples are marked sensitive on Android 13+, so the clipboard preview hides them.

## Deviations from the spec

- **Listener status**: `NotificationManagerCompat.getEnabledListenerPackages` only says access was
  granted, not that the service is running. The Setup screen also shows whether the service is
  currently bound (tracked from `onListenerConnected`/`onListenerDisconnected`) and offers
  `requestRebind`.
- **Raw log**: if a notification has no `EXTRA_BIG_TEXT` but has inbox-style `EXTRA_TEXT_LINES`,
  those lines are stored in `bigText` so the sample isn't lost.
- **Robolectric on JDK 17+** needs `--add-opens=java.base/jdk.internal.access=ALL-UNNAMED` for the
  SDK 36 sandbox (set in `app/build.gradle.kts`). Robolectric tests run against SDK 36, the newest
  it supports.
