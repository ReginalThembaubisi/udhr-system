# Stops anything listening on the backend/frontend dev ports, so you can
# start fresh without hunting for stray processes or "port already in use" errors.
$ports = 8085, 3000, 3001

foreach ($port in $ports) {
    $conns = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    foreach ($conn in $conns) {
        $procId = $conn.OwningProcess
        Write-Host "Stopping process $procId on port $port..."
        Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
    }
}

Write-Host "Done. Ports 8085, 3000 and 3001 are free."
