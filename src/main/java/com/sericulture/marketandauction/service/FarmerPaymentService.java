package com.sericulture.marketandauction.service;


import com.sericulture.authentication.model.JwtPayloadData;
import com.sericulture.marketandauction.helper.MarketAuctionHelper;
import com.sericulture.marketandauction.helper.MarketAuctionQueryConstants;
import com.sericulture.marketandauction.helper.Util;
import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.api.RequestBody;
import com.sericulture.marketandauction.model.api.marketauction.FarmerPaymentInfoRequest;
import com.sericulture.marketandauction.model.api.marketauction.FarmerPaymentInfoRequestByLotList;
import com.sericulture.marketandauction.model.api.marketauction.FarmerPaymentInfoResponse;
import com.sericulture.marketandauction.model.api.marketauction.FarmerReadyForPaymentResponse;
import com.sericulture.marketandauction.model.entity.TransactionFileGenQueue;
import com.sericulture.marketandauction.model.enums.LotStatus;
import com.sericulture.marketandauction.model.exceptions.ValidationException;
import com.sericulture.marketandauction.repository.LotRepository;
import com.sericulture.marketandauction.repository.TransactionFileGenQueueRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.time.LocalDate;
import java.util.*;

@Service
@Slf4j
public class FarmerPaymentService {
    @Autowired
    LotRepository lotRepository;
    @PersistenceUnit
    private EntityManagerFactory entityManagerFactory;
    @Autowired
    MarketAuctionHelper marketAuctionHelper;
    @Autowired
    TransactionFileGenQueueRepository transactionFileGenQueueRepository;

    public Map<String, Object>  getWeighmentCompletedTxnByAuctionDateAndMarket(RequestBody requestBody, final Pageable pageable) {
        Map<String, Object> response = new HashMap<>();
        marketAuctionHelper.getAuthToken(requestBody);
        List<FarmerPaymentInfoResponse> farmerPaymentInfoResponseList = new ArrayList<>();
        FarmerReadyForPaymentResponse farmerReadyForPaymentResponse = new FarmerReadyForPaymentResponse();
        Page<Object[]> paginatedResponse = lotRepository.getAllWeighmentCompletedTxnByMarket(pageable, requestBody.getMarketId());
        farmerReadyForPaymentResponse.setFarmerPaymentInfoResponseList(farmerPaymentInfoResponseList);
        if (paginatedResponse == null || paginatedResponse.isEmpty()) {
//            throw new ValidationException("No lot  found");
            response.put("farmerReadyForPaymentResponse", farmerReadyForPaymentResponse);
            response.put("currentPage", 0);
            response.put("totalItems", 0);
            response.put("totalPages", 0);
            return response;
        }

        farmerReadyForPaymentResponse.setTotalAmountToFarmer(prepareFarmerPaymentInfoResponseList(paginatedResponse.getContent(), farmerPaymentInfoResponseList));
        farmerReadyForPaymentResponse.setFarmerPaymentInfoResponseList(farmerPaymentInfoResponseList);

        response.put("farmerReadyForPaymentResponse", farmerReadyForPaymentResponse);
        response.put("currentPage", paginatedResponse.getNumber());
        response.put("totalItems", paginatedResponse.getTotalElements());
        response.put("totalPages", paginatedResponse.getTotalPages());
        return response;
    }

    public static double prepareFarmerPaymentInfoResponseList(List<Object[]> paginatedResponse, List<FarmerPaymentInfoResponse> farmerPaymentInfoResponseList) {
        double totalFarmerAmount = 0;
        for (Object[] response : paginatedResponse) {
            float lotSoldAmount = Util.objectToFloat(response[14]);
            float farmerMarketFee = Util.objectToFloat(response[15]);
            double farmerAmount = lotSoldAmount - farmerMarketFee;
            totalFarmerAmount = farmerAmount + totalFarmerAmount;
            FarmerPaymentInfoResponse farmerPaymentInfoResponse = new FarmerPaymentInfoResponse
                    (Integer.parseInt(Util.objectToString(response[0])), Integer.parseInt(Util.objectToString(response[2])),
                            Util.objectToString(response[3]), Util.objectToString(response[4]), Util.objectToString(response[5]), Util.objectToString(response[6])
                            , Util.objectToString(response[7]), Util.objectToString(response[8]),
                            Util.objectToString(response[9]), Util.objectToString(response[10]), Util.objectToString(response[11]),
                            Util.objectToString(response[12]), Util.objectToString(response[13]), lotSoldAmount,
                            farmerMarketFee, Util.objectToFloat(response[16]), Long.valueOf(Util.objectToString(response[1])), farmerAmount);
            farmerPaymentInfoResponseList.add(farmerPaymentInfoResponse);
        }
        return totalFarmerAmount;
    }

    public ResponseEntity<?> updateLotlistByChangingTheStatus(FarmerPaymentInfoRequestByLotList farmerPaymentInfoRequestByLotList, boolean selectedLot, String fromlotStatus, String toLotStatus) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        JwtPayloadData token = marketAuctionHelper.getAuthToken(farmerPaymentInfoRequestByLotList);
        EntityManager entityManager = null;
        try {
            boolean exists = transactionFileGenQueueRepository.existsTransactionFileGenQueueByMarketIdAndAuctionDateAndStatusIn(farmerPaymentInfoRequestByLotList.getMarketId(), farmerPaymentInfoRequestByLotList.getPaymentDate(), Set.of(LotStatus.REQUESTED.getLabel(), LotStatus.PROCESSING.getLabel()));
            if (exists) {
                throw new ValidationException("Payment Request is under process please try after sometime.");
            }
            List<Integer> lotList = null;
            if (selectedLot) {
                lotList = farmerPaymentInfoRequestByLotList.getAllottedLotList();
            }
            if (selectedLot && Util.isNullOrEmptyList(lotList)) {
                throw new ValidationException("Lot list is empty");
            }
            List<Object[]> paginatedResponse = lotRepository.getAllEligiblePaymentTxnByOptionalLotListAndLotStatus(farmerPaymentInfoRequestByLotList.getPaymentDate(), farmerPaymentInfoRequestByLotList.getMarketId(), lotList, fromlotStatus);

            if (paginatedResponse == null || paginatedResponse.size() == 0) {
                throw new ValidationException("no lots to update");
            }
            List<Long> lotIds = new ArrayList<>();
            for (Object[] response : paginatedResponse) {
                lotIds.add(Util.objectToLong(response[1]));
            }
            entityManager = entityManagerFactory.createEntityManager();
            entityManager.getTransaction().begin();
            Query nativeQuery = entityManager.createNativeQuery("UPDATE Lot set status = ? where lot_id in ( ? )");
            nativeQuery.setParameter(1, toLotStatus);
            nativeQuery.setParameter(2, lotIds);
            nativeQuery.executeUpdate();
            entityManager.getTransaction().commit();
        }catch (ValidationException validationException){
            throw validationException;
        }catch (Exception ex) {
            entityManager.getTransaction().rollback();
            return marketAuctionHelper.retrunIfError(rw, "Exception while updating the readyForPayement to list:" + farmerPaymentInfoRequestByLotList + " error: " + ex);
        } finally {
            if (entityManager != null && entityManager.isOpen()) {
                entityManager.close();
            }
        }
        return ResponseEntity.ok(rw);
    }

    public ResponseEntity<?> getAllWeighmentCompletedOrReadyForPaymentAuctionDatesByMarket(RequestBody requestBody, String status) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        marketAuctionHelper.getAuthToken(requestBody);
       final String t = MarketAuctionQueryConstants.AUCTION_DATE_LIST_BY_LOT_STATUS;
        List<Object> auctionDates = lotRepository.getAllWeighmentCompletedOrReadyForPaymentAuctionDatesByMarket(requestBody.getMarketId(), status);
        if (Util.isNullOrEmptyList(auctionDates)) {
           throw new ValidationException("No Auction dates found for bulk send");
        }
        rw.setContent(auctionDates);
        return ResponseEntity.ok(rw);
    }

    public ResponseEntity<?> generateBankStatementForAuctionDate(FarmerPaymentInfoRequest farmerPaymentInfoRequest) {
        marketAuctionHelper.getAuthToken(farmerPaymentInfoRequest);
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        FarmerReadyForPaymentResponse farmerReadyForPaymentResponse = getReadyForPaymentTxns(farmerPaymentInfoRequest.getPaymentDate(), farmerPaymentInfoRequest.getMarketId());
        rw.setContent(farmerReadyForPaymentResponse);
        return ResponseEntity.ok(rw);
    }

    private FarmerReadyForPaymentResponse getReadyForPaymentTxns(LocalDate auctionDate, int marketId) {

        FarmerReadyForPaymentResponse farmerReadyForPaymentResponse = new FarmerReadyForPaymentResponse();
        List<Object[]> paginatedResponse = lotRepository.getAllEligiblePaymentTxnByOptionalLotListAndLotStatus(auctionDate, marketId, null, LotStatus.READYFORPAYMENT.getLabel());
        List<FarmerPaymentInfoResponse> farmerPaymentInfoResponseList = new ArrayList<>();
        farmerReadyForPaymentResponse.setTotalAmountToFarmer(prepareFarmerPaymentInfoResponseList(paginatedResponse, farmerPaymentInfoResponseList));
        farmerReadyForPaymentResponse.setFarmerPaymentInfoResponseList(farmerPaymentInfoResponseList);
        return farmerReadyForPaymentResponse;
    }

    public ByteArrayInputStream generateCSV(int marketId, LocalDate auctionDate) {

        FarmerReadyForPaymentResponse farmerReadyForPaymentResponse = getReadyForPaymentTxns(auctionDate, marketId);
        final CSVFormat format = CSVFormat.DEFAULT.withQuoteMode(QuoteMode.MINIMAL);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             CSVPrinter csvPrinter = new CSVPrinter(new PrintWriter(out), format);) {

            csvPrinter.printRecord(Arrays.asList("Serial Number", "Lot Id", " Farmer Name", "Farmer Number", "Farmer Mobile Number",
                    "Reeler License Number", "Farmer Bank", "IFSC", "Account Number", "Amount"));
            for (FarmerPaymentInfoResponse farmerPaymentInfoResponse : farmerReadyForPaymentResponse.getFarmerPaymentInfoResponseList()) {
                List<? extends Serializable> data = Arrays.asList(
                        farmerPaymentInfoResponse.getSerialNumber(),
                        farmerPaymentInfoResponse.getAllottedLotId(),
                        farmerPaymentInfoResponse.getFarmerFirstName() + " " + farmerPaymentInfoResponse.getFarmerMiddleName() + " " + farmerPaymentInfoResponse.getFarmerLastName(),
                        farmerPaymentInfoResponse.getFarmerNumber(), farmerPaymentInfoResponse.getFarmerMobileNumber(),
                        farmerPaymentInfoResponse.getReelerLicense(), farmerPaymentInfoResponse.getBankName() + " " + farmerPaymentInfoResponse.getBranchName(),
                        farmerPaymentInfoResponse.getIfscCode(), farmerPaymentInfoResponse.getAccountNumber(), (farmerPaymentInfoResponse.getLotSoldOutAmount() - farmerPaymentInfoResponse.getFarmerMarketFee())
                );

                csvPrinter.printRecord(data);
            }
            csvPrinter.printRecord("Total Amount to be given to farmer is: " + farmerReadyForPaymentResponse.getTotalAmountToFarmer());
            csvPrinter.flush();
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("fail to import data to CSV file: " + e.getMessage());
        }
    }

    @Transactional
    public ResponseEntity<?> requestJobToProcessPayment(FarmerPaymentInfoRequest farmerPaymentInfoRequest) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        marketAuctionHelper.getAuthToken(farmerPaymentInfoRequest);
        try {
            boolean exists = transactionFileGenQueueRepository.existsTransactionFileGenQueueByMarketIdAndAuctionDateAndStatusIn(farmerPaymentInfoRequest.getMarketId(), farmerPaymentInfoRequest.getPaymentDate(), Set.of("requested", "processing"));
            if (exists) {
                return marketAuctionHelper.retrunIfError(rw, "Payment Request is under process please try after sometime.");
            }
            exists = transactionFileGenQueueRepository.existsTransactionFileGenQueueByMarketIdAndFileName(farmerPaymentInfoRequest.getMarketId(), farmerPaymentInfoRequest.getFileName());
            if (exists) {
                return marketAuctionHelper.retrunIfError(rw, "Cannot create duplicate request for same fileName");
            }
            TransactionFileGenQueue transactionFileGenQueue = TransactionFileGenQueue.builder().
                    status(LotStatus.REQUESTED.getLabel())
                    .fileName(farmerPaymentInfoRequest.getFileName())
                    .marketId(farmerPaymentInfoRequest.getMarketId())
                    .auctionDate(farmerPaymentInfoRequest.getPaymentDate())
                    .build();
            transactionFileGenQueueRepository.save(transactionFileGenQueue);
        } catch (Exception ex) {

            return marketAuctionHelper.retrunIfError(rw, "Exception while creating job for the readyForPayement with error: " + ex);

        }
        return ResponseEntity.ok(rw);
    }
}
