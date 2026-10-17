# UDHR Fix List — Status

Checked on 17 October 2026 against the code on `main`. This tracks the 44 items in [UDHR-Fix-List.md](UDHR-Fix-List.md); the Status column there was never updated, so use this file instead.

**32 of 44 are done. 12 are still open**, mostly in the pharmacy flow.

## Done (32)

| Group | Items | Where |
| --- | --- | --- |
| P0 — security and patient safety | P0-1 to P0-10 (all 10) | PRs #36, #39 |
| P1 — sign-in and sessions | P1-1 to P1-8 | PRs #37, #42 |
| P2 — front desk, nurse, doctor, patient portal | P2-1 to P2-13 (all 13) | PRs #36, #41 |
| P3 — engineering | P3-1 (automated tests, run in CI) | 98 tests at the time of this check |

## Still open (12)

| # | Problem | What's there now | Size |
| --- | --- | --- | --- |
| P1-9 | Two dispensing paths that disagree | Still true. "Mark dispensed" closes the prescription but never reduces stock; "Log dispense event" reduces stock separately. | Medium |
| P1-10 | Dispensing doesn't check the prescription is still active; two people can dispense at once | "Already dispensed" is blocked, but there's no active or end-date check and no protection against two people dispensing at once. | Small |
| P1-11 | Stock matched by exact name ("Paracetamol" never reduces "Paracetamol 500mg") | Half done. The doctor's stock check understands names and brands; dispensing still matches stock by exact name. | Small |
| P1-12 | Pharmacy shows "from Front Desk Clerk" | Still shows whoever opened the visit, not the prescribing doctor. | Small |
| P1-13 | Same patient twice in queues | Mostly fixed: check-in reuses an open visit. Duplicates could only come from old data. | Small |
| P1-14 | Dispense screen lacks duration and total quantity | Half done. Statuses are plain words; the dispense row shows "dose · frequency" but not "7 days · 14 tablets". | Small |
| P1-15 | One expiry date per item, so a new batch overwrites the old date | No batch tracking. | Large |
| P1-16 | No stock history; low-stock alerts went to the front desk | Half done. Alerts go to the pharmacist and expired items are flagged; there's no history view on screen. | Small |
| P3-2 | One huge `App.jsx` | Now 5,997 lines with 135 `useState` calls. | Large |
| P3-3 | Unused old queue code | `QueueController`, `QueueService` and `QueueEntry` are still there; the app uses visit status. | Small |
| P3-4 | Production logs every SQL statement and changes tables on startup | `show-sql=true` and `ddl-auto=update` are still set. Turning off SQL logging is small; moving to Flyway migrations is large. | Small / Large |
| P3-5 | Demo values pre-filled in search boxes | Patient ID 9001015000083 and a barcode are still pre-filled. | Small |

(P1-13, P1-14 and P1-16 count as open until the last part is done.)

## Recommended next

1. **The pharmacy flow: P1-9, P1-10, P1-11, P1-12 and P1-14 together.** These are what a panel would notice in a demo. When they're done, "Mark dispensed" reduces the right stock item, refuses an expired or ended prescription, and shows the prescribing doctor and "7 days · 14 tablets".
2. **Quick wins: P3-5 and the SQL-logging half of P3-4.**
3. **Later: P1-15, P3-2 and Flyway (the rest of P3-4).** Large changes with little to show in a demo.
