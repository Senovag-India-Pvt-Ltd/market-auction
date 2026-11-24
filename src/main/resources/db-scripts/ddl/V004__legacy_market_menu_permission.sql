-- =====================================================================
-- V004 XC-004: menu permission for the "Legacy Market Transactions" screen
-- (UI route /seriui/legacy-market-transactions, map code
-- Market_Legacy_Transactions) under Market & Auction > Commercial Market.
--
-- 1. Adds the page to rp_page_permission (parent = Commercial Market group,
--    map code Market_CommercialMarket) so it shows in Role Config.
-- 2. Grants it to every role that already has the Commercial Market group;
--    remove it from any role in Role Config if that role should not import.
--
-- rp_page_permission / rp_role_association ids are not IDENTITY: next id is
-- taken under an exclusive table lock. Idempotent (keyed on map_code /
-- role_id + value). SQL Server.
--
-- IMPORTANT: run with AUTO-COMMIT ON (sqlcmd / SSMS default). In DBeaver with
-- manual commit, the table locks stay held until you press Commit, and every
-- login menu load (rp_role_association) blocks - the whole menu goes empty.
-- Rollback:
--   DELETE a FROM dbo.rp_role_association a JOIN dbo.rp_page_permission p
--     ON a.value = CAST(p.rp_page_permission_id AS VARCHAR(20))
--     WHERE p.map_code = 'Market_Legacy_Transactions';
--   DELETE FROM dbo.rp_page_permission WHERE map_code = 'Market_Legacy_Transactions';
-- =====================================================================

SET XACT_ABORT ON;
BEGIN TRAN;

DECLARE @parentId INT = (SELECT TOP 1 rp_page_permission_id FROM dbo.rp_page_permission
                         WHERE map_code = 'Market_CommercialMarket' AND active = 1);
DECLARE @root INT = (SELECT root FROM dbo.rp_page_permission WHERE rp_page_permission_id = @parentId);

IF @parentId IS NULL
BEGIN
    RAISERROR('Commercial Market permission group (Market_CommercialMarket) not found; nothing done.', 16, 1);
    ROLLBACK TRAN;
    RETURN;
END

IF NOT EXISTS (SELECT 1 FROM dbo.rp_page_permission WITH (UPDLOCK, HOLDLOCK) WHERE map_code = 'Market_Legacy_Transactions')
BEGIN
    DECLARE @newId INT = (SELECT ISNULL(MAX(rp_page_permission_id), 0) + 1 FROM dbo.rp_page_permission WITH (TABLOCKX, HOLDLOCK));
    INSERT INTO dbo.rp_page_permission
        (rp_page_permission_id, root, parent, page_name, route, is_page, active,
         created_by, modified_by, created_date, modified_date, map_code)
    VALUES
        (@newId, @root, @parentId, 'Legacy Market Transactions', 'legacy-market-transactions', 0, 1,
         'XC-004', 'XC-004', GETDATE(), GETDATE(), 'Market_Legacy_Transactions');
END

DECLARE @pageId VARCHAR(20) = (SELECT CAST(rp_page_permission_id AS VARCHAR(20)) FROM dbo.rp_page_permission
                               WHERE map_code = 'Market_Legacy_Transactions');
DECLARE @parentVal VARCHAR(20) = CAST(@parentId AS VARCHAR(20));
DECLARE @maxAssoc BIGINT = (SELECT ISNULL(MAX(rp_role_association_id), 0) FROM dbo.rp_role_association WITH (TABLOCKX, HOLDLOCK));

INSERT INTO dbo.rp_role_association
    (rp_role_association_id, role_id, value, active, created_by, modified_by, created_date, modified_date, rp_role_permission_id)
SELECT @maxAssoc + ROW_NUMBER() OVER (ORDER BY g.role_id), g.role_id, @pageId, 1,
       'XC-004', 'XC-004', GETDATE(), GETDATE(), g.rp_role_permission_id
FROM (SELECT role_id, MAX(rp_role_permission_id) AS rp_role_permission_id
      FROM dbo.rp_role_association
      WHERE value = @parentVal AND active = 1
      GROUP BY role_id) g
WHERE NOT EXISTS (SELECT 1 FROM dbo.rp_role_association x WHERE x.role_id = g.role_id AND x.value = @pageId);

COMMIT TRAN;
GO
