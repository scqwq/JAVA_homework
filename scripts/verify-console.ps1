param(
    [string]$PostgresHost = '127.0.0.1',
    [int]$PostgresPort = 5432,
    [string]$PostgresUser = 'postgres',
    [string]$PostgresPassword = $env:PGPASSWORD,
    [string]$PostgresBin = $env:PG_BIN
)

$ErrorActionPreference = 'Stop'
$consoleEncoding = [System.Text.Encoding]::GetEncoding(936)
[Console]::InputEncoding = $consoleEncoding
[Console]::OutputEncoding = $consoleEncoding
$OutputEncoding = $consoleEncoding

if ([string]::IsNullOrWhiteSpace($PostgresPassword)) {
    throw 'Set PGPASSWORD or pass -PostgresPassword before running this script.'
}

$projectRoot = Split-Path -Parent $PSScriptRoot
$psql = if ($PostgresBin) {
    Join-Path $PostgresBin 'psql.exe'
} else {
    'G:\develop\PostgreSQL\bin\psql.exe'
}
$javaHome = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'User')
$mavenHome = [Environment]::GetEnvironmentVariable('MAVEN_HOME', 'User')
$maven = Join-Path $mavenHome 'bin\mvn.cmd'

if (-not (Test-Path $psql)) { throw "psql not found: $psql" }
if (-not (Test-Path (Join-Path $javaHome 'bin\java.exe'))) { throw "Java not found under JAVA_HOME: $javaHome" }
if (-not (Test-Path $maven)) { throw "Maven not found under MAVEN_HOME: $mavenHome" }

$database = "java_homework_console_$([guid]::NewGuid().ToString('N'))"
$tempRoot = Join-Path $env:TEMP "JAVA_homework_console_$([guid]::NewGuid().ToString('N'))"
$env:PGPASSWORD = $PostgresPassword
$env:JAVA_HOME = $javaHome
$env:Path = "$(Join-Path $javaHome 'bin');$(Join-Path $mavenHome 'bin');$env:Path"

function Invoke-PsqlCommand {
    param([string]$Database, [string]$Command)

    $result = & $psql -h $PostgresHost -p $PostgresPort -U $PostgresUser -d $Database -v ON_ERROR_STOP=1 -tAc $Command
    if ($LASTEXITCODE -ne 0) { throw "psql command failed against $Database" }
    return ($result | Out-String).Trim()
}

function Invoke-PsqlFile {
    param([string]$Database, [string]$Path)

    & $psql -h $PostgresHost -p $PostgresPort -U $PostgresUser -d $Database -v ON_ERROR_STOP=1 -f $Path | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "psql file import failed: $Path" }
}

function Assert-Contains {
    param([string]$Text, [string]$Expected)

    if (-not $Text.Contains($Expected)) {
        throw "Expected console output to contain: $Expected"
    }
}

function Convert-CodePoints {
    param([int[]]$CodePoints)

    return -join ($CodePoints | ForEach-Object { [char]$_ })
}

function Get-Scalar {
    param([string]$Command)

    return Invoke-PsqlCommand -Database $database -Command $Command
}

try {
    Invoke-PsqlCommand -Database 'postgres' -Command "CREATE DATABASE $database" | Out-Null
    New-Item -ItemType Directory -Path $tempRoot | Out-Null
    robocopy $projectRoot $tempRoot /E /XD .git .claude target /XF .env | Out-Null
    if ($LASTEXITCODE -gt 7) { throw "Project copy failed with exit code $LASTEXITCODE" }

    $dsn = "jdbc:postgresql://${PostgresHost}:${PostgresPort}/${database}?user=${PostgresUser}&password=${PostgresPassword}"
    [System.IO.File]::WriteAllText((Join-Path $tempRoot '.env'), "DATABASE_DSN=$dsn`n")

    Push-Location $tempRoot
    try {
        $startupOutput = ('0' | & $maven -q compile exec:java 2>&1 | Out-String)
        if ($LASTEXITCODE -ne 0) { throw 'Application failed while creating the temporary schema.' }
    } finally {
        Pop-Location
    }
    $systemTitle = Convert-CodePoints @(0x5B66, 0x751F, 0x5BBF, 0x820D, 0x7BA1, 0x7406, 0x7CFB, 0x7EDF)
    $exitMessage = Convert-CodePoints @(0x5DF2, 0x9000, 0x51FA, 0x7CFB, 0x7EDF, 0x3002)
    Assert-Contains -Text $startupOutput -Expected $systemTitle
    Assert-Contains -Text $startupOutput -Expected $exitMessage

    $seedFile = Join-Path $projectRoot 'src\main\resources\sql\init.sql'
    Invoke-PsqlFile -Database $database -Path $seedFile
    Invoke-PsqlFile -Database $database -Path $seedFile

    $m1BuildingId = Get-Scalar "SELECT building_id FROM buildings WHERE building_code = 'M1'"
    $f1RoomId = Get-Scalar "SELECT r.room_id FROM rooms r JOIN buildings b ON b.building_id = r.building_id WHERE b.building_code = 'F1' AND r.room_number = '101'"
    $m1RoomId = Get-Scalar "SELECT r.room_id FROM rooms r JOIN buildings b ON b.building_id = r.building_id WHERE b.building_code = 'M1' AND r.room_number = '101'"
    $m1Room201Id = Get-Scalar "SELECT r.room_id FROM rooms r JOIN buildings b ON b.building_id = r.building_id WHERE b.building_code = 'M1' AND r.room_number = '201'"

    $inputLines = @(
        'abc',
        '2', '2', '', '0',
        '1', '3', '20240005', '', '0',
        '4', '3', $m1RoomId, '',
        '1', '20240001', $m1BuildingId, $m1RoomId, '3', '',
        '1', '20240005', $m1BuildingId, $m1RoomId, '1', '',
        '1', '20240006', $m1BuildingId, $m1RoomId, '4', '',
        '1', '20240005', $m1BuildingId, $f1RoomId, '3', '',
        '1', '20240005', $m1BuildingId, $m1RoomId, '3', '',
        '2', '20240005', $m1BuildingId, $m1Room201Id, '1', '',
        '0',
        '1', '1', '20249999', 'RegressionStudent', 'TestClass', '2024', 'MALE', '',
        '1', '20249999', 'DuplicateStudent', 'TestClass', '2024', 'MALE', '',
        '0',
        '3', '1', '101', $m1BuildingId, '1', '',
        '0', '0'
    )

    Push-Location $tempRoot
    try {
        $consoleOutput = ($inputLines | & $maven -q exec:java 2>&1 | Out-String)
        if ($LASTEXITCODE -ne 0) { throw 'Console workflow exited with a non-zero code.' }
    } finally {
        Pop-Location
    }

    @(
        (Convert-CodePoints @(0x8BF7, 0x8F93, 0x5165, 0x6574, 0x6570, 0x3002)),
        ((Convert-CodePoints @(0x697C, 0x53F7)) + '=M1'),
        (Convert-CodePoints @(0x8BE5, 0x5B66, 0x751F, 0x6682, 0x672A, 0x5206, 0x914D, 0x5BBF, 0x820D, 0x3002)),
        ((Convert-CodePoints @(0x5B66, 0x53F7)) + '=20240001'),
        (Convert-CodePoints @(0x8BE5, 0x5B66, 0x751F, 0x5DF2, 0x5206, 0x914D, 0x5BBF, 0x820D, 0xFF0C, 0x8BF7, 0x4F7F, 0x7528, 0x8C03, 0x5BBF, 0x529F, 0x80FD)),
        (Convert-CodePoints @(0x8BE5, 0x5E8A, 0x4F4D, 0x5DF2, 0x88AB, 0x5360, 0x7528)),
        (Convert-CodePoints @(0x8BE5, 0x5BBF, 0x820D, 0x697C, 0x4E0D, 0x5141, 0x8BB8, 0x5F53, 0x524D, 0x5B66, 0x751F, 0x6027, 0x522B, 0x5165, 0x4F4F)),
        (Convert-CodePoints @(0x623F, 0x95F4, 0x4E0D, 0x5C5E, 0x4E8E, 0x8BE5, 0x5BBF, 0x820D, 0x697C)),
        (Convert-CodePoints @(0x5206, 0x914D, 0x6210, 0x529F, 0x3002)),
        (Convert-CodePoints @(0x8C03, 0x5BBF, 0x6210, 0x529F, 0x3002)),
        ((Convert-CodePoints @(0x65B0, 0x589E, 0x6210, 0x529F)) + ': RegressionStudent'),
        ((Convert-CodePoints @(0x65B0, 0x589E, 0x5931, 0x8D25)) + ': ' + (Convert-CodePoints @(0x5B66, 0x53F7, 0x5DF2, 0x5B58, 0x5728)) + ': 20249999'),
        ((Convert-CodePoints @(0x65B0, 0x589E, 0x5931, 0x8D25)) + ': ' + (Convert-CodePoints @(0x8BE5, 0x5BBF, 0x820D, 0x697C, 0x5185, 0x623F, 0x95F4, 0x53F7, 0x5DF2, 0x5B58, 0x5728)) + ': 101'),
        $exitMessage
    ) | ForEach-Object { Assert-Contains -Text $consoleOutput -Expected $_ }

    $counts = Get-Scalar "SELECT (SELECT count(*) FROM buildings) || ',' || (SELECT count(*) FROM rooms) || ',' || (SELECT count(*) FROM students) || ',' || (SELECT count(*) FROM dorm_assignments)"
    if ($counts -ne '3,4,7,5') { throw "Expected final counts 3,4,7,5; got $counts" }

    $movedDorm = Get-Scalar "SELECT b.building_code || ',' || r.room_number || ',' || d.bed_number FROM dorm_assignments d JOIN buildings b ON b.building_id = d.building_id JOIN rooms r ON r.room_id = d.room_id WHERE d.student_id = '20240005'"
    if ($movedDorm -ne 'M1,201,1') { throw "Expected 20240005 to move to M1,201,1; got $movedDorm" }

    Write-Output 'Console verification passed.'
} finally {
    if (Test-Path $tempRoot) {
        Remove-Item -LiteralPath $tempRoot -Recurse -Force
    }
    & $psql -h $PostgresHost -p $PostgresPort -U $PostgresUser -d postgres -v ON_ERROR_STOP=1 -c "DROP DATABASE IF EXISTS $database WITH (FORCE)" | Out-Null
}
