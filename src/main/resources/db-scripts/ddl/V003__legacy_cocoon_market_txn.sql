-- =====================================================================
-- V003 XC-004: cocoon market transactions of the pre-project (legacy)
-- market system, imported from its Excel / CSV export, kept apart from the
-- live auction tables (market_auction / lot / payments are never touched).
--
--   legacy_cocoon_market_import        one row per import run (file, counts)
--   legacy_cocoon_market_txn           one row per legacy transaction;
--                                      unique (source_system, legacy_txn_id),
--                                      so re-importing an export updates /
--                                      skips instead of duplicating
--   legacy_cocoon_market_import_error  rejected rows of an import run
--
-- new_system_lot_id is set when the same lot (market + auction date + lot
-- number) already exists in the new system; such rows are not shown twice.
--
-- ddl-auto is none for market-auction: run manually. Idempotent. SQL Server.
-- Rollback: DROP TABLE dbo.legacy_cocoon_market_import_error; DROP TABLE dbo.legacy_cocoon_market_txn;
--           DROP TABLE dbo.legacy_cocoon_market_import;
-- =====================================================================

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE object_id = OBJECT_ID(N'dbo.legacy_cocoon_market_import') AND type = N'U')
BEGIN
    CREATE TABLE dbo.legacy_cocoon_market_import (
        legacy_cocoon_market_import_id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        source_system VARCHAR(50) NOT NULL,
        file_name NVARCHAR(255) NULL,
        status VARCHAR(20) NOT NULL,              -- COMPLETED / FAILED
        total_rows INT NOT NULL DEFAULT 0,
        inserted_rows INT NOT NULL DEFAULT 0,
        updated_rows INT NOT NULL DEFAULT 0,
        unchanged_rows INT NOT NULL DEFAULT 0,
        rejected_rows INT NOT NULL DEFAULT 0,
        already_in_new_system_rows INT NOT NULL DEFAULT 0,
        message NVARCHAR(1000) NULL,
        CREATED_BY VARCHAR(255) NULL,
        MODIFIED_BY VARCHAR(255) NULL,
        CREATED_DATE DATETIME NULL,
        MODIFIED_DATE DATETIME NULL,
        ACTIVE TINYINT NOT NULL DEFAULT 1
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE object_id = OBJECT_ID(N'dbo.legacy_cocoon_market_txn') AND type = N'U')
BEGIN
    CREATE TABLE dbo.legacy_cocoon_market_txn (
        legacy_cocoon_market_txn_id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        source_system VARCHAR(50) NOT NULL,
        legacy_txn_id VARCHAR(100) NOT NULL,
        market_master_id BIGINT NOT NULL,
        market_name_raw NVARCHAR(200) NULL,
        auction_date DATE NOT NULL,
        lot_number VARCHAR(50) NOT NULL,
        farmer_id BIGINT NULL,                    -- resolved by FRUITS ID, else unique mobile
        farmer_fruits_id VARCHAR(50) NULL,
        farmer_name NVARCHAR(200) NULL,
        farmer_mobile VARCHAR(15) NULL,
        reeler_id BIGINT NULL,                    -- resolved by reeling licence number
        reeler_licence_no VARCHAR(100) NULL,
        reeler_name NVARCHAR(200) NULL,
        race_master_id BIGINT NULL,
        race_name_raw NVARCHAR(100) NULL,
        weight_kg DECIMAL(12,3) NOT NULL,
        rate_per_kg DECIMAL(12,2) NOT NULL,
        amount DECIMAL(14,2) NOT NULL,
        farmer_market_fee DECIMAL(12,2) NULL,
        reeler_market_fee DECIMAL(12,2) NULL,
        new_system_lot_id BIGINT NULL,            -- same lot already in the new system
        row_hash VARCHAR(64) NOT NULL,            -- detects changes between exports
        first_import_id BIGINT NOT NULL,
        last_import_id BIGINT NOT NULL,
        CREATED_BY VARCHAR(255) NULL,
        MODIFIED_BY VARCHAR(255) NULL,
        CREATED_DATE DATETIME NULL,
        MODIFIED_DATE DATETIME NULL,
        ACTIVE TINYINT NOT NULL DEFAULT 1,
        CONSTRAINT UQ_legacy_cocoon_market_txn UNIQUE (source_system, legacy_txn_id)
    );
    CREATE INDEX IX_legacy_cmt_farmer ON dbo.legacy_cocoon_market_txn(farmer_fruits_id, farmer_mobile);
    CREATE INDEX IX_legacy_cmt_lot ON dbo.legacy_cocoon_market_txn(market_master_id, auction_date, lot_number);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE object_id = OBJECT_ID(N'dbo.legacy_cocoon_market_import_error') AND type = N'U')
BEGIN
    CREATE TABLE dbo.legacy_cocoon_market_import_error (
        legacy_cocoon_market_import_error_id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        legacy_cocoon_market_import_id BIGINT NOT NULL,
        row_number_in_file INT NOT NULL,
        legacy_txn_id VARCHAR(100) NULL,
        message NVARCHAR(1000) NOT NULL
    );
    CREATE INDEX IX_legacy_cmie_import ON dbo.legacy_cocoon_market_import_error(legacy_cocoon_market_import_id);
END
GO
