param(
    [string]$Mysql = 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe',
    [string]$HostName = '127.0.0.1',
    [int]$Port = 3306,
    [string]$User = 'root',
    [string]$Password = '123456',
    [switch]$SkipBackup
)

$ErrorActionPreference = 'Stop'
$databases = @('live_start', 'ds_user_0', 'ds_user_1', 'ds_order_0', 'ds_order_1', 'ds_seat_0', 'ds_seat_1')

function Invoke-MySql([string]$Sql) {
    $mysqlArgs = @(
        "--host=$HostName",
        "--port=$Port",
        "-u$User",
        "-p$Password",
        '--batch',
        '--raw',
        '--skip-column-names',
        '-e',
        $Sql
    )
    $result = & $Mysql @mysqlArgs
    if ($LASTEXITCODE -ne 0) { throw "MySQL failed: $Sql" }
    return @($result)
}

function Q([string]$Name) { return ('`' + $Name.Replace('`', '``') + '`') }

if (-not (Test-Path -LiteralPath $Mysql)) { throw "mysql.exe not found: $Mysql" }

if (-not $SkipBackup) {
    $backupNames = @($databases | ForEach-Object { "${_}_backup" })
    $backupList = ($backupNames | ForEach-Object { "'" + $_ + "'" }) -join ','
    $existing = @(Invoke-MySql "SELECT SCHEMA_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME IN ($backupList)")
    if ($existing.Count -gt 0) { throw "Backup already exists: $($existing -join ', '). Refuse to overwrite." }

    foreach ($database in $databases) {
        $backup = "${database}_backup"
        Invoke-MySql "CREATE DATABASE $(Q $backup) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci" | Out-Null
        $tables = @(Invoke-MySql "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA='$database' AND TABLE_TYPE='BASE TABLE' ORDER BY TABLE_NAME")
        foreach ($table in $tables) {
            $source = "$(Q $database).$(Q $table)"
            $target = "$(Q $backup).$(Q $table)"
            Invoke-MySql "CREATE TABLE $target LIKE $source; INSERT INTO $target SELECT * FROM $source" | Out-Null
        }
        Write-Host "BACKUP $database -> $backup"
    }
} else {
    Write-Host 'BACKUP_SKIPPED'
}

function Assert-Unique([string]$Database, [string]$Table, [string]$Column) {
    $sql = "SELECT COUNT(*) FROM (SELECT $(Q $Column) FROM $(Q $Database).$(Q $Table) GROUP BY $(Q $Column) HAVING COUNT(*) > 1) d"
    $duplicates = [int](Invoke-MySql $sql | Select-Object -First 1)
    if ($duplicates -gt 0) { throw "Duplicate groups found: $Database.$Table.$Column = $duplicates" }
}

function Add-Index([string]$Database, [string]$Table, [string]$IndexName, [string]$Definition) {
    $exists = [int](Invoke-MySql "SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='$Database' AND TABLE_NAME='$Table' AND INDEX_NAME='$IndexName'" | Select-Object -First 1)
    if ($exists -eq 0) {
        Invoke-MySql "ALTER TABLE $(Q $Database).$(Q $Table) ADD $Definition" | Out-Null
        Write-Host "INDEX $Database.$Table.$IndexName"
    }
}

foreach ($database in @('ds_user_0', 'ds_user_1')) {
    foreach ($i in 0..7) {
        Add-Index $database "t_user_$i" 'PRIMARY' 'PRIMARY KEY (`id`)'
        Add-Index $database "t_user_$i" 'idx_phone' 'KEY `idx_phone` (`phone`)'
        Add-Index $database "t_user_profile_$i" 'PRIMARY' 'PRIMARY KEY (`user_id`)'
        Add-Index $database "t_user_visitor_$i" 'PRIMARY' 'PRIMARY KEY (`id`)'
        Add-Index $database "t_user_visitor_$i" 'uk_user_card' 'UNIQUE KEY `uk_user_card` (`user_id`,`card_no_hash`,`del_flag`)'
    }
}

foreach ($database in @('ds_order_0', 'ds_order_1')) {
    foreach ($i in 0..15) {
        Assert-Unique $database "t_order_$i" 'id'
        Assert-Unique $database "t_order_$i" 'order_no'
        Assert-Unique $database "t_order_item_$i" 'id'
        Assert-Unique $database "t_order_item_$i" 'check_code'
        Assert-Unique $database "t_user_ticket_$i" 'id'
        Assert-Unique $database "t_user_ticket_$i" 'check_code'

        Add-Index $database "t_order_$i" 'PRIMARY' 'PRIMARY KEY (`id`)'
        Add-Index $database "t_order_$i" 'uk_order_no' 'UNIQUE KEY `uk_order_no` (`order_no`)'
        Add-Index $database "t_order_$i" 'idx_user_id' 'KEY `idx_user_id` (`user_id`)'
        Add-Index $database "t_order_item_$i" 'PRIMARY' 'PRIMARY KEY (`id`)'
        Add-Index $database "t_order_item_$i" 'uk_check_code' 'UNIQUE KEY `uk_check_code` (`check_code`)'
        Add-Index $database "t_order_item_$i" 'idx_order_no' 'KEY `idx_order_no` (`order_no`)'
        Add-Index $database "t_order_item_$i" 'idx_user_id' 'KEY `idx_user_id` (`user_id`)'
        Add-Index $database "t_user_ticket_$i" 'PRIMARY' 'PRIMARY KEY (`id`)'
        Add-Index $database "t_user_ticket_$i" 'uk_check_code' 'UNIQUE KEY `uk_check_code` (`check_code`)'
        Add-Index $database "t_user_ticket_$i" 'idx_user_id' 'KEY `idx_user_id` (`user_id`)'
    }
}

foreach ($database in @('ds_seat_0', 'ds_seat_1')) {
    foreach ($i in 0..7) {
        Add-Index $database "t_seat_$i" 'PRIMARY' 'PRIMARY KEY (`id`)'
        Add-Index $database "t_seat_$i" 'idx_event_sku_section' 'KEY `idx_event_sku_section` (`event_id`,`sku_id`,`section`)'
        Add-Index $database "t_seat_$i" 'idx_row_col' 'KEY `idx_row_col` (`row_num`,`col_num`)'
    }
}

Write-Host 'BACKUP_AND_REPAIR_DONE'
