# UDHR Fix List — Status

Checked on 17 October 2026 against the code on `main`; updated 8 November 2026 after a full end-to-end test of every portal. This tracks the 44 items in [UDHR-Fix-List.md](UDHR-Fix-List.md); the Status column there was never updated, so use this file instead.

**37 of 44 are done. 7 are still open**, mostly engineering clean-up.

## Done (37)

| Group | Items | Where |
| --- | --- | --- |
| P0 — security and patient safety | P0-1 to P0-10 (all 10) | PRs #36, #39 |
| P1 — sign-in and sessions | P1-1 to P1-8 | PRs #37, #42 |
| P1 — pharmacy | P1-9, P1-10, P1-11, P1-12, P1-14 | One dispensing path that takes stock, locks the prescription, refuses stopped or finished ones, matches stock by medicine, shows the prescriber and "hand over 20 tablets" |
| P2 — front desk, nurse, doctor, patient portal | P2-1 to P2-13 (all 13) | PRs #36, #41 |
| P3 — engineering | P3-1 (automated tests, run in CI) | 112 tests on 8 November |

## Still open (7)

| # | Problem | What's there now | Size |
| --- | --- | --- | --- |
| P1-13 | Same patient twice in queues | Mostly fixed: check-in reuses an open visit. Duplicates could only come from old data. | Small |
| P1-15 | One expiry date per item, so a new batch overwrites the old date | No batch tracking. | Large |
| P1-16 | No stock history; low-stock alerts went to the front desk | Half done. Alerts go to the pharmacist and expired items are flagged; there's no history view on screen. | Small |
| P3-2 | One huge `App.jsx` | Now 6,644 lines with 154 `useState` calls. | Large |
| P3-3 | Unused old queue code | `QueueController`, `QueueService` and `QueueEntry` are still there; the app uses visit status. | Small |
| P3-4 | Production logs every SQL statement and changes tables on startup | `show-sql=true` and `ddl-auto=update` are still set. Turning off SQL logging is small; moving to Flyway migrations is large. | Small / Large |
| P3-5 | Demo values pre-filled in search boxes | Half done: the patient ID is no longer pre-filled; the food barcode box still is. | Small |

(P1-13 and P1-16 count as open until the last part is done.)

## Recommended next

1. **Quick wins: P3-5 and the SQL-logging half of P3-4.**
2. **A stock history view (rest of P1-16)**: the history is recorded and reported, but there's no screen for one item's history.
3. **Later: P1-15, P3-2 and Flyway (the rest of P3-4).** Large changes with little to show in a demo.
