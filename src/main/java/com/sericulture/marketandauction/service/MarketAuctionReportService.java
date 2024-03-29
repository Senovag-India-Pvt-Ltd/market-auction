package com.sericulture.marketandauction.service;

import com.sericulture.authentication.model.JwtPayloadData;
import com.sericulture.marketandauction.helper.MarketAuctionHelper;
import com.sericulture.marketandauction.helper.Util;
import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.api.RequestBody;
import com.sericulture.marketandauction.model.api.marketauction.*;
import com.sericulture.marketandauction.model.api.marketauction.reporting.*;
import com.sericulture.marketandauction.model.api.marketauction.reporting.AudioVisual.AudioReport;
import com.sericulture.marketandauction.model.api.marketauction.reporting.AudioVisual.AudioReportResponse;
import com.sericulture.marketandauction.model.api.marketauction.reporting.AudioVisual.AudioVisualReportRequest;
import com.sericulture.marketandauction.model.api.marketauction.reporting.AudioVisual.MonthWiseReport;
import com.sericulture.marketandauction.model.api.marketauction.reporting.DTR.*;
import com.sericulture.marketandauction.model.api.marketauction.reporting.MarketReport.*;
import com.sericulture.marketandauction.model.api.marketauction.reporting.MonthlyReport.*;
import com.sericulture.marketandauction.model.entity.Bin;
import com.sericulture.marketandauction.model.entity.ExceptionalTime;
import com.sericulture.marketandauction.model.entity.MarketMaster;
import com.sericulture.marketandauction.model.exceptions.ValidationException;
import com.sericulture.marketandauction.repository.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

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

    @Autowired
    private BinRepository binRepository;


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
                    .farmerAddress(Util.objectToString(unit[20]))
                    .auctionDate(((java.sql.Date) unit[21]).toLocalDate())
                    .build();
            dtrOnlineReportUnitDetail.setReelerAmount(dtrOnlineReportUnitDetail.getLotSoldOutAmount() + dtrOnlineReportUnitDetail.getReelerMarketFee());
            dtrOnlineReportUnitDetail.setFarmerAmount(dtrOnlineReportUnitDetail.getLotSoldOutAmount() - dtrOnlineReportUnitDetail.getFarmerMarketFee());
            dtrOnlineReportResponse.setTotalFarmerMarketFee(dtrOnlineReportResponse.getTotalFarmerMarketFee() + dtrOnlineReportUnitDetail.getFarmerMarketFee());
            dtrOnlineReportResponse.setTotalReelerMarketFee(dtrOnlineReportResponse.getTotalReelerMarketFee() + dtrOnlineReportUnitDetail.getReelerMarketFee());
            dtrOnlineReportResponse.setTotalFarmerAmount(dtrOnlineReportResponse.getTotalFarmerAmount() + dtrOnlineReportUnitDetail.getFarmerAmount());
            dtrOnlineReportResponse.setTotalReelerAmount(dtrOnlineReportResponse.getTotalReelerAmount() + dtrOnlineReportUnitDetail.getReelerAmount());
            dtrOnlineReportResponse.setTotalWeight(dtrOnlineReportResponse.getTotalWeight() + dtrOnlineReportUnitDetail.getWeight());
            dtrOnlineReportResponse.setTotalBidAmount(dtrOnlineReportResponse.getTotalBidAmount()+dtrOnlineReportUnitDetail.getBidAmount());
            dtrOnlineReportResponse.setTotallotSoldOutAmount(dtrOnlineReportResponse.getTotallotSoldOutAmount()+dtrOnlineReportUnitDetail.getLotSoldOutAmount());
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
        List<Object[]> reportPaymentSuccessResponse = lotRepository.
                getPaymentSuccessLots(dtrOnlineReportRequest.getMarketId(), dtrOnlineReportRequest.getFromDate(), dtrOnlineReportRequest.getToDate(), reelerIdList);
        List<Object[]> reportResponse = lotRepository.
                getDTROnlineReport(dtrOnlineReportRequest.getMarketId(), dtrOnlineReportRequest.getFromDate(), dtrOnlineReportRequest.getToDate(), reelerIdList);
        DTROnlineReportResponse dtrOnlineReportResponse = new DTROnlineReportResponse();
        if(reportResponse.size()>0) {
            dtrOnlineReportResponse.setMarketNameKannada(Util.objectToString(reportResponse.get(0)[19]));
        }
        prepareDTROnlineInfo(dtrOnlineReportResponse, reportResponse);
        if(reportPaymentSuccessResponse.size()>0) {
            dtrOnlineReportResponse.setPaymentSuccessLots(Util.objectToInteger(reportPaymentSuccessResponse.get(0)[0]));
        }
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
        List<LotPendingReportResponse> lotPendingReportResponses = new ArrayList<>();
        List<Object[]> lotDetails = lotRepository.getAcceptedLotDetailsForPendingReport(requestBody.getReportFromDate(), requestBody.getMarketId());
        List<Object[]> binForPendingReportList = binRepository.findAllByAuctionDateAndMarketId(requestBody.getReportFromDate(),requestBody.getMarketId());


        Map<BigInteger, List<Integer>> bigBins = new HashMap<>();
        Map<BigInteger, List<Integer>> smallBins = new HashMap<>();
        for(Object[] binForPendingReport:binForPendingReportList){
            if(binForPendingReport[1].equals("big")){
                List<Integer> bigones = bigBins.get(binForPendingReport[2]);
                if(bigones==null){
                    bigones = new ArrayList<>();
                    bigBins.put((BigInteger) binForPendingReport[2],bigones);
                }
                bigones.add((Integer) binForPendingReport[0]);
            }else {
                List<Integer> smallOnes = smallBins.get(binForPendingReport[2]);
                if(smallOnes==null){
                    smallOnes = new ArrayList<>();
                    smallBins.put((BigInteger) binForPendingReport[2],smallOnes);
                }
                smallOnes.add((Integer) binForPendingReport[0]);
            }
        }

        long serailNumberForPagination = 0;
        List<Integer> acceptedLotList = new ArrayList<>();

        for (Object[] response : lotDetails) {
            BigInteger lotId = BigInteger.valueOf(Long.parseLong(String.valueOf(response[16])));
            LotPendingReportResponse lotPendingReportResponse = prepareResponseForLotBaseResponse(token, response, lotId);
            lotPendingReportResponse.setAuctionDateWithTime((Date) (response[23]));
            lotPendingReportResponse.setReelerLicense(Util.objectToString(response[24]));
            lotPendingReportResponse.setReelerName(Util.objectToString(response[25]));
            lotPendingReportResponse.setReelerAddress(Util.objectToString(response[26]));
            lotPendingReportResponse.setLotWeight(Util.objectToFloat(response[27]));
            lotPendingReportResponse.setBidAmount(Util.objectToFloat(response[31]));
            lotPendingReportResponse.setReelerCurrentBalance(Util.objectToFloat(response[32]));
            lotPendingReportResponse.setReelerNameKannada(Util.objectToString(response[33]));
            lotPendingReportResponse.setReelerMobileNumber(Util.objectToString(response[34]));
            lotPendingReportResponse.setReelerNumber(Util.objectToString(response[35]));
            lotPendingReportResponse.setAcceptedBy(Util.objectToString(response[36]));
            lotPendingReportResponse.setSerailNumberForPagination(++serailNumberForPagination);
            lotPendingReportResponse.setBigBinList(bigBins.get(lotPendingReportResponse.getMarketAuctionId().toBigInteger()));
            lotPendingReportResponse.setSmallBinList(smallBins.get(lotPendingReportResponse.getMarketAuctionId().toBigInteger()));
            lotPendingReportResponses.add(lotPendingReportResponse);
            acceptedLotList.add(lotPendingReportResponse.getAllottedLotId());
        }

        List<Object[]> newlyCreatedLotDetails = lotRepository.getNewlyCreatedLotDetailsForPendingReport(requestBody.getReportFromDate(), requestBody.getMarketId(), acceptedLotList.size() == 0 ? null : acceptedLotList);

        for (Object[] response : newlyCreatedLotDetails) {
            BigInteger lotId = BigInteger.valueOf(Long.parseLong(String.valueOf(response[16])));
            LotPendingReportResponse lotPendingReportResponse = prepareResponseForLotBaseResponse(token, response, lotId);
            lotPendingReportResponse.setSerailNumberForPagination(++serailNumberForPagination);
            lotPendingReportResponse.setBigBinList(bigBins.get(lotPendingReportResponse.getMarketAuctionId().toBigInteger()));
            lotPendingReportResponse.setSmallBinList(smallBins.get(lotPendingReportResponse.getMarketAuctionId().toBigInteger()));
            lotPendingReportResponse.setShed(String.valueOf(response[24]));
            lotPendingReportResponses.add(lotPendingReportResponse);
        }
        rw.setContent(lotPendingReportResponses);
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
        farmerTxnReportResponse.setVillage(Util.objectToString(responses.get(0)[12]));
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
                    .farmerMarketFee(Util.objectToFloat(response[10]))
                    .breed(Util.objectToString(response[11])).build();
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

    public ResponseEntity<?> getReelerPendingReport(RequestBody requestBody) {

        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);

        List<Object[]> responses = lotRepository.getReelerPendingReport(requestBody.getMarketId());

        List<Object[]> responsesDebitTotal = lotRepository.geTotalDebitTxnToday(requestBody.getMarketId(), LocalDate.now());

        List<Object[]> responsesCreditTotal = lotRepository.getTotalCreditTxnToday(requestBody.getMarketId(), LocalDate.now());

        List<Object[]> responsesBalance = lotRepository.getTotalReelerBalance(requestBody.getMarketId());

        List<Object[]> responsesMarket = lotRepository.getMarketName(requestBody.getMarketId());

        if(Util.isNullOrEmptyList(responses))
        {
            throw new ValidationException("No data found");
        }
        ReelerPendingReportResponse reelerPendingReportResponse = new ReelerPendingReportResponse();
        if(responsesBalance.size()>0){
            if(responsesBalance.get(0) != null) {
                reelerPendingReportResponse.setBalance(Util.objectToString(responsesBalance.get(0)[0]));
            }
        }
        if(responsesCreditTotal.size()>0){
            if(responsesCreditTotal.get(0) != null) {
                reelerPendingReportResponse.setCreditTotal(Util.objectToString(responsesCreditTotal.get(0)[0]));
            }
        }
        if(responsesDebitTotal.size()>0){
            if(responsesDebitTotal.get(0) != null) {
                reelerPendingReportResponse.setDebitTotal(Util.objectToString(responsesDebitTotal.get(0)[0]));
            }
        }
        if(responsesMarket.size()>0){
            if(responsesMarket.get(0) != null) {
                reelerPendingReportResponse.setMarketName(Util.objectToString(responsesMarket.get(0)[0]));
            }
        }
        List<ReelerPendingInfo> reelerPendingInfoList = new ArrayList<>();
        int i= 1;
        for(Object[] response:responses){
            String lastTxn = "";
            if(Util.objectToString(response[6]) != null && !Util.objectToString(response[6]).equals("")){
                lastTxn = Util.objectToString(response[6]);
            }else{
                lastTxn = Util.objectToString(response[5]);
            }

            String suspend = "";
            if(Util.objectToFloat(response[4])< 0){
                suspend = "Yes";
            }

            ReelerPendingInfo reelerPendingInfo = ReelerPendingInfo.builder()
                    .reelerName(Util.objectToString(response[1]))
                    .reelerNumber(Util.objectToString(response[0]))
                    .reelingLicenseNumber(Util.objectToString(response[2]))
                    .mobileNumber(Util.objectToString(response[3]))
                    .currentBalance(Util.objectToString(response[4]))
                    .lastTxnTime(lastTxn)
                    .serialNumber(String.valueOf(i))
                    .counter("")
                    .onlineTxn("Yes")
                    .suspend(suspend)
                    .build();
            reelerPendingInfoList.add(reelerPendingInfo);
            i=i+1;
        }
        reelerPendingReportResponse.setReelerPendingInfoList(reelerPendingInfoList);
        rw.setContent(reelerPendingReportResponse);
        return ResponseEntity.ok(rw);

    }

    public ResponseEntity<?> getReelerReportForMobileApp(ReelerReportForAppRequest requestBody) {

        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);

        List<Object[]> responses = lotRepository.getReelerReportForApp(requestBody.getReelerId(),requestBody.getMarketId(), requestBody.getAuctionDate());

        List<Object[]> responsesBalance = lotRepository.getReelerCurrentBalance(requestBody.getReelerId());

        List<Object[]> responsesAppxPurchase = lotRepository.getReelerPurchaseAmount(requestBody.getReelerId(), requestBody.getMarketId(), requestBody.getAuctionDate());

        List<Object[]> responsesAmountDeposited = lotRepository.getReelerDepositedAmount(requestBody.getReelerId(), requestBody.getAuctionDate());


        if(Util.isNullOrEmptyList(responses))
        {
            throw new ValidationException("No data found");
        }
        ReelerReportResponse reelerReportResponse = new ReelerReportResponse();
        if(responsesBalance.size()>0){
            if(responsesBalance.get(0) != null) {
                reelerReportResponse.setReelerCurrentBalance(Util.objectToFloat(responsesBalance.get(0)[0]));
            }
        }
        if(responsesAppxPurchase.size()>0){
            if(responsesAppxPurchase.get(0) != null) {
                reelerReportResponse.setApproximatePurchase(Util.objectToFloat(responsesAppxPurchase.get(0)[0]));
            }
        }
        if(responsesAmountDeposited.size()>0){
            if(responsesAmountDeposited.get(0) != null) {
                reelerReportResponse.setTotalAmountDeposited(Util.objectToFloat(responsesAmountDeposited.get(0)[0]));
            }
        }
        List<ReelerReport> reelerReportList = new ArrayList<>();
        int i=1;
        for(Object[] response:responses){

            ReelerReport reelerReport = ReelerReport.builder()
                    .serialNumber(i)
                    .allottedLotId(Util.objectToInteger(response[0]))
                    .bidAmount(Util.objectToFloat(response[1]))
                    .weight(Util.objectToFloat(response[2]))
                    .amount(Util.objectToFloat(response[3]))
                    .marketFee(Util.objectToFloat(response[4]))
                    .build();
            reelerReportList.add(reelerReport);
            i= i+1;
        }
        reelerReportResponse.setReelerReportList(reelerReportList);
        rw.setContent(reelerReportResponse);
        return ResponseEntity.ok(rw);

    }

    public ResponseEntity<?> getForm13Report(Form13Request requestBody) {

        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);

        List<Object[]> avgResponse = lotRepository.getAvgLotStatus(requestBody.getMarketId(), requestBody.getAuctionDate());

        List<Object[]> totalLotStatusResponse = lotRepository.getTotalLotStatus(requestBody.getMarketId(), requestBody.getAuctionDate());

        List<Object[]> stateWiseLotStatusResponse = lotRepository.getStateWiseLotStatus(requestBody.getMarketId(), requestBody.getAuctionDate());

        List<Object[]> raceWiseLotStatusResponse = lotRepository.getRaceWiseStatus(requestBody.getMarketId(), requestBody.getAuctionDate());

        List<Object[]> marketResponse = lotRepository.getMarketName(requestBody.getMarketId());

        float totalWeight = 0.0F;

        if(Util.isNullOrEmptyList(totalLotStatusResponse))
        {
            throw new ValidationException("No data found");
        }
        Form13Response form13Response = new Form13Response();
        form13Response.setAverageRate(Util.objectToString(avgResponse.get(0)[2]));
        form13Response.setMarketNameKannada(Util.objectToString(marketResponse.get(0)[1]));

        List<GroupLotStatus> totalLotStatus = prepareGroup13Report(totalLotStatusResponse, "Reeler");
        form13Response.setTotalLotStatus(totalLotStatus);
        totalWeight = Util.objectToFloat(totalLotStatusResponse.get(0)[3]);

        List<GroupLotStatus> stateWiseLotStatus = prepareGroup13Report(stateWiseLotStatusResponse, "");
        form13Response.setStateWiseLotStatus(stateWiseLotStatus);

        List<GroupLotStatus> raceWiseLotStatus = prepareGroup13Report(raceWiseLotStatusResponse, "");
        form13Response.setRaceWiseLotStatus(raceWiseLotStatus);

        List<BreakdownLotStatus> lotsFrom0to351 = new ArrayList<>();

        List<Object[]> lotBetween1to100Response = lotRepository.getLotBreakDownStatus(1, 100,requestBody.getMarketId(), requestBody.getAuctionDate());
        BreakdownLotStatus breakdownLotStatusList1to100 = prepareBreakdown13Report(lotBetween1to100Response, 000, 100, totalWeight, "");
        lotsFrom0to351.add(breakdownLotStatusList1to100);

        List<Object[]> lotBetween101to150Response = lotRepository.getLotBreakDownStatus(101, 150,requestBody.getMarketId(), requestBody.getAuctionDate());
        BreakdownLotStatus breakdownLotStatusList101to150 = prepareBreakdown13Report(lotBetween101to150Response, 101, 150, totalWeight, "");
        lotsFrom0to351.add(breakdownLotStatusList101to150);

        List<Object[]> lotBetween150to200Response = lotRepository.getLotBreakDownStatus(151, 200,requestBody.getMarketId(), requestBody.getAuctionDate());
        BreakdownLotStatus breakdownLotStatusList150to200 = prepareBreakdown13Report(lotBetween150to200Response, 150, 200, totalWeight, "");
        lotsFrom0to351.add(breakdownLotStatusList150to200);

        List<Object[]> lotBetween201to250Response = lotRepository.getLotBreakDownStatus(201, 250,requestBody.getMarketId(), requestBody.getAuctionDate());
        BreakdownLotStatus breakdownLotStatusList201to250 = prepareBreakdown13Report(lotBetween201to250Response, 201, 250, totalWeight, "");
        lotsFrom0to351.add(breakdownLotStatusList201to250);

        List<Object[]> lotBetween250to300Response = lotRepository.getLotBreakDownStatus(251, 300,requestBody.getMarketId(), requestBody.getAuctionDate());
        BreakdownLotStatus breakdownLotStatusList250to300 = prepareBreakdown13Report(lotBetween250to300Response, 250, 300, totalWeight, "");
        lotsFrom0to351.add(breakdownLotStatusList250to300);

        List<Object[]> lotBetween301to350Response = lotRepository.getLotBreakDownStatus(301, 350,requestBody.getMarketId(), requestBody.getAuctionDate());
        BreakdownLotStatus breakdownLotStatusList301to350 = prepareBreakdown13Report(lotBetween301to350Response, 301, 350, totalWeight, "");
        lotsFrom0to351.add(breakdownLotStatusList301to350);

        List<Object[]> lotGreaterThan350Response = lotRepository.getGreaterLotStatus( requestBody.getMarketId(), requestBody.getAuctionDate(), 350);
        BreakdownLotStatus breakdownLotStatusList350Above = prepareBreakdown13Report(lotGreaterThan350Response, 301, 350, totalWeight, "Lots Above Rs.351");
        lotsFrom0to351.add(breakdownLotStatusList350Above);

        form13Response.setLotsFrom0to351(lotsFrom0to351);

        List<BreakdownLotStatus> lotsFrom210to300 = new ArrayList<>();

        List<Object[]> lotBetween201to210Response = lotRepository.getLotBreakDownStatus(201, 210,requestBody.getMarketId(), requestBody.getAuctionDate());
        BreakdownLotStatus breakdownLotStatusList201to210 = prepareBreakdown13Report(lotBetween201to210Response, 201, 210, totalWeight, "");
        lotsFrom210to300.add(breakdownLotStatusList201to210);

        List<Object[]> lotBetween211to220Response = lotRepository.getLotBreakDownStatus(211, 220,requestBody.getMarketId(), requestBody.getAuctionDate());
        BreakdownLotStatus breakdownLotStatusList211to220 = prepareBreakdown13Report(lotBetween211to220Response, 211, 220, totalWeight, "");
        lotsFrom210to300.add(breakdownLotStatusList211to220);

        List<Object[]> lotBetween221to230Response = lotRepository.getLotBreakDownStatus(221, 230,requestBody.getMarketId(), requestBody.getAuctionDate());
        BreakdownLotStatus breakdownLotStatusList221to230 = prepareBreakdown13Report(lotBetween221to230Response, 221, 230, totalWeight, "");
        lotsFrom210to300.add(breakdownLotStatusList221to230);

        List<Object[]> lotBetween231to240Response = lotRepository.getLotBreakDownStatus(231, 240,requestBody.getMarketId(), requestBody.getAuctionDate());
        BreakdownLotStatus breakdownLotStatusList231to240 = prepareBreakdown13Report(lotBetween231to240Response, 231, 240, totalWeight, "");
        lotsFrom210to300.add(breakdownLotStatusList231to240);

        List<Object[]> lotBetween241to250Response = lotRepository.getLotBreakDownStatus(241, 250,requestBody.getMarketId(), requestBody.getAuctionDate());
        BreakdownLotStatus breakdownLotStatusList241to250 = prepareBreakdown13Report(lotBetween201to210Response, 241, 250, totalWeight, "");
        lotsFrom210to300.add(breakdownLotStatusList241to250);

        List<Object[]> lotBetween251to275Response = lotRepository.getLotBreakDownStatus(251, 275,requestBody.getMarketId(), requestBody.getAuctionDate());
        BreakdownLotStatus breakdownLotStatusList251to275 = prepareBreakdown13Report(lotBetween251to275Response, 251, 275, totalWeight, "");
        lotsFrom210to300.add(breakdownLotStatusList251to275);

        List<Object[]> lotBetween276to300Response = lotRepository.getLotBreakDownStatus(276, 300,requestBody.getMarketId(), requestBody.getAuctionDate());
        BreakdownLotStatus breakdownLotStatusList276to300 = prepareBreakdown13Report(lotBetween201to210Response, 276, 300, totalWeight, "");
        lotsFrom210to300.add(breakdownLotStatusList276to300);

        form13Response.setLotsFrom201to300(lotsFrom210to300);

        List<BreakdownLotStatus> averageLots = new ArrayList<>();

        List<Object[]> lotlesserThanAverageResponse = lotRepository.getLessLotStatus( requestBody.getMarketId(), requestBody.getAuctionDate(), Util.objectToFloat(avgResponse.get(0)[2]));
        BreakdownLotStatus lotlesserThanAverage = prepareBreakdown13Report(lotlesserThanAverageResponse, 301, 350, totalWeight, "Lots less than average");
        averageLots.add(lotlesserThanAverage);

        List<Object[]> lotGreaterThanAverageResponse = lotRepository.getGreaterLotStatus( requestBody.getMarketId(), requestBody.getAuctionDate(), Util.objectToFloat(avgResponse.get(0)[2]));
        BreakdownLotStatus lotGreaterThanAverage = prepareBreakdown13Report(lotGreaterThanAverageResponse, 301, 350, totalWeight, "Lots more than average");
        averageLots.add(lotGreaterThanAverage);

        form13Response.setAverageLotStatus(averageLots);

        rw.setContent(form13Response);
        return ResponseEntity.ok(rw);

    }

    BreakdownLotStatus prepareBreakdown13Report(List<Object[]> lotBetweenResponse, int fromCount, int toCount, float totalWeight, String lotText){
        BreakdownLotStatus breakdownLotStatus = new BreakdownLotStatus();
        if(lotBetweenResponse.size()>0) {
            for (Object[] response : lotBetweenResponse) {
                if(lotText.equals("")){
                    lotText = "Lot between Rs." + fromCount + " to " + toCount;
                }
                BreakdownLotStatus breakdownLotStatus1 = BreakdownLotStatus.builder()
                        .description(lotText)
                        .lot(Util.objectToString(response[4]))
                        .weight(Util.objectToString(response[2]))
                        .percentage(Util.objectToString((Util.objectToFloat(response[2]) / totalWeight) * 100))
                        .build();
                breakdownLotStatus = breakdownLotStatus1;
            }
        }else{
            BreakdownLotStatus breakdownLotStatus1 = BreakdownLotStatus.builder()
                    .description("Lot between " + fromCount + " to " + toCount)
                    .lot(Util.objectToString(0))
                    .weight(Util.objectToString(""))
                    .percentage(Util.objectToString(0.00))
                    .build();
            breakdownLotStatus = breakdownLotStatus1;
        }
        return breakdownLotStatus;
    }

    List<GroupLotStatus> prepareGroup13Report(List<Object[]> groupLotStatusResponse, String descriptionText) {
        List<GroupLotStatus> groupLotList = new ArrayList<>();
        for (Object[] response : groupLotStatusResponse) {
            if(descriptionText.equals("")){
                descriptionText = Util.objectToString(response[11]);
            }
            GroupLotStatus groupLotStatus = GroupLotStatus.builder()
                    .description(descriptionText)
                    .lot(Util.objectToString(response[2]))
                    .weight(Util.objectToString(response[3]))
                    .amount(Util.objectToString(response[4]))
                    .mf(String.valueOf(Util.objectToFloat(response[8]) + Util.objectToFloat(response[9])))
                    .min(Util.objectToString(response[5]))
                    .max(Util.objectToString(response[6]))
                    .avg(Util.objectToString(response[7]))
                    .build();
            groupLotList.add(groupLotStatus);
        }
        return groupLotList;
    }
    public ResponseEntity<?> getDashboardReport(DashboardReportRequest dashboardReportRequest) {

        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
        String formattedDateTime = now.format(formatter);

        List<Object[]> responsesIsAuctionStarted = lotRepository.getIsAuctionStarted(dashboardReportRequest.getMarketId(), formattedDateTime);

        List<Object[]> responsesIsAcceptanceStarted = lotRepository.getIsAcceptanceStarted(dashboardReportRequest.getMarketId(),formattedDateTime);

        List<Object[]> marketNameResponse = lotRepository.getMarketName(dashboardReportRequest.getMarketId());

        List<Object[]> responses = lotRepository.getDashboardCount(dashboardReportRequest.getMarketId(),dashboardReportRequest.getDashboardReportDate());

        if(Util.isNullOrEmptyList(responses))
        {
            throw new ValidationException("No data found");
        }
        DashboardReport dashboardReport = new DashboardReport();
        dashboardReport.setMarketName(Util.objectToString(marketNameResponse.get(0)[0]));
        dashboardReport.setAuctionStarted(Util.objectToString(responsesIsAuctionStarted.get(0)[0]));
        dashboardReport.setAcceptanceStarted(Util.objectToString(responsesIsAcceptanceStarted.get(0)[0]));

        List<DashboardReportInfo> dashboardReportInfoList = new ArrayList<>();
        for(Object[] response:responses){
            DashboardReportInfo dashboardReportInfo = DashboardReportInfo.builder()
                    .raceName(Util.objectToString(response[0]))
                    .totalLots(Util.objectToString(response[1]))
                    .totalSoldOutAmount(Util.objectToString(response[2]))
                    .totalLotsBid(Util.objectToString(response[3]))
                    .totalBids(Util.objectToString(response[4]))
                    .totalReelers(Util.objectToString(response[5]))
                    .accecptedLots(Util.objectToString(response[6]))
                    .accecptedLotsMaxBid(Util.objectToString(response[7]))
                    .accectedLotsMinBid(Util.objectToString(response[8]))
                    .averagRate(Util.objectToString(response[9]))
                    .weighedLots(Util.objectToString(response[10])).build();


            dashboardReportInfo.setTotalLotsNotBid(String.valueOf(Integer.parseInt(dashboardReportInfo.getTotalLots()) - Integer.parseInt(dashboardReportInfo.getTotalLotsBid())));
            dashboardReportInfoList.add(dashboardReportInfo);
        }
        dashboardReport.setDashboardReportInfoList(dashboardReportInfoList);
        rw.setContent(dashboardReport);
        return ResponseEntity.ok(rw);

    }

    public ResponseEntity<?> getAverageReportForYearsReport(AverageReportRequest averageReportRequest) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        AverageReportResponse averageReportResponse = new AverageReportResponse();
        List<AverageRateYearWise> averageRateYearWiseList = new ArrayList<>();
        for (int year = averageReportRequest.getStartYear().getYear(); year < averageReportRequest.getEndYear().getYear(); year++) {
            LocalDate startDate = LocalDate.of(year, averageReportRequest.getStartYear().getMonth(), averageReportRequest.getStartYear().getDayOfMonth());
            LocalDate endDate = LocalDate.of(year + 1, averageReportRequest.getEndYear().getMonth(), averageReportRequest.getEndYear().getDayOfMonth());
            AverageRateYearWise averageRateYearWise = new AverageRateYearWise();
            averageRateYearWise.setYear(year + "-" + (year + 1));

            //Response is active race
            List<Object[]> responseRaces = lotRepository.getActiveRaceForAverageReport(startDate, endDate, averageReportRequest.getMarketId());

            List<AverageRateRaceWise> averageRateRaceWiseList = new ArrayList<>();
            for(int i=0; i<responseRaces.size(); i++){
                AverageRateRaceWise averageRateRaceWise = new AverageRateRaceWise();
                averageRateRaceWise.setRaceName(Util.objectToString(responseRaces.get(i)[1]));

                List<AverageRateValues> averageRateValuesList = new ArrayList<>();
                List<Object[]> responsesAvgDate = lotRepository.getAverageReportForYearsReport(startDate, endDate, Util.objectToInteger(responseRaces.get(i)[0]), averageReportRequest.getMarketId());
                if(responsesAvgDate.size()>0){

                    for(int j=0; j<responsesAvgDate.size(); j++) {

                        AverageRateValues averageRateValues = new AverageRateValues();
                        averageRateValues.setWeight(Util.objectToString(responsesAvgDate.get(j)[2]));
                        averageRateValues.setLotSoldAmount(Util.objectToString(responsesAvgDate.get(j)[1]));
                        averageRateValues.setMonth(Util.objectToString(responsesAvgDate.get(j)[0]));

                        averageRateValuesList.add(averageRateValues);
                    }
                }
                averageRateRaceWise.setAverageRateValues(averageRateValuesList);

                averageRateRaceWiseList.add(averageRateRaceWise);
            }
            averageRateYearWise.setAverageRateRaceWiseList(averageRateRaceWiseList);

            averageRateYearWiseList.add(averageRateYearWise);

        }
        averageReportResponse.setAverageRateYearWiseList(averageRateYearWiseList);
        rw.setContent(averageReportResponse);
        return ResponseEntity.ok(rw);

    }

    public ResponseEntity<?> getAverageCocoonReport(AverageReportRequest averageReportRequest) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        AverageCocoonResponse averageCocoonResponse = new AverageCocoonResponse();
        List<AverageCocoonYearWise> averageCocoonYearWises = new ArrayList<>();
        for (int year = averageReportRequest.getStartYear().getYear(); year < averageReportRequest.getEndYear().getYear(); year++) {
            LocalDate startDate = LocalDate.of(year, averageReportRequest.getStartYear().getMonth(), averageReportRequest.getStartYear().getDayOfMonth());
            LocalDate endDate = LocalDate.of(year + 1, averageReportRequest.getEndYear().getMonth(), averageReportRequest.getEndYear().getDayOfMonth());

            AverageCocoonYearWise averageCocoonYearWise = new AverageCocoonYearWise();
            averageCocoonYearWise.setYear(year + "-" + (year + 1));
            List<AverageCocoonReport> averageCocoonReports = new ArrayList<>();
            List<Object[]> responsesAvgDate = lotRepository.getAverageCocoonReport(startDate, endDate, averageReportRequest.getMarketId());
            if(responsesAvgDate.size()>0) {
                for (int j = 0; j < responsesAvgDate.size(); j++) {
                    AverageCocoonReport averageCocoonReport = new AverageCocoonReport();
                    averageCocoonReport.setWeight(Util.objectToString(responsesAvgDate.get(j)[2]));
                    averageCocoonReport.setLotSoldAmount(Util.objectToString(responsesAvgDate.get(j)[3]));
                    averageCocoonReport.setMonth(Util.objectToString(responsesAvgDate.get(j)[1]));
                    averageCocoonReports.add(averageCocoonReport);
                }
            }
            averageCocoonYearWise.setAverageCocoonReports(averageCocoonReports);
            averageCocoonYearWises.add(averageCocoonYearWise);
        }
        averageCocoonResponse.setAverageCocoonYearWises(averageCocoonYearWises);
        rw.setContent(averageCocoonResponse);
        return ResponseEntity.ok(rw);
    }

    public ResponseEntity<?> getDTRReport(Form13Request request) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        DTRInfoResponse dtrInfoResponse = new DTRInfoResponse();
        DTRDataResponse dtrDataResponse = new DTRDataResponse();

        List<DTRMarketResponse> dtrMarketResponses = new ArrayList<>();
        List<Object[]> responseMarkets = lotRepository.getMarketsForDTRReport();
        if(responseMarkets.size()>0){
            for(int i=0; i<responseMarkets.size(); i++){
                DTRMarketResponse dtrMarketResponse = new DTRMarketResponse();
                dtrMarketResponse.setMarketNameInKannada(Util.objectToString(responseMarkets.get(i)[2]));

                List<Object[]> responseRaces = lotRepository.getRacesByMarket(Util.objectToInteger(responseMarkets.get(i)[0]));
                List<DTRRaceResponse> dtrRaceResponses = new ArrayList<>();
                if(responseRaces.size()>0){

                    for(int j=0; j<responseRaces.size(); j++){
                        DTRRaceResponse dtrRaceResponse = new DTRRaceResponse();
                        dtrRaceResponse.setRaceNameInKannada(Util.objectToString(responseRaces.get(j)[3]));
                        List<Object[]> responseData = lotRepository.getDTRReport(Util.objectToInteger(responseMarkets.get(i)[0]), Util.objectToInteger(responseRaces.get(j)[1]), request.getAuctionDate());
                        if(responseData.size()>0){
                            List<DTRResponse> dtrResponses = new ArrayList<>();
                            for(int k=0; k<responseData.size(); k++){
                                DTRResponse dtrResponse = new DTRResponse();
                                dtrResponse.setAvgAmount(Util.objectToString(responseData.get(k)[2]));
                                dtrResponse.setMinAmount(Util.objectToString(responseData.get(k)[1]));
                                dtrResponse.setMaxAmount(Util.objectToString(responseData.get(k)[0]));
                                dtrResponse.setWeight(Util.objectToString(responseData.get(k)[3]));
                                dtrResponses.add(dtrResponse);
                            }
                            dtrRaceResponse.setDtrResponses(dtrResponses);
                        }
                        LocalDate prevDate = request.getAuctionDate().minusDays(1);
                        List<Object[]> responsePrevData = lotRepository.getDTRReport(Util.objectToInteger(responseMarkets.get(i)[0]), Util.objectToInteger(responseRaces.get(j)[1]), prevDate);
                        if(responsePrevData.size()>0){
                            List<DTRResponse> dtrResponses = new ArrayList<>();
                            for(int k=0; k<responsePrevData.size(); k++){
                                DTRResponse dtrResponse = new DTRResponse();
                                dtrResponse.setAvgAmount(Util.objectToString(responsePrevData.get(k)[2]));
                                dtrResponse.setMinAmount(Util.objectToString(responsePrevData.get(k)[1]));
                                dtrResponse.setMaxAmount(Util.objectToString(responsePrevData.get(k)[0]));
                                dtrResponse.setWeight(Util.objectToString(responsePrevData.get(k)[3]));
                                dtrResponses.add(dtrResponse);
                            }
                            dtrRaceResponse.setPrevResponses(dtrResponses);
                        }

                        LocalDate lastYearDate = request.getAuctionDate().minusYears(1);
                        List<Object[]> responseLastYearData = lotRepository.getDTRReport(Util.objectToInteger(responseMarkets.get(i)[0]), Util.objectToInteger(responseRaces.get(j)[1]),lastYearDate);
                        if(responseLastYearData.size()>0){
                            List<DTRResponse> dtrResponses = new ArrayList<>();
                            for(int k=0; k<responseLastYearData.size(); k++){
                                DTRResponse dtrResponse = new DTRResponse();
                                dtrResponse.setAvgAmount(Util.objectToString(responseLastYearData.get(k)[2]));
                                dtrResponse.setMinAmount(Util.objectToString(responseLastYearData.get(k)[1]));
                                dtrResponse.setMaxAmount(Util.objectToString(responseLastYearData.get(k)[0]));
                                dtrResponse.setWeight(Util.objectToString(responseLastYearData.get(k)[3]));
                                dtrResponses.add(dtrResponse);
                            }
                            dtrRaceResponse.setLastYearResponses(dtrResponses);
                        }

                        dtrRaceResponses.add(dtrRaceResponse);
                    }

                }
                dtrMarketResponse.setDtrRaceResponses(dtrRaceResponses);
                dtrMarketResponses.add(dtrMarketResponse);
            }
        }

        DTRResponse dtrResponseToday = new DTRResponse();
        List<Object[]> responseSumOfDTRToday = lotRepository.getSumDTRReport(request.getAuctionDate());
        if(responseSumOfDTRToday.size()>0){
            dtrResponseToday.setWeight(Util.objectToString(responseSumOfDTRToday.get(0)[3]));
            dtrResponseToday.setMinAmount(Util.objectToString(responseSumOfDTRToday.get(0)[1]));
            dtrResponseToday.setMaxAmount(Util.objectToString(responseSumOfDTRToday.get(0)[0]));
            dtrResponseToday.setAvgAmount(Util.objectToString(responseSumOfDTRToday.get(0)[2]));
            dtrDataResponse.setSumOfToday(dtrResponseToday);
        }

        DTRResponse dtrResponseLastYear = new DTRResponse();
        List<Object[]> responseSumOfDTRPreviousYear = lotRepository.getSumDTRReport(request.getAuctionDate().minusYears(1));
        if(responseSumOfDTRPreviousYear.size()>0){
            dtrResponseLastYear.setWeight(Util.objectToString(responseSumOfDTRPreviousYear.get(0)[3]));
            dtrResponseLastYear.setMinAmount(Util.objectToString(responseSumOfDTRPreviousYear.get(0)[1]));
            dtrResponseLastYear.setMaxAmount(Util.objectToString(responseSumOfDTRPreviousYear.get(0)[0]));
            dtrResponseLastYear.setAvgAmount(Util.objectToString(responseSumOfDTRPreviousYear.get(0)[2]));
            dtrDataResponse.setSumOfPreviousYear(dtrResponseLastYear);
        }

        if(dtrResponseToday.getWeight().equals("")){
            dtrResponseToday.setWeight("0.000");
        }
        if(dtrResponseLastYear.getWeight().equals("")){
            dtrResponseLastYear.setWeight("0.000");
        }
        dtrDataResponse.setTotalWeightDiff(String.format("%.3f",Float.parseFloat(dtrResponseToday.getWeight()) - Float.parseFloat(dtrResponseLastYear.getWeight())));

        List<Object[]> racesList = lotRepository.getAllRaces();
        List<DTRResponse> raceByToday = new ArrayList<>();
        List<DTRResponse> raceByPrevYear = new ArrayList<>();
        if(racesList.size()>0){
            for(int i=0; i<racesList.size(); i++) {
                DTRResponse dtrResponseRaceToday = new DTRResponse();
                List<Object[]> dataByRace = lotRepository.getSumDTRReportByRace(Util.objectToInteger(racesList.get(i)[0]), request.getAuctionDate());
                if(dataByRace.size()>0){
                    dtrResponseRaceToday.setRaceName(Util.objectToString(dataByRace.get(0)[4]));
                    dtrResponseRaceToday.setWeight(Util.objectToString(dataByRace.get(0)[3]));
                    dtrResponseRaceToday.setAvgAmount(Util.objectToString(dataByRace.get(0)[2]));
                    dtrResponseRaceToday.setMaxAmount(Util.objectToString(dataByRace.get(0)[0]));
                    dtrResponseRaceToday.setMinAmount(Util.objectToString(dataByRace.get(0)[1]));
                    raceByToday.add(dtrResponseRaceToday);
                }

                DTRResponse dtrResponseRaceLastYear = new DTRResponse();
                List<Object[]> dataByRaceLastYear = lotRepository.getSumDTRReportByRace(Util.objectToInteger(racesList.get(i)[0]), request.getAuctionDate().minusYears(1));
                if(dataByRaceLastYear.size()>0){
                    dtrResponseRaceLastYear.setRaceName(Util.objectToString(dataByRaceLastYear.get(0)[4]));
                    dtrResponseRaceLastYear.setWeight(Util.objectToString(dataByRaceLastYear.get(0)[3]));
                    dtrResponseRaceLastYear.setAvgAmount(Util.objectToString(dataByRaceLastYear.get(0)[2]));
                    dtrResponseRaceLastYear.setMaxAmount(Util.objectToString(dataByRaceLastYear.get(0)[0]));
                    dtrResponseRaceLastYear.setMinAmount(Util.objectToString(dataByRaceLastYear.get(0)[1]));
                    raceByPrevYear.add(dtrResponseRaceLastYear);
                }
            }
        }
        dtrDataResponse.setRaceByToday(raceByToday);
        dtrDataResponse.setRaceByPrevYear(raceByPrevYear);

        LocalDate startDate = request.getAuctionDate().withDayOfMonth(1);
        List<Object[]> thisYearTotal = lotRepository.getTotalByMonth(startDate, request.getAuctionDate());
        List<Object[]> prevYearTotal = lotRepository.getTotalByMonth(startDate.minusYears(1), request.getAuctionDate().minusYears(1));

        if(thisYearTotal.size()>0){
            dtrDataResponse.setThisYearAmount(Util.objectToString(thisYearTotal.get(0)[1]));
            dtrDataResponse.setThisYearWeight(Util.objectToString(thisYearTotal.get(0)[0]));
        }

        if(prevYearTotal.size()>0){
            dtrDataResponse.setPrevYearAmount(Util.objectToString(prevYearTotal.get(0)[1]));
            dtrDataResponse.setPrevYearWeight(Util.objectToString(prevYearTotal.get(0)[0]));
        }

        dtrDataResponse.setDtrMarketResponses(dtrMarketResponses);
        dtrInfoResponse.setDtrDataResponse(dtrDataResponse);
        rw.setContent(dtrInfoResponse);
        return ResponseEntity.ok(rw);

    }

    public ResponseEntity<?> getMarketWiseReport(MonthlyReportRequest request) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        MarketReportResponse marketReportResponse = new MarketReportResponse();
        MarketReports marketReports = new MarketReports();

        List<MarketWiseInfo> marketWiseInfos = new ArrayList<>();
        List<Object[]> responseMarkets = lotRepository.getMarketsForDTRReport();
        if(responseMarkets.size()>0){
            for(int i=0; i<responseMarkets.size(); i++){
                MarketWiseInfo marketWiseInfo = new MarketWiseInfo();
                marketWiseInfo.setMarketName(Util.objectToString(responseMarkets.get(i)[2]));

                List<Object[]> responseRaces = lotRepository.getRacesByMarket(Util.objectToInteger(responseMarkets.get(i)[0]));
                List<MarketReportRaceWise> marketReportRaceWises = new ArrayList<>();

                LocalDate startDate = request.getStartDate();
                LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
                LocalDate financialYearStartDate = LocalDate.of(startDate.getYear() - 1, 4, 1);

                if(responseRaces.size()>0){
                    for(int j=0; j<responseRaces.size(); j++){
                        MarketReportRaceWise marketReportRaceWise = new MarketReportRaceWise();
                        marketReportRaceWise.setRaceName(Util.objectToString(responseRaces.get(j)[3]));
                        List<Object[]> responseData = lotRepository.getMarketReport(Util.objectToInteger(responseMarkets.get(i)[0]), Util.objectToInteger(responseRaces.get(j)[1]), startDate, endDate);
                        List<Object[]> responseDataMonthEnd = lotRepository.getMarketReport(Util.objectToInteger(responseMarkets.get(i)[0]), Util.objectToInteger(responseRaces.get(j)[1]), financialYearStartDate, endDate);

                        MarketReportInfo marketReportInfo = new MarketReportInfo();
                        if(responseData.size()>0){
                            marketReportInfo.setStartingAvg(Util.objectToString(responseData.get(0)[1]));
                            marketReportInfo.setStartingAmount(Util.objectToString(responseData.get(0)[0]));
                            marketReportInfo.setStartingWeight(Util.objectToString(responseData.get(0)[2]));
                        }else{
                            marketReportInfo.setStartingWeight("0.00");
                        }

                        if(responseDataMonthEnd.size()>0){
                            marketReportInfo.setEndingAvg(Util.objectToString(responseDataMonthEnd.get(0)[1]));
                            marketReportInfo.setEndingAmount(Util.objectToString(responseDataMonthEnd.get(0)[0]));
                            marketReportInfo.setEndingWeight(Util.objectToString(responseDataMonthEnd.get(0)[2]));
                        }else{
                            marketReportInfo.setEndingWeight("0.00");
                        }
                        marketReportRaceWise.setMarketReportInfo(marketReportInfo);
                        marketReportRaceWises.add(marketReportRaceWise);
                    }

                }
                marketWiseInfo.setMarketReportRaceWises(marketReportRaceWises);

                List<Object[]> responseSumData = lotRepository.getMarketReportSum(Util.objectToInteger(responseMarkets.get(i)[0]), startDate, endDate);
                List<Object[]> responseSumDataMonthEnd = lotRepository.getMarketReportSum(Util.objectToInteger(responseMarkets.get(i)[0]), financialYearStartDate, endDate);

                if(responseSumData.size()>0){
                    marketWiseInfo.setTotalWeightStarting(Util.objectToString(responseSumData.get(0)[2]));
                    marketWiseInfo.setAvgAmountStarting(Util.objectToString(responseSumData.get(0)[1]));
                    marketWiseInfo.setTotalAmountStarting(Util.objectToString(responseSumData.get(0)[0]));
                    marketWiseInfo.setLotsStarting(Util.objectToString(responseSumData.get(0)[3]));
                    marketWiseInfo.setMarketFeeStarting(Util.objectToString(responseSumData.get(0)[4]));
                }

                if(responseSumDataMonthEnd.size()>0){
                    marketWiseInfo.setTotalWeightEnding(Util.objectToString(responseSumDataMonthEnd.get(0)[2]));
                    marketWiseInfo.setAvgAmountEnding(Util.objectToString(responseSumDataMonthEnd.get(0)[1]));
                    marketWiseInfo.setTotalAmountEnding(Util.objectToString(responseSumDataMonthEnd.get(0)[0]));
                    marketWiseInfo.setLotsEnding(Util.objectToString(responseSumDataMonthEnd.get(0)[3]));
                    marketWiseInfo.setMarketFeeEnding(Util.objectToString(responseSumDataMonthEnd.get(0)[4]));
                }
                marketWiseInfos.add(marketWiseInfo);

            }
        }
        marketReports.setMarketWiseInfos(marketWiseInfos);
        marketReportResponse.setMarketReports(marketReports);

        rw.setContent(marketReportResponse);
        return ResponseEntity.ok(rw);

    }

    public ResponseEntity<?> getAudioVisualReport(AudioVisualReportRequest request) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        AudioReport audioReport = new AudioReport();
        AudioReportResponse audioReportResponse = new AudioReportResponse();
        List<MonthWiseReport> monthWiseReports = new ArrayList<>();

        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();

        LocalDate currentDate = startDate;
        Period oneMonth = Period.ofMonths(1);

        while (!currentDate.isAfter(endDate)) {
            MonthWiseReport monthWiseReport = new MonthWiseReport();
            List<DTRMarketResponse> dtrMarketResponses = new ArrayList<>();
            // Your logic or processing code here
            LocalDate endDateForThisMonth = currentDate.withDayOfMonth(currentDate.lengthOfMonth());

            for(int i=0; i<request.getMarketList().size(); i++){
                List<Object[]> responseMarkets = lotRepository.getMarketName(request.getMarketList().get(i));

                DTRMarketResponse dtrMarketResponse = new DTRMarketResponse();
                dtrMarketResponse.setMarketNameInKannada(Util.objectToString(responseMarkets.get(0)[1]));

                List<Object[]> responseRaces = lotRepository.getRacesByMarket(Util.objectToInteger(request.getMarketList().get(i)));
                List<DTRRaceResponse> dtrRaceResponses = new ArrayList<>();
                if(responseRaces.size()>0){
                    for(int j=0; j<responseRaces.size(); j++){
                        DTRRaceResponse dtrRaceResponse = new DTRRaceResponse();
                        dtrRaceResponse.setRaceNameInKannada(Util.objectToString(responseRaces.get(j)[3]));
                        List<Object[]> responseData = lotRepository.getAudioVisualReport(Util.objectToInteger(request.getMarketList().get(i)), Util.objectToInteger(responseRaces.get(j)[1]), currentDate, endDateForThisMonth);
                        if(responseData.size()>0){
                            List<DTRResponse> dtrResponses = new ArrayList<>();
                            for(int k=0; k<responseData.size(); k++){
                                DTRResponse dtrResponse = new DTRResponse();
                                dtrResponse.setAvgAmount(Util.objectToString(responseData.get(k)[2]));
                                dtrResponse.setMinAmount(Util.objectToString(responseData.get(k)[1]));
                                dtrResponse.setMaxAmount(Util.objectToString(responseData.get(k)[0]));
                                dtrResponse.setWeight(Util.objectToString(responseData.get(k)[3]));
                                dtrResponses.add(dtrResponse);
                            }
                            dtrRaceResponse.setDtrResponses(dtrResponses);
                        }
                        dtrRaceResponses.add(dtrRaceResponse);
                    }

                }
                dtrMarketResponse.setDtrRaceResponses(dtrRaceResponses);
                dtrMarketResponses.add(dtrMarketResponse);
            }

            DTRMarketResponse dtrMarketResponse = new DTRMarketResponse();
            dtrMarketResponse.setMarketNameInKannada("ಇಥಾರೆ ಮರುಕಟ್ಟೆಗಳ್ಳಿ");

            List<Object[]> responseRaces = lotRepository.getRacesByMarketNotIn(request.getMarketList());
            List<DTRRaceResponse> dtrRaceResponses = new ArrayList<>();
            if(responseRaces.size()>0){
                for(int j=0; j<responseRaces.size(); j++){
                    DTRRaceResponse dtrRaceResponse = new DTRRaceResponse();
                    dtrRaceResponse.setRaceNameInKannada(Util.objectToString(responseRaces.get(j)[3]));
                    List<Object[]> responseData = lotRepository.getAudioVisualReport(Util.objectToInteger(responseRaces.get(j)[0]), Util.objectToInteger(responseRaces.get(j)[1]), currentDate, endDateForThisMonth);
                    if(responseData.size()>0){
                        List<DTRResponse> dtrResponses = new ArrayList<>();
                        for(int k=0; k<responseData.size(); k++){
                            DTRResponse dtrResponse = new DTRResponse();
                            dtrResponse.setAvgAmount(Util.objectToString(responseData.get(k)[2]));
                            dtrResponse.setMinAmount(Util.objectToString(responseData.get(k)[1]));
                            dtrResponse.setMaxAmount(Util.objectToString(responseData.get(k)[0]));
                            dtrResponse.setWeight(Util.objectToString(responseData.get(k)[3]));
                            dtrResponses.add(dtrResponse);
                        }
                        dtrRaceResponse.setDtrResponses(dtrResponses);
                    }
                    dtrRaceResponses.add(dtrRaceResponse);
                }

            }
            dtrMarketResponse.setDtrRaceResponses(dtrRaceResponses);
            dtrMarketResponses.add(dtrMarketResponse);


            monthWiseReport.setDtrMarketResponses(dtrMarketResponses);
            monthWiseReport.setMonth(currentDate.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) +'-'+currentDate.getYear());
            monthWiseReports.add(monthWiseReport);

            currentDate = currentDate.plus(oneMonth);

        }
        audioReportResponse.setMonthWiseReports(monthWiseReports);
        audioReport.setAudioReportResponse(audioReportResponse);
        rw.setContent(audioReport);
        return ResponseEntity.ok(rw);

    }

    public ResponseEntity<?> getMonthlyReport(MonthlyReportRequest request) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        MonthlyReport monthlyReport = new MonthlyReport();
        MonthlyReportResponse monthlyReportResponse = new MonthlyReportResponse();
        List<MonthlyReportRaceWise> monthlyReportRaceWiseList = new ArrayList<>();

        LocalDate startDate = request.getStartDate();
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        LocalDate financialYearStartDate = LocalDate.of(startDate.getYear() - 1, 4, 1);

        List<Object[]> responseThisMonth = lotRepository.getMonthlyReport(startDate, endDate);

        if(responseThisMonth.size()>0){
            for(int i=0; i<responseThisMonth.size(); i++){
                MonthlyReportRaceWise monthlyReportRaceWise = new MonthlyReportRaceWise();
                monthlyReportRaceWise.setRaceName(Util.objectToString(responseThisMonth.get(i)[4]));
                MonthlyReportInfo monthlyReportInfo = new MonthlyReportInfo();
                monthlyReportInfo.setStartWeight(Util.objectToString(responseThisMonth.get(i)[2]));
                monthlyReportInfo.setStartAmount(Util.objectToString(responseThisMonth.get(i)[1]));
                monthlyReportInfo.setStartAvg(Util.objectToString(responseThisMonth.get(i)[3]));

                List<Object[]> responseMonthEnd = lotRepository.getMonthlyReportByRace(financialYearStartDate, endDate,Util.objectToInteger(responseThisMonth.get(i)[0]) );
                if(responseMonthEnd.size()>0){
                    monthlyReportInfo.setEndWeight(Util.objectToString(responseMonthEnd.get(0)[2]));
                    monthlyReportInfo.setEndAmount(Util.objectToString(responseMonthEnd.get(0)[1]));
                    monthlyReportInfo.setEndAvg(Util.objectToString(responseMonthEnd.get(0)[3]));
                }else{
                    monthlyReportInfo.setEndWeight("0.000");
                }

                MonthlyReportInfo prevMonthlyReportInfo = new MonthlyReportInfo();
                List<Object[]> responsePrevYearStart = lotRepository.getMonthlyReportByRace(startDate.minusYears(1), endDate.minusYears(1), Util.objectToInteger(responseThisMonth.get(i)[0]));
                if(responsePrevYearStart.size()>0){
                    prevMonthlyReportInfo.setStartWeight(Util.objectToString(responsePrevYearStart.get(0)[2]));
                    prevMonthlyReportInfo.setStartAmount(Util.objectToString(responsePrevYearStart.get(0)[1]));
                    prevMonthlyReportInfo.setStartAvg(Util.objectToString(responsePrevYearStart.get(0)[3]));
                }else{
                    prevMonthlyReportInfo.setStartWeight("0.000");
                }

                List<Object[]> responsePrevMonthEnd = lotRepository.getMonthlyReportByRace(financialYearStartDate.minusYears(1), endDate.minusYears(1), Util.objectToInteger(responseThisMonth.get(i)[0]));
                if(responsePrevMonthEnd.size()>0){
                    prevMonthlyReportInfo.setEndWeight(Util.objectToString(responsePrevMonthEnd.get(0)[2]));
                    prevMonthlyReportInfo.setEndAmount(Util.objectToString(responsePrevMonthEnd.get(0)[1]));
                    prevMonthlyReportInfo.setEndAvg(Util.objectToString(responsePrevMonthEnd.get(0)[3]));
                }else{
                    prevMonthlyReportInfo.setEndWeight("0.000");
                }

                monthlyReportRaceWise.setThisYearReport(monthlyReportInfo);
                monthlyReportRaceWise.setPrevYearReport(prevMonthlyReportInfo);
                monthlyReportRaceWiseList.add(monthlyReportRaceWise);
            }

        }

        monthlyReportResponse.setMonthlyReportRaceWiseList(monthlyReportRaceWiseList);
        monthlyReport.setMonthlyReportResponse(monthlyReportResponse);
        rw.setContent(monthlyReport);
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
                    .marketName(Util.objectToString(response[7]))
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


    public LotPendingReportResponse prepareResponseForLotBaseResponse(JwtPayloadData token, Object[] response, BigInteger lotId) {
        LotPendingReportResponse lotPendingReportResponse;
        lotPendingReportResponse = LotPendingReportResponse.builder().
                farmerNumber(Util.objectToString(response[0]))
                .farmerFirstName(Util.objectToString(response[1]))
                .farmerMiddleName(Util.objectToString(response[2]))
                .farmerLastName(Util.objectToString(response[3]))
                .farmerAddress(Util.objectToString(response[4]))
                .farmerTaluk(Util.objectToString(response[5]))
                .farmerVillage(Util.objectToString(response[6]))
                .ifscCode(Util.objectToString(response[7]))
                .accountNumber(Util.objectToString(response[8]))
                .allottedLotId(Integer.parseInt(String.valueOf(response[9])))
                .auctionDate(Util.objectToString(response[10]))
                .farmerEstimatedWeight(Integer.parseInt(String.valueOf(response[11])))
                .marketName(Util.objectToString(response[12]))
                .race(Util.objectToString(response[13]))
                .source(Util.objectToString(response[14]))
                .tareWeight(Util.objectToFloat(response[15]))
                .serialNumber(Util.objectToString(response[17])+ lotId)
                .marketNameKannada(Util.objectToString(response[19]))
                .farmerNameKannada(Util.objectToString(response[20]))

                .farmerMobileNumber(Util.objectToString(response[21]))
                .marketAuctionId((BigDecimal) response[22])
                .auctionDateWithTime((Date)(response[23]))
                .build();
        lotPendingReportResponse.setLoginName(token.getUsername());
        return lotPendingReportResponse;
    }

}
