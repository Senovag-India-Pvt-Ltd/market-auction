package com.sericulture.marketandauction.repository;

import com.sericulture.marketandauction.helper.LotTransactionQueryConstants;
import com.sericulture.marketandauction.helper.MarketAuctionQueryConstants;
import com.sericulture.marketandauction.model.entity.Lot;
import com.sericulture.marketandauction.service.MarketAuctionReportService;
import org.springframework.cglib.core.Local;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LotRepository extends PagingAndSortingRepository<Lot, BigInteger> {

    public Lot save(Lot lot);

//    @Query("SELECT l FROM Lot l WHERE l.id = :lotId AND CAST(l.auctionDate AS date) = :date AND l.active = true")
//    Optional<Lot> findByIdAndAuctionDateActiveTrue(@Param("lotId") BigInteger lotId, @Param("date") LocalDate date);

    @Query("SELECT l FROM Lot l WHERE l.allottedLotId = :lotNo AND l.auctionDate = :date AND l.active = true")
    List<Lot> findByAllottedLotIdAndAuctionDateActiveTrue(@Param("lotNo") int lotNo, @Param("date") LocalDate date);

    @Query("SELECT l FROM Lot l WHERE l.id = :lotId AND l.auctionDate = :date AND l.active = true")
    Optional<Lot> findByIdAndAuctionDateAndActiveTrue(@Param("lotId") BigInteger lotId, @Param("date") LocalDate date);
    public Iterable<Lot> saveAll(Iterable<Lot> lotList);

    @Query("select max(l.allottedLotId) from Lot l where l.marketId=:marketId and l.auctionDate=:auctionDate")
    public Integer findByMarketIdAndAuctionDate(@Param("marketId") int marketId, @Param("auctionDate") LocalDate auctionDate);

    /*
    @Query("select max(l.allottedLotId) from Lot l where l.marketId=:marketId and l.godownId =:godownId and l.auctionDate=:auctionDate")
    public Integer findByMarketIdAndAuctionDate(@Param("marketId") int marketId, @Param("auctionDate") LocalDate auctionDate);
*/
    @Query("select l.allottedLotId from Lot l where l.marketAuctionId=:marketAuctionId")
    public List<Integer> findAllAllottedLotsByMarketAuctionId(@Param("marketAuctionId") BigInteger marketAuctionId);

    public List<Lot> findAllByMarketAuctionId(BigInteger marketAuctionId);

    public Lot findByMarketIdAndAllottedLotIdAndAuctionDate(int marketId, int allottedLotId, LocalDate auctionDate);

    @Query(nativeQuery = true, value = """
            select  ROW_NUMBER() OVER(ORDER BY l.lot_id ASC) AS row_id,l.lot_id as FARMER_PAYMENT_ID ,l.allotted_lot_id,l.auction_date ,f.first_name,f.middle_name,f.last_name,f.farmer_number,
            f.mobile_number,r.reeling_license_number,
            fba.farmer_bank_name,fba.farmer_bank_branch_name ,fba.farmer_bank_ifsc_code ,fba.farmer_bank_account_number ,
            l.LOT_SOLD_OUT_AMOUNT ,l.MARKET_FEE_FARMER 
            ,rvcb.CURRENT_BALANCE,
            rvba.virtual_account_number
            from 
            dbo.FARMER f
            INNER JOIN dbo.market_auction ma ON ma.farmer_id = f.FARMER_ID 
            INNER JOIN dbo.lot l ON l.market_auction_id =ma.market_auction_id and l.auction_date = ma.market_auction_date 
            INNER JOIN dbo.REELER_AUCTION ra ON ra.REELER_AUCTION_ID  = l.REELER_AUCTION_ID and ra.STATUS ='accepted' and ra.AUCTION_DATE =l.auction_date 
            INNER JOIN dbo.reeler r ON r.reeler_id =ra.REELER_ID  
            LEFT JOIN dbo.reeler_virtual_bank_account rvba ON rvba.reeler_id =r.reeler_id and rvba.market_master_id = ma.market_id
            LEFT JOIN dbo.REELER_VID_CURRENT_BALANCE rvcb ON rvcb.reeler_virtual_account_number= rvba.virtual_account_number
            LEFT JOIN dbo.farmer_address fa ON f.FARMER_ID = fa.FARMER_ID and fa.default_address = 1 
            LEFT JOIN  dbo.farmer_bank_account fba  ON   fba.FARMER_ID  = f.FARMER_ID 
            INNER JOIN dbo.market_master mm on mm.market_master_id = ma.market_id 
            LEFT JOIN dbo.market_type_master mtm ON mtm.market_type_master_id = mm.market_master_id 
             where l.status in ('weighmentcompleted','paymentfailed') 
             and l.market_id =:marketId 
            ORDER by l.lot_id""")
    public Page<Object[]> getAllWeighmentCompletedTxnByMarket(final Pageable pageable, int marketId);


    @Query(nativeQuery = true, value = """
            select  ROW_NUMBER() OVER(ORDER BY l.lot_id ASC) AS row_id,l.lot_id as FARMER_PAYMENT_ID ,l.allotted_lot_id,l.auction_date ,f.first_name,f.middle_name,f.last_name,f.farmer_number,
            f.mobile_number,r.reeling_license_number,
            fba.farmer_bank_name,fba.farmer_bank_branch_name ,fba.farmer_bank_ifsc_code ,fba.farmer_bank_account_number ,
            l.LOT_SOLD_OUT_AMOUNT ,l.MARKET_FEE_FARMER 
            ,rvcb.CURRENT_BALANCE,
            rvba.virtual_account_number
            from 
            dbo.FARMER f
            INNER JOIN dbo.market_auction ma ON ma.farmer_id = f.FARMER_ID 
            INNER JOIN dbo.lot l ON l.market_auction_id =ma.market_auction_id and l.auction_date = ma.market_auction_date 
            INNER JOIN dbo.REELER_AUCTION ra ON ra.REELER_AUCTION_ID  = l.REELER_AUCTION_ID and ra.STATUS ='accepted' and ra.AUCTION_DATE =l.auction_date 
            INNER JOIN dbo.reeler r ON r.reeler_id =ra.REELER_ID  
            LEFT JOIN dbo.reeler_virtual_bank_account rvba ON rvba.reeler_id =r.reeler_id and rvba.market_master_id = ma.market_id
            LEFT JOIN dbo.REELER_VID_CURRENT_BALANCE rvcb ON rvcb.reeler_virtual_account_number= rvba.virtual_account_number
            LEFT JOIN dbo.farmer_address fa ON f.FARMER_ID = fa.FARMER_ID and fa.default_address = 1 
            LEFT JOIN  dbo.farmer_bank_account fba  ON   fba.FARMER_ID  = f.FARMER_ID 
            INNER JOIN dbo.market_master mm on mm.market_master_id = ma.market_id 
            where l.status =:lotStatus 
             and l.auction_date =:paymentDate
             and l.market_id =:marketId 
             and (:lotList is null OR l.allotted_lot_id in (:lotList))
             and fba.farmer_bank_account_number != '' 
             and fba.farmer_bank_ifsc_code !='' 

            ORDER by l.lot_id""")
    public List<Object[]> getAllEligiblePaymentTxnByOptionalLotListAndLotStatus(LocalDate paymentDate, int marketId, List<Integer> lotList,String lotStatus);

    //removed condition              and rvcb.CURRENT_BALANCE > 0.0


    @Query(nativeQuery = true, value = MarketAuctionQueryConstants.PRINT_REPORT_ACCEPTED_LOT_ID)
    public Object[][] getAcceptedLotDetails(LocalDate paymentDate,int marketId,int allottedLotId);

    @Query(nativeQuery = true, value = MarketAuctionQueryConstants.PRINT_REPORT_ACCEPTED_LOT_ID_SEED_COCOON)
    public Object[][] getAcceptedLotDetailsSeedCocoon(LocalDate paymentDate,int marketId,int allottedLotId);


    @Query(nativeQuery = true, value = MarketAuctionQueryConstants.PRINT_REPORT_ACCEPTED_LOT_ID_SILK)
    public Object[][] getAcceptedLotDetailsForSilk(LocalDate paymentDate,int marketId,int allottedLotId);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.PRINT_REPORT_NEWLY_CREATED_LOT_ID)
    public Object[][] getNewlyCreatedLotDetails(LocalDate paymentDate,int marketId,int allottedLotId);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.PRINT_REPORT_NEWLY_CREATED_LOT_ID_SEED)
    public Object[][] getNewlyCreatedLotDetailsSeedCocoon(LocalDate paymentDate,int marketId,int allottedLotId);
//
@Query(nativeQuery = true, value = MarketAuctionQueryConstants.PRINT_REPORT_NEWLY_CREATED_LOT_ID_SEED_NEW)
public Object[][] getNewlyCreatedLotDetailsSeedCocoons(LocalDate auctionDate, int marketId, int allottedLotId);


    @Query(nativeQuery = true, value = MarketAuctionQueryConstants.PRINT_REPORT_NEWLY_CREATED_LOT_ID_SEED_NEW_TRIPLET)
    public Object[][] getNewlyCreatedLotDetailsSeedCocoonsTriplet(LocalDate auctionDate, int marketId, int allottedLotId);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.PRINT_REPORT_NEWLY_CREATED_LOT_ID_SILK)
    public Object[][] getNewlyCreatedLotDetailsForSilk(LocalDate paymentDate,int marketId,int allottedLotId);


    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.AUCTION_DATE_LIST_BY_LOT_STATUS)
    public List<Object> getAllWeighmentCompletedOrReadyForPaymentAuctionDatesByMarket(int marketId,String lotStatus);

    @Query("select allottedLotId from Lot lot where status!='cancelled' and auctionDate=:auctionDate and marketId=:marketId and createdBy=:userName order by createdDate limit 1")
    public int findByMarketIdAndAuctionDateAndCreatedBy(int marketId, LocalDate auctionDate,String userName);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.DTR_ONLINE_REPORT_QUERY)
    public List<Object[]> getDTROnlineReport(int marketId, LocalDate fromDate,LocalDate toDate,List<Integer> reelerIdList);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.DTR_ONLINE_REPORT_QUERY_SILK)
    public List<Object[]> getDTROnlineReportSilk(int marketId, LocalDate fromDate,LocalDate toDate,List<Integer> traderLicenseList);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.DTR_ONLINE_REPORT_QUERY)
    public List<Object[]> getDTROnlineReportForBlankReport(int marketId, LocalDate fromDate,LocalDate toDate,List<Integer> reelerIdList);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.BLANK_REPORT)
    public List<Object[]> getBlankReport(int marketId, LocalDate fromDate,LocalDate toDate,List<Integer> reelerIdList);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.BLANK_REPORT_SILK)
    public List<Object[]> getBlankReportSilk(int marketId, LocalDate fromDate,LocalDate toDate,List<Integer> traderLicenseList);


    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.DTR_ONLINE_REPORT_QUERY_FOR_CASH)
    public List<Object[]> getDTROnlineReportForCash(int marketId, LocalDate fromDate,LocalDate toDate,List<Integer> reelerIdList);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.DTR_ONLINE_REPORT_QUERY_FOR_CASH_SILK)
    public List<Object[]> getDTROnlineReportForCashSilk(int marketId, LocalDate fromDate,LocalDate toDate,List<Integer> traderLicenseList);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.DTR_ONLINE_REPORT_QUERY_FOR_CASH_BLANK_REPORT)
    public List<Object[]> getDTROnlineReportForCashForBlankReport(int marketId, LocalDate fromDate,LocalDate toDate,List<Integer> reelerIdList);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.DTR_ONLINE_REPORT_QUERY_FOR_CASH_BLANK_REPORT_SILK)
    public List<Object[]> getDTROnlineReportForCashForBlankReportSilk(int marketId, LocalDate fromDate,LocalDate toDate,List<Integer> traderLicenseList);


    @Query(nativeQuery = true, value = MarketAuctionQueryConstants.PENDING_REPORT_ACCEPTED_LOTS)
    public List<Object[]> getAcceptedLotDetailsForPendingReport(LocalDate paymentDate,int marketId);

    @Query(nativeQuery = true, value = MarketAuctionQueryConstants.PENDING_REPORT_NULL_ACCEPTED_LOTS)
    public List<Object[]> getAcceptedLotDetailsNullForPendingReport(LocalDate paymentDate,int marketId);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.PENDING_REPORT_NEWLY_CREATED_LOTS)
    public List<Object[]> getNewlyCreatedLotDetailsForPendingReport(LocalDate paymentDate,int marketId,List lotList);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.PENDING_REPORT_NEWLY_CREATED_LOTS_NULL)
    public List<Object[]> getNewlyCreatedLotDetailsNullForPendingReport(LocalDate paymentDate,int marketId);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.FARMER_TXN_REPORT)
    public List<Object[]> getFarmerReport(int marketId, LocalDate fromDate,LocalDate toDate,String farmerNumber);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.FARMER_TXN_REPORT_CASH)
    public List<Object[]> getFarmerReportCash(int marketId, LocalDate fromDate,LocalDate toDate,String farmerNumber);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.DASHBOARD_COUNT)
    public List<Object[]> getDashboardCount(int marketId, LocalDate marketAuctionDate);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.DASHBOARD_COUNT_SILK_TYPE)
    public List<Object[]> getDashboardCountSilk(int marketId, LocalDate marketAuctionDate);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.MONTHLY_DISTRICT_REPORT)
    public List<Object[]> getMonthlyDistrictReport(int marketId, LocalDate startDate, LocalDate endDate);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.MONTHLY_DISTRICT_REPORT_SILK)
    public List<Object[]> getMonthlyDistrictReportSilk(int marketId, LocalDate startDate, LocalDate endDate);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.SUM_OF_MONTHLY_DISTRICT_REPORT)
    public List<Object[]> getSumOfMonthlyDistrictReport(int marketId, LocalDate startDate, LocalDate endDate);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.SUM_OF_MONTHLY_DISTRICT_REPORT_SILK)
    public List<Object[]> getSumOfMonthlyDistrictReportSilk(int marketId, LocalDate startDate, LocalDate endDate);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.AUCTION_STARTED)
    public List<Object[]> getIsAuctionStarted(int marketId, String marketAuctionDate);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.ACCEPTANCE_STARTED)
    public List<Object[]> getIsAcceptanceStarted(int marketId, String marketAuctionDate);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.GET_MARKET_NAME)
    public List<Object[]> getMarketName(int marketId);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.GET_RACE_NAME)
    public List<Object[]> getRaceName(int raceId);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.PAYMENT_SUCCESS_LOTS)
    public List<Object[]> getPaymentSuccessLots(int marketId, LocalDate fromDate,LocalDate toDate,List<Integer> reelerIdList);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.PAYMENT_SUCCESS_LOTS_SILK)
    public List<Object[]> getPaymentSuccessLotsSilk(int marketId, LocalDate fromDate,LocalDate toDate,List<Integer> traderLicenseList);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.PAYMENT_SUCCESS_LOTS_FOR_BLANK_REPORT)
    public List<Object[]> getPaymentSuccessLotsForBlankReport(int marketId, LocalDate fromDate,LocalDate toDate,List<Integer> reelerIdList);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.PAYMENT_SUCCESS_LOTS_FOR_BLANK_REPORT_SILK)
    public List<Object[]> getPaymentSuccessLotsForBlankReportSilk(int marketId, LocalDate fromDate,LocalDate toDate,List<Integer> traderLicenseList);


    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.REELER_PENDING_REPORT)
    public List<Object[]> getReelerPendingReport(int marketId);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.break_down_of_lot_amount)
    public List<Object[]> getLotBreakDownStatus(int fromAmount, int toAmount, int marketId, LocalDate fromDate,LocalDate toDate);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.break_down_of_lot_amount_silk)
    public List<Object[]> getLotBreakDownStatusSilk(int fromAmount, int toAmount, int marketId, LocalDate fromDate,LocalDate toDate);


    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.break_down_of_lot_amount_by_dist)
    public List<Object[]> getLotBreakDownStatusByDist(int fromAmount, int toAmount, int marketId, LocalDate fromDate,LocalDate toDate, Long districtId);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.break_down_of_lot_amount_by_dist_silk)
    public List<Object[]> getLotBreakDownStatusByDistSilk(int fromAmount, int toAmount, int marketId, LocalDate fromDate,LocalDate toDate, Long districtId);


    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.avg_of_lot_amount)
    public List<Object[]> getAvgLotStatus(int marketId, LocalDate fromDate,LocalDate toDate);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.avg_of_lot_amount_silk)
    public List<Object[]> getAvgLotStatusSilk(int marketId, LocalDate fromDate,LocalDate toDate);


    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.avg_of_lot_amount_by_dist)
    public List<Object[]> getAvgLotStatusByDist(int marketId, LocalDate fromDate,LocalDate toDate, Long districtId);


    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.avg_of_lot_amount_by_dist_silk)
    public List<Object[]> getAvgLotStatusByDistSilk(int marketId, LocalDate fromDate,LocalDate toDate, Long districtId);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.less_than_lot_amount)
    public List<Object[]> getLessLotStatus(int marketId, LocalDate fromDate,LocalDate toDate, float amount);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.less_than_lot_amount_silk)
    public List<Object[]> getLessLotStatusSilk(int marketId, LocalDate fromDate,LocalDate toDate, float amount);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.less_than_lot_amount_dist)
    public List<Object[]> getLessLotStatusByDist(int marketId, LocalDate fromDate,LocalDate toDate, float amount, Long districtId);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.less_than_lot_amount_dist_silk)
    public List<Object[]> getLessLotStatusByDistSilk(int marketId, LocalDate fromDate,LocalDate toDate, float amount, Long districtId);


    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.greater_than_lot_amount)
    public List<Object[]> getGreaterLotStatus(int marketId, LocalDate fromDate,LocalDate toDate, float amount);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.greater_than_lot_amount_silk)
    public List<Object[]> getGreaterLotStatusSilk(int marketId, LocalDate fromDate,LocalDate toDate, float amount);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.greater_than_lot_amount_dist)
    public List<Object[]> getGreaterLotStatusByDist(int marketId, LocalDate fromDate,LocalDate toDate, float amount, Long districtId);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.greater_than_lot_amount_dist_silk)
    public List<Object[]> getGreaterLotStatusByDistSilk(int marketId, LocalDate fromDate,LocalDate toDate, float amount, Long districtId);


    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.total_lot_status)
    public List<Object[]> getTotalLotStatus(int marketId, LocalDate fromDate,LocalDate toDate);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.total_lot_status_silk)
    public List<Object[]> getTotalLotStatusSilk(int marketId, LocalDate fromDate,LocalDate toDate);


    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.total_lot_status_by_dist)
    public List<Object[]> getTotalLotStatusByDist(int marketId, LocalDate fromDate,LocalDate toDate, Long districtId);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.total_lot_status_by_dist_silk)
    public List<Object[]> getTotalLotStatusByDistSilk(int marketId, LocalDate fromDate,LocalDate toDate, Long districtId);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.state_wise_lot_status)
    public List<Object[]> getStateWiseLotStatus(int marketId, LocalDate auctionDate);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.all_state_wise_lot_status)
    public List<Object[]> getAllStateWiseLotStatus(int marketId, LocalDate fromDate,LocalDate toDate);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.all_state_wise_lot_status_silk)
    public List<Object[]> getAllStateWiseLotStatusSilk(int marketId, LocalDate fromDate,LocalDate toDate);


    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.all_state_wise_lot_status_by_dist)
    public List<Object[]> getAllStateWiseLotStatusByDist(int marketId, LocalDate fromDate,LocalDate toDate, Long districtId);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.all_state_wise_lot_status_by_dist_silk)
    public List<Object[]> getAllStateWiseLotStatusByDistSilk(int marketId, LocalDate fromDate,LocalDate toDate, Long districtId);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.gender_wise_lot_status)
    public List<Object[]> getGenderWiseLotStatus(int marketId, LocalDate fromDate,LocalDate toDate);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.gender_wise_lot_status_silk)
    public List<Object[]> getGenderWiseLotStatusSilk(int marketId, LocalDate fromDate,LocalDate toDate);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.race_market_wise_lot_status)
    public List<Object[]> getRaceWiseStatus(int marketId, LocalDate fromDate,LocalDate toDate,int raceId);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.race_market_wise_lot_status_silk)
    public List<Object[]> getRaceWiseStatusSilk(int marketId, LocalDate fromDate,LocalDate toDate,int raceId);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.race_market_wise_lot_status_dist)
    public List<Object[]> getRaceWiseStatusByDist(int marketId,LocalDate fromDate,LocalDate toDate, Long districtId,int raceId);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.race_market_wise_lot_status_dist_silk)
    public List<Object[]> getRaceWiseStatusByDistSilk(int marketId,LocalDate fromDate,LocalDate toDate, Long districtId,int raceId);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.gender_wise_lot_status_by_dist)
    public List<Object[]> getGenderWiseLotStatusByDist(int marketId, LocalDate fromDate,LocalDate toDate, Long districtId);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.gender_wise_lot_status_by_dist_silk)
    public List<Object[]> getGenderWiseLotStatusByDistSilk(int marketId, LocalDate fromDate,LocalDate toDate, Long districtId);


    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.reeler_report_for__app)
    public List<Object[]> getReelerReportForApp(int reelerId, int marketId, LocalDate auctionDate);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.reeler_current_balance)
    public List<Object[]> getReelerCurrentBalance(String virtualNumber);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.reeler_virtual_account)
    public List<Object[]> getReelerVirtualAccount(int reelerId,int marketId);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.reeler_deposited_amount)
    public List<Object[]> getReelerDepositedAmount(String virtualNumber, LocalDate auctionDate);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.reeler_purchase_amount)
    public List<Object[]> getReelerPurchaseAmount(int reelerId, int marketId, LocalDate auctionDate);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.total_reeler_balance)
    public List<Object[]> getTotalReelerBalance(int marketId);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.total_credit_txn_balance_today)
    public List<Object[]> getTotalCreditTxnToday(int marketId, LocalDate auctionDate);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.total_debit_txn_balance_today)
    public List<Object[]> geTotalDebitTxnToday(int marketId, LocalDate auctionDate);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.AVERAGE_REPORT_FOR_YEAR)
    public List<Object[]> getAverageReportForYearsReport(LocalDate startDate, LocalDate endDate, int raceId, int marketId);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.ACTIVE_RACE_FOR_AVERAGE_REPORT)
    public List<Object[]> getActiveRaceForAverageReport(LocalDate startDate, LocalDate endDate, int marketId);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.AVERAGE_COCOON_REPORT)
    public List<Object[]> getAverageCocoonReport(LocalDate startDate, LocalDate endDate, int marketId);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.MARKETS_FOR_DTR_REPORT)
    public List<Object[]> getMarketsForDTRReport();

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.RACES_BY_MARKET)
    public List<Object[]> getRacesByMarket(int marketId);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.RACES_BY_NOT_MARKET)
    public List<Object[]> getRacesByMarketNotIn(List<Integer> marketList);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.DTR_REPORT)
    public List<Object[]> getDTRReport(int marketId, int raceId, LocalDate auctionDate);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.SUM_DTR_REPORT)
    public List<Object[]> getSumDTRReport(LocalDate auctionDate);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.SUM_DTR_REPORT_BY_RACE)
    public List<Object[]> getSumDTRReportByRace(int raceId, LocalDate auctionDate);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.GET_ALL_RACES)
    public List<Object[]> getAllRaces();

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.TOTAL_BY_MONTH)
    public List<Object[]> getTotalByMonth(LocalDate startDate, LocalDate endDate);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.AUDIO_VISUAL_REPORT)
    public List<Object[]> getAudioVisualReport(int marketId, int raceId, LocalDate startDate, LocalDate endDate);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.MONTHLY_REPORT)
    public List<Object[]> getMonthlyReport(LocalDate startDate, LocalDate endDate);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.MONTHLY_REPORT_BY_RACE)
    public List<Object[]> getMonthlyReportByRace(LocalDate startDate, LocalDate endDate, int raceId);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.MONTHLY_REPORT_BY_STATE)
    public List<Object[]> getMonthlyReportByState(LocalDate startDate, LocalDate endDate, int stateId);
    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.GET_STATE_BY_STATE_NAME)
    public List<Object[]> getStateByStateName(String stateName);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.GET_OTHER_STATES_DATA)
    public List<Object[]> getOtherStatesData(LocalDate startDate, LocalDate endDate,List<Integer> states);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.GET_OVER_ALL_STATE_SUM)
    public List<Object[]> getOverAllStateSum(int raceId, LocalDate startDate, LocalDate endDate);
    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.MARKET_REPORT)
    public List<Object[]> getMarketReport(int marketId, int raceId, LocalDate startDate, LocalDate endDate);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.MARKET_REPORT_BY_STATE_AND_RACE)
    public List<Object[]> getMarketReportByStateAndRace(int stateId, int raceId, LocalDate startDate, LocalDate endDate);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.MARKET_REPORT_BY_STATE_AND_RACE_NOT_IN)
    public List<Object[]> getMarketReportByStateAndRaceNotIn(List<Integer> states, int raceId, LocalDate startDate, LocalDate endDate);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.DIVISION_WISE_SUM)
    public List<Object[]> getDivisionSum(int divisionMasterId, int raceId, LocalDate startDate, LocalDate endDate);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.MARKET_REPORT_SUM)
    public List<Object[]> getMarketReportSum(int marketId, LocalDate startDate, LocalDate endDate);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.DISTRICT_BY_FARMER_ADDRESS)
    public List<Object[]> getDistrictByFarmerAddress();
    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.VAHIVAATU_REPORT)
    public List<Object[]> get27bReport(int districtId, int raceId, LocalDate startDate, LocalDate endDate);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.VAHIVAATU_REPORT_TOTAL)
    public List<Object[]> getVahivaatuTotal(int raceId, LocalDate startDate, LocalDate endDate);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.GET_ALL_DIVISIONS)
    public List<Object[]> getDivisions();

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.GET_MARKET_BY_DIVISION)
    public List<Object[]> getMarketByDivision(int divisionId);

//    @Query(nativeQuery = true, value = """
//           SELECT ROW_NUMBER() OVER (ORDER BY l.lot_id ASC) AS row_id,
//           l.lot_id AS FARMER_PAYMENT_ID,
//           lg.allotted_lot_id,
//           lg.auction_date,
//           f.first_name,
//           f.middle_name,
//           f.last_name,
//           f.farmer_number,
//           f.mobile_number,
//           lg.lot_groupage_id,
//           lg.buyer_type,
//           lg.lot_weight,
//           lg.amount,
//           lg.market_fee,
//           lg.sold_amount,
//           CASE
//               WHEN lg.buyer_type = 'Reeler' THEN r.name
//               WHEN lg.buyer_type = 'ExternalStakeHolders' THEN es.name
//               ELSE NULL
//           END AS buyer_name,
//           CASE
//               WHEN lg.buyer_type = 'Reeler' THEN r.reeler_id
//               WHEN lg.buyer_type = 'ExternalStakeHolders' THEN es.external_unit_registration_id
//               ELSE NULL
//           END AS buyer_id,
//           l.lot_id
//        FROM dbo.FARMER f
//        INNER JOIN dbo.market_auction ma ON ma.farmer_id = f.FARMER_ID
//        INNER JOIN dbo.lot l ON l.market_auction_id = ma.market_auction_id AND l.auction_date = ma.market_auction_date
//        LEFT JOIN dbo.farmer_address fa ON f.FARMER_ID = fa.FARMER_ID AND fa.default_address = 1
//        INNER JOIN dbo.market_master mm ON mm.market_master_id = ma.market_id
//        LEFT JOIN dbo.market_type_master mtm ON mtm.market_type_master_id = mm.market_master_id
//        LEFT JOIN lot_groupage lg ON l.lot_id = lg.lot_id
//        LEFT JOIN dbo.reeler r ON lg.buyer_id = r.reeler_id AND lg.buyer_type = 'Reeler'
//        LEFT JOIN dbo.external_unit_registration es ON lg.buyer_id = es.external_unit_registration_id AND lg.buyer_type = 'ExternalStakeHolders'
//        WHERE l.status = 'weighmentcompleted'
//        and l.market_id =:marketId
//        ORDER by lg.lot_id""")
@Query(nativeQuery = true, value = """
            SELECT 
    ROW_NUMBER() OVER(ORDER BY lg.lot_groupage_id ASC) AS row_id,
    lg.lot_groupage_id AS FARMER_PAYMENT_ID,
    l.allotted_lot_id,
    l.auction_date,
    f.first_name,
    f.middle_name,
    f.last_name,
    f.farmer_number,
    f.mobile_number,
    lg.buyer_type,

    CASE
        WHEN lg.buyer_type = 'Reeling' THEN r.name
        ELSE eur.license_number
    END AS name,

    lg.lot_weight,
    lg.amount,
    lg.sold_amount,
    lg.market_fee,

    fba.farmer_bank_name,
    fba.farmer_bank_branch_name,
    fba.farmer_bank_ifsc_code,
    fba.farmer_bank_account_number,

    rvcb.CURRENT_BALANCE,

    COALESCE(rvba.virtual_account_number, evba.virtual_account_number) AS virtual_account_number

FROM dbo.FARMER f

INNER JOIN dbo.market_auction ma
    ON ma.farmer_id = f.FARMER_ID

INNER JOIN dbo.lot l
    ON l.market_auction_id = ma.market_auction_id
    AND l.auction_date = ma.market_auction_date

INNER JOIN dbo.lot_groupage lg
    ON lg.lot_id = l.lot_id
    AND lg.auction_date = ma.market_auction_date

-- Reeler
LEFT JOIN dbo.reeler r
    ON lg.buyer_type = 'Reeling'
    AND r.reeler_id = lg.buyer_id 

LEFT JOIN dbo.reeler_virtual_bank_account rvba
    ON lg.buyer_type = 'Reeling'
    AND rvba.reeler_id = r.reeler_id
    AND rvba.market_master_id = ma.market_id
    
        -- External
        LEFT JOIN dbo.external_unit_registration eur
    ON (lg.buyer_type <> 'Reeling' OR lg.buyer_type IS NULL)
    AND eur.external_unit_registration_id = lg.external_unit_id
    

LEFT JOIN dbo.external_unit_type_master et
    ON eur.external_unit_type_id = et.external_unit_type_id

LEFT JOIN dbo.eu_virtual_bank_account evba
    ON lg.buyer_type <> 'Reeling'
    AND evba.eu_id = eur.external_unit_registration_id
    AND evba.market_master_id = ma.market_id

-- Balance
LEFT JOIN dbo.REELER_VID_CURRENT_BALANCE rvcb
    ON rvcb.reeler_virtual_account_number =
        COALESCE(rvba.virtual_account_number, evba.virtual_account_number)

LEFT JOIN dbo.farmer_address fa
    ON f.FARMER_ID = fa.FARMER_ID
    AND fa.default_address = 1

LEFT JOIN dbo.farmer_bank_account fba 
    ON fba.FARMER_ID = f.FARMER_ID

INNER JOIN dbo.market_master mm
    ON mm.market_master_id = ma.market_id

         WHERE lg.status IN ('DISTRIBUTED', 'paymentfailed')
             AND l.market_id = :marketId
             AND (
                 lg.buyer_type = 'Reeling'
                 OR (
                     lg.buyer_type = 'RSP'
                     AND et.payment_via_bank = 1
                 )
             )
ORDER BY l.lot_id;
""")
public Page<Object[]> getAllWeighmentCompletedTxnForSeedMarketByMarket(final Pageable pageable, int marketId);

    @Query(nativeQuery = true, value = """
SELECT
    ROW_NUMBER() OVER(ORDER BY lg.lot_groupage_id ASC) AS row_id,
    lg.lot_groupage_id AS FARMER_PAYMENT_ID,
    l.allotted_lot_id,
    l.auction_date,
    f.first_name,
    f.middle_name,
    f.last_name,
    f.farmer_number,
    f.mobile_number,
    lg.buyer_type,

    CASE
        WHEN lg.buyer_type = 'Reeling' THEN r.name
        ELSE eur.license_number
    END AS name,
    
        lg.lot_weight,
    lg.amount,
    lg.sold_amount,
    lg.market_fee,

    fba.farmer_bank_name,
    fba.farmer_bank_branch_name,
    fba.farmer_bank_ifsc_code,
    fba.farmer_bank_account_number,


    rvcb.CURRENT_BALANCE,

    COALESCE(rvba.virtual_account_number, evba.virtual_account_number) AS virtual_account_number

FROM dbo.FARMER f

INNER JOIN dbo.market_auction ma
    ON ma.farmer_id = f.FARMER_ID

INNER JOIN dbo.lot l
    ON l.market_auction_id = ma.market_auction_id
    AND l.auction_date = ma.market_auction_date

INNER JOIN dbo.lot_groupage lg
    ON lg.lot_id = l.lot_id
    AND lg.auction_date = ma.market_auction_date

-- Reeler
LEFT JOIN dbo.reeler r
    ON lg.buyer_type = 'Reeling'
    AND r.reeler_id = lg.buyer_id

LEFT JOIN dbo.reeler_virtual_bank_account rvba
    ON lg.buyer_type = 'Reeling'
    AND rvba.reeler_id = r.reeler_id
    AND rvba.market_master_id = ma.market_id

-- External
LEFT JOIN dbo.external_unit_registration eur
    ON (lg.buyer_type <> 'Reeling' OR lg.buyer_type IS NULL)
    AND eur.external_unit_registration_id = lg.external_unit_id

LEFT JOIN dbo.external_unit_type_master et
    ON eur.external_unit_type_id = et.external_unit_type_id

LEFT JOIN dbo.eu_virtual_bank_account evba
    ON (lg.buyer_type <> 'Reeling' OR lg.buyer_type IS NULL)
    AND evba.eu_id = eur.external_unit_registration_id
    AND evba.market_master_id = ma.market_id

-- Balance
LEFT JOIN dbo.REELER_VID_CURRENT_BALANCE rvcb
    ON rvcb.reeler_virtual_account_number =
        COALESCE(rvba.virtual_account_number, evba.virtual_account_number)

LEFT JOIN dbo.farmer_address fa
    ON f.FARMER_ID = fa.FARMER_ID
    AND fa.default_address = 1

LEFT JOIN dbo.farmer_bank_account fba
    ON fba.FARMER_ID = f.FARMER_ID

INNER JOIN dbo.market_master mm
    ON mm.market_master_id = ma.market_id

WHERE lg.status IN ('DISTRIBUTED','paymentfailed','readyforpayment')
    AND l.market_id = :marketId

    -- ✅ Added from your old query
    AND lg.status = :lotStatus
    AND l.auction_date = :paymentDate

    -- ✅ Optional lot filter
    AND (:lotList IS NULL OR 
         l.allotted_lot_id IN (:lotList))

    -- ✅ Bank validation (from your old query)
    AND fba.farmer_bank_account_number <> ''
    AND fba.farmer_bank_ifsc_code <> ''

    -- ✅ MOST IMPORTANT CONDITION (unchanged)
    AND (
        lg.buyer_type = 'Reeling'
        OR (
            (lg.buyer_type <> 'Reeling' OR lg.buyer_type IS NULL)
            AND et.payment_via_bank = 1
        )
    )

ORDER BY lg.lot_groupage_id
""")
    public List<Object[]> getAllEligiblePaymentTxnByOptionalLotListAndLotStatusForSeedMarket(LocalDate paymentDate, int marketId, List<Integer> lotList,String lotStatus);

    @Query(nativeQuery = true, value = """
SELECT DISTINCT lg.auction_date
FROM dbo.lot_groupage lg

INNER JOIN dbo.lot l
    ON l.lot_id = lg.lot_id

LEFT JOIN dbo.external_unit_registration eur
    ON (lg.buyer_type IS NULL OR lg.buyer_type <> 'Reeling')
    AND eur.external_unit_registration_id = lg.external_unit_id

LEFT JOIN dbo.external_unit_type_master et
    ON eur.external_unit_type_id = et.external_unit_type_id

WHERE  lg.status = :lotStatus
    AND l.market_id = :marketId
    AND (
        lg.buyer_type = 'Reeling'
        OR (
            (lg.buyer_type IS NULL OR lg.buyer_type <> 'Reeling')
            AND et.payment_via_bank = 1
        )
    )
ORDER BY lg.auction_date DESC
""")
    public List<Object> getAllWeighmentCompletedOrReadyForPaymentAuctionDatesByMarketForSeedMarket(int marketId,String lotStatus);



//    @Query(nativeQuery = true, value = LotTransactionQueryConstants.QUERY_ELIGIBLE_FOR_PAYMENT_FOR_SEED_MARKET_LOTS_CASH)
//    public List<Object[]> getAllEligiblePaymentTxnByOptionalLotListAndLotStatusForCashPaymentModeForSeedMarket(LocalDate paymentDate, int marketId, List<Long> lotList, String lotStatus);

//    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.AUCTION_DATE_LIST_BY_LOT_STATUS_FOR_SEED_MARKET_CASH_PAYMENT)
//    public List<Object> getAllWeighmentCompletedOrReadyForPaymentsSeedMarketAuctionDatesByMarketCashPayment(int marketId,String lotStatus);

    @Query(nativeQuery = true, value = LotTransactionQueryConstants.QUERY_ELIGIBLE_FOR_PAYMENT_LOTS_ONLINE)
    public List<Object[]> getAllEligiblePaymentTxnByOptionalLotListAndLotStatusForOnlinePaymentMode(LocalDate paymentDate, int marketId, List<Long> lotList, String lotStatus);

    @Query(nativeQuery = true, value = LotTransactionQueryConstants.QUERY_ELIGIBLE_FOR_PAYMENT_LOTS_CASH)
    public List<Object[]> getAllEligiblePaymentTxnByOptionalLotListAndLotStatusForCashPaymentMode(LocalDate paymentDate, int marketId, List<Long> lotList, String lotStatus);

//    @Query("SELECT l FROM Lot l WHERE l.allottedLotId = :lotNo AND l.auctionDate = :date AND l.active = true")
//    Optional<Lot> findByAllottedLotIdAndAuctionDateActiveTrue(@Param("lotNo") int lotNo, @Param("date") LocalDate date);
//
//    @Query("SELECT l FROM Lot l WHERE l.id = :lotId AND l.auctionDate = :date AND l.active = true")
//    Optional<Lot> findByIdAndAuctionDateAndActiveTrue(@Param("lotId") BigInteger lotId, @Param("date") LocalDate date);

    @Query(nativeQuery = true, value = """
            SELECT\s
                l.allotted_lot_id,
                l.auction_date,
            
                lg.buyer_type,
            
                CASE\s
                    WHEN lg.buyer_type = 'Reeling' THEN r.name
                    ELSE eur.name
                END AS bidder_name,
            
                CASE\s
                    WHEN lg.buyer_type = 'Reeling' THEN r.reeling_license_number
                    ELSE eur.license_number
                END AS license_number,
            
                lg.lot_weight,
                lg.amount,
                lg.sold_amount,
                lg.market_fee,
            
                mm.market_name,
            
                lg.created_date,
                lg.lot_groupage_id
            
            FROM lot l
            
            -- ✅ IMPORTANT CHANGE
            INNER JOIN lot_groupage lg\s
                ON lg.lot_id = l.lot_id \s
            
            LEFT JOIN reeler r\s
                ON lg.buyer_type = 'Reeling'
                AND r.reeler_id = lg.buyer_id
            
            LEFT JOIN external_unit_registration eur\s
                ON lg.buyer_type IN ('RSP','NSSO','Govt Grainage')
                AND eur.external_unit_registration_id = lg.external_unit_id
            
            LEFT JOIN market_auction ma\s
                ON ma.market_auction_id = l.market_auction_id
            
            LEFT JOIN market_master mm\s
                ON mm.market_master_id = ma.market_id
            
            WHERE\s
                l.market_id = :marketId
                AND l.auction_date = :auctionDate
            
            ORDER BY l.lot_id ASC;
            
            """)
    List<Object[]> getSeedMarketBiddingReport(
            @Param("marketId") int marketId,
            @Param("auctionDate") LocalDate auctionDate
    );
}
