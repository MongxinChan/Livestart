param(
    [string]$DatabaseHost = "127.0.0.1",
    [int]$DatabasePort = 3306,
    [string]$DatabaseUser = "root",
    [Parameter(Mandatory = $true)]
    [string]$DatabasePassword,
    [ValidateSet("Auto", "Single", "Sharded")]
    [string]$PayMode = "Auto"
)

$ErrorActionPreference = "Stop"
$env:MYSQL_PWD = $DatabasePassword

function Invoke-MySqlScalar {
    param([string]$Sql)

    $mysqlArguments = @(
        "--host=$DatabaseHost",
        "--port=$DatabasePort",
        "--user=$DatabaseUser",
        "--batch",
        "--raw",
        "--skip-column-names",
        "-e",
        $Sql
    )
    $result = & mysql @mysqlArguments 2>$null
    if ($LASTEXITCODE -ne 0) {
        throw "MySQL query failed"
    }
    return [long]$result
}

$commonTables = @(
    "t_artist_commission_record",
    "t_comment",
    "t_event",
    "t_event_config",
    "t_event_performer",
    "t_event_sale_stage",
    "t_event_sale_stage_config",
    "t_event_sale_stage_sku",
    "t_event_style_relation",
    "t_event_ticket_stage",
    "t_invite_code",
    "t_invite_relation",
    "t_artist_wallet_account",
    "t_artist_wallet_ledger",
    "t_artist_withdrawal",
    "t_operation_log",
    "t_performer",
    "t_performer_style_relation",
    "t_refund_policy",
    "t_settlement",
    "t_settlement_notification_read",
    "t_stock_restore_task",
    "t_style",
    "t_ticket_reminder",
    "t_ticket_sku",
    "t_ticket_task",
    "t_user_phone_mapping",
    "t_venue"
)

$errors = [System.Collections.Generic.List[string]]::new()
foreach ($table in $commonTables) {
    $count = Invoke-MySqlScalar "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='live_start' AND TABLE_NAME='$table'"
    if ($count -ne 1) {
        $errors.Add("Missing table: live_start.$table")
    }
}

$sourceEventColumn = Invoke-MySqlScalar "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='live_start' AND TABLE_NAME='t_event' AND COLUMN_NAME='source_event_id'"
if ($sourceEventColumn -ne 1) {
    $errors.Add("Missing live_start.t_event.source_event_id; run sql/01_livestart_existing_db_upgrade.sql")
}
$sourceEventUniqueIndex = Invoke-MySqlScalar "SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='live_start' AND TABLE_NAME='t_event' AND INDEX_NAME='uq_source_event_id' AND COLUMN_NAME='source_event_id' AND NON_UNIQUE=0"
if ($sourceEventUniqueIndex -ne 1) {
    $errors.Add("Missing unique live_start.t_event.source_event_id index; run sql/01_livestart_existing_db_upgrade.sql")
}

$shardedPaySchemaCount = Invoke-MySqlScalar "SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME IN ('live_start_pay_0','live_start_pay_1')"
if ($PayMode -eq "Auto") {
    $PayMode = if ($shardedPaySchemaCount -eq 2) { "Sharded" } else { "Single" }
}
if ($PayMode -eq "Single") {
    foreach ($table in @("t_pay", "t_refund", "t_pay_outbox")) {
        $count = Invoke-MySqlScalar "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='live_start_pay' AND TABLE_NAME='$table'"
        if ($count -ne 1) {
            $errors.Add("Missing table: live_start_pay.$table")
        }
    }
} else {
    foreach ($schema in @("live_start_pay_0", "live_start_pay_1")) {
        foreach ($prefix in @("t_pay_", "t_refund_")) {
            $count = Invoke-MySqlScalar "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='$schema' AND TABLE_NAME REGEXP '^${prefix}([0-9]|1[0-5])$'"
            if ($count -ne 16) {
                $errors.Add("Unexpected $prefix shard count in ${schema}: expected 16, actual $count")
            }
        }
    }
    $outboxCount = Invoke-MySqlScalar "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='live_start_pay_0' AND TABLE_NAME='t_pay_outbox'"
    if ($outboxCount -ne 1) {
        $errors.Add("Missing table: live_start_pay_0.t_pay_outbox")
    }
}

$operationLogColumns = Invoke-MySqlScalar "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='live_start' AND TABLE_NAME='t_operation_log' AND COLUMN_NAME IN ('tenant','type','sub_type','biz_no','operator_id','operator_name','operation_log','original_data','modified_data','fail','create_time')"
if ($operationLogColumns -ne 11) {
    $errors.Add("Operation-log audit columns are incomplete")
}
$operationLogLength = Invoke-MySqlScalar "SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='live_start' AND TABLE_NAME='t_operation_log' AND COLUMN_NAME='operation_log'"
if ($operationLogLength -lt 512) {
    $errors.Add("Operation-log description column is shorter than 512 characters")
}

$venueOwnerUniqueIndex = Invoke-MySqlScalar "SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='live_start' AND TABLE_NAME='t_venue' AND INDEX_NAME='idx_owner_user_id' AND NON_UNIQUE=0"
if ($venueOwnerUniqueIndex -ne 1) {
    $errors.Add("Venue owner binding must have a unique idx_owner_user_id index")
}

$expectedPhysicalTables = @(
    @("ds_user_0", "^t_user(_profile|_visitor)?_[0-7]$", 24),
    @("ds_user_1", "^t_user(_profile|_visitor)?_[0-7]$", 24),
    @("ds_order_0", "^t_(order|order_item|user_ticket)_[0-9]+$", 48),
    @("ds_order_1", "^t_(order|order_item|user_ticket)_[0-9]+$", 48),
    @("ds_seat_0", "^t_seat_[0-7]$", 8),
    @("ds_seat_1", "^t_seat_[0-7]$", 8)
)
foreach ($physicalGroup in $expectedPhysicalTables) {
    $schema = $physicalGroup[0]
    $pattern = $physicalGroup[1]
    $expectedCount = [int]$physicalGroup[2]
    $actualCount = Invoke-MySqlScalar "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='$schema' AND TABLE_NAME REGEXP '$pattern'"
    if ($actualCount -ne $expectedCount) {
        $errors.Add("Unexpected physical table count in ${schema}: expected $expectedCount, actual $actualCount")
    }
}

foreach ($databaseIndex in 0..1) {
    foreach ($tableSpec in @(@("ds_user", "t_user", 8), @("ds_user", "t_user_profile", 8), @("ds_user", "t_user_visitor", 8), @("ds_order", "t_user_ticket", 16))) {
        $schema = "$($tableSpec[0])_$databaseIndex"
        $prefix = $tableSpec[1]
        $expected = [int]$tableSpec[2]
        $primaryCount = Invoke-MySqlScalar "SELECT COUNT(DISTINCT TABLE_NAME) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='$schema' AND TABLE_NAME REGEXP '^${prefix}_[0-9]+$' AND INDEX_NAME='PRIMARY'"
        if ($primaryCount -ne $expected) {
            $errors.Add("Missing primary keys in ${schema}.${prefix} shards: expected $expected, actual $primaryCount")
        }
    }
    $schema = "ds_order_$databaseIndex"
    $ticketCodeUniqueCount = Invoke-MySqlScalar "SELECT COUNT(DISTINCT TABLE_NAME) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='$schema' AND TABLE_NAME REGEXP '^t_user_ticket_[0-9]+$' AND COLUMN_NAME='check_code' AND NON_UNIQUE=0 AND SEQ_IN_INDEX=1"
    if ($ticketCodeUniqueCount -ne 16) {
        $errors.Add("Missing unique ticket-code indexes in ${schema}: expected 16, actual $ticketCodeUniqueCount")
    }
    $verifyRecordCount = Invoke-MySqlScalar "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='$schema' AND TABLE_NAME REGEXP '^t_order_item_[0-9]+$' AND COLUMN_NAME IN ('checked_at','checked_by')"
    if ($verifyRecordCount -ne 32) {
        $errors.Add("Missing ticket verification columns in ${schema}: expected 32, actual $verifyRecordCount")
    }
}

$userRouteChecks = @()
$userRows = @()
foreach ($databaseIndex in 0..1) {
    foreach ($tableIndex in 0..7) {
        foreach ($tableSpec in @(@("t_user", "id"), @("t_user_profile", "user_id"), @("t_user_visitor", "user_id"))) {
            $tablePrefix = $tableSpec[0]
            $shardingColumn = $tableSpec[1]
            $hash = "(((CAST($shardingColumn AS UNSIGNED) & 4294967295) ^ ((CAST($shardingColumn AS UNSIGNED) >> 32) & 4294967295)) & 2147483647)"
            $userRouteChecks += "SELECT COUNT(*) bad FROM ds_user_$databaseIndex.${tablePrefix}_$tableIndex WHERE MOD($hash,2)<>$databaseIndex OR MOD(FLOOR($hash/2),8)<>$tableIndex"
        }
        $userRows += "SELECT id,phone FROM ds_user_$databaseIndex.t_user_$tableIndex"
    }
}
$badUserRoutes = Invoke-MySqlScalar ("SELECT COALESCE(SUM(bad),0) FROM (" + ($userRouteChecks -join " UNION ALL ") + ") routes")
if ($badUserRoutes -ne 0) {
    $errors.Add("Misrouted user/profile/visitor rows: $badUserRoutes")
}

$allUsersSql = $userRows -join " UNION ALL "
$duplicatePhones = Invoke-MySqlScalar "SELECT COUNT(*) FROM (SELECT phone FROM ($allUsersSql) users GROUP BY phone HAVING COUNT(*) > 1) duplicates"
if ($duplicatePhones -ne 0) {
    $errors.Add("Duplicate phones across user shards: $duplicatePhones")
}
$invalidPhoneMappings = Invoke-MySqlScalar "SELECT COUNT(*) FROM ($allUsersSql) users LEFT JOIN live_start.t_user_phone_mapping mapping ON mapping.phone=users.phone AND mapping.user_id=users.id WHERE mapping.phone IS NULL"
if ($invalidPhoneMappings -ne 0) {
    $errors.Add("Users missing an exact global phone mapping: $invalidPhoneMappings")
}
$orphanPhoneMappings = Invoke-MySqlScalar "SELECT COUNT(*) FROM live_start.t_user_phone_mapping mapping LEFT JOIN ($allUsersSql) users ON users.phone=mapping.phone AND users.id=mapping.user_id WHERE users.id IS NULL"
if ($orphanPhoneMappings -ne 0) {
    $errors.Add("Orphan or mismatched global phone mappings: $orphanPhoneMappings")
}

$seatRouteChecks = @()
foreach ($databaseIndex in 0..1) {
    foreach ($tableIndex in 0..7) {
        $seatRouteChecks += "SELECT COUNT(*) bad FROM ds_seat_$databaseIndex.t_seat_$tableIndex WHERE MOD(event_id,2)<>$databaseIndex OR MOD(event_id,8)<>$tableIndex"
    }
}
$badSeatRoutes = Invoke-MySqlScalar ("SELECT COALESCE(SUM(bad),0) FROM (" + ($seatRouteChecks -join " UNION ALL ") + ") routes")
if ($badSeatRoutes -ne 0) {
    $errors.Add("Misrouted seat rows: $badSeatRoutes")
}

$orderShardCount = Invoke-MySqlScalar "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA IN ('ds_order_0','ds_order_1') AND TABLE_NAME REGEXP '^t_(order|order_item|user_ticket)_[0-9]+$'"
if ($orderShardCount -eq 96) {
    $ticketChecks = @()
    $orderChecks = @()
    foreach ($databaseIndex in 0..1) {
        foreach ($tableIndex in 0..15) {
            $hash = "(((CAST(user_id AS UNSIGNED) & 4294967295) ^ ((CAST(user_id AS UNSIGNED) >> 32) & 4294967295)) & 2147483647)"
            $ticketChecks += "SELECT COUNT(*) bad FROM ds_order_$databaseIndex.t_user_ticket_$tableIndex WHERE MOD($hash,2)<>$databaseIndex OR MOD(FLOOR($hash/2),16)<>$tableIndex"
            foreach ($tablePrefix in @("t_order", "t_order_item")) {
                $orderChecks += "SELECT COUNT(*) bad FROM ds_order_$databaseIndex.${tablePrefix}_$tableIndex WHERE MOD($hash,2)<>$databaseIndex OR MOD(FLOOR($hash/2),16)<>$tableIndex"
            }
        }
    }
    $badTicketRoutes = Invoke-MySqlScalar ("SELECT SUM(bad) FROM (" + ($ticketChecks -join " UNION ALL ") + ") routes")
    if ($badTicketRoutes -ne 0) {
        $errors.Add("Misrouted user-ticket rows: $badTicketRoutes")
    }
    $badOrderRoutes = Invoke-MySqlScalar ("SELECT SUM(bad) FROM (" + ($orderChecks -join " UNION ALL ") + ") routes")
    if ($badOrderRoutes -ne 0) {
        $errors.Add("Misrouted order rows: $badOrderRoutes")
    }
}

if ($errors.Count -gt 0) {
    $errors | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Host "LiveStart database schema, $PayMode pay mode, and shard routes are valid."
