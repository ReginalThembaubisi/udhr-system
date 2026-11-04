# UDHR sign-in that stays on “Signing in…”

## What changed

- Staff/password, patient/PIN, and staff two-factor confirmation requests have a **25-second client-side deadline**. If a server request is stuck, the button becomes usable again and the app displays a helpful message. The server may still be processing the timed-out request, so do not immediately click repeatedly.
- The server still validates the account, password/PIN, lockouts, and 2FA exactly as before. No database records or credentials were reset.
- Vite now insists on port **3000**, so starting a second, older UDHR frontend will not silently move the new frontend to port 3001. Stop any old `npm.cmd run dev` process first.

## If sign-in times out

Check these **on the Windows PC running UDHR**, in a new PowerShell window:

```powershell
Test-NetConnection 127.0.0.1 -Port 3306
Test-NetConnection 127.0.0.1 -Port 8085
Test-NetConnection 127.0.0.1 -Port 3000
Get-CimInstance Win32_LogicalDisk -Filter "DeviceID='C:'" | Select-Object DeviceID,@{Name='FreeGB';Expression={[math]::Round($_.FreeSpace / 1GB,2)}}
```

All three connections should be `True`, with adequate disk space. A passing port check only proves that something is listening; it does **not** prove MySQL can complete queries.

The system previously logged **`C:\xampp\tmp\... Disk full; waiting for someone to free some space`** and **`There is not enough space on the disk`**. Free several GB on C: if necessary and restart MySQL and the Java backend. Do **not** delete `C:\xampp\mysql\data` or drop `udhr_db`.

Confirm the backend terminal shows `Started UdhrApplication` and no `Application run failed` message. In the backend terminal, watch the log immediately after clicking Sign in. Send the *error lines only*, without any passwords, JWT secrets, patient identity numbers, tokens or medical records, if you still cannot sign in.

## Starting the project

- Unzip the project, open its **`udhr-system-main`** folder in VS Code.
- Start MySQL using XAMPP. Ensure the existing, private `JWT_SECRET` is configured in the PowerShell session where you run the backend. For a fictional-data demo only, `DEMO_ACCOUNTS=enabled` and `SYSADMIN_2FA=optional` may be set.
- Backend PowerShell: `mvn.cmd spring-boot:run` (from `udhr-system-main`, where `pom.xml` exists).
- Frontend PowerShell: `cd .\frontend`, then `npm.cmd install` (if needed), then `npm.cmd run dev`.
- Open `http://localhost:3000/`.

The UI timeout is **not** a repair for a server or database that is out of disk space. It makes the problem visible rather than leaving the form spinning forever.
