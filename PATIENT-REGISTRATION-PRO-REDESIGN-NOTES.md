# UDHR — Front Desk Patient Registration Redesign

Starting ZIP: UDHR-Two-Column-Patient-Registration.zip (November 4, 2026).

## Interface improvements
- Replaced the cramped registration card with a dedicated front-desk registration workspace.
- The patient lookup panel is hidden only while the new registration form is displayed; closing registration returns to the search view.
- Improved page hierarchy: title, supporting description, clearly labelled sections for Identification, Personal details, Contact & Visit, and Privacy & Consent.
- Preserved the requested two equal-width input fields per row on desktop and added responsive single-column layout on mobile.
- Improved field labels, required markers, focus states, spacing, accessibility label associations, and submit/cancel actions.
- Optional contact, next-of-kin and newborn information stays in the existing collapsible section, now clearly styled and labelled.
- Patient portal six-digit PIN is still generated only by the existing backend on registration.
- Retained all input state keys, input handlers, API calls, validators, consent handling, registration behaviour, and other screens.

## Files
Updated: frontend/src/App.jsx
Added: frontend/src/udhr-registration.css
No Java/backend file changed.

## Running locally (from udhr-system-main/frontend)
- `npm.cmd ci` (first time)
- `npm.cmd run dev` (frontend, default localhost:3000)
- `npm.cmd run build` to verify production frontend build.

Note: JSX parsed successfully with the TypeScript parser and the source ZIP passed file checks. A full dependency-based Vite build could not be completed in this environment because npm installation timed out. Functional/browser QA should be completed locally.
