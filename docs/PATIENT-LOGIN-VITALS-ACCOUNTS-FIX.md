# UDHR patient login, vitals, and patient-account oversight update

## Patient login / XAMPP MySQL

Symptom: A newly registered patient cannot log in, and the database returns `Column 'staff_id' cannot be null` while inserting into `refresh_tokens`.

Cause: Earlier versions of the schema marked `refresh_tokens.staff_id` as `NOT NULL`, although patient tokens have a `patient_id` and do not have a staff member. Hibernate's automatic schema update does not relax existing NOT NULL constraints.

Fix: `SchemaPatchRunner` now inspects that column when the Java backend starts and, for an existing MySQL/MariaDB schema, makes `staff_id` nullable without deleting token rows. PostgreSQL retains its existing migration. If the database account does not have ALTER permission, the server console reports the failure. A DBA can run the equivalent statement manually after taking a backup:

```sql
ALTER TABLE refresh_tokens MODIFY COLUMN staff_id BIGINT NULL;
```

**Important:** Restart the Java backend. Refreshing only the browser or restarting only the frontend does not apply the database patch. The registered patient should use their temporary six-digit PIN and then set a new one on first sign-in. Keep `DEMO_ACCOUNTS=enabled` only with fictional demo data.

## Vitals

Blood pressure now only accepts digits and one `/` (example `120/80`); letters and extra punctuation are rejected during typing/pasting. The existing server-side range and format validation stays in place.

## Audit trail and accounts

* Staff registration of a patient produces a `REGISTER_PATIENT` audit event with actor, patient record, outcome and timestamp, and a `PATIENT_PORTAL_ACCOUNT_CREATED` audit event for the account. PINs, ID numbers and birth dates are not included in these new event descriptions.
* The System Administrator can view a **Patient accounts** directory under **Accounts**. It lists MRN, name, registration time and portal PIN status, with search and pagination and a shortcut to that patient's audit history. No PIN hashes, identity numbers, medical notes or other clinical details are sent to this page.
* The **Audit trail** now has quick filters for patient registrations, patient portal account creation and all events, plus a Patient portal actor-role filter.
* Existing patient records remain in the database and appear in the accounts directory. Historic events that were never saved because of earlier database/audit errors cannot be retroactively reconstructed.

## Local start (from `udhr-system-main`)

Run MySQL in XAMPP. In PowerShell backend terminal: set your existing private `JWT_SECRET` in that terminal, then run `mvn.cmd spring-boot:run`. In a second terminal: `cd .\\frontend`, `npm.cmd install`, `npm.cmd run dev`. Visit `http://localhost:3000/`. Both backend and frontend must be running.
