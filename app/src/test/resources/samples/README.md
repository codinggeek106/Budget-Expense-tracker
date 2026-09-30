# Parser samples

Real notification text from each UPI app, used as fixtures by the parser tests (Phase 2).
Parser regexes are written only against these samples, never against guessed formats.

Files: `gpay.txt`, `phonepe.txt`, `paytm.txt`.

## Getting samples

1. In the app, open **Setup → Captured notifications (debug)**.
2. Tap **Copy all** (or the share icon) and paste the text into the matching file below.
3. Fill in the `expect:` line of every block.

## Format

One block per notification, separated by a blank line. Lines starting with `#` are comments.
Every field is on a single line; newlines inside a value are written as `\n` and backslashes as `\\`.

```
# 2026-09-30 14:45:10
pkg: com.phonepe.app
posted: 1790757310000
title: <EXTRA_TITLE, omitted if absent>
text: <EXTRA_TEXT, omitted if absent>
bigText: <EXTRA_BIG_TEXT, omitted if absent>
expect: 250.00 | Payee Name
```

`expect:` is one of:

- `<amount in rupees> | <payee>` for a successful outgoing payment, e.g. `expect: 1,250.50 | Ramesh Kumar`
  (commas are optional; the amount is compared in paise).
- `none` for anything that must not be recorded: failed, pending, processing, refund, cashback,
  money received, payment requests, promotions, reminders.

Remove or anonymise anything you don't want in the repo (UPI IDs, account digits). Keep the
structure of the text intact, because the parser depends on it.
