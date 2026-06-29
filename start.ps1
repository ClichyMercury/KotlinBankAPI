# start.ps1 — démarre la stack FinSim (Docker + API) en une commande.
# Usage : .\start.ps1
$ErrorActionPreference = "Stop"

# Se placer dans le dossier du script (racine du projet)
Set-Location -Path $PSScriptRoot

# 1. JAVA_HOME — pointe sur le JDK 17 si pas déjà défini
if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
}
if (-not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    Write-Host "JDK 17 introuvable a $env:JAVA_HOME" -ForegroundColor Red
    Write-Host "Installe-le : winget install EclipseAdoptium.Temurin.17.JDK" -ForegroundColor Yellow
    exit 1
}
Write-Host "JAVA_HOME = $env:JAVA_HOME" -ForegroundColor DarkGray

# 2. Verifier que Docker repond
try { docker info *> $null } catch {
    Write-Host "Docker ne repond pas. Lance Docker Desktop puis reessaie." -ForegroundColor Red
    exit 1
}

# 3. Demarrer Postgres + Redis (docker ecrit sa progression sur stderr -> on la masque)
Write-Host "Demarrage de Postgres + Redis..." -ForegroundColor Cyan
docker compose up -d 2>$null

# 4. Attendre que les containers soient healthy (max ~60s)
Write-Host "Attente des containers healthy..." -ForegroundColor Cyan
$deadline = (Get-Date).AddSeconds(60)
do {
    Start-Sleep -Seconds 2
    $states = docker compose ps --format "{{.Health}}"
    $allHealthy = $states -and -not ($states | Where-Object { $_ -ne "healthy" })
    if ((Get-Date) -gt $deadline) {
        Write-Host "Timeout : containers pas healthy. Verifie 'docker compose ps'." -ForegroundColor Red
        exit 1
    }
} until ($allHealthy)
Write-Host "Postgres + Redis healthy." -ForegroundColor Green

# 5. Lancer l'API (foreground, Ctrl+C pour stopper)
Write-Host "Lancement de l'API sur http://localhost:8080 (Ctrl+C pour arreter)..." -ForegroundColor Cyan
.\gradlew.bat run
