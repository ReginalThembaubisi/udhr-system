# One-command local dev startup for Windows: starts the backend and frontend
# each in their own window, so you don't have to type the same commands
# in fresh terminals every time.
$root = $PSScriptRoot

function Test-PortOpen($port) {
    (Test-NetConnection -ComputerName "127.0.0.1" -Port $port -WarningAction SilentlyContinue).TcpTestSucceeded
}

Write-Host "==> Checking MySQL (port 3306)..."
if (-not (Test-PortOpen 3306)) {
    Write-Host "MySQL isn't running on port 3306. Start it from the XAMPP Control Panel first, then re-run this script." -ForegroundColor Yellow
    exit 1
}

Write-Host "==> Checking backend port 8085..."
if (Test-PortOpen 8085) {
    Write-Host "Something is already listening on 8085 (likely a previous backend run)." -ForegroundColor Yellow
    Write-Host "Run stop.bat first if you want a clean restart." -ForegroundColor Yellow
} else {
    Write-Host "==> Starting backend in a new window..."
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$root'; mvn spring-boot:run"
}

Write-Host "==> Checking frontend port 3000..."
if (Test-PortOpen 3000) {
    Write-Host "Something is already listening on 3000 (likely a previous frontend run)." -ForegroundColor Yellow
    Write-Host "Run stop.bat first if you want a clean restart." -ForegroundColor Yellow
} else {
    Write-Host "==> Starting frontend in a new window..."
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$root\frontend'; if (!(Test-Path node_modules)) { npm install }; npm run dev"
}

Write-Host ""
Write-Host "======================================================"
Write-Host "  Frontend:  http://localhost:3000"
Write-Host "  Backend:   http://localhost:8085"
Write-Host ""
Write-Host "  Test logins:"
Write-Host "    Admin:      ADMIN001 / Admin@123"
Write-Host "    Doctor:     DOC001   / Doctor@123"
Write-Host "    Nurse:      NUR001   / Nurse@123"
Write-Host "    Pharmacist: PHARM001 / Pharmacist@123"
Write-Host "    Patient:    ID number 9001015000083 (no password)"
Write-Host ""
Write-Host "  Backend and frontend are each running in their own window."
Write-Host "  Run stop.bat to stop both."
Write-Host "======================================================"
