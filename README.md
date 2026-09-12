# UDHR — Universal Digital Health Record System

Spring Boot backend + React (Vite) frontend for a clinic/hospital patient record,
symptom-triage, and medication-adherence system.

## Required configuration

The backend reads secrets and environment-specific settings from environment
variables (see `src/main/resources/application.properties`). None of these
have safe committed defaults for a shared or production environment.

| Variable | Required | Purpose |
|---|---|---|
| `JWT_SECRET` | **Yes** | Signs authentication tokens. The app refuses to start without it. Generate one with `openssl rand -base64 32` and keep it out of source control. |
| `DB_URL` | No (defaults to `jdbc:mysql://localhost:3306/udhr_db`) | MySQL connection string. |
| `DB_USERNAME` | No (defaults to `root`) | MySQL username. |
| `DB_PASSWORD` | No (defaults to empty) | MySQL password. |
| `CORS_ALLOWED_ORIGINS` | No (defaults to `http://localhost:3000`) | Comma-separated list of frontend origins allowed to call the API. |
| `SHOW_SQL` | No (defaults to `false`) | Set to `true` to log SQL locally for debugging. Leave off elsewhere — it can log patient data. |

## Running locally

```bash
# 1. Start MySQL and create the database
mysql -u root -e "CREATE DATABASE IF NOT EXISTS udhr_db"

# 2. Backend
export JWT_SECRET=$(openssl rand -base64 32)
./mvnw spring-boot:run

# 3. Frontend (separate terminal)
cd frontend
npm install
npm run dev
```

The frontend dev server proxies `/api` to `http://localhost:8085` (see
`frontend/vite.config.js`). In any deployment where the frontend and backend
are served from different origins, set `CORS_ALLOWED_ORIGINS` to the
frontend's real origin.

On first run against an empty database, `DataSeeder` seeds a demo facility,
three staff accounts (admin/doctor/nurse), and one demo patient. Check
`src/main/java/com/udhr/config/DataSeeder.java` for the seeded credentials —
they are intentionally not shown in the application UI.

## Known limitations

This is a teaching/demo project. Notably: there is no automated test suite,
the food-label scanner requires a working Tesseract installation to perform
real OCR (it fails explicitly rather than fabricating results if Tesseract
isn't available), and the symptom checker requires Infermedica API
credentials (`infermedica.app.id` / `infermedica.app.key`) for full triage —
without them it falls back to a conservative, limited local ruleset.
