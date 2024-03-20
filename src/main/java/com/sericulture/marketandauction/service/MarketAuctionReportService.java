package com.sericulture.marketandauction.service;

import com.sericulture.authentication.model.JwtPayloadData;
import com.sericulture.marketandauction.helper.MarketAuctionHelper;
import com.sericulture.marketandauction.helper.Util;
import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.api.RequestBody;
import com.sericulture.marketandauction.model.api.marketauction.*;
import com.sericulture.marketandauction.model.api.marketauction.reporting.*;
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
import java.time.format.DateTimeFormatter;
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
