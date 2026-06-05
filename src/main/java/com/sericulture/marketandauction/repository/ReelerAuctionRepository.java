package com.sericulture.marketandauction.repository;

import com.sericulture.marketandauction.helper.MarketAuctionQueryConstants;
import com.sericulture.marketandauction.model.api.marketauction.ReelerBalanceResponse;
import com.sericulture.marketandauction.model.api.marketauction.ReelerReport;
import com.sericulture.marketandauction.model.entity.ReelerAuction;
import com.sericulture.marketandauction.model.entity.ReelerAuctionAccepted;
import com.sericulture.marketandauction.service.MarketAuctionReportService;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;

public interface ReelerAuctionRepository  extends PagingAndSortingRepository<ReelerAuction, Integer> {

    public ReelerAuction save(ReelerAuction reelerAuction);

    public ReelerAuctionAccepted save(ReelerAuctionAccepted reelerAuctionAccepted);

    @Query("select r from ReelerAuction r where r.allottedLotId =:lotId and" +
            " r.marketId =:marketId and r.auctionSession = :auctionSession and r.auctionDate =:auctionDate order by amount desc,createdDate asc limit 1")
    public ReelerAuction getHighestBidForLot(int lotId,int marketId, LocalDate auctionDate, int auctionSession);

    @Query(nativeQuery = true , value = """
    select top (1) * from reeler_auction r1_0 where r1_0.allotted_lot_id=:lotId and r1_0.market_id=:marketId and r1_0.auction_date=:auctionDate and r1_0.auction_session = :auctionSession and active = 1 order by r1_0.amount desc,r1_0.created_date""")
    public ReelerAuction getHighestBidForLotAndActive(int lotId,int marketId, LocalDate auctionDate, int auctionSession);

    @Query(nativeQuery = true , value = """
    select * from REELER_AUCTION where ALLOTTED_LOT_ID = :lotId and MARKET_ID = :marketId and AUCTION_DATE = :auctionDate and ACTIVE = 1""")
    public List<ReelerAuction> getBidsForLotAndActiveByMarketIdAndAuctionDate(int lotId,int marketId, LocalDate auctionDate);

    @Query(nativeQuery = true , value = """
            select f.first_name,f.middle_name,f.last_name ,f.farmer_number ,v.Village_Name,l.LOT_APPROX_WEIGHT_BEFORE_WEIGHMENT,l.status,l.BID_ACCEPTED_BY  from 
            FARMER f
            INNER JOIN market_auction ma ON ma.farmer_id = f.FARMER_ID 
            INNER JOIN lot l ON l.market_auction_id =ma.market_auction_id and l.auction_date = ma.market_auction_date 
            LEFT JOIN farmer_address fa ON f.FARMER_ID = fa.FARMER_ID and fa.default_address = 1
            LEFT JOIN  Village v ON   fa.Village_ID=v.village_id where 
            l.auction_date =:auctionDate and l.market_id =:marketId and l.allotted_lot_id =:lotId""")
    public Object[][] getLotBidDetailResponse(int lotId, LocalDate auctionDate, int marketId);

    @Query(nativeQuery = true , value = """
            SELECT
                r.name,
                r.reeler_number,
                v.Village_Name,
                l.LOT_APPROX_WEIGHT_BEFORE_WEIGHMENT,
                l.status,
                l.BID_ACCEPTED_BY
            FROM
                REELER r
            INNER JOIN
                market_auction ma ON ma.reeler_id = r.REELER_ID
            INNER JOIN
                lot l ON l.market_auction_id = ma.market_auction_id
                AND l.auction_date = ma.market_auction_date
            LEFT JOIN
                Village v ON r.Village_ID = v.village_id
            WHERE
                l.auction_date = :auctionDate
                AND l.market_id = :marketId
                AND l.allotted_lot_id = :lotId""")
    public Object[][] getLotBidReelerDetailResponseOfSilkMarket(int lotId, LocalDate auctionDate, int marketId);

    public ReelerAuction findById(BigInteger id);

    @Query(nativeQuery = true, value = "SELECT r.name ,r.fruits_id  from REELER_AUCTION ra, reeler r  where ra.REELER_ID = r.reeler_id and ra.active = 1 and REELER_AUCTION_ID =:reelerAuctionId")
    public Object[][] getReelerDetailsForHighestBid(BigInteger reelerAuctionId);

    @Query(nativeQuery = true, value = "SELECT r.name ,r.fruits_id,r.reeling_license_number  from REELER_AUCTION ra, reeler r  where ra.REELER_ID = r.reeler_id and ra.active = 1 and REELER_AUCTION_ID =:reelerAuctionId")
    public Object[][] getReelerDetailsForHighestBidWithReelingNumber(BigInteger reelerAuctionId);

    @Query(nativeQuery = true, value = "SELECT tl.first_name, tl.trader_license_number FROM REELER_AUCTION ra LEFT JOIN trader_license tl ON ra.trader_license_id = tl.trader_license_id WHERE ra.active = 1 AND ra.REELER_AUCTION_ID = :reelerAuctionId")
    public Object[][] getTraderDetailsForHighestBidWithTradingNumber(BigInteger reelerAuctionId);

    @Query("SELECT DISTINCT allottedLotId  from ReelerAuction ra  where ra.auctionDate =:today and ra.marketId =:marketId and ra.reelerId  =:reelerId")
    public List<Integer> findByAuctionDateAndMarketIdAndReelerId(LocalDate today,int marketId,int reelerId);

    @Query(nativeQuery = true, value = """
    SELECT DISTINCT ALLOTTED_LOT_ID  from REELER_AUCTION ra  where ra.AUCTION_DATE = :today and ra.MARKET_ID = :marketId and ra.REELER_ID  = :reelerId and ra.ACTIVE = 1 """)
    public List<Integer> findByAuctionDateAndMarketIdAndReelerIdByActive(LocalDate today,int marketId,int reelerId);


    @Query(nativeQuery = true, value = """
            SELECT REELER_AUCTION_ID,AMOUNT ,ALLOTTED_LOT_ID, 'HIGHEST',R.Name  
            FROM REELER_AUCTION RAA INNER JOIN REELER R ON RAA.REELER_ID = R.REELER_ID 
            INNER JOIN (
            select MIN(REELER_AUCTION_ID) ID, RA.ALLOTTED_LOT_ID as AL from REELER_AUCTION RA,
            ( 
            SELECT MAX(AMOUNT) AMT, ALLOTTED_LOT_ID  from REELER_AUCTION ra
            where AUCTION_DATE = :today and ALLOTTED_LOT_ID in ( :lotList) AND MARKET_ID =:marketId and RA.auction_session = :auctionSession GROUP by ALLOTTED_LOT_ID ) as RAB 
            WHERE RAB.AMT=RA.AMOUNT AND  RA.MARKET_ID =:marketId AND RA.ALLOTTED_LOT_ID = RAB.ALLOTTED_LOT_ID and RA.auction_session = :auctionSession AND AUCTION_DATE = :today
            GROUP by  RA.ALLOTTED_LOT_ID ) RA ON RA.ID= RAA.REELER_AUCTION_ID
            UNION
            SELECT REELER_AUCTION_ID,AMOUNT ,ALLOTTED_LOT_ID, 'MYBID',R.Name  
            FROM REELER_AUCTION RAA INNER JOIN REELER R ON RAA.REELER_ID = R.REELER_ID 
            INNER JOIN (
            select MIN(REELER_AUCTION_ID) ID, RA.ALLOTTED_LOT_ID as AL from REELER_AUCTION RA,
            ( 
            SELECT MAX(AMOUNT) AMT, ALLOTTED_LOT_ID  from REELER_AUCTION ra
            where AUCTION_DATE = :today and RA.auction_session = :auctionSession and ALLOTTED_LOT_ID in ( :lotList) AND MARKET_ID =:marketId AND ra.REELER_ID =:reelerId  GROUP by ALLOTTED_LOT_ID ) as RAB 
            WHERE RAB.AMT=RA.AMOUNT AND   RA.MARKET_ID =:marketId and RA.auction_session = :auctionSession AND RA.ALLOTTED_LOT_ID = RAB.ALLOTTED_LOT_ID AND AUCTION_DATE = :today AND ra.REELER_ID =:reelerId
            GROUP by  RA.ALLOTTED_LOT_ID ) RA ON RA.ID= RAA.REELER_AUCTION_ID ORDER BY ALLOTTED_LOT_ID """)
    public Object[][] getHighestAndReelerBidAmountForLotList(LocalDate today,int marketId,List<Integer> lotList,int reelerId, int auctionSession);


    public long deleteByIdAndMarketIdAndAllottedLotIdAndReelerId(BigInteger id,int marketId,int allottedLotId,BigInteger reelerId);

    public long deleteByIdAndMarketIdAndAllottedLotIdAndTraderLicenseId(BigInteger id,int marketId,int allottedLotId,BigInteger traderLicenseId);

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
            r.reeler_id = :reelerId and rvba.market_master_id = :marketId and rvba.active = 1 """)
    public Object[][] getReelerBalance(int reelerId,int marketId);

    @Query(nativeQuery = true, value = """
        SELECT ROW_NUMBER() OVER (ORDER BY r.reeler_id) AS serial_number, r.reeler_id, r.reeling_license_number, r.name, rvba.virtual_account_number, r.mobile_number,
        rvcb.CURRENT_BALANCE, rvcb.MODIFIED_DATE,SUM(rvcb.CURRENT_BALANCE) OVER () AS total
        FROM reeler r
        LEFT JOIN reeler_virtual_bank_account rvba ON rvba.reeler_id = r.reeler_id
        LEFT JOIN REELER_VID_CURRENT_BALANCE rvcb ON rvcb.reeler_virtual_account_number = rvba.virtual_account_number
        LEFT JOIN market_master mm ON mm.market_master_id = rvba.market_master_id
        WHERE ((:reelingLicenseNumber IS NULL OR :reelingLicenseNumber = '') OR r.reeling_license_number = :reelingLicenseNumber)
        AND ((:mobileNumber IS NULL OR :mobileNumber = '') OR r.mobile_number = :mobileNumber)
        AND rvba.market_master_id = :marketId AND rvba.active = 1 order by rvcb.CURRENT_BALANCE desc
        """)
        public Object[][] getReelerCurrentBalance(String reelingLicenseNumber, String mobileNumber, int marketId);


    @Query(nativeQuery = true, value = """
         SELECT ROW_NUMBER() OVER (ORDER BY r.reeler_id) AS serial_number, r.reeler_id, r.reeling_license_number, r.name, rvba.virtual_account_number, r.mobile_number,
         rvct.REMITTER_ACCOUNT, rvct.REMITTER_BANK, rvct.ALERT_SEQUENCE_NO, rvct.USER_REFERENCE_NUMBER,
         rvct.AMOUNT, rvct.MODIFIED_DATE, rvct.VALUE_DATE,SUM(rvct.AMOUNT) OVER () AS total
         FROM reeler r
         LEFT JOIN reeler_virtual_bank_account rvba ON rvba.reeler_id = r.reeler_id
         LEFT JOIN REELER_VID_CREDIT_TXN rvct ON rvct.VIRTUAL_ACCOUNT = rvba.virtual_account_number
         LEFT JOIN market_master mm ON mm.market_master_id = rvba.market_master_id
         WHERE ((:reelingLicenseNumber IS NULL OR :reelingLicenseNumber = '') OR r.reeling_license_number = :reelingLicenseNumber)
        AND ((:mobileNumber IS NULL OR :mobileNumber = '') OR r.mobile_number = :mobileNumber) 
         AND rvba.market_master_id = :marketId AND rvba.active = 1 AND CONVERT(date, rvct.TRANSACTION_DATE) = :transactionDate order by  rvct.MODIFIED_DATE desc
        """)
    public Object[][] getReelerTransaction(String reelingLicenseNumber,String mobileNumber, int marketId , LocalDate transactionDate);

    @Query(nativeQuery = true, value = MarketAuctionQueryConstants.getAllHighestBids)
    public Object[][] getHighestBidAmountForAllLotList(LocalDate today,int marketId,List<Integer> lotList);

    @Query(nativeQuery = true, value = MarketAuctionQueryConstants.ALLTTOTED_LOT_LIST_PER_MARKET_ID)
    public List<Integer> getAllottedLotListByMarketId(LocalDate auctionDate,int marketId);

    @Query(nativeQuery = true, value = MarketAuctionQueryConstants.ALLTTOTED_LOT_LIST_PER_MARKET_ID_AND_GODOWNID)
    public List<Integer> getAllottedLotListByMarketIdAndGoDownId(LocalDate auctionDate,int marketId,int godownId);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.UNIT_COUNTER_REPORT)
    public List<Object[]> getUnitCounterReport(LocalDate fromDate,LocalDate toDate,int marketId,String reelerLicenseNumber);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.UNIT_COUNTER_REPORT_WITHOUT_REELER_QUERY)
    public List<Object[]> getUnitCounterReportWithoutReelerNumber(LocalDate fromDate,LocalDate toDate,int marketId);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.UNIT_COUNTER_REPORT_SILK)
    public List<Object[]> getUnitCounterReportSilk(LocalDate fromDate,LocalDate toDate,int marketId,String traderLicenseNumber);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.UNIT_COUNTER_REPORT_WITHOUT_REELER_QUERY_SILK)
    public List<Object[]> getUnitCounterReportWithoutReelerNumberSilk(LocalDate fromDate,LocalDate toDate,int marketId);


    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.REELER_MF_REPORT)
    public List<Object[]> getReelerMFReport(LocalDate fromDate,LocalDate toDate,int marketId,String reelerLicenseNumber);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.REELER_MF_REPORT_WITHOUT_REELER_QUERY)
    public List<Object[]> getReelerMFReportWithoutReelerNumber(LocalDate fromDate,LocalDate toDate,int marketId);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.BIDDING_REPORT_QUERY_LOT)
    public List<Object[]> getBiddingReport(int marketId, LocalDate auctionDate,int lotId);

    @Query(nativeQuery = true, value = MarketAuctionQueryConstants.BIDDING_REPORT_QUERY_WITHOUT_LOT)
    public List<Object[]> getBiddingReportWithoutLot(int marketId, LocalDate auctionDate, Integer lotId);


    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.BIDDING_REPORT_QUERY_REELER)
    public List<Object[]> getReelerBiddingReport(int marketId, LocalDate auctionDate,String reelerLicenseNumber);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.BIDDING_REPORT_QUERY_WITHOUT_REELER)
    public List<Object[]> getReelerBiddingReportWithoutReeler(int marketId, LocalDate auctionDate);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.reeler_auction_status)
    public List<Object[]> getReelerAuctionStatus(BigInteger reelerAuctionId);

    @Query(nativeQuery = true,value = """
            SELECT
                SUM(rvct.AMOUNT) AS total_Amount,
                COUNT(rvct.REELER_VID_CREDIT_TXN_ID) AS deposit_count,
                COUNT(DISTINCT rvba.reeler_id) AS reeler_count,
                mm2.market_name,
                mm2.market_master_id,
                (SELECT SUM(rvct_inner.AMOUNT)
                 FROM REELER_VID_CREDIT_TXN rvct_inner
                 LEFT JOIN reeler_virtual_bank_account rvba_inner ON rvct_inner.VIRTUAL_ACCOUNT = rvba_inner.virtual_account_number
                 WHERE rvba_inner.market_master_id IN (
                     SELECT market_master_id
                     FROM market_master mm
                     WHERE active = 1
                       AND market_type_master_id = 13
                       AND (is_test IS NULL OR is_test = 0)
                 )
                 AND CAST(rvct_inner.TRANSACTION_DATE AS DATE) = :today
                ) AS total_sum_of_all_markets
            FROM REELER_VID_CREDIT_TXN rvct
            LEFT JOIN reeler_virtual_bank_account rvba ON rvct.VIRTUAL_ACCOUNT = rvba.virtual_account_number
            LEFT JOIN market_master mm2 ON rvba.market_master_id = mm2.market_master_id
            WHERE rvba.market_master_id IN (
                SELECT market_master_id
                FROM market_master mm
                WHERE active = 1
                  AND market_type_master_id = 13
                  AND (is_test IS NULL OR is_test = 0)
            )
            AND CAST(rvct.TRANSACTION_DATE AS DATE) = :today
            GROUP BY rvba.market_master_id, mm2.market_name, mm2.market_master_id
            ORDER BY total_Amount DESC;
            """)
    public List<Object[]> getReelerCreditDetailsAllMarket(LocalDate today);

    // REPOSITORY

    @Query(nativeQuery = true,value = """
SELECT
    market_name,
    market_master_id,
    ISNULL(SUM(reeler_amount),0) AS total_reeler_amount,

    ISNULL(SUM(external_unit_amount),0)
        AS total_external_unit_amount,

    ISNULL(SUM(reeler_deposit_count),0)
        AS total_reeler_deposit_count,

    ISNULL(SUM(external_unit_deposit_count),0)
        AS total_external_unit_deposit_count,

    ISNULL(SUM(total_amount),0)
        AS total_amount

FROM
(
    SELECT
        mm.market_name AS market_name,
        mm.market_master_id AS market_master_id,
        SUM(rvct.AMOUNT) AS reeler_amount,
        0 AS external_unit_amount,
        SUM(rvct.AMOUNT) AS total_amount,
        COUNT(rvct.REELER_VID_CREDIT_TXN_ID)AS reeler_deposit_count,
        0 AS external_unit_deposit_count
    FROM REELER_VID_CREDIT_TXN rvct
    INNER JOIN reeler_virtual_bank_account rvba
        ON rvct.VIRTUAL_ACCOUNT =
           rvba.virtual_account_number
    INNER JOIN market_master mm
        ON rvba.market_master_id =
           mm.market_master_id
    WHERE mm.market_type_master_id = 1
      AND mm.active = 1
      AND CAST(rvct.TRANSACTION_DATE AS DATE) = :today
    GROUP BY
        mm.market_name,
        mm.market_master_id
    UNION ALL
    SELECT
        mm.market_name AS market_name,
        mm.market_master_id AS market_master_id,
        0 AS reeler_amount,
        SUM(rvct.AMOUNT) AS external_unit_amount,
        SUM(rvct.AMOUNT) AS total_amount,
        0 AS reeler_deposit_count,
        COUNT(rvct.REELER_VID_CREDIT_TXN_ID)
            AS external_unit_deposit_count
    FROM REELER_VID_CREDIT_TXN rvct
    INNER JOIN eu_virtual_bank_account evba
        ON rvct.VIRTUAL_ACCOUNT =
           evba.virtual_account_number
    INNER JOIN external_unit_registration eur
        ON eur.external_unit_registration_id =
           evba.eu_id
    INNER JOIN market_master mm
        ON evba.market_master_id =
           mm.market_master_id
    WHERE mm.market_type_master_id = 1
      AND mm.active = 1
      AND CAST(rvct.TRANSACTION_DATE AS DATE) = :today
    GROUP BY
        mm.market_name,
        mm.market_master_id
) A
GROUP BY
    market_name,
    market_master_id
ORDER BY total_amount DESC

""")
    public List<Object[]> getSeedMarketCreditReport(
            @Param("today") LocalDate today);

    @Query(nativeQuery = true, value = """
            SELECT r.reeler_id, r.name, r.reeling_license_number,
                   rvba.virtual_account_number,
                    ISNULL(rvcb.CURRENT_BALANCE, 0)     AS current_balance,
                   ISNULL(mm.releer_minimum_balance, 0) AS minimum_balance,
                   r.bank_account_number, r.ifsc_code, r.bank_name, r.branch_name,
                   rvba.market_master_id               AS reeler_market_id
            FROM reeler r
            LEFT JOIN reeler_virtual_bank_account rvba
                ON rvba.reeler_Id = r.reeler_Id AND rvba.active = 1
            LEFT JOIN REELER_VID_CURRENT_BALANCE rvcb
                ON rvcb.reeler_virtual_account_number = rvba.virtual_account_number
            LEFT JOIN market_master mm
                ON mm.market_master_id = rvba.market_master_id
            WHERE r.reeler_id = :reelerId
            """)
    Object[][] getReelerBankDetails(@Param("reelerId") int reelerId);

    @Query(nativeQuery = true, value = """
            SELECT r.reeler_id, r.name, r.reeling_license_number, r.mobile_number,
                   rvba.virtual_account_number,
                   ISNULL(rvcb.CURRENT_BALANCE, 0) AS current_balance,
                   ISNULL(mm.releer_minimum_balance, 0) AS minimum_balance,
                   r.bank_account_number, r.ifsc_code, r.bank_name, r.branch_name
            FROM reeler r
            LEFT JOIN reeler_virtual_bank_account rvba
                ON rvba.reeler_Id = r.reeler_Id
                AND rvba.active = 1
            LEFT JOIN REELER_VID_CURRENT_BALANCE rvcb
                ON rvcb.reeler_virtual_account_number = rvba.virtual_account_number
            LEFT JOIN market_master mm
                ON mm.market_master_id = rvba.market_master_id
            WHERE r.reeling_license_number = :licenseNumber
            """)
    Object[][] getReelerDetailsByLicense(@Param("licenseNumber") String licenseNumber);

    @Query(nativeQuery = true, value = """
            SELECT r.reeler_id AS buyerId, r.name, r.reeling_license_number AS licenseNumber
            FROM reeler r
            INNER JOIN reeler_virtual_bank_account rvba
                ON rvba.reeler_id = r.reeler_id AND rvba.active = 1
            WHERE rvba.market_master_id = :marketId AND r.active = 1
            ORDER BY r.name
            """)
    List<Object[]> getReelerListByMarket(@Param("marketId") int marketId);

}
