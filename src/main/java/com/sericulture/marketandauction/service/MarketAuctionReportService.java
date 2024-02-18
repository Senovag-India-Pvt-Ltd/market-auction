package com.sericulture.marketandauction.service;

import com.sericulture.authentication.model.JwtPayloadData;
import com.sericulture.marketandauction.helper.MarketAuctionHelper;
import com.sericulture.marketandauction.helper.Util;
import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.api.RequestBody;
import com.sericulture.marketandauction.model.api.marketauction.*;
import com.sericulture.marketandauction.model.api.marketauction.reporting.*;
import com.sericulture.marketandauction.model.entity.ExceptionalTime;
import com.sericulture.marketandauction.model.entity.MarketMaster;
import com.sericulture.marketandauction.model.exceptions.ValidationException;
import com.sericulture.marketandauction.repository.ExceptionalTimeRepository;
import com.sericulture.marketandauction.repository.LotRepository;
import com.sericulture.marketandauction.repository.MarketMasterRepository;
import com.sericulture.marketandauction.repository.ReelerAuctionRepository;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
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

    @Autowired
    MarketMasterRepository marketMasterRepository;

    @Autowired
    ExceptionalTimeRepository exceptionalTimeRepository;


    private void prepareDTROnlineInfo(DTROnlineReportResponse dtrOnlineReportResponse, List<Object[]> queryResponse) {

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
            dtrOnlineReportUnitDetail.setReelerAmount(dtrOnlineReportUnitDetail.getLotSoldOutAmount() + dtrOnlineReportUnitDetail.getReelerMarketFee());
            dtrOnlineReportUnitDetail.setFarmerAmount(dtrOnlineReportUnitDetail.getLotSoldOutAmount() - dtrOnlineReportUnitDetail.getFarmerMarketFee());
            dtrOnlineReportResponse.setTotalFarmerMarketFee(dtrOnlineReportResponse.getTotalFarmerMarketFee() + dtrOnlineReportUnitDetail.getFarmerMarketFee());
            dtrOnlineReportResponse.setTotalReelerMarketFee(dtrOnlineReportResponse.getTotalReelerMarketFee() + dtrOnlineReportUnitDetail.getReelerMarketFee());
            dtrOnlineReportResponse.setTotalFarmerAmount(dtrOnlineReportResponse.getTotalFarmerAmount() + dtrOnlineReportUnitDetail.getFarmerAmount());
            dtrOnlineReportResponse.setTotalReelerAmount(dtrOnlineReportResponse.getTotalReelerAmount() + dtrOnlineReportUnitDetail.getReelerAmount());
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
        prepareDTROnlineInfo(dtrOnlineReportResponse, reportResponse);
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
        List<Object[]> resultSet = reelerAuctionRepository.getUnitCounterReport(reportRequest.getReportFromDate(), reportRequest.getMarketId());
        if (Util.isNullOrEmptyList(resultSet)) {
            marketAuctionHelper.retrunIfError(rw, "No data found");
        }
        for (Object[] row : resultSet) {
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

    public ResponseEntity<?> getPendingLotReport(ReportRequest requestBody) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        JwtPayloadData token = marketAuctionHelper.getAuthToken(requestBody);
        List<MarketAuctionForPrintResponse> marketAuctionForPrintResponseList = new ArrayList<>();
        List<Object[]> lotDetails = lotRepository.getAcceptedLotDetailsForPendingReport(requestBody.getReportFromDate(), requestBody.getMarketId());
        List<Integer> acceptedLotList = new ArrayList<>();

        for (Object[] response : lotDetails) {
            BigInteger lotId = BigInteger.valueOf(Long.parseLong(String.valueOf(response[16])));
            MarketAuctionForPrintResponse marketAuctionForPrintResponse = marketAuctionPrinterService.prepareResponseForLotBaseResponse(token, response, lotId);
            marketAuctionForPrintResponse.setAuctionDateWithTime((Date) (response[23]));
            marketAuctionForPrintResponse.setReelerLicense(Util.objectToString(response[24]));
            marketAuctionForPrintResponse.setReelerName(Util.objectToString(response[25]));
            marketAuctionForPrintResponse.setReelerAddress(Util.objectToString(response[26]));
            marketAuctionForPrintResponse.setLotWeight(Util.objectToFloat(response[27]));
            marketAuctionForPrintResponse.setBidAmount(Util.objectToFloat(response[31]));
            marketAuctionForPrintResponse.setReelerNameKannada(Util.objectToString(response[33]));
            marketAuctionForPrintResponse.setReelerMobileNumber(Util.objectToString(response[34]));
            marketAuctionForPrintResponseList.add(marketAuctionForPrintResponse);
            acceptedLotList.add(marketAuctionForPrintResponse.getAllottedLotId());
        }

        List<Object[]> newlyCreatedLotDetails = lotRepository.getNewlyCreatedLotDetailsForPendingReport(requestBody.getReportFromDate(), requestBody.getMarketId(), acceptedLotList.size() == 0 ? null : acceptedLotList);

        for (Object[] response : newlyCreatedLotDetails) {
            BigInteger lotId = BigInteger.valueOf(Long.parseLong(String.valueOf(response[16])));
            marketAuctionForPrintResponseList.add(marketAuctionPrinterService.prepareResponseForLotBaseResponse(token, response, lotId));
        }
        rw.setContent(marketAuctionForPrintResponseList);
        return ResponseEntity.ok(rw);
    }

    public ResponseEntity<?> getFarmerTxnReport(FarmerTxnReportRequest farmerTxnReportRequest) {

        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);

        List<Object[]> responses = lotRepository.getFarmerReport(farmerTxnReportRequest.getMarketId(),farmerTxnReportRequest.getReportFromDate(),farmerTxnReportRequest.getReportToDate(),farmerTxnReportRequest.getFarmerNumber());

        if(Util.isNullOrEmptyList(responses))
        {
            throw new ValidationException("No data found");
        }
        FarmerTxnReportResponse farmerTxnReportResponse = new FarmerTxnReportResponse();
        farmerTxnReportResponse.setFarmerFirstName(Util.objectToString(responses.get(0)[3]));
        farmerTxnReportResponse.setFarmerMiddleName(Util.objectToString(responses.get(0)[4]));
        farmerTxnReportResponse.setFarmerLastName(Util.objectToString(responses.get(0)[5]));
        farmerTxnReportResponse.setFarmerNumber(Util.objectToString(responses.get(0)[6]));
        float totalFarmerAmount = 0;
        float totalMarketFee = 0;
        float totalLotSoldAmount = 0;
        List<FarmerTxnInfo> farmerTxnInfoList = new ArrayList<>();
        farmerTxnReportResponse.setFarmerTxnInfoList(farmerTxnInfoList);
        for(Object[] response:responses){
            FarmerTxnInfo farmerTxnInfo = FarmerTxnInfo.builder()
                    .serialNumber(Util.objectToInteger(response[0]))
                    .allottedLotId(Util.objectToInteger(response[1]))
                    .lotTransactionDate(Util.objectToString(response[2]))
                    .weight(Util.objectToFloat(response[7]))
                    .bidAmount(Util.objectToInteger(response[8]))
                    .lotSoldOutAmount(Util.objectToFloat(response[9]))
                    .farmerMarketFee(Util.objectToFloat(response[10])).build();
            double farmerAmount = farmerTxnInfo.getLotSoldOutAmount() - farmerTxnInfo.getFarmerMarketFee();
            totalFarmerAmount +=farmerAmount;
            farmerTxnInfo.setFarmerAmount(farmerAmount);
            totalMarketFee+=farmerTxnInfo.getFarmerMarketFee();
            totalLotSoldAmount+=farmerTxnInfo.getLotSoldOutAmount();
            farmerTxnInfoList.add(farmerTxnInfo);
        }
        farmerTxnReportResponse.setTotalMarketFee(totalMarketFee);
        farmerTxnReportResponse.setTotalSaleAmount(totalLotSoldAmount);
        farmerTxnReportResponse.setTotalFarmerAmount(totalFarmerAmount);
        rw.setContent(farmerTxnReportResponse);
        return ResponseEntity.ok(rw);

    }

    public ResponseEntity<?> getBiddingReport(LotReportRequest reportRequest) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        List<LotReportResponse> lotReportResponseList = new ArrayList<>();
        List<Object[]> responses = reelerAuctionRepository.getBiddingReport(reportRequest.getMarketId(),reportRequest.getReportFromDate(),reportRequest.getLotId());
        return getBiddingReportLotOrReeler(reportRequest.getMarketId(), rw, lotReportResponseList, responses);
    }

    private ResponseEntity<ResponseWrapper> getBiddingReportLotOrReeler(int marketId, ResponseWrapper rw, List<LotReportResponse> lotReportResponseList, List<Object[]> responses) {
        if(Util.isNullOrEmptyList(responses))
        {
            throw new ValidationException("No data found");
        }
        ExceptionalTime exceptionalTime = exceptionalTimeRepository.findByMarketIdAndAuctionDate(marketId, Util.getISTLocalDate());
        MarketMaster marketMaster = marketMasterRepository.findById(marketId);
        for(Object[] response: responses){
            LotReportResponse lotReportResponse = LotReportResponse.builder()
                    .lotId(Util.objectToInteger(response[0]))
                    .reelerLicenseNumber(Util.objectToString(response[1]))
                    .bidAmount(Util.objectToInteger(response[2]))
                    .bidTime(((Timestamp)response[3]).toLocalDateTime().toLocalTime())
                    .accepted(Util.objectToString(response[4]))
                    .build();
            lotReportResponse.setAuctionNumber(marketAuctionHelper.getAuctionNumber(exceptionalTime,marketMaster,lotReportResponse.getBidTime()));
            if(StringUtils.isNotBlank(lotReportResponse.getAccepted())){
                lotReportResponse.setAcceptedTime(((Timestamp)response[5]).toLocalDateTime().toLocalTime());
                lotReportResponse.setAcceptedBy(Util.objectToString(response[6]));
            }
            lotReportResponseList.add(lotReportResponse);
        }
        rw.setContent(lotReportResponseList);
        return ResponseEntity.ok(rw);
    }

    public ResponseEntity<?> getReelerBiddingReport(ReelerReportRequest reportRequest) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        List<LotReportResponse> lotReportResponseList = new ArrayList<>();
        List<Object[]> responses = reelerAuctionRepository.getReelerBiddingReport(reportRequest.getMarketId(),reportRequest.getReportFromDate(),reportRequest.getReelerNumber());
        return getBiddingReportLotOrReeler(reportRequest.getMarketId(), rw, lotReportResponseList, responses);
    }

}
