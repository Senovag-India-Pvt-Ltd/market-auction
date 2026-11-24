-- =====================================================================
-- V005 SR-005: market transactions done through a farmer's authorised
-- agent. One row per e-Inward transaction (market_auction) that was brought
-- by an agent. The agents themselves live in farmer_representative
-- (farmer-registration: db-scripts/ddl/farmer_representative.sql - run that
-- first). market_auction / lot are NOT altered: the link is written by a
-- separate API call after a successful e-Inward, so e-Inward works exactly
-- as before even without this table.
--
-- ddl-auto is none for market-auction: run manually with AUTO-COMMIT ON.
-- Idempotent. SQL Server.
-- Rollback: DROP TABLE dbo.farmer_representative_txn;
-- =====================================================================

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE object_id = OBJECT_ID(N'dbo.farmer_representative_txn') AND type = N'U')
BEGIN
    CREATE TABLE dbo.farmer_representative_txn (
        farmer_representative_txn_id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        market_auction_id BIGINT NOT NULL,
        farmer_id BIGINT NOT NULL,
        farmer_representative_id BIGINT NOT NULL,
        market_id BIGINT NULL,
        auction_date DATE NULL,
        CREATED_BY VARCHAR(255) NULL,
        MODIFIED_BY VARCHAR(255) NULL,
        CREATED_DATE DATETIME NULL,
        MODIFIED_DATE DATETIME NULL,
        ACTIVE TINYINT NOT NULL DEFAULT 1,
        CONSTRAINT UQ_farmer_representative_txn_auction UNIQUE (market_auction_id)
    );
    CREATE INDEX IX_farmer_rep_txn_rep ON dbo.farmer_representative_txn(farmer_representative_id, ACTIVE);
    CREATE INDEX IX_farmer_rep_txn_farmer ON dbo.farmer_representative_txn(farmer_id, ACTIVE);
END
GO
