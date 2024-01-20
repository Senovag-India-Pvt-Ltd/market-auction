package com.sericulture.marketandauction.service;

import com.sericulture.marketandauction.helper.MarketAuctionHelper;
import com.sericulture.marketandauction.helper.Util;
import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.api.RequestBody;
import com.sericulture.marketandauction.model.api.marketauction.*;
import com.sericulture.marketandauction.repository.LotRepository;
import com.sericulture.marketandauction.repository.ReelerAuctionRepository;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class MarketAuctionReportService {

    @Autowired
    private LotRepository lotRepository;

    @Autowired
    private MarketAuctionPrinterService marketAuctionPrinterService;

    @Autowired
    private ReelerAuctionRepository reelerAuctionRepository;

    @Autowired
    private MarketAuctionHelper marketAuctionHelper;

    @Autowired
    private FarmerPaymentService farmerPaymentService;

    public static final String getAllHighestBids = """
            SELECT ALLOTTED_LOT_ID,AMOUNT,R.Name  
            FROM REELER_AUCTION RAA INNER JOIN REELER R ON RAA.REELER_ID = R.REELER_ID 
            INNER JOIN (
            select MIN(REELER_AUCTION_ID) ID, RA.ALLOTTED_LOT_ID as AL from REELER_AUCTION RA,
            ( 
            SELECT MAX(AMOUNT) AMT, ALLOTTED_LOT_ID  from REELER_AUCTION ra
            where AUCTION_DATE = :today and ALLOTTED_LOT_ID in ( :lotList) AND MARKET_ID =:marketId GROUP by ALLOTTED_LOT_ID ) as RAB 
            WHERE RAB.AMT=RA.AMOUNT AND  RA.MARKET_ID =:marketId AND RA.ALLOTTED_LOT_ID = RAB.ALLOTTED_LOT_ID AND AUCTION_DATE = :today
            GROUP by  RA.ALLOTTED_LOT_ID ) RA ON RA.ID= RAA.REELER_AUCTION_ID
            """;

    public static final String ALLTTOTED_LOT_LIST_PER_MARKET_ID = """
            SELECT l.allotted_lot_id  from lot l
            INNER JOIN market_auction ma
            on ma.market_auction_id = l.market_auction_id
            where ma.market_auction_date =:auctionDate and ma.market_id = :marketId  and l.status is NULL """;

    public static final String ALLTTOTED_LOT_LIST_PER_MARKET_ID_AND_GODOWNID = ALLTTOTED_LOT_LIST_PER_MARKET_ID + " and ma.godown_id =:godownId";

    public static final String DTR_ONLINE_REPORT_QUERY = """
            select  ROW_NUMBER() OVER(ORDER BY l.lot_id ASC) AS row_id,l.allotted_lot_id ,f.first_name,f.middle_name,f.last_name,f.farmer_number,
            f.mobile_number,l.LOT_WEIGHT_AFTER_WEIGHMENT,ra.AMOUNT,l.LOT_SOLD_OUT_AMOUNT ,l.MARKET_FEE_FARMER,l.MARKET_FEE_REELER,
            r.reeling_license_number,r.name,r.mobile_number,
            fba.farmer_bank_name,fba.farmer_bank_branch_name ,fba.farmer_bank_ifsc_code ,fba.farmer_bank_account_number 
            from 
            dbo.FARMER f
            INNER JOIN dbo.market_auction ma ON ma.farmer_id = f.FARMER_ID 
            INNER JOIN dbo.lot l ON l.market_auction_id =ma.market_auction_id and l.auction_date = ma.market_auction_date 
            INNER JOIN dbo.REELER_AUCTION ra ON ra.REELER_AUCTION_ID  = l.REELER_AUCTION_ID and ra.STATUS ='accepted' and ra.AUCTION_DATE =l.auction_date 
            INNER JOIN dbo.reeler r ON r.reeler_id =ra.REELER_ID  
            LEFT JOIN dbo.reeler_virtual_bank_account rvba ON rvba.reeler_id =r.reeler_id and rvba.market_master_id = ma.market_id
            LEFT JOIN dbo.REELER_VID_CURRENT_BALANCE rvcb ON rvcb.reeler_virtual_account_number= rvba.virtual_account_number
            LEFT JOIN dbo.farmer_address fa ON f.FARMER_ID = fa.FARMER_ID and fa.default_address = 1 
            LEFT JOIN  dbo.farmer_bank_account fba  ON   fba.FARMER_ID = f.FARMER_ID 
            LEFT JOIN dbo.market_master mm on mm.market_master_id = ma.market_auction_id 
            where l.status in ('readyforpayment','paymentsuccess','paymentfailed','paymentprocessing')
             and l.auction_date BETWEEN :fromDate and :toDate 
             and l.market_id =:marketId 
             and (:reelerIdList is null OR r.reeler_id in (:reelerIdList))
             and fba.farmer_bank_account_number != '' 
             and fba.farmer_bank_ifsc_code !='' 
             and rvcb.CURRENT_BALANCE > 0.0
            ORDER by l.lot_id""";

    private void prepareDTROnlineInfo(DTROnlineReportResponse dtrOnlineReportResponse,List<Object[]> queryResponse) {

        for (Object[] unit : queryResponse) {
            DTROnlineReportUnitDetail dtrOnlineReportUnitDetail = DTROnlineReportUnitDetail.builder()
                    .serialNumber(Util.objectToInteger(unit[0]))
                    .allottedLotId(Util.objectToInteger(unit[1]))
                    .farmerFirstName(Util.objectToString(unit[2]))
                    .farmerMiddleName(Util.objectToString(unit[3]))
                    .farmerLastName(Util.objectToString(unit[4]))
                    .farmerNumber(Util.objectToString(unit[5]))
                    .farmerMobileNumber(Util.objectToString(unit[6]))
                    .weight(Util.objectToFloat(unit[7]))
                    .bidAmount(Util.objectToInteger(unit[8]))
                    .lotSoldOutAmount(Util.objectToFloat(unit[9]))
                    .farmerMarketFee(Util.objectToFloat(unit[10]))
                    .reelerMarketFee(Util.objectToFloat(unit[11]))
                    .reelerLicense(Util.objectToString(unit[12]))
                    .reelerName(Util.objectToString(unit[13]))
                    .reelerMobile(Util.objectToString(unit[14]))
                    .bankName(Util.objectToString(unit[15]))
                    .branchName(Util.objectToString(unit[16]))
                    .ifscCode(Util.objectToString(unit[17]))
                    .accountNumber(Util.objectToString(unit[18]))
                    .build();
            dtrOnlineReportUnitDetail.setReelerAmount(dtrOnlineReportUnitDetail.getLotSoldOutAmount() - dtrOnlineReportUnitDetail.getReelerMarketFee());
            dtrOnlineReportUnitDetail.setFarmerAmount(dtrOnlineReportUnitDetail.getLotSoldOutAmount() - dtrOnlineReportUnitDetail.getFarmerMarketFee());
            dtrOnlineReportResponse.setTotalFarmerMarketFee(dtrOnlineReportResponse.getTotalFarmerMarketFee() + dtrOnlineReportUnitDetail.getFarmerMarketFee());
            dtrOnlineReportResponse.setTotalReelerMarketFee(dtrOnlineReportResponse.getTotalReelerMarketFee() + dtrOnlineReportUnitDetail.getReelerMarketFee());
            dtrOnlineReportResponse.setTotalFarmerAmount(dtrOnlineReportResponse.getTotalFarmerAmount() + dtrOnlineReportUnitDetail.getFarmerAmount());
            dtrOnlineReportResponse.setTotalReelerAmount(dtrOnlineReportResponse.getTotalReelerAmount() +  dtrOnlineReportUnitDetail.getReelerAmount());
            dtrOnlineReportResponse.getDtrOnlineReportUnitDetailList().add(dtrOnlineReportUnitDetail);
        }
        dtrOnlineReportResponse.setTotalLots(queryResponse.size());
    }
    public ResponseEntity<?> getDTROnlineReport(DTROnlineReportRequest dtrOnlineReportRequest) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        List<Integer> reelerIdList = null;
        if (dtrOnlineReportRequest.getReelerId() > 0) {
            reelerIdList = List.of(dtrOnlineReportRequest.getReelerId());
        }
        List<Object[]> reportResponse = lotRepository.
                getDTROnlineReport(dtrOnlineReportRequest.getMarketId(), dtrOnlineReportRequest.getFromDate(), dtrOnlineReportRequest.getToDate(), reelerIdList);
       DTROnlineReportResponse dtrOnlineReportResponse = new DTROnlineReportResponse();
       prepareDTROnlineInfo(dtrOnlineReportResponse,reportResponse);
        rw.setContent(dtrOnlineReportResponse);
        return ResponseEntity.ok(rw);
    }

    public ResponseEntity<?> getAllHighestBidsByMarketIdAndOptionalGodownId(RequestBody requestBody) {
        List<LotHighestBidResponse> lotHighestBidResponseList = new ArrayList<>();
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        List<Integer> allottedLotList = null;
        if (requestBody.getGodownId() != 0) {
            allottedLotList = reelerAuctionRepository.getAllottedLotListByMarketIdAndGoDownId(Util.getISTLocalDate(), requestBody.getMarketId(), requestBody.getGodownId());
        } else {
            allottedLotList = reelerAuctionRepository.getAllottedLotListByMarketId(Util.getISTLocalDate(), requestBody.getMarketId());
        }
        if (Util.isNullOrEmptyList(allottedLotList)) {
            marketAuctionHelper.retrunIfError(rw, "No Lots found for the given market:" + requestBody.getMarketId() + " and godownId: " + requestBody.getGodownId());
        }
        List<Integer> highestBidsFoundList = new ArrayList<>();
        Object[][] highestBids = reelerAuctionRepository.getHighestBidAmountForAllLotList(Util.getISTLocalDate(), requestBody.getMarketId(), allottedLotList);

        if (highestBids != null && highestBids.length > 0) {
            for (Object[] bids : highestBids) {
                highestBidsFoundList.add(Util.objectToInteger(bids[0]));
                LotHighestBidResponse lotHighestBidResponse = new LotHighestBidResponse(
                        Util.objectToInteger(bids[0]), Util.objectToInteger(bids[1]), Util.objectToString(bids[2]));
                lotHighestBidResponseList.add(lotHighestBidResponse);
            }
        }

        rw.setContent(lotHighestBidResponseList);
        return ResponseEntity.ok(rw);
    }


}
