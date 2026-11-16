# Audit Status Summary (2026-11-16)

Reconstructed status summary. The original audit file was not committed and is no longer available; descriptions below are condensed from earlier reports.

| Finding | Severity | Description | Status |
|---|---|---|---|
| F-01 | High | remoteCanReceive returns true when capabilities are null | ACCEPTED (governs outbound packets only; peers ignore unknown types) |
| F-02 | High | wallpaper unpair/disable during transfer race | FIXED |
| F-03 | Medium | unbounded send channel | FIXED (e50dff4) |
| F-04 | Medium | LineTooLongException read-loop DoS | FIXED (205eeef) |
| F-05 | Medium | onSuccess fires before async payload completes | FIXED (6b86b30) |
| F-06 | Medium | send-coroutine exceptional completion | FIXED (e50dff4) |
| F-07 | Medium | cleartext traffic permitted | FIXED |
| F-08 | Medium | exported components | PARTLY FIXED (TransactionService not exported, 6490223; RunCommandUrlActivity still runs commands without confirmation until its PR merges) |
| F-09 | Low | empty device_dashboard_strings.xml files | FIXED |
| F-10 | Low | "KDE Connect" strings / applicationId | LEFT ALONE (strings refer to the desktop app or credit contributors; applicationId must never change) |
| F-11 | Low | deprecated APP_WIDE settings scope | FIXED (48ad58f) |
| F-12 | Low | orphaned .wallpaper.tmp files | FIXED |
