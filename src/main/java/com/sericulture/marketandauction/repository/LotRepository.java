package com.sericulture.marketandauction.repository;

import com.sericulture.marketandauction.helper.LotTransactionQueryConstants;
import com.sericulture.marketandauction.helper.MarketAuctionQueryConstants;
import com.sericulture.marketandauction.model.entity.Lot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;

public interface LotRepository extends PagingAndSortingRepository<Lot, BigInteger> {

    public Lot save(Lot lot);

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
             where l.status ='weighmentcompleted' 
             and l.market_id =:marketId 
            ORDER by l.lot_id""")
    public Page<Object[]> getAllWeighmentCompletedTxnByMarket(final Pageable pageable, int marketId);


    @Query(nativeQuery = true, value = LotTransactionQueryConstants.QUERY_ELIGIBLE_FOR_PAYMENT_LOTS_ONLINE)
    public List<Object[]> getAllEligiblePaymentTxnByOptionalLotListAndLotStatusForOnlinePaymentMode(LocalDate paymentDate, int marketId, List<Long> lotList, String lotStatus);

    @Query(nativeQuery = true, value = LotTransactionQueryConstants.QUERY_ELIGIBLE_FOR_PAYMENT_LOTS_CASH)
    public List<Object[]> getAllEligiblePaymentTxnByOptionalLotListAndLotStatusForCashPaymentMode(LocalDate paymentDate, int marketId, List<Long> lotList, String lotStatus);


    @Query(nativeQuery = true, value = MarketAuctionQueryConstants.PRINT_REPORT_ACCEPTED_LOT_ID)
    public Object[][] getAcceptedLotDetails(LocalDate paymentDate,int marketId,int allottedLotId);

    
    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.PRINT_REPORT_NEWLY_CREATED_LOT_ID)
    public Object[][] getNewlyCreatedLotDetails(LocalDate paymentDate,int marketId,int allottedLotId);


    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.AUCTION_DATE_LIST_BY_LOT_STATUS_ONLINE_PAYMENT)
    public List<Object> getAllWeighmentCompletedOrReadyForPaymentAuctionDatesByMarket(int marketId,String lotStatus);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.AUCTION_DATE_LIST_BY_LOT_STATUS_CASH_PAYMENT)
    public List<Object> getAllWeighmentCompletedOrReadyForPaymentAuctionDatesByMarketCashPayment(int marketId,String lotStatus);

    @Query("select allottedLotId from Lot lot where status!='cancelled' and auctionDate=:auctionDate and marketId=:marketId and createdBy=:userName order by createdDate limit 1")
    public int findByMarketIdAndAuctionDateAndCreatedBy(int marketId, LocalDate auctionDate,String userName);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.DTR_ONLINE_REPORT_QUERY)
    public List<Object[]> getDTROnlineReport(int marketId, LocalDate fromDate,LocalDate toDate,List<Integer> reelerIdList);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.DTR_ONLINE_REPORT_QUERY_FOR_CASH)
    public List<Object[]> getDTROnlineReportForCash(int marketId, LocalDate fromDate,LocalDate toDate,List<Integer> reelerIdList);


    @Query(nativeQuery = true, value = MarketAuctionQueryConstants.PENDING_REPORT_ACCEPTED_LOTS)
    public List<Object[]> getAcceptedLotDetailsForPendingReport(LocalDate paymentDate,int marketId);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.PENDING_REPORT_NEWLY_CREATED_LOTS)
    public List<Object[]> getNewlyCreatedLotDetailsForPendingReport(LocalDate paymentDate,int marketId,List lotList);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.FARMER_TXN_REPORT)
    public List<Object[]> getFarmerReport(int marketId, LocalDate fromDate,LocalDate toDate,String farmerNumber);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.DASHBOARD_COUNT)
    public List<Object[]> getDashboardCount(int marketId, LocalDate marketAuctionDate);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.AUCTION_STARTED)
    public List<Object[]> getIsAuctionStarted(int marketId, String marketAuctionDate);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.ACCEPTANCE_STARTED)
    public List<Object[]> getIsAcceptanceStarted(int marketId, String marketAuctionDate);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.GET_MARKET_NAME)
    public List<Object[]> getMarketName(int marketId);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.PAYMENT_SUCCESS_LOTS)
    public List<Object[]> getPaymentSuccessLots(int marketId, LocalDate fromDate,LocalDate toDate,List<Integer> reelerIdList);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.REELER_PENDING_REPORT)
    public List<Object[]> getReelerPendingReport(int marketId);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.break_down_of_lot_amount)
    public List<Object[]> getLotBreakDownStatus(int fromAmount, int toAmount, int marketId, LocalDate auctionDate);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.avg_of_lot_amount)
    public List<Object[]> getAvgLotStatus(int marketId, LocalDate auctionDate);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.less_than_lot_amount)
    public List<Object[]> getLessLotStatus(int marketId, LocalDate auctionDate, float amount);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.greater_than_lot_amount)
    public List<Object[]> getGreaterLotStatus(int marketId, LocalDate auctionDate, float amount);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.total_lot_status)
    public List<Object[]> getTotalLotStatus(int marketId, LocalDate auctionDate);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.state_wise_lot_status)
    public List<Object[]> getStateWiseLotStatus(int marketId, LocalDate auctionDate);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.race_wise_lot_status)
    public List<Object[]> getRaceWiseStatus(int marketId, LocalDate auctionDate);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.reeler_report_for__app)
    public List<Object[]> getReelerReportForApp(int reelerId, int marketId, LocalDate auctionDate);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.reeler_current_balance)
    public List<Object[]> getReelerCurrentBalance(int reelerId);

    @Query(nativeQuery = true,value = MarketAuctionQueryConstants.reeler_deposited_amount)
    public List<Object[]> getReelerDepositedAmount(int reelerId, LocalDate auctionDate);

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
}
