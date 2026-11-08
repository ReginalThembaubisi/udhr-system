# UDHR: examiner demo script

Follows the order of the final testing rubric (45 marks). Each section says
what to click, what the examiner should see, and which code to show.

Live site: https://udhr-system.onrender.com
Demo logins: DOC001 / Doctor@123, NUR001 / Nurse@123, PHARM001 / Pharmacist@123,
ADMIN001 / Admin@123 (front desk), SYSADMIN001 (system admin, your own password).
Demo patient: ID 9001015000083, date of birth 1 Jan 1990, PIN 246810 (or reset it with "Forgot your PIN?").

Have two browser windows open: one as the system admin (to read demo text
messages and emails in **Security alerts → Demo inbox**), one for the role you are showing.

---

## 1. User interfaces (10 marks): the checklist

| Checklist item | What to show |
|---|---|
| 0.1.1 / 0.1.5 Standard link colours | Links ("Forgot your password?", "Back to sign in") are blue and underlined. |
| 0.1.2 / 0.1.3 Field and prompt colours | White fields, dark labels above each field, the same on every screen. |
| 0.1.4 Non-editable fields | Read-only or disabled fields are grey with a dashed border and a "not allowed" cursor. |
| 0.1.6 Standard buttons | One blue main button per form, white secondary buttons, all the same height. |
| 0.1.7 / 0.1.8 Background | Plain light background, no distracting images behind forms. |
| 0.2.1 / 0.2.2 / 0.2.5 One font | Inter everywhere: headings, labels, fields, buttons. |
| 0.2.3 Content stays when you go back | Type in a form, switch tab, press the browser **Back** button: you're on the previous screen and what you typed is still there. |
| 0.2.4 / 0.2.6 Alignment | All headings and text are left-aligned. |
| 0.2.7 Second word in lowercase | Sentence case: "Staff number", "New password", "Add stock item". (Product names keep their capitals: Google Authenticator.) |

## 2. Functionality (10 marks): one patient through the whole clinic

1. **Front desk (ADMIN001):** search the patient, check them in. Show "Reset PIN" after checking ID.
2. **Nurse (NUR001):** Vitals, pick the patient from the queue, record blood pressure and so on. Abnormal values are flagged.
3. **Doctor (DOC001):** Today's patients, open the patient, diagnose and prescribe. Allergy warnings appear before prescribing.
   - Open a patient who isn't on today's list: the system asks for a **reason**, which is saved in the audit trail.
4. **Pharmacist (PHARM001):** Dispense. The quantity is worked out ("20 tablets"), and stock goes down by itself.
   - **Update stock** shows: delivery arrived, remove (with a reason), I counted the shelf, fix expiry.
5. **Patient portal:** sign in with the ID, date of birth and PIN. Show the record, medications, symptom checker, food label scan and chatbot.
6. **Security:**
   - **Forgot your PIN?** sends a code by text (read it in the demo inbox).
   - **Forgot your password?** sends a code by email.
   - **New device:** a patient on a new phone needs a texted code; staff need the authenticator app.

## 3. Database connection (5 marks): typing limits and error prevention

**Typing limits:** every text box has a limit that matches its database column:
- names 100, emails 120, phone numbers 15, passwords 128
- notes 2,000 (TEXT columns), photo link 255
- the ID number is cut at 13 digits and its date of birth fills in by itself

Try typing a long name: it stops at 100.

**Error prevention** (refused before anything is saved):
- Stock: 500001 is refused (more than 10,000 at once); stock can't go below zero; expired stock can't be received.
- Removing stock with no reason is refused.
- Blood pressure must look like 120/80; numbers have minimums and maximums.
- Prescribing something the patient is allergic to needs an override reason.
- Wrong password: one message ("Invalid staff number or password"), and 5 tries lock the account for 15 minutes.

Code: `frontend/src/App.jsx` (search `maxLength`), `src/main/java/com/udhr/service/StockService.java` (`MAX_SINGLE_CHANGE`, `requireReason`).

## 4. Data and error handling (10 marks)

### Where is the path to the database stored?

`src/main/resources/application.properties`:

```properties
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:mysql://localhost:3306/udhr_db}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:root}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:}
```

The live address is **not in the code**. It comes from the `SPRING_DATASOURCE_URL` setting on
the Render dashboard (Environment tab), with a local default for development. The password is set
the same way, so it never appears in the public repository.

Show it in the app: **System admin → System status**. It shows "Connected", the database software, the
address (password hidden), where it's configured, and how many patients, staff, visits, stock items
and audit entries are stored.

### What happens if the database is empty?

The system fills itself in. `src/main/java/com/udhr/config/DataSeeder.java` checks each table on start-up
(`if (patientRepository.count() == 0)` and so on) and adds the facilities, demo staff, demo patient,
medication list, health tips and food ingredient warnings. A brand-new database is ready to demo
in one start-up. It never overwrites data that's already there.

### What happens if the database is moved or can't be reached?

- Every screen shows one red banner: *"The database can't be reached right now, so nothing was saved
  or loaded. Try again in a minute. If it keeps happening, tell the system administrator."*
- Nothing is half-saved: requests are stopped before they start.
- No error codes, SQL or table names are shown (they'd help an attacker).
- **System status** shows "Not reachable". When the database comes back, the banner disappears by itself.
- Anyone can check https://udhr-system.onrender.com/api/system-status. It says
  `"database": "connected"` or `"unavailable"`, and nothing else about the setup.

How to show it: run the system on a laptop and stop the database service, or point
`SPRING_DATASOURCE_URL` at a wrong address. If the address is wrong **when the system starts**, it
refuses to start and the log says why. It never runs half-working.

Code to show:
- `src/main/java/com/udhr/health/DatabaseHealth.java`: checks the database every few seconds.
- `src/main/java/com/udhr/health/DatabaseGuardFilter.java`: stops requests and gives the friendly message.
- `src/main/java/com/udhr/health/DatabaseErrorAdvice.java`: turns any database error into a plain message.
- `src/main/java/com/udhr/health/SystemStatusController.java`: the status screen, with the password hidden.
- `frontend/src/lib/api.js` (`onDatabaseStatus`): the banner on every screen.

Tests: `src/test/java/com/udhr/security/DatabaseDownTest.java`.

## 5. Reports (10 marks)

- **System admin → Reports:**
  - prescriptions, dispensing and stock, each with a date range
  - CSV download (opens in Excel)
  - patients referred in and out
- **System admin → Audit trail:**
  - every sign-in, record opened, change and refusal: who, when, from where
  - filter by staff member or patient
  - "View activity" on any staff account
- **System admin → Security alerts:** worked out from the audit trail:
  - someone opening many records
  - records opened outside today's list (with the reasons given)
  - sign-ins from new places
  - locked accounts
  - High alerts are also emailed (demo inbox).
- **Pharmacist → Stock:** low stock, expired and expiring soon; each item's history with reasons.
- **System admin → System status:** record counts in the database.
