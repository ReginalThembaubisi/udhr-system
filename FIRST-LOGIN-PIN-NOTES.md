# UDHR — temporary portal PINs and first-login credentials

## What changed

### New patient registration

- The receptionist registers a patient using the existing **Front Desk → Register** form.
- When the form sends no `portalPin` (the default UI behavior), the backend uses `PortalPinGenerator` and `SecureRandom` to issue a random six-digit **temporary** portal PIN. Leading zeros are preserved.
- The PIN is hashed with BCrypt before the new patient is stored. The plaintext PIN is added **only to the successful registration response**, under `temporaryPortalPin`.
- The receptionist sees a one-time handover card with the generated PIN, patient name and MRN. They should share it privately with the patient and dismiss the card. It is not saved in browser local storage or audit trails.
- The patient signs in with their ID number, date of birth and temporary PIN. Until they choose a new, different six-digit PIN, the patient portal and backend API refuse access to clinical data.
- The patient changes the PIN using `/api/patient/me/change-pin`. The app updates the session, and patient access becomes available. PIN hashes are stored, not the PIN itself.
- Clinic personnel can still set/reset a PIN from **Visitors**, after identity verification; that PIN is now temporary as well. Reset PINs also require the patient to change them.
- Existing patients are **not automatically forced** to change their PIN. The new nullable `patients.must_change_portal_pin` field is treated as false for existing records.
- Legacy API clients that explicitly submit an existing patient-chosen `portalPin` still work and do not receive a generated temporary PIN. The current front-desk UI uses the generated-PIN flow.

### New staff accounts

- On `POST /api/staff`, `mustChangePassword=true` is explicitly set for all new staff (Doctor, Nurse, Pharmacist, Reception and System Administrator).
- The existing forced-password-change screen is reused after first sign-in, with the server-side JWT restriction blocking other API access until completion.
- A new password must be at least eight characters and **cannot be identical to the temporary/current password**.
- Existing staff accounts are **not retroactively flagged**; a System Administrator password reset already sets that flag.

## Files changed

- Backend: `PatientService`, `PatientPortalService`, `StaffService`, `AuthService`, `TokenService`, `JwtUtil`, `JwtAuthenticationFilter`, `Patient`, `PatientController`, `PatientPortalController`, `AuthController`, `PatientLoginResponse`, new `PortalPinGenerator`.
- Frontend: `frontend/src/App.jsx` (registration one-time PIN handover, patient PIN gate, forms and session handling).
- Tests: `FirstLoginCredentialsTest`, `PasswordChangeSessionTest`, `AccessControlTest`.

## Database note

**Back up `udhr_db` before running this version.** On startup the current development setting `spring.jpa.hibernate.ddl-auto=update` adds the nullable `patients.must_change_portal_pin` field. Do not use automatic schema updates in a production environment without a reviewed migration.

## Local test checklist (fictional patient data only)

1. Stop the older backend, start MySQL and run this project's backend from `udhr-system-main` (folder containing `pom.xml`) with your private `JWT_SECRET` configured.
2. Run `npm.cmd ci` and `npm.cmd run dev` in `udhr-system-main/frontend`.
3. Log in as the front-desk role and register a **new test patient**, leaving the optional PIN unset (the front-desk UI has no manual new-patient PIN entry now).
4. Confirm the six-digit PIN is shown **once**. Dismiss the panel, navigate away and confirm it does not reappear.
5. Log in to the patient portal with the ID, DOB and temporary PIN; confirm that medical records are blocked until the PIN is changed.
6. Try the same PIN as the new PIN; confirm rejection. Choose a new nontrivial six-digit PIN; confirm access is allowed after the change.
7. Sign out and check the old temporary PIN no longer signs in; the new PIN does.
8. As System Administrator, create a **new test staff** account with a temporary password. Sign in as that staff member; confirm the forced change-password screen appears and other pages are blocked until the password changes.
9. Confirm existing staff and existing patient accounts still sign in as before; check audit events for successful and denied actions (no plaintext PIN).

## Validation status

- Java source parsing succeeded for all 237 application and test `.java` files.
- The new `PortalPinGenerator` compiled and executed under Java 21.
- `frontend/src/App.jsx` parsed without JSX syntax errors; existing stylesheets parsed successfully.
- Full Maven integration tests and Vite build **have not been run** in this environment. Run them on your Windows PC before merging. As this project handles sensitive clinical data, this is not a production certification.

Keep the project limited to local test data until the security controls, account provisioning and operational processes are reviewed.
