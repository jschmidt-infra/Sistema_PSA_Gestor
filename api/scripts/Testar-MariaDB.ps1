$ErrorActionPreference = 'Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
$nome = 'psa-db-test-' + [Guid]::NewGuid().ToString('N').Substring(0,12)
$senha = [Guid]::NewGuid().ToString('N')
$criado = $false
try {
    docker run -d --name $nome -e "MARIADB_ROOT_PASSWORD=$senha" -p '127.0.0.1::3306' mariadb:13.0.2 | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Não foi possível criar o MariaDB isolado.' }
    $criado = $true
    $pronto = $false
    for ($i=0; $i -lt 60; $i++) {
        docker exec $nome sh -c 'healthcheck.sh --connect --innodb_initialized >/dev/null 2>&1' | Out-Null
        if ($LASTEXITCODE -eq 0) { $pronto=$true; break }
        Start-Sleep -Seconds 2
    }
    if (!$pronto) { throw 'MariaDB de testes não ficou pronto.' }
    $sql = "CREATE DATABASE psa_auth_test; CREATE DATABASE psa_fluxo_test; CREATE USER 'psa_test'@'%' IDENTIFIED BY '$senha'; GRANT ALL ON psa_auth_test.* TO 'psa_test'@'%'; GRANT ALL ON psa_fluxo_test.* TO 'psa_test'@'%';"
    $sql | docker exec -i -e "MYSQL_PWD=$senha" $nome mariadb -uroot
    if ($LASTEXITCODE -ne 0) { throw 'Falha na preparação dos bancos isolados.' }
    $porta = ((docker port $nome 3306/tcp).Trim() -split ':')[-1]
    $env:TEST_DB_URL = "jdbc:mariadb://127.0.0.1:$porta/psa_auth_test"
    $env:TEST_FLOW_DB_URL = "jdbc:mariadb://127.0.0.1:$porta/psa_fluxo_test"
    $env:TEST_DB_USERNAME = 'psa_test'
    $env:TEST_DB_PASSWORD = $senha
    if (!$env:JAVA_HOME) { $env:JAVA_HOME = 'C:/Program Files/Java/jdk-25.0.4.1' }
    $env:MAVEN_USER_HOME = "$PWD/tmp/api-bootstrap/wrapper-home"
    & ./mvnw.cmd '-Dmaven.repo.local=tmp/api-bootstrap/repository' -B -ntp clean verify
    if ($LASTEXITCODE -ne 0) { throw 'A verificação falhou. Consulte os relatórios em target/surefire-reports.' }
} finally {
    if ($criado) { docker rm -fv $nome | Out-Null }
    'TEST_DB_URL','TEST_FLOW_DB_URL','TEST_DB_USERNAME','TEST_DB_PASSWORD' | ForEach-Object { Remove-Item "Env:$_" -ErrorAction SilentlyContinue }
    Pop-Location
}
