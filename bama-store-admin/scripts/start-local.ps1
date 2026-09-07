param([int]$Port = 8092)
$ErrorActionPreference = 'Stop'
$backendRoot = Split-Path $PSScriptRoot -Parent
Push-Location $backendRoot
try {
    if (!(Test-Path 'src/main/resources/application-local.yml')) { throw '缺少本地 MySQL 配置 application-local.yml' }
    if (!(Test-Path '.local/application-local.yml')) { throw '缺少本地微信配置 .local/application-local.yml' }
    if (!$env:JAVA_HOME -and (Test-Path 'C:\software\jdk\java17\jdk')) { $env:JAVA_HOME = 'C:\software\jdk\java17\jdk' }
    $runArguments = "--server.port=$Port --spring.config.additional-location=file:.local/application-local.yml --spring.sql.init.data-locations=optional:classpath:db/no-seed.sql --logging.level.root=WARN --logging.level.com.bama.store=WARN --logging.level.org.springframework.boot.autoconfigure.security=ERROR --mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.nologging.NoLoggingImpl"
    & mvn -q spring-boot:run '-Dspring-boot.run.profiles=local' "-Dspring-boot.run.arguments=$runArguments"
    if ($LASTEXITCODE -ne 0) { throw '后端启动失败，请查看上方错误' }
} finally { Pop-Location }
