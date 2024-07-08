CREATE       PROCEDURE [dbo].[SUBMIT_BID_EXCEPTIONAL]
   (@UserId varchar(25),
   @MarketId INT,
   @GodownId INT,
   @LotId INT,
   @ReelerId varchar(25),
   @AuctionDate date,
   @SurrogateBid int,
   @Amount int,
   @Error varchar(500) OUTPUT,
   @Success INT OUTPUT)
AS
BEGIN
	DECLARE @FetchedlotId INT  = NULL;
	DECLARE @Status VARCHAR(255);
	DECLARE @Auction1StartTime time;
	DECLARE @Auction2StartTime time;
	DECLARE @Auction3StartTime time;
	DECLARE @Auction1EndTime time;
	DECLARE @Auction2EndTime time;
	DECLARE @Auction3EndTime time;
	DECLARE @CurrentTime time;
	DECLARE @InTime INT;
    DECLARE @AuctionSlot INT;
	PRINT '--------------------------------------------START--------------------------------------------';
    SET @Success = 0;
    SET @InTime = 0;
    SET @AuctionSlot = 0;
    PRINT '1';
    SELECT  @CurrentTime =cast(SWITCHOFFSET(SYSDATETIMEOFFSET(), '+05:30') as time);
	PRINT @CurrentTime
    IF (EXISTS (SELECT 1 FROM EXCEPTIONAL_TIME where AUCTION_DATE = @AuctionDate AND market_id =@MarketId  ))
    BEGIN
    	SELECT
    	    @InTime = 1,
    	    @AuctionSlot = CASE
	    	    WHEN @CurrentTime BETWEEN AUCTION_1_START_TIME AND AUCTION_1_END_TIME THEN 1
	    	    WHEN @CurrentTime BETWEEN AUCTION_2_START_TIME AND AUCTION_2_END_TIME THEN 2
	    	    WHEN @CurrentTime BETWEEN AUCTION_3_START_TIME AND AUCTION_3_END_TIME THEN 3
	    	    END
    	FROM EXCEPTIONAL_TIME
    	WHERE market_id =@MarketId
    	AND auction_date = @AuctionDate
    	AND (  (@CurrentTime BETWEEN AUCTION_1_START_TIME AND AUCTION_1_END_TIME)
    	     OR(@CurrentTime BETWEEN AUCTION_2_START_TIME AND AUCTION_2_END_TIME)
    	     OR(@CurrentTime BETWEEN AUCTION_3_START_TIME AND AUCTION_3_END_TIME));
        PRINT '--------------------------------------------EXCEPTIONAL TIME--------------------------------------------';
       PRINT @CurrentTime;
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
        FROM MARKET_MASTER mm
        WHERE mm.market_master_id =@MarketId
        AND (  (@CurrentTime BETWEEN AUCTION_1_START_TIME AND AUCTION_1_END_TIME)
    	     OR(@CurrentTime BETWEEN AUCTION_2_START_TIME AND AUCTION_2_END_TIME)
    	     OR(@CurrentTime BETWEEN AUCTION_3_START_TIME AND AUCTION_3_END_TIME));
    	PRINT '--------------------------------------------IN USUAL  TIME--------------------------------------------'
    END


    IF (@InTime = 0)
	BEGIN
	   	SET @Error='Cannot accept bid as time either over or not started';
	   	PRINT 'Cannot accept bid as time either over or not started';
	    RETURN;
	END;
    PRINT '3';
    PRINT @FetchedlotId ;
	SELECT @FetchedlotId=lot_id, @Status=Status from lot where allotted_lot_id =@LotId AND market_id=@MarketId AND auction_date=@AuctionDate
    PRINT @FetchedlotId ;
	IF (@FetchedlotId IS NULL )
    BEGIN
		SET @Error='Lot not found';
	    RETURN;
	END;
    IF (@Status IS NOT NULL)
    BEGIN
		SET @Error='cannot bid as the status is:' + @Status;
	    RETURN;
	END;
    PRINT '4';
	INSERT INTO REELER_AUCTION
    (REELER_AUCTION_ID, ACTIVE, ALLOTTED_LOT_ID, REELER_ID, AMOUNT, SURROGATE_BID, STATUS, AUCTION_DATE, CREATED_BY, MODIFIED_BY, CREATED_DATE, MODIFIED_DATE, MARKET_ID, AUCTION_SESSION)
    VALUES(NEXT VALUE FOR [REELER_AUCTION_SEQ], 1, @LotId, @ReelerId, @Amount, @SurrogateBid, NULL, @AuctionDate, @UserId, @UserId, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, @MarketId, @AuctionSlot);

    IF (@@ROWCOUNT  = 0)
    BEGIN
        SET @Error='Error occurrent while submitting the bid, plase try again';
	END;

    SET @Success = 1;
    PRINT '5';
    PRINT '--------------------------------------------END--------------------------------------------';
--METHOD CALL EXAMPLE FOR STORED PROCEDURE
--DECLARE @Error varchar(500);
--DECLARE @Success varchar(500);
--SET @Success = -1;
--EXEC dbo.SUBMIT_BID_EXCEPTIONAL @UserId ='sac',
--   @MarketId =1,
--   @GodownId =0,
--   @LotId =25,
--   @ReelerId =4,
--   @AuctionDate ='2024-01-12',
--   @SurrogateBid =0,
--   @Amount =100,
--   @Error= @Error  output,
--   @Success =@Success output;
--   SELECT  @Error, @Success
END;
