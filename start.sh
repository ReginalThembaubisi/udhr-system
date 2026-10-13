#!/usr/bin/env bash
# One-command local dev startup: MySQL (docker), Spring Boot backend, Vite frontend.
set -e
cd "$(dirname "$0")"

echo "==> Starting MySQL..."
docker compose up -d db

echo "==> Waiting for MySQL to be ready..."
until docker compose exec -T db mysqladmin ping -h 127.0.0.1 --silent >/dev/null 2>&1; do
  sleep 2
done

# The app refuses to start without a JWT secret, on purpose — a default baked
# into the committed code would let anyone who reads this public repository mint
# a valid token for any role. This is a throwaway value for local development
# only; production sets its own via the JWT_SECRET environment variable.
export JWT_SECRET="${JWT_SECRET:-local-development-only-secret-not-for-production-use}"

echo "==> Starting backend (Spring Boot) on http://localhost:8085 ..."
mvn -q spring-boot:run > backend.log 2>&1 &
BACKEND_PID=$!

echo "==> Installing frontend dependencies (if needed)..."
cd frontend
if [ ! -d node_modules ]; then
  npm install
fi

echo "==> Starting frontend (Vite) on http://localhost:3000 ..."
npm run dev > ../frontend.log 2>&1 &
FRONTEND_PID=$!
cd ..

cleanup() {
  echo ""
  echo "==> Stopping backend and frontend..."
  kill "$BACKEND_PID" "$FRONTEND_PID" 2>/dev/null || true
}
trap cleanup EXIT INT TERM

echo ""
echo "======================================================"
echo "  Frontend:  http://localhost:3000"
echo "  Backend:   http://localhost:8085"
echo ""
echo "  Test logins:"
echo "    Front desk:  ADMIN001    / Admin@123"
echo "    System admin: SYSADMIN001 / SysAdmin@123"
echo "    Doctor:     DOC001   / Doctor@123"
echo "    Nurse:      NUR001   / Nurse@123"
echo "    Pharmacist: PHARM001 / Pharmacist@123"
echo "    Patient:    ID number 9001015000083 (Reginal Themba, no password)"
echo ""
echo "  Logs: backend.log / frontend.log"
echo "  Press Ctrl+C to stop everything."
echo "======================================================"

wait
