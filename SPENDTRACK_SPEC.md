# SpendTrack: Build Spec for Coding Agent

Read this whole file before writing any code. Build in the phase order given. Do not skip ahead.

## 1. Goal

A personal, fully offline Android expense tracker. When the user pays via UPI (GPay, PhonePe, Paytm), the app detects the payment from the app's notification, immediately prompts the user to pick a spending category, and stores everything locally. At month end the user sees where money went and compares it with budgets.

Target device: Redmi Note 13 Pro (HyperOS / MIUI, Android 13 or 14). Single user, sideloaded APK, no Play Store requirements.

## 2. Hard constraints

- Fully offline. Do NOT declare the `INTERNET` permission. No analytics, no crash-reporting SDKs, no network libraries.
- Do NOT request SMS permissions. Detection is notification-only.
- Language: Kotlin. UI: Jetpack Compose (Material 3). Storage: Room. Async: Coroutines + Flow.
- Single Gradle module named `app`. Use a Gradle version catalog (`libs.versions.toml`) and the latest stable versions of everything.
- minSdk 29, targetSdk and compileSdk = latest stable.
- Package: `com.spendtrack.app`
- Dependency injection: manual, via an `AppContainer` created in the `Application` class. Do not add Hilt or Koin.
- Set `android:allowBackup="false"` (financial data).

## 3. Architecture

MVVM + Repository, four layers. Dependencies point downward only.

```
UPI app notification
        |
        v
system/   PaymentListenerService -> ParserRegistry -> Deduper -> Repository -> Room
        |                                                             |
        v                                                             v
   PromptNotifier (heads-up prompt)                       StatsViewModel / Compose UI
        |
        v
   CategoryActionReceiver -> Categorize use case -> Repository
   PendingReminderWorker -> re-posts prompts for PENDING rows
```

### Package layout

```
app/src/main/java/com/spendtrack/app/
├── SpendTrackApp.kt              (Application, builds AppContainer)
├── AppContainer.kt
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt
│   │   ├── TransactionEntity.kt
│   │   ├── BudgetEntity.kt
│   │   ├── RawNotificationEntity.kt   (debug log, see Phase 1)
│   │   ├── TransactionDao.kt
│   │   ├── BudgetDao.kt
│   │   └── RawNotificationDao.kt
│   └── TransactionRepository.kt
├── domain/
│   ├── model/ (ParsedPayment, TxnStatus, Category, MonthlySummary)
│   ├── parser/
│   │   ├── PaymentParser.kt          (interface)
│   │   ├── GPayParser.kt
│   │   ├── PhonePeParser.kt
│   │   ├── PaytmParser.kt
│   │   ├── ParserRegistry.kt
│   │   └── AmountParser.kt           (shared helper)
│   ├── Deduper.kt
│   └── usecase/ (RecordPayment, Categorize, GetMonthlySummary, SetBudget)
├── system/
│   ├── PaymentListenerService.kt
│   ├── PromptNotifier.kt
│   ├── CategoryActionReceiver.kt
│   ├── PendingReminderWorker.kt
│   └── NotificationChannels.kt
└── ui/
    ├── MainActivity.kt
    ├── CategorizeActivity.kt         (small dialog-style activity)
    ├── nav/ (NavGraph)
    ├── onboarding/ (PermissionScreen)
    ├── pending/ (PendingScreen, PendingViewModel)
    ├── dashboard/ (DashboardScreen, StatsViewModel)
    ├── budget/ (BudgetScreen, BudgetViewModel)
    ├── manual/ (ManualEntryScreen)
    └── theme/
```

## 4. Data model

```kotlin
enum class TxnStatus { PENDING, CATEGORIZED, IGNORED }

@Entity(tableName = "transactions", indices = [Index("timestamp"), Index("status")])
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountPaise: Long,          // store money as paise (Long), never Double
    val payee: String,
    val category: String? = null,
    val note: String? = null,
    val timestamp: Long,            // epoch millis, payment time
    val appPkg: String,             // "manual" for cash entries
    val rawText: String,            // original notification text, always kept
    val status: TxnStatus = TxnStatus.PENDING
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val category: String,
    val monthlyLimitPaise: Long
)

@Entity(tableName = "raw_notifications")   // debug log for collecting real samples
data class RawNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pkg: String,
    val title: String?,
    val text: String?,
    val bigText: String?,
    val postedAt: Long
)
```

Default categories (strings, user can add custom ones via "Other"): Food, Groceries, Travel, Bills, Shopping, Health, Entertainment, Other.

## 5. Component behavior

### PaymentListenerService (`NotificationListenerService`)
- In `onNotificationPosted`, ignore any package not in the allowlist and ignore the app's own package.
- Allowlist: `com.google.android.apps.nbu.paisa.user` (GPay), `com.phonepe.app` (PhonePe), `net.one97.paytm` (Paytm). Keep the list in one constant.
- Read `EXTRA_TITLE`, `EXTRA_TEXT`, `EXTRA_BIG_TEXT` from the notification extras.
- Always write the raw notification to `raw_notifications` (this table powers the sample collection in Phase 1; cap it at the latest 500 rows).
- Hand the extracted text to `ParserRegistry`. If it returns a `ParsedPayment`, call the `RecordPayment` use case.
- Do all work on a coroutine scope with `Dispatchers.IO`. Never block the callback.

### Parsers
```kotlin
interface PaymentParser {
    val packageName: String
    fun parse(title: String?, text: String?, postedAt: Long): ParsedPayment?
}
data class ParsedPayment(val amountPaise: Long, val payee: String, val timestamp: Long, val appPkg: String, val rawText: String)
```
- Return `null` for anything that is not a successful outgoing debit: failed, pending, processing, refund, cashback, "received", "request", promotional text.
- `AmountParser` handles `₹`, `Rs.`, `INR`, commas, and decimals, and returns paise as `Long`.
- IMPORTANT: Do not invent notification formats. Regexes must be written against real samples supplied by the user in `app/src/test/resources/samples/<app>.txt` (one sample per line block, with expected amount and payee). If the sample files are empty, write the parser skeleton with the interface and `TODO` regexes, add failing-but-ignored tests, and stop to ask the user for samples.

### Deduper
- A payment is a duplicate if a stored transaction has the same `amountPaise` and the same normalized payee (trimmed, lowercased) within a 2 minute window, or if the same `rawText` hash was seen within 10 minutes.
- Implement as a pure, unit-testable function plus a DAO query that fetches candidates in the window.

### PromptNotifier
- Channel `payment_prompt`, `IMPORTANCE_HIGH`, sound and vibration on.
- Notification shows: "Paid ₹250 to <payee>. What was this for?"
- Android shows at most 3 action buttons. Actions = the user's 3 most frequently used categories (fall back to Food, Travel, Bills), plus tapping the notification body opens `CategorizeActivity` for the full list, custom category, and note.
- Each action fires a `PendingIntent` to `CategoryActionReceiver` carrying `txnId` and `category`. Use `FLAG_IMMUTABLE` and a unique request code per (txnId, action).
- Notification id = derived from `txnId`. Set `setOngoing(true)` and `setOnlyAlertOnce(false)`. Cancel it after categorization.
- Also attach a full-screen intent to `CategorizeActivity`, treated as best-effort (Android 14 may deny it). The app must work correctly without it.
- On Android 13+, request `POST_NOTIFICATIONS` at runtime in onboarding.

### CategoryActionReceiver
- Updates the row to `CATEGORIZED` with the chosen category via the `Categorize` use case, then cancels the notification. Use `goAsync()` with a coroutine.

### PendingReminderWorker
- Unique periodic WorkManager job, every 30 minutes (15 min is the minimum allowed).
- Re-posts prompt notifications for all `PENDING` rows older than 10 minutes. Skip posting when there are none.
- Enqueue on app start with `ExistingPeriodicWorkPolicy.KEEP`.

## 6. UI screens (Compose, Material 3)

1. **Onboarding / Permission screen:** explains why notification access is needed, button opens `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`, requests `POST_NOTIFICATIONS`, and shows HyperOS checklist (autostart, battery "No restrictions", lock in recents, floating notifications, "display pop-up windows while running in background"). Show live status: is the listener actually connected? (Use `NotificationManagerCompat.getEnabledListenerPackages`.)
2. **Pending:** list of uncategorized transactions, tap to categorize or ignore.
3. **Dashboard:** month picker, total spent, category totals as a bar or donut chart (Vico, or a simple Compose Canvas chart), top 5 payees, budget vs actual per category with a progress bar (turns red over 100%).
4. **Budget:** set a monthly limit per category.
5. **Manual entry:** amount, payee, category, note, date. For cash payments (`appPkg = "manual"`, status `CATEGORIZED`).
6. **History:** all transactions for the month, editable category, swipe to mark IGNORED.

Bottom navigation: Dashboard, Pending (with badge count), History, Budget.

## 7. Build phases and acceptance criteria

Complete each phase, make sure `./gradlew assembleDebug` and `./gradlew test` pass, then commit before continuing.

**Phase 1: Skeleton + listener + sample collection**
- Project setup, manifest (listener service with `BIND_NOTIFICATION_LISTENER_SERVICE` permission, receiver, activities), `AppContainer`, Room with `raw_notifications`.
- Permission screen with live connection status.
- A debug screen that lists captured raw notifications with a "Copy all" button, so the user can paste real samples into the sample files.
- Done when: a real UPI payment on the device appears in the debug list.

**Phase 2: Parsers**
- Interface, registry, `AmountParser`, three parsers, fixtures-driven unit tests.
- Done when: all tests pass against the user's real samples, and failure/refund/received samples return `null`.

**Phase 3: Data + dedupe**
- Full Room schema, DAOs, repository, `Deduper`, `RecordPayment` use case.
- Done when: in-memory Room tests cover insert, duplicate rejection, month range queries, and category totals.

**Phase 4: Prompting**
- `PromptNotifier`, `CategoryActionReceiver`, `CategorizeActivity`, `PendingReminderWorker`, Pending screen.
- Done when: a real payment produces a heads-up prompt, tapping a category button saves it and dismisses the notification, and ignored prompts come back after the worker runs.

**Phase 5: Reports and budgets**
- Dashboard, Budget, History, Manual entry screens, `GetMonthlySummary`.
- Done when: month totals match the sum of stored transactions and budget progress bars are correct.

**Phase 6 (stretch):** CSV export via the Storage Access Framework, recurring-payee auto-categorization (suggest last used category for the same payee), dark theme polish.

## 8. Working rules for the agent

- Money is `Long` paise everywhere. Format to rupees only at the UI edge.
- Never guess notification text formats. If samples are missing, stop and ask the user.
- Keep parsers isolated: a format change must only require editing one parser file and its fixtures.
- No blocking work on the main thread. Use `Dispatchers.IO` for DB and parsing.
- Handle process death: everything the receiver and worker need must come from the DB or Intent extras, not in-memory state.
- Write small, focused commits per phase. Update `README.md` with build steps and the HyperOS setup checklist at the end of Phase 1 and keep it current.
- If a requirement here conflicts with an Android platform restriction, implement the closest working behavior and document the deviation in `README.md`.
