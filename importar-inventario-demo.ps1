[CmdletBinding()]
param(
    [string]$Archivo = (Join-Path $env:USERPROFILE ('Downloads\INVENTARIO Equipos grandes con c' + [char]0x00F3 + 'digo.xlsx')),
    [switch]$Aplicar
)

$ErrorActionPreference = 'Stop'
$pythonBundled = Join-Path $env:USERPROFILE '.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe'
$pythonCommand = Get-Command python -ErrorAction SilentlyContinue
$pythonImport = if (Test-Path -LiteralPath $pythonBundled) { $pythonBundled } elseif ($pythonCommand) { $pythonCommand.Source } else { $null }
if (-not $pythonImport) { throw 'Se necesita Python con openpyxl para leer el Excel.' }
if (-not (Test-Path -LiteralPath $Archivo -PathType Leaf)) { throw 'No se encontro el Excel indicado.' }

$sourceHash = (Get-FileHash -LiteralPath $Archivo -Algorithm SHA256).Hash.ToLowerInvariant()
$planDirectory = Join-Path $env:LOCALAPPDATA ('inventario-laboratorios\importacion-demo\' + $sourceHash.Substring(0, 12))
$planFile = Join-Path $planDirectory 'plan.json'
New-Item -ItemType Directory -Path $planDirectory -Force | Out-Null
$importIdentitySid = [System.Security.Principal.WindowsIdentity]::GetCurrent().User.Value
& icacls.exe $planDirectory '/inheritance:r' '/grant:r' ('*' + $importIdentitySid + ':(OI)(CI)F') | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'No se pudo proteger la carpeta privada de importacion.' }
& $pythonImport (Join-Path $PSScriptRoot 'scripts\preparar_inventario_demo.py') --archivo $Archivo --output $planDirectory
if ($LASTEXITCODE -ne 0) { throw 'La preparacion fallo; no se modifico la base.' }
if (-not $Aplicar) {
    Write-Host 'Preparacion completada sin conectar ni escribir en la base de datos.'
    Write-Host 'Para cargar, repite este comando con -Aplicar. Se solicitara acceso local.'
    return
}

try {
    $localHealth = Invoke-RestMethod -Uri 'http://127.0.0.1:8080/actuator/health' -TimeoutSec 8
    if ($localHealth.status -ne 'UP') { throw 'El backend no esta saludable.' }
} catch {
    throw 'Inicia el backend local en otra terminal con iniciar-backend.ps1 y espera Started InventarioApplication antes de cargar.'
}

$pgDump = 'C:\Program Files\PostgreSQL\18\bin\pg_dump.exe'
$pgRestore = 'C:\Program Files\PostgreSQL\18\bin\pg_restore.exe'
if (-not (Test-Path -LiteralPath $pgDump) -or -not (Test-Path -LiteralPath $pgRestore)) {
    throw 'No se encontraron pg_dump/pg_restore de PostgreSQL 18. Hace falta un respaldo verificable antes de cargar.'
}
Write-Host 'Destino: API local 127.0.0.1:8080 y respaldo de inventario_laboratorios en PostgreSQL local:5432.'
Write-Host 'El backend debe estar iniciado con esa misma base local.'
$manifestImportFile = Join-Path $planDirectory 'manifest.json'
$backupFile = $null
if (Test-Path -LiteralPath $manifestImportFile) {
    $existingImportManifest = Get-Content -LiteralPath $manifestImportFile -Raw | ConvertFrom-Json
    if ($existingImportManifest.source_sha256 -ne $sourceHash -or $existingImportManifest.backup_sha256 -notmatch '^[a-f0-9]{64}$') {
        throw 'El manifiesto previo no corresponde a esta fuente o no tiene un respaldo valido.'
    }
    foreach ($backupCandidate in (Get-ChildItem -LiteralPath $planDirectory -Filter '*.backup' -File)) {
        if ((Get-FileHash -LiteralPath $backupCandidate.FullName -Algorithm SHA256).Hash.ToLowerInvariant() -eq $existingImportManifest.backup_sha256) {
            $backupFile = $backupCandidate.FullName
            break
        }
    }
    if (-not $backupFile) { throw 'Falta el respaldo original del manifiesto. No se inicia otra carga sobre el mismo plan.' }
    Write-Host 'Reanudacion: se conserva el respaldo original anterior a la carga.'
} else {
    $backupUser = Read-Host 'Usuario PostgreSQL para el respaldo [postgres]'
    if ([string]::IsNullOrWhiteSpace($backupUser)) { $backupUser = 'postgres' }
    $backupPassword = Read-Host 'Contrasena PostgreSQL (entrada oculta)' -AsSecureString
    $previousPgPassword = [Environment]::GetEnvironmentVariable('PGPASSWORD', 'Process')
    $previousPgTimeout = [Environment]::GetEnvironmentVariable('PGCONNECT_TIMEOUT', 'Process')
    $backupFile = Join-Path $planDirectory ('antes-carga-' + [DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss-fff') + '.backup')
    try {
        $env:PGPASSWORD = [System.Net.NetworkCredential]::new('', $backupPassword).Password
        $env:PGCONNECT_TIMEOUT = '10'
        & $pgDump -h 127.0.0.1 -p 5432 -U $backupUser -d inventario_laboratorios -w -Fc -f $backupFile
        if ($LASTEXITCODE -ne 0) { throw 'No se pudo completar el respaldo. No se ejecutara la importacion.' }
    } finally {
        [Environment]::SetEnvironmentVariable('PGPASSWORD', $previousPgPassword, 'Process')
        [Environment]::SetEnvironmentVariable('PGCONNECT_TIMEOUT', $previousPgTimeout, 'Process')
        $backupPassword.Dispose()
    }
}
& $pgRestore --list $backupFile | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'No se pudo verificar el respaldo. No se ejecutara la importacion.' }
$backupVerification = [ordered]@{
    sha256 = (Get-FileHash -LiteralPath $backupFile -Algorithm SHA256).Hash.ToLowerInvariant()
    format = 'PostgreSQL custom'
    verified_by = 'pg_restore --list'
}
$backupVerification | ConvertTo-Json | Set-Content -LiteralPath ($backupFile + '.verified.json') -Encoding UTF8
Write-Host 'Respaldo creado y listado correctamente. Se pediran ahora las claves de Marko y Aldo de la aplicacion.'
& $pythonImport (Join-Path $PSScriptRoot 'scripts\aplicar_inventario_demo.py') --plan $planFile --backup $backupFile --aplicar
if ($LASTEXITCODE -ne 0) { throw 'La carga no termino. Revisa el mensaje y el manifiesto local antes de reanudar.' }
