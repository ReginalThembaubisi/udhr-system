# New Patient Registration — Two-column layout

This update continues from UDHR-Compact-Patient-Registration.zip and keeps the existing project files.

- All standard registration fields remain in two equal-width columns (left and right).
- Email and Address now sit side by side in the optional section.
- Next-of-kin fields remain side by side.
- Mother’s ID number occupies a single half-width field (not a stretched full-width field).
- Birth weight/length and Apgar 1/5-minute fields remain in paired columns.
- For patients under 18, the guardian-consent name field is half-width where space permits.
- The registration card maximum width is reduced to 840px to avoid unnecessarily long fields.
- On screens narrower than 720px, fields stack into one column for readability.
- Temporary PIN, patient registration, consent, and validation behavior were not modified.

Modified files:
- frontend/src/App.jsx
- frontend/src/udhr-standard.css

Note: Frontend JSX syntax was parsed successfully, but the full npm build could not be run here due to unavailable dependencies/network timeout. Please run `npm ci` and `npm run build` from `frontend/` when npm access is available.
