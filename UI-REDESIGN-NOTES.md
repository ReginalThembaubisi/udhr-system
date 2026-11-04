# UDHR — Frontend UI Redesign (visual-only working copy)

This package is based on **udhr-system-main (2).zip**. The original backend,
Spring Boot configuration, database scripts, launch scripts, `package.json`,
and `package-lock.json` are unchanged.

## What changed

- `frontend/src/App.jsx`: Presentational layout and markup for the four staff
  workspaces (Doctor/Nurse, System Administrator, Pharmacist, Reception).
  Adds a shared healthcare brand header, visual icons/illustrations, better
  grouping of existing records, and moves the **existing last-sign-in notice**
  into the existing account/sidebar area. The notice retains its previous
  sign-in time, originating address, failed-attempt warning, and dismiss action.
- `frontend/src/udhr-polish.css`: A separate styling layer for colours, cards,
  typography, report layout, forms, navigation, empty states, focus states, and
  responsive screens. Original App.css remains untouched.
- `frontend/public/images/`: Three self-contained illustrative SVG assets.

## Preserved functionality

There are **no changes** to API paths, data fields, API calls, login logic,
role permissions, forms' submit handlers, CRUD operations, audits, dispensing,
stock calculations, validations, or backend/business logic.

The visual-only layout does not implement a new profile page. The existing
last-sign-in notice is displayed *within* the existing staff account card.

## How to run on Windows

1. Extract this ZIP into a **new** folder (do not overwrite your working copy).
2. Start MySQL in XAMPP as you did for the original system.
3. In a PowerShell terminal inside `frontend`, run `npm.cmd ci` then
   `npm.cmd run dev`.
4. In another PowerShell terminal in the project root, start the Spring Boot
   backend using your existing local configuration and a private `JWT_SECRET`.
5. Open the local address displayed by Vite (normally `http://localhost:3000`).

Do not commit `node_modules`, and use demonstration account settings only with
fictional/test data.

## Validation / known limits

- JSX parse check: passed (TypeScript JSX parser).
- CSS parse check: passed (tinycss2).
- SVG XML parse check: passed.
- Original-source comparison: only `frontend/src/App.jsx` has been changed;
  the new CSS/illustrations and this readme are additions.
- **Vite build and end-to-end interaction tests could not be run in the
  packaging environment** because the npm registry was unavailable (DNS
  `EAI_AGAIN`). These should be run on a computer with dependencies installed
  before merging or pushing the redesign to the team repository.

This is a visual implementation of the agreed design direction, not a claim
that all pages will be pixel-for-pixel identical to the concept art at every
screen size. Run and review it locally before replacing your team's version.
