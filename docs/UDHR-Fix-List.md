# UDHR System — Fix List for the Team

Oct 13, 2026 · @Themba

## How to use this list

There are 44 fixes below, found in a full audit on 13 October 2026. Every P0 and P1 item was reproduced on a running copy of the system, not just read in the code. Fix P0 before anything else: those let someone take over accounts or put a patient at risk.

- **P0** = security hole or patient-safety risk. Fix this week.
- **P1** = broken core flow or weak login. Fix next.
- **P2** = UX bugs that confuse staff or patients.
- **P3** = code quality, so the fixes stay fixed.

To claim a task, put your name in the **Owner** column and set **Status** to In progress. Work on your own branch, one fix (or one small group of related fixes) per pull request.

A fix is **done** when:

1. The "Done when" check in its row passes on a running app.
2. A test covers it (backend tests go in `src/test/java`, which does not exist yet; see P3).
3. The PR describes how to reproduce the bug before and confirm the fix after.
4. A second group member has reviewed it.

File paths are from the repo root. Seed logins for testing: `ADMIN001 / Admin@123`, `DOC001 / Doctor@123`, `NUR001 / Nurse@123`, `PHARM001 / Pharmacist@123`, `SYSADMIN001 / SysAdmin@123`; patient ID `9001015000083`, date of birth 1990-01-01.

## P0 — Critical security and patient safety

Ten fixes. Each one either lets the wrong person into accounts or records, or lets a dangerous medicine reach a patient unchecked.

| # | Problem | Where | Fix | Done when | Owner | Status |
| --- | --- | --- | --- | --- | --- | --- |
| P0-1 | Front desk (ADMIN) can create a SYSTEM\_ADMIN account and deactivate any account, including the system admin | `StaffService.java:20`, `SecurityConfig.java` rule for `/api/staff/**` | Only SYSTEM\_ADMIN may create, change role of, or deactivate staff. Remove the Staff tab from the front-desk screen | `ADMIN001` calling `POST /api/staff` with role SYSTEM\_ADMIN gets 403 |  | Not started |
| P0-2 | Doctor can prescribe Amoxicillin to a patient with a SEVERE penicillin allergy, no warning | `PrescriptionService.addPrescription` | Check the medicine against the patient's allergies (incl. drug classes like penicillin → amoxicillin). Block, or require an override reason that is saved and audited | Prescribing Amoxicillin to patient `9001015000083` shows a warning and cannot be sent without a reason |  | Not started |
| P0-3 | Pharmacist sees no allergies while dispensing | `PharmacyService.lookup`, `PharmacyLookupResponse`, dispense panel in `App.jsx` | Return allergies with the pharmacy lookup and show them as red chips at the top of the dispense screen | Opening Reginal Themba on the pharmacy screen shows "Penicillin allergy" |  | Not started |
| P0-4 | No screen anywhere to record an allergy (API exists, UI never calls it) | `AllergyController.java`, doctor record tabs in `App.jsx` | Add an Allergies tab (add / mark inactive) for doctor and nurse | A new allergy added in the UI appears on the doctor and pharmacy screens |  | Not started |
| P0-5 | After logout, the next person on the same computer sees the previous user's data (a patient saw another patient's chat message) | `handleLogout` in `App.jsx:269` | On logout clear all state; simplest is `window.location.replace('/')` after clearing storage | Patient A chats, logs out, patient B logs in: chat shows only the greeting |  | Not started |
| P0-6 | A pharmacist at another facility can change your stock and dispense your prescriptions | `StockService.java:68,93`, `PharmacyService.dispense` (`:89`), `DispenseService.dispense` | Check that the stock item / prescription belongs to the logged-in staff member's facility | Pharmacist from facility 2 adjusting facility 1 stock or dispensing its prescription gets 403 |  | Not started |
| P0-7 | Patient login is ID number + date of birth, and an SA ID number already contains the date of birth | `PatientPortalService.java:50` | Add a second factor: SMS one-time code (Africa's Talking is already wired in) or a PIN set at the clinic | Knowing only an ID number is not enough to log in |  | Not started |
| P0-8 | Front desk can read referral clinical summaries (diagnosis, HIV status etc.) | `SecurityConfig.java:97`, `ReferralController` | Front desk gets counts and destinations only; clinical summary only for DOCTOR / NURSE | `ADMIN001` calling `GET /api/referrals/patient/{id}` gets no clinical summary |  | Not started |

Two more P0 fixes came from testing the patient health assistant with 15 questions. "I'm thinking about suicide" got a cheerful greeting back.

| # | Problem | Where | Fix | Done when | Owner | Status |
| --- | --- | --- | --- | --- | --- | --- |
| P0-9 | Self-harm and emergencies are missed. "I'm thinking about suicide" → greeting; "I want to end my life" and "I'm pregnant and bleeding" → "I didn't catch a specific symptom". The list has "suicidal" but not "suicide" | `EMERGENCY_PHRASES` in `ChatbotService.java` | Widen the list (suicide, end my life, hurt myself, pregnant + bleeding, chest pain, can't wake up…) and check it first. Reply with emergency steps and South African crisis numbers (ambulance 10177; SADAG suicide line 0800 567 567) | All three messages get the emergency reply with numbers |  | Not started |
| P0-10 | The greeting check matches "hi" or "hey" inside any word (which, this, think, child, HIV, they), so real questions get "Hi! I'm your health assistant". "Which medication should I avoid?" and "My child has a rash" both got the greeting | `containsAny(lower, "hi", "hello", "hey")` in `ChatbotService.respond` | Match whole words only (regex `\bhi\b`), and only when the message is just a greeting; move the check after the real intents | Those two questions get the medication-avoid and symptom replies |  | Not started |

## P1 — Session and login hardening

Eight fixes. Today a session ends at 30 minutes with errors instead of a login screen, so staff walk away from open sessions, and a 7-day refresh token is left in the browser.

| # | Problem | Where | Fix | Done when | Owner | Status |
| --- | --- | --- | --- | --- | --- | --- |
| P1-1 | After changing a password, the new token is rejected immediately ("Session has been ended"). Every new staff member hits this on first login | `TokenService.java:115` | JWT issue time is in whole seconds; `sessionsValidFrom` has fractions. Truncate `sessionsValidFrom` to seconds (or compare with 1 s tolerance) | Change password, then the next request with the returned token gets 200 |  | Not started |
| P1-2 | Frontend never uses the refresh token and ignores 401/403, so screens break at 30 minutes | `App.jsx` (75 raw `fetch` calls) | One shared `apiFetch` helper: on 401 call `/api/auth/refresh` once and retry; if that fails, clear state and show login | Leave a session idle 31 minutes; next click either works or lands on the login screen |  | Not started |
| P1-3 | Access and refresh tokens are kept in `localStorage`, readable by any script on the page | `handleLogin` in `App.jsx`, `AuthController` | Send the refresh token as an `HttpOnly; Secure; SameSite=Strict` cookie; keep the access token in memory only | `localStorage` holds no tokens after login |  | Not started |
| P1-4 | No auto-logout on shared clinic computers; closing the tab leaves the session alive | `App.jsx` | Idle timer: warning at 10 minutes of no activity, logout at 12 | An idle session logs itself out and revokes its tokens on the server |  | Not started |
| P1-5 | No limit on wrong passwords or wrong dates of birth | `AuthService.login`, `PatientPortalService.login` | Lock for 15 minutes after 5 failures, per account and per IP; write each lockout to the audit log | 6th wrong attempt is refused even with the right password, and shows in the audit trail |  | Not started |
| P1-6 | Login errors reveal which staff numbers exist ("Staff not found" vs "Invalid password") | `AuthService.login` | Always return "Invalid staff number or password" | Both cases return the same text |  | Not started |
| P1-7 | No browser security headers on the app | `SecurityConfig.java` | Add Content-Security-Policy (`script-src 'self'`), Strict-Transport-Security, `X-Frame-Options: DENY`, Referrer-Policy | `curl -I` on the deployed site shows all four headers |  | Not started |
| P1-8 | Pharmacist can create patient visits | `SecurityConfig.java:80` | Limit `POST /api/visits` to DOCTOR and NURSE | Pharmacist `POST /api/visits` gets 403 |  | Not started |

## P1 — Pharmacy and stock

Eight fixes. The pharmacy is only half connected to stock: in testing, dispensing Paracetamol through "Mark dispensed" left stock at 100, and the other path counted the same prescription twice.

| # | Problem | Where | Fix | Done when | Owner | Status |
| --- | --- | --- | --- | --- | --- | --- |
| P1-9 | Two dispensing paths that disagree. "Mark dispensed" closes the prescription but never touches stock. "Log dispense event" reduces stock but asks for a Prescription ID the screen never shows, and works on already-dispensed prescriptions | `PharmacyService.dispense`, `DispenseService.dispense`, dispense panel in `App.jsx` | One action per prescription: enter quantity → confirm. One backend endpoint marks it dispensed, reduces stock and records the dispense together | Dispensing 30 tablets once drops stock by exactly 30; a second dispense of the same prescription is refused |  | Not started |
| P1-10 | Dispensing doesn't check the prescription is still active, and two people can dispense the same item at once | `PharmacyService.dispense` | Add `@Transactional`, check `active` and end date, add `@Version` to `Prescription` and `StockItem` | Two simultaneous dispenses: one succeeds, one gets a clear error |  | Not started |
| P1-11 | Stock is matched to prescriptions by free text, so "Paracetamol" never reduces "Paracetamol 500mg" | `StockService.decrementForDispense`, prescription form | Add a medication list (formulary). Doctor picks from it; stock links by medication ID, not name | Prescribing from the list always reduces the matching stock item |  | Not started |
| P1-12 | Pharmacy queue says the prescription is "from Front Desk Clerk" (it shows whoever opened the visit) | `PharmacyService.getQueue`, dispense panel in `App.jsx` | Show the prescribing doctor from the prescription itself | Queue row shows the doctor's name |  | Not started |
| P1-13 | The same patient appears twice in the nurse, doctor and pharmacy queues when they have two open visits | `VisitService.findOrCreateOpenVisit`, `DataSeeder` | Allow one open visit per patient; group queue rows by patient | Each patient appears once per queue |  | Not started |
| P1-14 | Dispense screen doesn't show duration or total quantity to hand over, and shows raw codes like `SENT_TO_PHARMACY` | dispense panel in `App.jsx` | Show "7 days · 14 tablets" and plain-language status | A pharmacist can tell how much to give without asking |  | Not started |
| P1-15 | Each stock item has one expiry date, so receiving a new batch overwrites the old batch's date | `StockItem`, `StockService.receiveStock` | Track batches (lot number, expiry, quantity); dispense the earliest-expiring batch first | Two deliveries with different expiry dates are both visible and tracked |  | Not started |
| P1-16 | Pharmacist has no stock history or report, and low-stock alerts go only to the front desk | `StockService.sendReorderAlerts`, Stock panel in `App.jsx` | Add a history view per item and a low / expired / expiring list; send reorder alerts to the facility's pharmacists | Pharmacist sees why stock changed and gets the low-stock alert |  | Not started |

## P2 — Front desk, nurse, doctor and patient portal

Thirteen fixes to the day-to-day screens. In testing, vitals of BP "hello", 50 °C, pulse 300 and SpO2 150% were all accepted and sent to the doctor.

| # | Screen | Problem | Fix | Done when | Owner | Status |
| --- | --- | --- | --- | --- | --- | --- |
| P2-1 | Front desk | Checking in a patient who already has an open visit silently keeps the old visit and throws away the new reason, but says "Checked in" (`CheckInService.checkIn`) | Tell the clerk the patient is already in the queue and offer to update the reason | Second check-in shows "already waiting" and the reason is saved if updated |  | Not started |
| P2-2 | Front desk | No "already registered?" search before the registration form, so duplicates are easy | Search by ID / name + date of birth first; show the registration form only when nothing matches | Registering an existing ID number is caught before the form is filled |  | Not started |
| P2-3 | Front desk / System admin | Staff list says "this facility" but lists every facility (`StaffService.getAllStaff`) | Filter by the viewer's facility (system admin can switch) | List shows only that facility's staff |  | Not started |
| P2-4 | Nurse | Vitals accept impossible values and nothing is flagged as abnormal (`VitalsService.recordVitals`) | Validate ranges on server and form (e.g. temp 30–45 °C, SpO2 50–100%, BP as `120/80`); mark out-of-range values red | BP "hello" or SpO2 150 is refused; 39.5 °C is saved and shown as high |  | Not started |
| P2-5 | Nurse, pharmacy | Many fields are labelled only by placeholder text, which disappears as you type | Give every input a visible label | Every field still shows what it is after typing |  | Not started |
| P2-6 | Nurse | Vitals form doesn't show the patient's allergies or reason for visit | Show reason for visit and allergy chips above the form | Nurse sees both before taking vitals |  | Not started |
| P2-7 | Nurse | Referrals tab says "load a patient under Patients first", but the nurse has no Patients tab | Let the nurse search a patient inside the Referrals tab | Nurse can create a referral without a dead end |  | Not started |
| P2-8 | Doctor | The vitals just taken are hidden behind a tab | Show latest vitals (with abnormal flags) at the top of the consult | Doctor sees vitals without clicking |  | Not started |
| P2-9 | Doctor | No "finish consultation" step, no clear way to close a visit with no medicines, and "Add clinical alert" sits beside prescribing | Add Finish consultation (sends to pharmacy or discharges); move clinical alert to a secondary place | Every visit can be closed from the doctor screen |  | Not started |
| P2-10 | All queues | "Automatic" queues only update when you click a tab | Refresh queues every 20–30 seconds | A patient checked in at the front desk appears on the nurse screen without clicking |  | Not started |
| P2-11 | Patient portal (mobile) | Tab row is cut off with no scroll hint, chat bubble covers quick actions, sign-in form sits below the notice card | Scrollable tabs with fade edge or a menu; move the bubble; put sign-in first on small screens | Usable at 390 px width with no hidden controls |  | Not started |
| P2-12 | Patient portal | Referral endpoint returns an empty body when there is no referral, causing a JSON error on every load (`PatientPortalController.getReferral`) | Return 204 or `null` JSON; handle it in the frontend | No console error on portal load |  | Not started |
| P2-13 | Patient portal | Health assistant matches symptom words without understanding the sentence. "I don't have a fever" and "What temperature should I store my insulin at?" were both triaged YELLOW and saved to the patient's triage history (5 false records from 15 test messages). "Can I take ibuprofen with my metformin?" got no answer (`ChatbotService`) | Handle negation ("don't have", "no"); confirm symptoms before saving a triage record; for drug interaction questions reply "ask your pharmacist" or check the drug database | Negated or unrelated messages create no triage record; the interaction question gets a safe answer |  | Not started |

## P3 — Engineering quality

Five fixes so the work above stays fixed. The project has no tests, and CI builds with tests skipped.

| # | Problem | Where | Fix | Done when | Owner | Status |
| --- | --- | --- | --- | --- | --- | --- |
| P3-1 | No automated tests; CI runs `mvn -DskipTests` | `.github/workflows/build.yml`, `src/test/java` (missing) | Add Spring Boot tests for every P0/P1 fix (H2 or Testcontainers) and remove `-DskipTests` | CI fails if any P0/P1 bug comes back |  | Not started |
| P3-2 | Whole frontend is one 4,644-line `App.jsx` with \~150 `useState` calls | `frontend/src/App.jsx` | Split into one folder per role (front desk, nurse, doctor, pharmacy, patient, system admin) plus the shared `apiFetch` from P1-2 | No file over \~600 lines; each role's screen in its own folder |  | Not started |
| P3-3 | Two queue systems: `QueueController` / `QueueEntry` is never used; the app runs on visit status | `QueueController.java`, `QueueService.java`, `QueueEntry.java` | Delete the unused queue code (or switch to it fully, not both) | One queue model in the codebase |  | Not started |
| P3-4 | Production auto-modifies tables on startup and logs every SQL statement | `application.properties` (`ddl-auto=update`, `show-sql=true`) | Use Flyway migrations, `ddl-auto=validate`, `show-sql=false` in production | Deploy runs migrations; no SQL in production logs |  | Not started |
| P3-5 | Demo values are pre-filled in real search boxes (patient ID `9001015000083`, a barcode) | `App.jsx` lines 58, 82, 120 | Start search boxes empty; keep demo values only in placeholders | Search boxes are empty on load |  | Not started |

## Suggested split and order

Split the work into four workstreams so people don't edit the same files at once. Group size wasn't given, so merge or split these to match how many members you have.

| Workstream | Fixes | Main files | Skills needed |
| --- | --- | --- | --- |
| A. Access control (backend) | P0-1, P0-6, P0-8, P1-8, P2-3 | `SecurityConfig.java`, `StaffService`, `StockService`, `PharmacyService`, `ReferralController` | Spring Security, Java |
| B. Login and sessions | P0-5, P0-7, P1-1 to P1-7 | `AuthService`, `TokenService`, `PatientPortalService`, login/logout in `App.jsx` | Java + React |
| C. Allergy safety and pharmacy | P0-2, P0-3, P0-4, P1-9 to P1-16 | `PrescriptionService`, `PharmacyService`, `DispenseService`, `StockService`, doctor and pharmacy screens | Java + React, domain knowledge |
| D. Screens, UX and quality | P0-9, P0-10, P2-1, P2-2, P2-4 to P2-13, P3-1 to P3-5 | Nurse, doctor, front desk and patient screens, CI, `application.properties` | React, CSS, testing |

Order of work:

1. **Week 1:** all P0 items, plus P1-1 (password-change lockout) and P1-2 (refresh / redirect to login).
2. **Week 2:** rest of P1, with P3-1 tests written alongside each fix.
3. **Week 3:** P2 screen fixes, then P3-2 to P3-5.

Two dependencies to watch: P3-2 (splitting `App.jsx`) touches every screen, so do it last or agree a freeze first. P1-11 (medication list) changes the prescription form, so workstream C should finish it before D reworks the doctor screen.
