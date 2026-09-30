[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$backendPath = Join-Path $PSScriptRoot 'backend\inventario'

# Este script nunca crea ni elimina bases. La base de verificacion debe existir.
# Sin parametros JDBC adicionales: no se permite redirigir la conexion por URL.
$verificationUrlPattern = '^jdbc:postgresql://(?:localhost|127\.0\.0\.1|\[::1\])(?::[0-9]{1,5})?/inventario_verificacion_[A-Za-z0-9_]+$'
if ([string]::IsNullOrWhiteSpace($env:DB_URL) -or $env:DB_URL -cnotmatch $verificationUrlPattern) {
    throw 'DB_URL debe apuntar a una base local existente inventario_verificacion_*. La base habitual no esta permitida.'
}
foreach ($requiredVariable in @('DB_USER', 'DB_PASSWORD', 'DEMO_USER_PASSWORD')) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($requiredVariable, 'Process'))) {
        throw "Configura $requiredVariable en esta terminal. No escribas credenciales en el script."
    }
}
if (-not (Test-Path -LiteralPath (Join-Path $backendPath 'gradlew.bat'))) {
    throw 'No se encontro backend\inventario\gradlew.bat.'
}
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw 'Java 21 debe estar disponible en esta terminal.'
}

# Evita que una configuracion externa con mayor prioridad cambie el destino.
foreach ($overrideVariable in @('SPRING_APPLICATION_JSON', 'SPRING_CONFIG_LOCATION',
        'SPRING_CONFIG_ADDITIONAL_LOCATION', 'SPRING_CONFIG_NAME', 'JAVA_TOOL_OPTIONS',
        'JDK_JAVA_OPTIONS', 'JAVA_OPTS', 'GRADLE_OPTS')) {
    if (-not [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($overrideVariable, 'Process'))) {
        throw "Ejecuta la verificacion en una terminal sin $overrideVariable para garantizar la configuracion de prueba."
    }
}

$managedVariables = @('DEBUG', 'JWT_SECRET', 'SPRING_PROFILES_ACTIVE',
    'SPRING_DATASOURCE_URL', 'SPRING_DATASOURCE_USERNAME', 'SPRING_DATASOURCE_PASSWORD',
    'SPRING_FLYWAY_URL', 'SPRING_FLYWAY_USER', 'SPRING_FLYWAY_PASSWORD',
    'SPRING_FLYWAY_ENABLED', 'SPRING_FLYWAY_CLEAN_DISABLED',
    'SPRING_JPA_HIBERNATE_DDL_AUTO', 'SPRING_JPA_OPEN_IN_VIEW')
$previousEnvironment = @{}
foreach ($variableName in $managedVariables) {
    $previousEnvironment[$variableName] = [Environment]::GetEnvironmentVariable($variableName, 'Process')
}

try {
    # gradlew.bat activa eco de comandos cuando DEBUG esta definido.
    Remove-Item Env:DEBUG -ErrorAction SilentlyContinue
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
    $env:SPRING_PROFILES_ACTIVE = 'dev'
    $env:SPRING_DATASOURCE_URL = $env:DB_URL
    $env:SPRING_DATASOURCE_USERNAME = $env:DB_USER
    $env:SPRING_DATASOURCE_PASSWORD = $env:DB_PASSWORD
    $env:SPRING_FLYWAY_URL = $env:DB_URL
    $env:SPRING_FLYWAY_USER = $env:DB_USER
    $env:SPRING_FLYWAY_PASSWORD = $env:DB_PASSWORD
    $env:SPRING_FLYWAY_ENABLED = 'true'
    $env:SPRING_FLYWAY_CLEAN_DISABLED = 'true'
    $env:SPRING_JPA_HIBERNATE_DDL_AUTO = 'validate'
    $env:SPRING_JPA_OPEN_IN_VIEW = 'false'

    Write-Host 'Verificando compileJava, test y bootJar en la base temporal configurada...'
    Push-Location -LiteralPath $backendPath
    try {
        # Reejecuta la suite para no confundir resultados en cache con evidencia nueva.
        & .\gradlew.bat compileJava test bootJar --rerun-tasks --no-daemon --console=plain
        if ($LASTEXITCODE -ne 0) {
            throw 'La compilacion o la suite fallo. Revisa build/reports/tests/test/index.html.'
        }
        $reports = @(Get-ChildItem -LiteralPath 'build/test-results/test' -Filter 'TEST-*.xml')
        if ($reports.Count -eq 0) {
            throw 'No se generaron reportes JUnit; no se puede confirmar la verificacion.'
        }
        $totals = @{ tests = 0; failures = 0; errors = 0; skipped = 0 }
        foreach ($report in $reports) {
            [xml]$document = Get-Content -LiteralPath $report.FullName -Raw
            foreach ($field in @('tests', 'failures', 'errors', 'skipped')) {
                $totals[$field] += [int]$document.testsuite.$field
            }
        }
        if ($totals.tests -eq 0 -or $totals.failures -ne 0 -or $totals.errors -ne 0 -or $totals.skipped -ne 0) {
            throw 'La suite debe ejecutar pruebas y terminar sin fallos, errores ni omitidas.'
        }
        Write-Host ("Verificacion correcta: {0} pruebas, 0 fallos, 0 errores, 0 omitidas." -f $totals.tests)
        Write-Host 'JAR generado en backend/inventario/build/libs. La base temporal permanece para su revision y limpieza.'
    } finally {
        Pop-Location
    }
} finally {
    foreach ($variableName in $managedVariables) {
        [Environment]::SetEnvironmentVariable($variableName, $previousEnvironment[$variableName], 'Process')
    }
}
