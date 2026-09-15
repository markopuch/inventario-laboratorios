[CmdletBinding()]
param(
    [switch]$CrearUsuariosDemo
)

$ErrorActionPreference = 'Stop'
$backendPath = Join-Path $PSScriptRoot 'backend\inventario'

if (-not (Test-Path -LiteralPath (Join-Path $backendPath 'gradlew.bat'))) {
    throw 'No se encontro backend\inventario\gradlew.bat junto a este script.'
}
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw 'Instala Java 21 y verifica que java -version funcione en esta terminal.'
}

if ([string]::IsNullOrWhiteSpace($env:DB_URL)) {
    $env:DB_URL = 'jdbc:postgresql://localhost:5432/inventario_laboratorios'
}
if ([string]::IsNullOrWhiteSpace($env:DB_USER)) {
    $databaseUser = Read-Host 'Usuario de PostgreSQL [postgres]'
    $env:DB_USER = if ([string]::IsNullOrWhiteSpace($databaseUser)) { 'postgres' } else { $databaseUser.Trim() }
}
if ([string]::IsNullOrWhiteSpace($env:DB_PASSWORD)) {
    $databasePassword = Read-Host 'Contrasena de PostgreSQL (no la del usuario marko)' -AsSecureString
    try {
        $env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $databasePassword).Password
    } finally {
        $databasePassword.Dispose()
    }
}
if ([string]::IsNullOrWhiteSpace($env:DB_PASSWORD)) {
    throw 'La contrasena de PostgreSQL es obligatoria.'
}

if ([string]::IsNullOrWhiteSpace($env:JWT_SECRET)) {
    $jwtKeyBytes = New-Object byte[] 32
    $jwtRandom = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $jwtRandom.GetBytes($jwtKeyBytes)
        $env:JWT_SECRET = [Convert]::ToBase64String($jwtKeyBytes)
    } finally {
        $jwtRandom.Dispose()
        [Array]::Clear($jwtKeyBytes, 0, $jwtKeyBytes.Length)
    }
}

$activeProfiles = @($env:SPRING_PROFILES_ACTIVE -split ',' | ForEach-Object { $_.Trim() } | Where-Object { $_ })
if ($CrearUsuariosDemo -and $activeProfiles -notcontains 'dev') {
    $activeProfiles += 'dev'
    $env:SPRING_PROFILES_ACTIVE = $activeProfiles -join ','
}
if ($activeProfiles -contains 'dev' -and [string]::IsNullOrWhiteSpace($env:DEMO_USER_PASSWORD)) {
    $demoPassword = Read-Host 'Contrasena inicial demo (las cuentas existentes no se modifican)' -AsSecureString
    try {
        $env:DEMO_USER_PASSWORD = [System.Net.NetworkCredential]::new('', $demoPassword).Password
    } finally {
        $demoPassword.Dispose()
    }
}

Write-Host 'Iniciando Inventario en http://localhost:8080 ...'
Write-Host 'Espera el mensaje Started InventarioApplication y deja esta terminal abierta.'
Write-Host 'Para detener el backend, presiona Ctrl+C.'

Push-Location -LiteralPath $backendPath
try {
    & .\gradlew.bat bootRun --no-daemon --console=plain
    if ($LASTEXITCODE -ne 0) {
        throw 'El backend no pudo iniciarse. Revisa el error mostrado arriba.'
    }
} finally {
    Pop-Location
}
