# UDHR Reports & Patient Search Fixes

Base project: UDHR-Meaningful-Audit-Trails.zip (preserved, including its audit backend and standardized UI).

## 1. Empty reports
On System Administrator > Reports, the Download CSV action is disabled and visually muted when the corresponding report has no records. This applies independently to Prescriptions, Dispensing, Stock, Lab Results and Referrals; reports with records remain exportable. Disabled buttons show the explanation 'No records available to export' on hover. The counts are taken from the existing report summary endpoints; no report API or CSV logic changed.

## 2. Patient search inputs
The Doctor and Pharmacist search boxes no longer begin with a sample ID. Returning to the queue clears the old ID so the next lookup begins blank. The corresponding role navigation resets lookup text when starting a fresh lookup, and the Nurse/Reception/Referrals lookup fields suppress browser autocomplete. Patient selection from existing queues and subsequent record refreshes still work with their existing identifiers internally. Reception Visitors clears its previous lookup when the page is opened again.

## Scope of modifications
Only frontend/src/App.jsx and frontend/src/udhr-polish.css were modified. No backend, database, role, API, audit, or dependency files were altered.

## Validation
- JSX parsed successfully with the TypeScript compiler's JSX parser.
- All five frontend stylesheets parsed successfully with PostCSS.
- Static assertions confirmed each of the five report export controls is tied to the corresponding record count, and both hardcoded patient ID defaults are removed.
- A full Vite build / interactive verification could not be completed here because this environment could not install npm dependencies. The project must be run and manually tested on the Windows development PC.

## Quick Windows test
1. Keep your local MySQL and Spring Boot backend from the meaningful-audit version running.
2. Extract this ZIP into a separate folder. From its udhr-system-main\frontend folder, run `npm.cmd ci` then `npm.cmd run dev`.
3. Log in as System Administrator and go to Reports. Confirm a report showing 0 records has a disabled CSV control; a report with records can still download CSV.
4. Log in as Doctor and Pharmacist and confirm the patient lookup box is empty. Select a patient from the queue, return to the queue, and confirm the search box is blank.
5. Check Nurse, Reception and Referrals search inputs for old IDs when opening a fresh lookup.

Note: If a different frontend is running on port 3000, stop it before testing this version.
