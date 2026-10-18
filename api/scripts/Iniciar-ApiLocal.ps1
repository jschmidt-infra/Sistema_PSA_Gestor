param([switch]$CriarAdministrador)
$ErrorActionPreference = 'Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    if (!(Test-Path 'config/application-local.yml')) { throw 'Configure config/application-local.yml conforme docs/MARIADB_LOCAL.md.' }
    $env:BOOTSTRAP_ADMIN_ENABLED = 'false'
    if ($CriarAdministrador) {
        $env:BOOTSTRAP_ADMIN_NAME = Read-Host 'Nome do administrador'
        $env:BOOTSTRAP_ADMIN_EMAIL = Read-Host 'E-mail do administrador'
        $senha = Read-Host 'Senha (mínimo 12 caracteres e máximo 72 bytes UTF-8)' -AsSecureString
        $env:BOOTSTRAP_ADMIN_PASSWORD = [Net.NetworkCredential]::new('', $senha).Password
        if ($env:BOOTSTRAP_ADMIN_PASSWORD.Length -lt 12 -or [Text.Encoding]::UTF8.GetByteCount($env:BOOTSTRAP_ADMIN_PASSWORD) -gt 72) { throw 'Senha fora dos limites.' }
        $env:BOOTSTRAP_ADMIN_ENABLED = 'true'
    }
    if (!$env:JAVA_HOME) { $env:JAVA_HOME = 'C:/Program Files/Java/jdk-25.0.4.1' }
    if (!(Test-Path 'target/psa-gestor-api.jar')) { throw 'Compile primeiro com scripts/Testar-MariaDB.ps1.' }
    & "$env:JAVA_HOME/bin/java.exe" -jar target/psa-gestor-api.jar --spring.profiles.active=local,demonstracao,r2,r2-local
    if ($LASTEXITCODE -ne 0) { throw 'A API terminou com erro. Consulte a mensagem acima.' }
} finally {
    Remove-Item Env:BOOTSTRAP_ADMIN_PASSWORD -ErrorAction SilentlyContinue
    $env:BOOTSTRAP_ADMIN_ENABLED = 'false'
    Pop-Location
}
