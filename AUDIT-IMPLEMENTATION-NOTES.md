# UDHR — meaningful audit-trail implementation

**Base:** `UDHR-UI-Standardized.zip` (latest approved frontend). **Scope:** audit features only; existing application API routes and clinical rules retained.

## Implemented

- Maintains stable audit action codes internally, while the SYSTEM_ADMIN screen displays plain-English event **titles**, understandable **summaries**, status, user, facility, relevant patient/record reference and timestamp. Click a record for additional details.
- Audit records carry actor type, actor label snapshot, facility label, entity type/ID and outcome in new nullable database columns. Existing audit log rows are retained. Existing response fields remain for compatibility.
- Added audit events for vitals, labs, immunization administration/missed/manual entries, referrals and responses, visits and discharges, front-desk check-ins, queue changes, the second dispensing workflow, chronic-condition additions, medication-adherence updates, symptom-check records, patient portal sign-ins and profile views, stock item creation, announcements and staff directory access.
- Successful management-report views and CSV exports are recorded. The existing report, security, login, stock and prescription auditing remains in place.
- Expired-stock receiving and dispensing blocks generate separately committed **failed events**. API role/access denials are logged with method, path and actor where available.
- Removed 10-minute suppression of patient access records for covered patient-data URLs; reads now record each successful covered request.
- Search in the administrator audit page supports date, natural-language activity, staff number, patient MRN, staff role, outcome; database-backed pagination (50 per page, max 200) and expandable details.
- Clinical before/after snapshots and old sensitive medical-note fields are **not returned to the general system-admin view**. Technical codes remain internal. Existing audit records are not modified or deleted; JPA pre-update/pre-delete hooks reject application-level changes.
- IP attribution uses the direct remote address rather than an untrusted X-Forwarded-For header.

## Installation / testing on Windows

1. Extract this ZIP **into a separate folder**. Keep your previously working standardized copy.
2. **Back up your test database** before first startup: the new nullable audit columns will be added by Hibernate (`ddl-auto=update`). No manual data-wiping SQL is needed.
3. Start MySQL using XAMPP. In the extracted root, start Spring Boot with your normal local `JWT_SECRET` and other *development-only* environment settings using `mvn spring-boot:run`.
4. Open a separate terminal in `frontend` and run `npm.cmd ci` then `npm.cmd run dev`.
5. Log in as SYSTEM_ADMIN. Open **Audit trail**, search, expand events and test the actions below using *fictional patients only*.

## Manual verification checklist (required before merge)

- Log in/out as staff; check successful and failed events have readable titles and correct actor.
- Create a patient, record vitals, lab result, immunization, diagnosis and prescription. Verify their activity and MRN are present; ensure clinical notes aren't exposed in the administrator audit UI.
- Create and respond to referrals; check in, call, send to pharmacy, complete/cancel queue events; verify recorded statuses.
- Use **both** pharmacy dispensing paths and stock receipt/adjustment. Confirm meaningful events and quantity changes.
- Try to receive or dispense an expired tracked stock item. Confirm the operation is still blocked and that a failed audit record remains afterward.
- View/download each management report; create/unpublish/delete an announcement; list staff and reset a test password. Verify corresponding events.
- Test role-based authorization denials and search filters/pagination; verify the audit history is readable on a laptop-sized screen.
- Verify application startup and complete frontend build: `mvn test` and `npm.cmd run build`.

## Known limits — do not claim production-complete compliance

- Source Java files parse successfully, and the React JSX parses; a **real Maven compile/test and Vite build were not possible** in the artifact environment (dependencies and Maven unavailable). Run both on your PC.
- Most newly added clinical events record the activity and record identifier, **not exact private medical values**; only authorised clinical workflows should expose those. Old stored clinical detail remains in the underlying database and requires a migration/privacy review.
- Audit failure handling still logs an error for failed audit writes rather than guaranteeing persistence for *all* successful actions. Some writes are not enclosed with business actions in a single transaction, so strict audit-data atomicity and alerting need further engineering and testing.
- Application-level append-only hooks **do not protect against privileged direct SQL modifications**. Database permissions, off-host/append-only archival, retention, and tamper-evident storage should be added before any real-world deployment.
- Existing pharmacy **batch/expiry handling** and the separate dispensing workflow behaviour have not been changed; audit events do not fix those previously identified stock-safety issues.
- Frontend existing functionality, screens and API handlers are preserved apart from the audit-trail screen. No group database dump is included.
