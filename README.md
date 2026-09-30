# SpendTrack

A personal, fully offline Android expense tracker. It reads UPI payment notifications from GPay,
PhonePe and Paytm, asks what each payment was for, and keeps everything in a local Room database.
The app declares no `INTERNET` permission and no SMS permissions.

See [`SPENDTRACK_SPEC.md`](SPENDTRACK_SPEC.md) for the full spec.

## Status

| Phase | Scope | State |
|---|---|---|
| 1 | Skeleton, notification listener, raw sample capture, setup screen | Built; needs on-device check |
| 2 | Parsers | Skeleton + `AmountParser` done; per-app regexes waiting for real samples |
| 3 | Data + dedupe | Done: Room v2 (auto-migrates v1), repository, `Deduper`, `RecordPayment` |
| 4 | Prompting | Done: heads-up prompt, category buttons, picker, reminder worker, Pending screen |
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
2026.09.00, Room 2.8.5, WorkManager 2.12.0. compileSdk/targetSdk 37, minSdk 29. Versions live in
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

## Payment prompts (Phase 4)

- Each new PENDING payment posts a heads-up notification on the `payment_prompt` channel:
  *Paid ₹250 to &lt;payee&gt;* / *What was this for?*, with your 3 most-used categories as buttons
  (Food, Travel, Bills until you have history). Tapping a button saves the category and dismisses
  the prompt. Tapping the notification opens the full picker (all categories, a custom one via
  *Other…*, and a note), which can also ignore the payment.
- A WorkManager job runs every 30 minutes and re-posts prompts for payments still PENDING after
  10 minutes. HyperOS may delay it further unless battery saver is set to *No restrictions*.
- The **Pending** screen (the home screen once notification access is on) lists uncategorized
  payments: tap one to categorize, or × to ignore it. The gear icon opens Setup.

### Testing prompts before the parsers exist

Setup → *Captured notifications (debug)* has two buttons:

- **Test prompt** stores a fake PENDING payment (payee *Test payment HH:mm:ss*, source *Test*)
  and shows its prompt. Try a category button, the picker, and Ignore.
- **Run reminder now** re-posts prompts for every PENDING payment immediately instead of
  waiting 30 minutes. Swipe a prompt away (Android 14+ allows it), tap this, and it comes back.

Test payments are real rows. Ignore them when you're done so they don't count in reports.

## Privacy and data

- No `INTERNET` permission. The manifest also strips it (`tools:node="remove"`) in case a library
  ever tries to merge it in.
- `android:allowBackup="false"`, and data-extraction rules exclude all data from cloud backup and
  device transfer.
- Copied samples are marked sensitive on Android 13+, so the clipboard preview hides them.

## Data and duplicate handling

- Money is stored as `Long` paise everywhere. `MonthRange` turns a calendar month in the phone's
  time zone into a half-open `[start, end)` millis range for queries.
- A detected payment is stored as `PENDING` unless it duplicates a stored transaction of any
  status (so a prompt you ignored does not come back as a new row):
  same amount and payee (trimmed, case-insensitive) within 2 minutes, or identical raw
  notification text (SHA-256) within 10 minutes.
- `RecordPayment` serialises check-then-insert with a mutex, so two copies of a notification
  arriving at once can't both be inserted.
- Month totals and category totals exclude `IGNORED` rows. `PENDING` rows count toward the total
  and are grouped under a `null` ("Uncategorized") category.
- Schema changes are migrations, never destructive: exported schemas live in `app/schemas/`, and
  `MigrationTest` upgrades a real v1 database.

## Deviations from the spec

- **Listener status**: `NotificationManagerCompat.getEnabledListenerPackages` only says access was
  granted, not that the service is running. The Setup screen also shows whether the service is
  currently bound (tracked from `onListenerConnected`/`onListenerDisconnected`) and offers
  `requestRebind`.
- **Raw log**: if a notification has no `EXTRA_BIG_TEXT` but has inbox-style `EXTRA_TEXT_LINES`,
  those lines are stored in `bigText` so the sample isn't lost.
- **Full-screen intent**: attached only when `canUseFullScreenIntent()` allows it (Android 14+ may
  deny it to non-calling apps). Without it the heads-up notification is the prompt. The picker
  doesn't show over the lock screen, so payment details stay hidden until you unlock.
- **Dismissable prompts**: Android 14+ lets users swipe away `setOngoing(true)` notifications.
  The reminder worker brings back any that are still PENDING.
- **Lock-screen privacy**: prompts are `VISIBILITY_PRIVATE`; the lock screen shows *New payment to
  categorize* without the amount or payee.
- **`ACCESS_NETWORK_STATE`** is merged in by WorkManager and left in place (it can't reach the
  network without `INTERNET`); stripping it risks WorkManager crashes.
- **Robolectric on JDK 17+** needs `--add-opens=java.base/jdk.internal.access=ALL-UNNAMED` for the
  SDK 36 sandbox (set in `app/build.gradle.kts`). Robolectric tests run against SDK 36, the newest
  it supports.
