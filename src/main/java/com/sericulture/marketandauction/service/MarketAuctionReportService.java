package com.sericulture.marketandauction.service;

import com.sericulture.marketandauction.helper.MarketAuctionHelper;
import com.sericulture.marketandauction.helper.Util;
import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.api.RequestBody;
import com.sericulture.marketandauction.model.api.marketauction.*;
import com.sericulture.marketandauction.model.api.marketauction.reporting.ReportRequest;
import com.sericulture.marketandauction.model.api.marketauction.reporting.UnitCounterReportResponse;
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

    public ResponseEntity<?> getUnitCounterReport(ReportRequest reportRequest) {
        List<UnitCounterReportResponse> unitCounterReportResponses = new ArrayList<>();
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        List<Object[]> resultSet = reelerAuctionRepository.getUnitCounterReport(reportRequest.getReportFromDate(),reportRequest.getMarketId());
        if (Util.isNullOrEmptyList(resultSet)){
            marketAuctionHelper.retrunIfError(rw,"No data found");
        }
        for(Object[] row: resultSet){
            UnitCounterReportResponse unitCounterReportResponse = UnitCounterReportResponse.builder()
                    .allottedLotId(Util.objectToInteger(row[0]))
                    .lotTransactionDate(Util.objectToString(row[1]))
                    .weight(Util.objectToFloat(row[2]))
                    .bidAmount(Util.objectToInteger(row[3]))
                    .lotSoldOutAmount(Util.objectToFloat(row[4]))
                    .farmerMarketFee(Util.objectToFloat(row[5]))
                    .reelerMarketFee(Util.objectToFloat(row[6]))
                    .reelerLicense(Util.objectToString(row[7]))
                    .reelerName(Util.objectToString(row[8])).build();
            unitCounterReportResponses.add(unitCounterReportResponse);
        }
        rw.setContent(unitCounterReportResponses);
        return ResponseEntity.ok(rw);
    }


}
