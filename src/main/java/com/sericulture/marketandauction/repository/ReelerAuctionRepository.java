package com.sericulture.marketandauction.repository;

import com.sericulture.marketandauction.helper.MarketAuctionQueryConstants;
import com.sericulture.marketandauction.model.api.marketauction.ReelerBalanceResponse;
import com.sericulture.marketandauction.model.entity.ReelerAuction;
import com.sericulture.marketandauction.service.MarketAuctionReportService;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;

public interface ReelerAuctionRepository  extends PagingAndSortingRepository<ReelerAuction, Integer> {

    public ReelerAuction save(ReelerAuction reelerAuction);

    @Query("select r from ReelerAuction r where r.allottedLotId =:lotId and" +
            " r.marketId =:marketId and r.auctionDate =:auctionDate order by amount desc,createdDate asc limit 1")
    public ReelerAuction getHighestBidForLot(int lotId,int marketId, LocalDate auctionDate);

    @Query(nativeQuery = true , value = """
            select f.first_name,f.middle_name,f.last_name ,f.farmer_number ,v.Village_Name,l.LOT_APPROX_WEIGHT_BEFORE_WEIGHMENT,l.status,l.BID_ACCEPTED_BY  from 
            FARMER f
            INNER JOIN market_auction ma ON ma.farmer_id = f.FARMER_ID 
            INNER JOIN lot l ON l.market_auction_id =ma.market_auction_id and l.auction_date = ma.market_auction_date 
            LEFT JOIN farmer_address fa ON f.FARMER_ID = fa.FARMER_ID and fa.default_address = 1
            LEFT JOIN  Village v ON   fa.Village_ID=v.village_id where 
            l.auction_date =:auctionDate and l.market_id =:marketId and l.allotted_lot_id =:lotId""")
    public Object[][] getLotBidDetailResponse(int lotId, LocalDate auctionDate, int marketId);

    public ReelerAuction findById(BigInteger id);

    @Query(nativeQuery = true, value = "SELECT r.name ,r.fruits_id  from REELER_AUCTION ra, reeler r  where ra.REELER_ID = r.reeler_id  and REELER_AUCTION_ID =:reelerAuctionId")
    public Object[][] getReelerDetailsForHighestBid(BigInteger reelerAuctionId);

    @Query("SELECT DISTINCT allottedLotId  from ReelerAuction ra  where ra.auctionDate =:today and ra.marketId =:marketId and ra.reelerId  =:reelerId")
    public List<Integer> findByAuctionDateAndMarketIdAndReelerId(LocalDate today,int marketId,int reelerId);


    @Query(nativeQuery = true, value = """
            SELECT REELER_AUCTION_ID,AMOUNT ,ALLOTTED_LOT_ID, 'HIGHEST',R.Name  
            FROM REELER_AUCTION RAA INNER JOIN REELER R ON RAA.REELER_ID = R.REELER_ID 
            INNER JOIN (
            select MIN(REELER_AUCTION_ID) ID, RA.ALLOTTED_LOT_ID as AL from REELER_AUCTION RA,
            ( 
            SELECT MAX(AMOUNT) AMT, ALLOTTED_LOT_ID  from REELER_AUCTION ra
            where AUCTION_DATE = :today and ALLOTTED_LOT_ID in ( :lotList) AND MARKET_ID =:marketId GROUP by ALLOTTED_LOT_ID ) as RAB 
            WHERE RAB.AMT=RA.AMOUNT AND  RA.MARKET_ID =:marketId AND RA.ALLOTTED_LOT_ID = RAB.ALLOTTED_LOT_ID AND AUCTION_DATE = :today
            GROUP by  RA.ALLOTTED_LOT_ID ) RA ON RA.ID= RAA.REELER_AUCTION_ID
            UNION
            SELECT REELER_AUCTION_ID,AMOUNT ,ALLOTTED_LOT_ID, 'MYBID',R.Name  
            FROM REELER_AUCTION RAA INNER JOIN REELER R ON RAA.REELER_ID = R.REELER_ID 
            INNER JOIN (
            select MIN(REELER_AUCTION_ID) ID, RA.ALLOTTED_LOT_ID as AL from REELER_AUCTION RA,
            ( 
            SELECT MAX(AMOUNT) AMT, ALLOTTED_LOT_ID  from REELER_AUCTION ra
            where AUCTION_DATE = :today and ALLOTTED_LOT_ID in ( :lotList) AND MARKET_ID =:marketId AND ra.REELER_ID =:reelerId  GROUP by ALLOTTED_LOT_ID ) as RAB 
            WHERE RAB.AMT=RA.AMOUNT AND  RA.MARKET_ID =:marketId AND RA.ALLOTTED_LOT_ID = RAB.ALLOTTED_LOT_ID AND AUCTION_DATE = :today AND ra.REELER_ID =:reelerId
            GROUP by  RA.ALLOTTED_LOT_ID ) RA ON RA.ID= RAA.REELER_AUCTION_ID""")
    public Object[][] getHighestAndReelerBidAmountForLotList(LocalDate today,int marketId,List<Integer> lotList,int reelerId);


    public long deleteByIdAndMarketIdAndAllottedLotIdAndReelerId(BigInteger id,int marketId,int allottedLotId,BigInteger reelerId);

    @Query(nativeQuery = true,value = """
            SELECT virtual_account_number  from reeler_virtual_bank_account rvba WHERE reeler_id = :reelerId and market_master_id = :marketId""")
    public String getReelerVirtualAccountByReelerIdAndMarketId(int reelerId,int marketId);
    
    @Query(nativeQuery = true,value = """
            SELECT r.reeler_id,rvba.virtual_account_number ,rvcb.CURRENT_BALANCE ,mm.releer_minimum_balance 
            from reeler r
            LEFT JOIN
            reeler_virtual_bank_account rvba
            on rvba.reeler_Id = r.reeler_Id
            LEFT JOIN
            REELER_VID_CURRENT_BALANCE rvcb
            on rvcb.reeler_virtual_account_number = rvba.virtual_account_number
            LEFT JOIN
            market_master mm
            on mm.market_master_id = rvba.market_master_id
            WHERE
            r.reeler_id = :reelerId and rvba.market_master_id = :marketId""")
    public Object[][] getReelerBalance(int reelerId,int marketId);

    @Query(nativeQuery = true, value = MarketAuctionQueryConstants.getAllHighestBids)
    public Object[][] getHighestBidAmountForAllLotList(LocalDate today,int marketId,List<Integer> lotList);

    @Query(nativeQuery = true, value = MarketAuctionQueryConstants.ALLTTOTED_LOT_LIST_PER_MARKET_ID)
    public List<Integer> getAllottedLotListByMarketId(LocalDate auctionDate,int marketId);

    @Query(nativeQuery = true, value = MarketAuctionQueryConstants.ALLTTOTED_LOT_LIST_PER_MARKET_ID_AND_GODOWNID)
    public List<Integer> getAllottedLotListByMarketIdAndGoDownId(LocalDate auctionDate,int marketId,int godownId);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.UNIT_COUNTER_REPORT_QUERY)
    public List<Object[]> getUnitCounterReport(LocalDate reportDate,int marketId);

}
