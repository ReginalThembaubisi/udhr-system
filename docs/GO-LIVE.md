# UDHR System — Go-Live Checklist

Do every step here before the first real patient is entered. The live demo at https://udhr-system.onrender.com runs with demo settings on purpose; these steps switch it to real use.

## 1. Accounts

- [ ] **Two system admins.** Sign in as `SYSADMIN001`, open Accounts → Create account, and choose the role System administrator. Give the account to a second trusted person. If one admin loses their phone, the other can reset it under Manage → "Reset sign-in app (lost phone)". The system refuses to deactivate the last active system admin.
- [ ] **Change `SYSADMIN001`'s password** if it was ever the seeded `SysAdmin@123` or was shared during the demo.
- [ ] Both system admins have set up an authenticator app. This is compulsory for system admins, and the system asks for it at first sign-in.

## 2. Render settings (Environment tab of the `udhr-system` service)

| Variable | Set to | Why |
| --- | --- | --- |
| `STAFF_2FA` | `required` | Every staff member must use an authenticator app. A stolen password alone can't open patient records. |
| `DEMO_ACCOUNTS` | delete it | Turns off the demo logins and their published passwords. |
| `SYSADMIN_2FA` | leave unset | The default (`required`) keeps two-step compulsory for system admins. |
| `SYSADMIN_RECOVERY`, `SYSADMIN_INITIAL_PASSWORD` | leave unset | Only for an emergency (see below). |
| `JWT_SECRET` | **never change** | Authenticator setups are encrypted with it. Changing it signs everyone out and makes all staff set up their app again. |

Saving these restarts the app. Staff are asked to set up their authenticator app the next time they sign in.

## 3. Hosting

- [ ] **Database on a paid plan, with backups.** The current database is a free one outside Render. Before real data, move to a paid plan with daily backups, either with that provider or a Render Postgres in Frankfurt next to the app. Point `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD` at it.
- [ ] **App on a paid Render plan.** The free plan sleeps when idle, so the first sign-in of the day waits about a minute.
- [ ] Check that the database is stored in a region you're allowed to use for South African health data (POPIA, section 72).

## 4. Data

- [ ] Remove the demo patients, visits and test stock (for example the 120 °C vitals entered before validation existed), or start from an empty database.
- [ ] Load the real pharmacy stock, so doctors see true availability when they prescribe.

## Emergency: the only system admin is locked out or hacked

1. On Render, set `SYSADMIN_RECOVERY` to the staff number (for example `SYSADMIN001`) and `SYSADMIN_INITIAL_PASSWORD` to a temporary password of at least 12 characters. Save, and the app restarts.
2. The account is unlocked and reactivated, its authenticator app is removed, and every device signed in to it is signed out. The reset is recorded in the audit log as `EMERGENCY_RECOVERY`.
3. Sign in with the temporary password. You'll be asked for a new password and a new authenticator app.
4. **Delete both variables.** A given temporary password only works once, but don't leave them set.
