ALTER PROCEDURE [dbo].[GET_AUCTION_DETAILS_1]
    @today DATE,
    @marketId INT,
    @reelerId VARCHAR(25),
    @Error VARCHAR(500) OUTPUT,
    @Success INT OUTPUT
AS
BEGIN
    DECLARE @CurrentTime TIME;
    DECLARE @AuctionSlot INT;
    DECLARE @InTime INT = 0;

    SET @Success = 0;
    SET @AuctionSlot = 0;

    -- Get current time in the desired time zone
    SELECT @CurrentTime = CAST(SWITCHOFFSET(SYSDATETIMEOFFSET(), '+05:30') AS TIME);

    -- Check if current time falls in exceptional times
    IF EXISTS (SELECT 1 FROM EXCEPTIONAL_TIME WHERE AUCTION_DATE = @today AND market_id = @marketId)
    BEGIN
        SELECT
            @InTime = 1,
            @AuctionSlot = CASE
                WHEN @CurrentTime BETWEEN AUCTION_1_START_TIME AND AUCTION_1_END_TIME THEN 1
                WHEN @CurrentTime BETWEEN AUCTION_2_START_TIME AND AUCTION_2_END_TIME THEN 2
                WHEN @CurrentTime BETWEEN AUCTION_3_START_TIME AND AUCTION_3_END_TIME THEN 3
            END
        FROM EXCEPTIONAL_TIME
        WHERE market_id = @marketId
        AND auction_date = @today
        AND (@CurrentTime BETWEEN AUCTION_1_START_TIME AND AUCTION_1_END_TIME
            OR @CurrentTime BETWEEN AUCTION_2_START_TIME AND AUCTION_2_END_TIME
            OR @CurrentTime BETWEEN AUCTION_3_START_TIME AND AUCTION_3_END_TIME);
    END
    ELSE
    BEGIN
        SELECT
            @InTime = 1,
            @AuctionSlot = CASE
                WHEN @CurrentTime BETWEEN AUCTION_1_START_TIME AND AUCTION_1_END_TIME THEN 1
                WHEN @CurrentTime BETWEEN AUCTION_2_START_TIME AND AUCTION_2_END_TIME THEN 2
                WHEN @CurrentTime BETWEEN AUCTION_3_START_TIME AND AUCTION_3_END_TIME THEN 3
            END
        FROM MARKET_MASTER
        WHERE market_master_id = @marketId
        AND (@CurrentTime BETWEEN AUCTION_1_START_TIME AND AUCTION_1_END_TIME
            OR @CurrentTime BETWEEN AUCTION_2_START_TIME AND AUCTION_2_END_TIME
            OR @CurrentTime BETWEEN AUCTION_3_START_TIME AND AUCTION_3_END_TIME);
    END

    IF (@InTime = 0)
    BEGIN
        SET @Error = 'No bids for this lot';
        RETURN;
    END;


    -- Get auction details
    SELECT
	REELER_AUCTION_ID,
	AMOUNT ,
	ALLOTTED_LOT_ID,
	'HIGHEST',
	R.Name
FROM
	REELER_AUCTION RAA
INNER JOIN REELER R ON
	RAA.REELER_ID = R.REELER_ID
INNER JOIN (
	select
		MIN(REELER_AUCTION_ID) ID,
		RA.ALLOTTED_LOT_ID as AL
	from
		REELER_AUCTION RA,
		(
		SELECT
			MAX(AMOUNT) AMT,
			ALLOTTED_LOT_ID
		from
			REELER_AUCTION ra
		where
			AUCTION_DATE =@today
			and ALLOTTED_LOT_ID in (select
										DISTINCT ALLOTTED_LOT_ID  from REELER_AUCTION ra  where ra.AUCTION_DATE = @today and
 											ra.MARKET_ID = @marketId and ra.REELER_ID  = @reelerId  and ra.ACTIVE = 1 and auction_session =@AuctionSlot
   									)
				AND MARKET_ID =@marketId
				and RA.auction_session =@AuctionSlot
			GROUP by
				ALLOTTED_LOT_ID ) as RAB
	WHERE
		RAB.AMT = RA.AMOUNT
		AND RA.MARKET_ID =@marketId
		AND RA.ALLOTTED_LOT_ID = RAB.ALLOTTED_LOT_ID
		and RA.auction_session =@AuctionSlot
		AND AUCTION_DATE =@today
	GROUP by
		RA.ALLOTTED_LOT_ID ) RA ON
	RA.ID = RAA.REELER_AUCTION_ID
UNION
SELECT
	REELER_AUCTION_ID,
	AMOUNT ,
	ALLOTTED_LOT_ID,
	'MYBID',
	R.Name
FROM
	REELER_AUCTION RAA
INNER JOIN REELER R ON
	RAA.REELER_ID = R.REELER_ID
INNER JOIN (
	select
		MIN(REELER_AUCTION_ID) ID,
		RA.ALLOTTED_LOT_ID as AL
	from
		REELER_AUCTION RA,
		(
		SELECT
			MAX(AMOUNT) AMT,
			ALLOTTED_LOT_ID
		from
			REELER_AUCTION ra
		where
			AUCTION_DATE =@today
			and RA.auction_session =@AuctionSlot
			and ALLOTTED_LOT_ID in (select
										DISTINCT ALLOTTED_LOT_ID  from REELER_AUCTION ra  where ra.AUCTION_DATE = @today and
 											ra.MARKET_ID = @marketId and ra.REELER_ID  = @reelerId  and ra.ACTIVE = 1 and auction_session =@AuctionSlot
   									)
				AND MARKET_ID =@marketId
				AND ra.REELER_ID =@reelerId
			GROUP by
				ALLOTTED_LOT_ID ) as RAB
	WHERE
		RAB.AMT = RA.AMOUNT
		AND RA.MARKET_ID =@marketId
		and RA.auction_session =@AuctionSlot
		AND RA.ALLOTTED_LOT_ID = RAB.ALLOTTED_LOT_ID
		AND AUCTION_DATE =@today
		AND ra.REELER_ID =@reelerId
	GROUP by
		RA.ALLOTTED_LOT_ID ) RA ON
	RA.ID = RAA.REELER_AUCTION_ID ORDER BY ALLOTTED_LOT_ID;

    SET @Success = 1;
END;


--DECLARE @Error varchar(500);
--DECLARE @Success varchar(500);
--SET @Success = -1;
--EXEC GET_AUCTION_DETAILS_1
--   @today = '2024-07-11',
--   @marketId =34,
--   @reelerId = 108,
--   @Error= @Error  output,
--   @Success =@Success output;
--   SELECT  @Error, @Success