# UDHR UI Standardization — 2026-11-03

## Base
This edition is based directly on `UDHR-UI-Redesign.zip` (the previously tested UI redesign), not on the older original ZIP. The original copy remains unchanged.

## Changes (appearance only)
- Standardized patient-lookup panels across Doctor, Nurse (Vitals/Lab Results), Pharmacist, Reception (Front Desk/Visitors), and Referrals, using the Doctor “Today's patients” search panel as the visual reference.
- Standardized field heights, search buttons, rounded cards, hover/focus states, label spacing and white/blue/teal surfaces across the existing staff workspaces.
- Reduced narrow desktop layouts so existing reception, stock and clinical panels make better use of available screen width.
- Added responsive layout rules for lookup cards, stock forms, lists and patient portal screens.
- Refreshed patient portal header, greeting and cards to match the shared design language.
- Preserved the last-sign-in card that is already integrated in the existing sidebar. No new profile or account-management functionality is included.

## Source files
- `frontend/src/App.jsx`: only wrappers, static labels, image tags, CSS classes and one CSS import.
- `frontend/src/udhr-standard.css`: new stylesheet for the shared visual pattern.

## Preservation / checks
- Backend, database migrations, authentication, routes, requests and endpoints: unchanged.
- `package.json` and `package-lock.json`: unchanged.
- Existing 94 `apiFetch` expressions and 267 JSX event-attribute bindings: unchanged, textually compared with the prior ZIP.
- JSX parsed/transpiled successfully using a TypeScript JSX parser and passed Node syntax validation after transpilation.
- All original and new CSS files parsed successfully with PostCSS.
- Full Vite build and browser workflow testing were **not** completed in the authoring environment because npm installation failed there. Run these on a Windows machine with working dependencies before merging with group work.

## Windows testing
1. Keep the existing backend and MySQL services running (typically `http://localhost:8085`). Use local development/test data only.
2. Open a terminal in the **new** project’s `frontend` directory.
3. Run `npm.cmd ci`, then `npm.cmd run dev`.
4. Open the local URL shown by Vite (normally `http://localhost:3000`).
5. Check at least one page in each role: Doctor (patients, alerts), Nurse (vitals, labs), Pharmacist (dispense, stock), Reception (front desk, visitors), System Admin (accounts, announcements, reports, audit), and Patient portal. Verify both desktop and mobile/tablet widths.
6. Check that all prior search, save, and navigation behaviors work as before. Test with fictional data only. Avoid pushing any ZIP directly over group changes until it has been reviewed.

## Security note
The three high-severity npm dependency audit warnings previously reported are unrelated to these visual changes. They were not automatically changed here. Review with the group before changing dependencies.
