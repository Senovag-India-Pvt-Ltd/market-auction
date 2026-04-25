package com.sericulture.marketandauction.service;

import com.sericulture.marketandauction.helper.MarketAuctionHelper;
import com.sericulture.marketandauction.helper.Util;
import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.api.marketauction.*;
import com.sericulture.marketandauction.model.entity.*;
import com.sericulture.marketandauction.model.enums.LotStatus;
import com.sericulture.marketandauction.model.enums.PAYMENTMODE;
import com.sericulture.marketandauction.repository.*;
import com.sericulture.marketandauction.model.exceptions.ValidationException;
import com.sericulture.marketandauction.model.mapper.Mapper;
import jakarta.persistence.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.io.File;
import java.util.List;


@Service
@Slf4j
public class LotGroupageService {

    @Autowired
    LotGroupageRepository lotGroupageRepository;

    @Autowired
    LotDeleteAuditRepository lotDeleteAuditRepository;

    @Autowired
    MarketAuctionRepository marketAuctionRepository;

    @Autowired
    SaleAndDisposalOfDflsRepository saleAndDisposalOfDflsRepository;

    @Autowired
    MarketMasterRepository marketMasterRepository;

    @Autowired
    LotRepository lotRepository;

    @Autowired
    ReelerAuctionRepository reelerAuctionRepository;

    @Autowired
    ReelerVidDebitTxnRepository reelerVidDebitTxnRepository;

    @Autowired
    MarketAuctionHelper marketAuctionHelper;


    @Autowired
    Mapper mapper;

    @Autowired
    CustomValidator validator;

    @PersistenceUnit
    private EntityManagerFactory entityManagerFactory;

    @Transactional

    public List<LotGroupageResponse> saveLotGroupage(LotGroupageDetailsRequest lotGroupageDetailsRequest) {
        List<LotGroupageResponse> responses = new ArrayList<>();

        // Check if lotGroupageRequests is null or empty
        if (lotGroupageDetailsRequest.getLotGroupageRequests() == null || lotGroupageDetailsRequest.getLotGroupageRequests().isEmpty()) {
            LotGroupageResponse errorResponse = new LotGroupageResponse();
            errorResponse.setError(true);
            errorResponse.setError_description("Lot groupage requests are null or empty.");
            responses.add(errorResponse);
            return responses;
        }

        for (int i = 0; i < lotGroupageDetailsRequest.getLotGroupageRequests().size(); i++) {
            LotGroupageRequest lotGroupageRequest = lotGroupageDetailsRequest.getLotGroupageRequests().get(i);
            LotGroupage lotGroupage = mapper.lotGroupageObjectToEntity(lotGroupageRequest, LotGroupage.class);

            // Retrieve and set the userMasterId from the JWT token
            lotGroupage.setUserMasterId(Util.getUserMasterId(Util.getTokenValues()));

            List<Object[]> list = lotGroupageRepository.getMarketAuctionIdByAllottedLotIdAndMarketAuctionDate(
                    lotGroupageRequest.getAllottedLotId().intValue(), lotGroupageRequest.getAuctionDate(), lotGroupageRequest.getMarketId());

            // ✅ Validate that the query returned a result
            if (list == null || list.isEmpty()) {
                LotGroupageResponse errorResponse = new LotGroupageResponse();
                errorResponse.setError(true);
                errorResponse.setError_description("No market auction or lot found for allottedLotId: "
                        + lotGroupageRequest.getAllottedLotId()
                        + ", auctionDate: " + lotGroupageRequest.getAuctionDate()
                        + ", marketId: " + lotGroupageRequest.getMarketId());
                responses.add(errorResponse);
                continue; // Skip saving this record, move to next
            }

            boolean hasValidIds = false;
            for (Object[] arr : list) {
                // ✅ Validate individual fields are not null before setting
                if (arr[0] == null || arr[1] == null) {
                    LotGroupageResponse errorResponse = new LotGroupageResponse();
                    errorResponse.setError(true);
                    errorResponse.setError_description("market_auction_id or lot_id is null for allottedLotId: "
                            + lotGroupageRequest.getAllottedLotId());
                    responses.add(errorResponse);
                    hasValidIds = false;
                    break;
                }
                lotGroupage.setMarketAuctionId(((BigDecimal) arr[0]).toBigIntegerExact());
                lotGroupage.setId(((BigDecimal) arr[1]).toBigIntegerExact());
                hasValidIds = true;
            }

            // ✅ Skip saving if IDs were not set due to null values
            if (!hasValidIds) {
                continue;
            }

            // Generate invoice number for each iteration
            String nextSeq = lotGroupageRepository.getNextValInvoiceSequence().toString();
            if (nextSeq.length() == 1)
                nextSeq = "0" + nextSeq;

            String invoiceNumber = "INV/LOTALLOT/" + nextSeq;
            lotGroupage.setInvoiceNumber(invoiceNumber);

            // Calculate and set market fee based on buyer type
            if (lotGroupageRequest.getBuyerType() != null) {
                BigDecimal soldAmount = (lotGroupageRequest.getSoldAmount() != null)
                        ? BigDecimal.valueOf(lotGroupageRequest.getSoldAmount())
                        : BigDecimal.ZERO;
                BigDecimal marketFee = BigDecimal.ZERO;

                switch (lotGroupageRequest.getBuyerType()) {
                    case "RSP":
                    case "NSSO":
                    case "Govt Grainage":
                    case "Reeling":
                        marketFee = soldAmount.multiply(BigDecimal.valueOf(0.01));
                        break;
                    default:
                        break;
                }

                lotGroupage.setMarketFee(marketFee.setScale(2, RoundingMode.HALF_UP).doubleValue());
            }

            // Fetch market master for ONLINE debit (validation done via separate API)
            MarketMaster marketMaster = marketMasterRepository.findById(lotGroupageRequest.getMarketId());
            String buyerVirtualAccount = null;
            if (marketMaster != null && PAYMENTMODE.ONLINE.getLabel().equalsIgnoreCase(marketMaster.getPaymentMode())) {
                buyerVirtualAccount = getVirtualAccountForBuyer(lotGroupageRequest.getBuyerType(),
                        lotGroupageRequest.getBuyerId(), lotGroupageRequest.getExternalUnitId(), lotGroupageRequest.getMarketId());
            }
//            String virtualAccount = buyerVirtualAccount;
            // Always set isDisposed = 1 in LotGroupage
            lotGroupage.setIsDisposed(1);

            Float remainingCocoon = lotGroupageRequest.getRemainingCocoonWeight();

            LotStatus status;
            if (remainingCocoon == null) {
                status = LotStatus.PAYMENTFAILED;
            } else if (remainingCocoon == 0) {
                status = LotStatus.DISTRIBUTED;
            } else {
                status = LotStatus.PAYMENTFAILED;
            }
            lotGroupage.setStatus(status.getLabel());

            // Fetch disposal entry
            SaleAndDisposalOfDfls disposalEntry = saleAndDisposalOfDflsRepository
                    .findByFruitsIdAndLotNumberAndNumberOfDflsDisposedAndIsVerifiedAndActive(
                            lotGroupageRequest.getFruitsId(),
                            lotGroupageRequest.getLotParentLevel(),
                            lotGroupageRequest.getDflLotNumber(),
                            1,
                            true
                    );

            // Update SaleAndDisposalOfDfls only if remainingCocoon = null or 0
            if (remainingCocoon == null || remainingCocoon == 0) {
                if (disposalEntry != null) {
                    disposalEntry.setIsDisposed(1);
                    saleAndDisposalOfDflsRepository.save(disposalEntry);
                }
            }

            // Save LotGroupage
            lotGroupage = lotGroupageRepository.save(lotGroupage);

            // Debit buyer's virtual account (ONLINE mode only) — inserting into REELER_VID_DEBIT_TXN
            // automatically updates REELER_VID_CURRENT_BALANCE (it is a view computed from that table)
            if (marketMaster != null && PAYMENTMODE.ONLINE.getLabel().equalsIgnoreCase(marketMaster.getPaymentMode())
                    && buyerVirtualAccount != null && lotGroupageRequest.getSoldAmount() != null) {
                double soldAmount = lotGroupageRequest.getSoldAmount().doubleValue();
                int buyerId = "RSP".equals(lotGroupageRequest.getBuyerType())
                        ? (lotGroupageRequest.getExternalUnitId() != null ? lotGroupageRequest.getExternalUnitId().intValue() : 0)
                        : (lotGroupageRequest.getBuyerId() != null ? lotGroupageRequest.getBuyerId().intValue() : 0);
                ReelerVidDebitTxn debitTxn = new ReelerVidDebitTxn(
                        lotGroupageRequest.getAllottedLotId().intValue(),
                        lotGroupageRequest.getMarketId(),
                        lotGroupageRequest.getAuctionDate(),
                        buyerId,
                        buyerVirtualAccount,
                        soldAmount
                );
                reelerVidDebitTxnRepository.save(debitTxn);
            }

            // Prepare Response
            LotGroupageResponse lotGroupageResponse =
                    mapper.lotGroupageEntityToObject(lotGroupage, LotGroupageResponse.class);

            lotGroupageResponse.setError(false);
            responses.add(lotGroupageResponse);
        }

        return responses;
    }

    //    public ResponseEntity<?> getLotDistributeDetailsByLotAndMarketAndAuctionDateForSeedMarket(LotStatusSeedMarketRequest lotStatusRequest) {
//        ResponseWrapper rw = ResponseWrapper.createWrapper(LotDistributeResponse.class);
//        LotDistributeResponse lotDistributeResponse = getLotDistributeResponseForSeedMarket(lotStatusRequest);
//        rw.setContent(lotDistributeResponse);
//        return ResponseEntity.ok(rw);
//
//    }
    public ResponseEntity<?> getLotDistributeDetailsByLotAndMarketAndAuctionDateForSeedMarket(LotStatusSeedMarketRequest lotStatusRequest) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(LotDistributeResponse.class);
        List<LotDistributeResponse> lotDistributeResponses = getLotDistributeResponseForSeedMarket(lotStatusRequest);
        rw.setContent(lotDistributeResponses);
        return ResponseEntity.ok(rw);
    }




    public List<LotDistributeResponse> getLotDistributeResponseForSeedMarket(LotStatusSeedMarketRequest lotStatusRequest) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        Query nativeQuery = entityManager.createNativeQuery("""
                WITH PrimaryAddress AS (
                SELECT
                    fa.farmer_id,
                    fa.STATE_ID,
                    fa.DISTRICT_ID,
                    fa.TALUK_ID,
                    fa.HOBLI_ID,
                    fa.VILLAGE_ID,
                    ROW_NUMBER() OVER (PARTITION BY fa.farmer_id ORDER BY fa.district_id DESC) AS rn
                FROM
                    farmer_address fa
                WHERE
                    fa.active = 1
            )
            SELECT
                f.farmer_number,
                f.fruits_id,
                f.first_name,
                f.middle_name,
                f.last_name,
                ma.RACE_MASTER_ID,
                v.VILLAGE_NAME,
                mm.market_name,
                rm.race_name,
                sm.source_name,
                mm.box_weight,
                l.status,
                lg.lot_groupage_id,
                lg.buyer_id,
                lg.buyer_type,
                lg.lot_weight,
                lg.amount,
                lg.market_fee,
                lg.sold_amount,
                ROUND(
                     CASE
                         WHEN lg.lot_groupage_id IS NOT NULL THEN lg.remaining_cocoon
                         ELSE l.LOT_WEIGHT_AFTER_WEIGHMENT
                     END,
                     2
                 ) AS weight_to_show,
                ma.dfl_lot_number,
                ma.lot_variety,
                ma.lot_Parental_Level,
                ma.estimated_weight,
                lbpf.PRICE_PER_KG,
                lbpf.FIXATION_DATE,
                ptaca.TEST_DATE,
                ptaca.NO_OF_COCOON_TAKEN_FOR_EXAMINATION,
                ptaca.NO_OF_DFL_FROM_FC,
                ptaca.DISEASE_FREE,
                ptaca.DISEASE_TYPE,
                ptaca.NO_OF_COCOON_PER_KG,
                ptaca.MELT_PERCENTAGE,
                ptaca.pupa_cocoon_status,
                ptaca.PUPA_TEST_RESULT,
                ma.market_auction_date,
                l.allotted_lot_id,
                lg.average_yield,
                lg.no_of_dfls,
                lg.invoice_number,
                ROUND((l.LOT_WEIGHT_AFTER_WEIGHMENT * 100) / ma.dfl_lot_number, 2) AS calculatedAverageYield,
                ROUND(lg.remaining_cocoon, 2) AS remaining_cocoon,
                SUM(lg.lot_weight) OVER (PARTITION BY l.lot_id) AS soldCocoonInKgs,
                ROUND(l.LOT_WEIGHT_AFTER_WEIGHMENT, 2) AS LOT_WEIGHT_AFTER_WEIGHMENT,
                CASE
                    WHEN lg.buyer_type = 'RSP' THEN es.license_number
                    WHEN lg.buyer_type = 'NSSO' THEN es.address
                    WHEN lg.buyer_type = 'Govt Grainage' THEN gm.grainage_master_name
                    WHEN lg.buyer_type = 'Reeling' THEN r.name
                    ELSE NULL
                END AS buyer_name
                FROM FARMER f
                    INNER JOIN market_auction ma
                        ON ma.farmer_id = f.FARMER_ID
                       AND ma.ACTIVE = 1
        
                    INNER JOIN lot l
                        ON l.market_auction_id = ma.market_auction_id
                       AND l.ACTIVE = 1
        
                    LEFT JOIN PrimaryAddress pa
                        ON pa.farmer_id = f.FARMER_ID
                       AND pa.rn = 1
        
                    LEFT JOIN Village v
                        ON pa.VILLAGE_ID = v.village_id
                       AND v.ACTIVE = 1
        
                    LEFT JOIN market_master mm
                        ON mm.market_master_id = ma.market_id
                       AND mm.ACTIVE = 1
        
                    LEFT JOIN race_master rm
                        ON rm.race_id = ma.lot_variety
                       AND rm.ACTIVE = 1
        
                    LEFT JOIN source_master sm
                        ON sm.source_id = ma.SOURCE_MASTER_ID
                       AND sm.ACTIVE = 1
        
                    LEFT JOIN lot_groupage lg
                        ON l.lot_id = lg.lot_id
                       AND lg.ACTIVE = 1
        
                    LEFT JOIN PUPA_TEST_AND_COCOON_ASSESSMENT ptaca
                        ON ptaca.MARKET_AUCTION_ID = ma.market_auction_id
                       AND ptaca.ACTIVE = 1
        
                    LEFT JOIN LOT_BASE_PRICE_FIXATION lbpf
                        ON lbpf.MARKET_ID = ma.market_id
                       AND lbpf.allotted_lot_id = l.allotted_lot_id
                       AND lbpf.FIXATION_DATE = CAST(GETDATE() AS DATE)
                       AND lbpf.ACTIVE = 1
        
                    LEFT JOIN reeler r
                        ON lg.buyer_id = r.reeler_id
                       AND lg.buyer_type = 'Reeling'
                       AND r.ACTIVE = 1
        
                    LEFT JOIN external_unit_registration es
                        ON lg.external_unit_id = es.external_unit_registration_id
                       AND lg.buyer_type IN ('RSP', 'NSSO')
                       AND es.ACTIVE = 1
        
                    LEFT JOIN grainage_master gm
                        ON lg.external_unit_id = gm.grainage_master_id
                       AND lg.buyer_type = 'Govt Grainage'
                       AND gm.ACTIVE = 1
        
                    WHERE f.ACTIVE = 1
                        AND l.allotted_lot_id = ?
                        AND l.auction_date = ?
                        AND l.market_id = ?
                        AND f.ACTIVE = 1
                        AND ma.active = 1
                        AND l.status = 'weighmentcompleted';
            """);

        nativeQuery.setParameter(1, lotStatusRequest.getAllottedLotId());
        nativeQuery.setParameter(2, lotStatusRequest.getAuctionDate());
        nativeQuery.setParameter(3, lotStatusRequest.getMarketId());

        List<Object[]> lotWeightDetailsList = nativeQuery.getResultList();
        entityManager.close();

        if (lotWeightDetailsList.isEmpty()) {
            throw new ValidationException(String.format("No data found for the given lot %s, Please check whether it is accepted or not", lotStatusRequest.getAllottedLotId()));
        }

        List<LotDistributeResponse> responses = new ArrayList<>();
        for (Object[] lotWeightDetails : lotWeightDetailsList) {
            LotDistributeResponse lotDistributeResponse = LotDistributeResponse.builder()
                    .farmerNumber(Util.objectToString(lotWeightDetails[0]))
                    .farmerFruitsId(Util.objectToString(lotWeightDetails[1]))
                    .farmerFirstName(Util.objectToString(lotWeightDetails[2]))
                    .farmerMiddleName(Util.objectToString(lotWeightDetails[3]))
                    .farmerLastName(Util.objectToString(lotWeightDetails[4]))
                    .raceMasterId(Util.objectToInteger(lotWeightDetails[5]))
                    .farmerVillage(Util.objectToString(lotWeightDetails[6]))
                    .marketName(Util.objectToString(lotWeightDetails[7]))
                    .race(Util.objectToString(lotWeightDetails[8]))
                    .source(Util.objectToString(lotWeightDetails[9]))
                    .tareWeight(Util.objectToFloat(lotWeightDetails[10]))
                    .lotStatus(Util.objectToString(lotWeightDetails[11]))
                    .lotGroupageId(Util.objectToLong(lotWeightDetails[12]))
                    .buyerId(Util.objectToLong(lotWeightDetails[13]))
                    .buyerType(Util.objectToString(lotWeightDetails[14]))
                    .lotWeight(Util.objectToFloat(lotWeightDetails[15]))
                    .amount(Util.objectToFloat(lotWeightDetails[16]))
                    .marketFee(Util.objectToFloat(lotWeightDetails[17]))
                    .soldAmount(Util.objectToFloat(lotWeightDetails[18]))
                    .netWeight(Util.objectToString(lotWeightDetails[19]))
                    .noOfDFLs(Util.objectToString(lotWeightDetails[20]))
                    .raceMasterId(Util.objectToInteger(lotWeightDetails[21]))
                    .lotParentLevel(Util.objectToString(lotWeightDetails[22]))
                    .initialWeighment(Util.objectToFloat(lotWeightDetails[23]))
                    .price(Util.objectToString(lotWeightDetails[24]))
                    .fixationDate(Util.objectToString(lotWeightDetails[25]))
                    .testDate(Util.objectToString(lotWeightDetails[26]))
                    .noOfCocoonTakenForExamination(Util.objectToLong(lotWeightDetails[27]))
                    .noOfDFLFromFc(Util.objectToLong(lotWeightDetails[28]))
                    .noOfCocoonPerKg(Util.objectToLong(lotWeightDetails[31]))
                    .meltPercentage(Util.objectToString(lotWeightDetails[32]))
                    .pupaCocoonStatus(Util.objectToString(lotWeightDetails[33]))
                    .noOfCocoonExamined(Util.objectToString(lotWeightDetails[34]))
                    .marketAuctionDate(Util.objectToString(lotWeightDetails[35]))
                    .allottedLotId(Util.objectToInteger(lotWeightDetails[36]))
                    .averageYield(Util.objectToString(lotWeightDetails[37]))
                    .dflLotNumber(Util.objectToString(lotWeightDetails[38]))
                    .invoiceNumber(Util.objectToString(lotWeightDetails[39]))
                    .calculatedAverageYield(Util.objectToString(lotWeightDetails[40]))
                    .remainingCocoonWeight(Util.objectToString(lotWeightDetails[41]))
                    .soldCocoonInKgs(Util.objectToString(lotWeightDetails[42]))
                    .lotWeightAfterWeighment(Util.objectToString(lotWeightDetails[43]))
                    .buyerName(Util.objectToString(lotWeightDetails[44]))
                    .build();
            responses.add(lotDistributeResponse);
        }

        return responses;
    }

//

    @Transactional
    public List<LotGroupageResponse> editLotGroupage(LotGroupageDetailsRequestEdit lotGroupageDetailsRequestEdit) {
        List<LotGroupageResponse> responses = new ArrayList<>();

        // Check if lotGroupageRequestEditList is null or empty
        if (lotGroupageDetailsRequestEdit.getLotGroupageRequestEditList() == null || lotGroupageDetailsRequestEdit.getLotGroupageRequestEditList().isEmpty()) {
            LotGroupageResponse errorResponse = new LotGroupageResponse();
            errorResponse.setError(true);
            errorResponse.setError_description("Lot groupage edit requests are null or empty.");
            responses.add(errorResponse);
            return responses;
        }

        for (LotGroupageRequestEdit lotGroupageRequestEdit : lotGroupageDetailsRequestEdit.getLotGroupageRequestEditList()) {
            LotGroupageResponse lotGroupageResponse = new LotGroupageResponse();
            LotGroupage lotGroupage;

            // Check for lotGroupageId and fetch existing lotGroupage if present
            if (lotGroupageRequestEdit.getLotGroupageId() != null) {
                Optional<LotGroupage> optionalLotGroupage = lotGroupageRepository.findByLotGroupageIdAndActiveIn(lotGroupageRequestEdit.getLotGroupageId(), Set.of(true, false));
                if (!optionalLotGroupage.isPresent()) {
                    lotGroupageResponse.setError(true);
                    lotGroupageResponse.setError_description("Lot groupage with ID " + lotGroupageRequestEdit.getLotGroupageId() + " not found.");
                    responses.add(lotGroupageResponse);
                    continue;
                }
                lotGroupage = optionalLotGroupage.get();
            } else {
                // Create a new LotGroupage if no ID is provided
                lotGroupage = new LotGroupage();

                // Generate invoice number for the new LotGroupage
                String nextSeq = lotGroupageRepository.getNextValInvoiceSequence().toString();
                if (nextSeq.length() == 1)
                    nextSeq = "0" + nextSeq;

                String invoiceNumber = "INV/LOTALLOT/" + nextSeq;
                lotGroupage.setInvoiceNumber(invoiceNumber);
            }

            // Capture values before mapper overwrites them
            Long oldSoldAmount = lotGroupage.getSoldAmount();
            Long existingExternalUnitId = lotGroupage.getExternalUnitId();
            Double existingMarketFee = lotGroupage.getMarketFee();

            // Set the userMasterId from JWT token
            lotGroupage.setUserMasterId(Util.getUserMasterId(Util.getTokenValues()));

            // Save the current invoice number (if it exists) to avoid overwriting
            String currentInvoiceNumber = lotGroupage.getInvoiceNumber();

            // Fetch market auction details and update lotGroupage
            List<Object[]> marketAuctionDetails = lotGroupageRepository.getMarketAuctionIdByAllottedLotIdAndMarketAuctionDate(lotGroupageRequestEdit.getAllottedLotId().intValue(), lotGroupageRequestEdit.getAuctionDate(),lotGroupageRequestEdit.getMarketId());
            for (Object[] arr : marketAuctionDetails) {
                lotGroupage.setMarketAuctionId(((BigDecimal) arr[0]).toBigIntegerExact());
                lotGroupage.setId(((BigDecimal) arr[1]).toBigIntegerExact());
            }

            // Update lotGroupage based on lotGroupageRequestEdit using the mapper method
            mapper.editLotGroupageObjectToEntity(lotGroupageRequestEdit, lotGroupage);

            // Set the userMasterId from JWT token
            lotGroupage.setUserMasterId(Util.getUserMasterId(Util.getTokenValues()));

            // Restore the invoice number to ensure it's not overwritten
            lotGroupage.setInvoiceNumber(currentInvoiceNumber);

            // Restore externalUnitId: use request value if provided, else keep existing DB value
            lotGroupage.setExternalUnitId(
                lotGroupageRequestEdit.getExternalUnitId() != null
                    ? lotGroupageRequestEdit.getExternalUnitId()
                    : existingExternalUnitId
            );

            // Restore marketFee for existing records; new records calculate it below
            if (lotGroupageRequestEdit.getLotGroupageId() != null) {
                lotGroupage.setMarketFee(existingMarketFee);
            }

            // Save debit txn for the difference only (ONLINE mode only)
            MarketMaster editMarketMaster = marketMasterRepository.findById(lotGroupageRequestEdit.getMarketId());
            if (editMarketMaster != null && PAYMENTMODE.ONLINE.getLabel().equalsIgnoreCase(editMarketMaster.getPaymentMode())) {
                long newSoldAmount = lotGroupageRequestEdit.getSoldAmount() != null ? lotGroupageRequestEdit.getSoldAmount() : 0L;
                long prevSoldAmount = oldSoldAmount != null ? oldSoldAmount : 0L;
                long debitDifference = newSoldAmount - prevSoldAmount;
                String newVirtualAccount = getVirtualAccountForBuyer(lotGroupageRequestEdit.getBuyerType(),
                        lotGroupageRequestEdit.getBuyerId(), lotGroupageRequestEdit.getExternalUnitId(), lotGroupageRequestEdit.getMarketId());
                if (newVirtualAccount != null && debitDifference > 0) {
                    int newBuyerIntId = "RSP".equals(lotGroupageRequestEdit.getBuyerType())
                            ? (lotGroupageRequestEdit.getExternalUnitId() != null ? lotGroupageRequestEdit.getExternalUnitId().intValue() : 0)
                            : (lotGroupageRequestEdit.getBuyerId() != null ? lotGroupageRequestEdit.getBuyerId().intValue() : 0);
                    reelerVidDebitTxnRepository.save(new ReelerVidDebitTxn(
                            lotGroupageRequestEdit.getAllottedLotId().intValue(),
                            lotGroupageRequestEdit.getMarketId(),
                            lotGroupageRequestEdit.getAuctionDate(),
                            newBuyerIntId,
                            newVirtualAccount,
                            (double) debitDifference
                    ));
                }
            }

            // Calculate market fee only for new records; existing records keep their saved value
            if (lotGroupageRequestEdit.getLotGroupageId() == null && lotGroupageRequestEdit.getBuyerType() != null) {
                BigDecimal soldAmount = (lotGroupageRequestEdit.getSoldAmount() != null)
                        ? BigDecimal.valueOf(lotGroupageRequestEdit.getSoldAmount())
                        : BigDecimal.ZERO; // default to zero if soldAmount is null
                BigDecimal marketFee = BigDecimal.ZERO;

                switch (lotGroupageRequestEdit.getBuyerType()) {
                    case "RSP":
                    case "NSSO":
                    case "Govt Grainage":
                    case "Reeling":
                        marketFee = soldAmount.multiply(BigDecimal.valueOf(0.01));
                        break;
                    default:
                        break;
                }

                lotGroupage.setMarketFee(marketFee.setScale(2, RoundingMode.HALF_UP).doubleValue());
            }

//            // Save updated or new lotGroupage
//            lotGroupage = lotGroupageRepository.save(lotGroupage);
//
//            // Map the saved lotGroupage to the response object
//            LotGroupageResponse singleResponse = mapper.lotGroupageEntityToObject(lotGroupage, LotGroupageResponse.class);
//            singleResponse.setError(false);
//            responses.add(singleResponse);  // Collect each response
//        }
//
//        return responses; // Return the list of responses
//    }

            lotGroupage.setIsDisposed(1);

            Float remainingCocoon = lotGroupageRequestEdit.getRemainingCocoonWeight();

            LotStatus editStatus;
            if (remainingCocoon == null) {
                editStatus = LotStatus.PAYMENTFAILED;
            } else if (remainingCocoon == 0) {
                editStatus = LotStatus.DISTRIBUTED;
            } else {
                editStatus = LotStatus.PAYMENTFAILED;
            }
            lotGroupage.setStatus(editStatus.getLabel());

            // Fetch disposal entry
            SaleAndDisposalOfDfls disposalEntry = saleAndDisposalOfDflsRepository
                    .findByFruitsIdAndLotNumberAndNumberOfDflsDisposedAndIsVerifiedAndActive(
                            lotGroupageRequestEdit.getFruitsId(),
                            lotGroupageRequestEdit.getLotParentLevel(),
                            lotGroupageRequestEdit.getDflLotNumber(),
                            1,
                            true
                    );

            // Update SaleAndDisposalOfDfls only if remainingCocoon = null or 0
            if (remainingCocoon == null || remainingCocoon == 0) {
                if (disposalEntry != null) {
                    disposalEntry.setIsDisposed(1);
                    saleAndDisposalOfDflsRepository.save(disposalEntry);
                }
            }

            // Save LotGroupage
            lotGroupage = lotGroupageRepository.save(lotGroupage);

            // Map the saved lotGroupage to the response object
            LotGroupageResponse singleResponse = mapper.lotGroupageEntityToObject(lotGroupage, LotGroupageResponse.class);
            singleResponse.setError(false);
            responses.add(singleResponse);  // Collect each response
        }

        return responses; // Return the list of responses
    }



    public ResponseEntity<?> validateReelerBalanceForLotGroupage(LotGroupageDetailsRequest lotGroupageDetailsRequest) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);

        if (lotGroupageDetailsRequest.getLotGroupageRequests() == null || lotGroupageDetailsRequest.getLotGroupageRequests().isEmpty()) {
            return marketAuctionHelper.retrunIfError(rw, "Lot groupage requests are null or empty.");
        }

        for (LotGroupageRequest lotGroupageRequest : lotGroupageDetailsRequest.getLotGroupageRequests()) {
            String buyerType = lotGroupageRequest.getBuyerType();

            // Balance check only applies to Reeling and RSP buyer types
            if (!"Reeling".equals(buyerType) && !"RSP".equals(buyerType)) {
                continue;
            }

            MarketMaster marketMaster = marketMasterRepository.findById(lotGroupageRequest.getMarketId());

            if (marketMaster == null || !PAYMENTMODE.ONLINE.getLabel().equalsIgnoreCase(marketMaster.getPaymentMode())) {
                continue;
            }

            float minimumBalance = marketMaster.getReleerMinimumBalance() != null ? marketMaster.getReleerMinimumBalance() : 0f;
            float currentBalance = getCurrentBalanceForBuyer(buyerType,
                    lotGroupageRequest.getBuyerId(), lotGroupageRequest.getExternalUnitId(), lotGroupageRequest.getMarketId());

            float hasEnoughMoney = currentBalance - minimumBalance;
            if (hasEnoughMoney < 0) {
                return marketAuctionHelper.retrunIfError(rw,
                        "Reeler/RSP current balance is not enough and needs " + Math.abs(hasEnoughMoney) + " more money");
            }

            long soldAmount = lotGroupageRequest.getSoldAmount() != null ? lotGroupageRequest.getSoldAmount() : 0L;
            if (currentBalance < soldAmount) {
                return marketAuctionHelper.retrunIfError(rw,
                        "Reeler/RSP current balance is not enough and needs " + (soldAmount - currentBalance) + " more money");
            }
        }

        rw.setContent("Balance validation successful");
        return ResponseEntity.ok(rw);
    }

    private boolean isBuyerChanged(String oldBuyerType, Long oldBuyerId, Long oldExternalUnitId,
                                   String newBuyerType, Long newBuyerId, Long newExternalUnitId) {
        // New record — no previous buyer exists, nothing to credit/debit
        if (oldBuyerType == null) return false;

        if ("Reeling".equals(oldBuyerType)) {
            return !Objects.equals(oldBuyerId, newBuyerId);
        } else {
            return !Objects.equals(oldExternalUnitId, newExternalUnitId);
        }
    }

    private String getVirtualAccountForBuyer(String buyerType, Long buyerId,
                                             Long externalUnitId, int marketId) {

        if ("Reeling".equals(buyerType) && buyerId != null) {
            return reelerAuctionRepository
                    .getReelerVirtualAccountByReelerIdAndMarketId(buyerId.intValue(), marketId);
        }
        else if ("RSP".equals(buyerType) && externalUnitId != null) {

            return  lotGroupageRepository
                    .getExternalUnitVirtualAccountAndBalanceSave(externalUnitId, marketId);

        }

        return null;
    }

    private float getCurrentBalanceForBuyer(String buyerType, Long buyerId, Long externalUnitId, int marketId) {
        if ("Reeling".equals(buyerType) && buyerId != null) {
            Object[][] balanceData = reelerAuctionRepository.getReelerBalance(buyerId.intValue(), marketId);
            if (balanceData != null && balanceData.length > 0 && balanceData[0][2] != null) {
                return Util.objectToFloat(balanceData[0][2]);
            }
        } else if ("RSP".equals(buyerType) && externalUnitId != null) {
            // RSP uses eu_virtual_bank_account with payment_via_bank = 1
            Object[][] result = lotGroupageRepository.getExternalUnitVirtualAccountBalance(externalUnitId, marketId);
            if (result != null && result.length > 0 && result[0][1] != null) {
                return Util.objectToFloat(result[0][1]);
            }
        }
        // NSSO, Govt Grainage — no balance check, return 0 (validation skipped for these types)
        return 0f;
    }

    public List<LotDistributeResponse> getLotDistributeResponseForInvoiceForSeedMarket(LotStatusSeedMarketRequest lotStatusRequest) {
        List<Object[]> lotWeightDetailsList = lotGroupageRepository.getLotDistributeDetailsForInvoice(
//                lotStatusRequest.getAllottedLotId(),
                lotStatusRequest.getFromDate(),
                lotStatusRequest.getToDate(),
                lotStatusRequest.getMarketId(),
                lotStatusRequest.getGrainageMasterId()
        );


        List<LotDistributeResponse> responses = new ArrayList<>();
        int serial = 1; // Initialize counter

        for (Object[] lotWeightDetails : lotWeightDetailsList) {
            LotDistributeResponse lotDistributeResponse = LotDistributeResponse.builder()
                    .serialNumber(serial++) // Set serial number from counter
                    .farmerNumber(Util.objectToString(lotWeightDetails[0]))
                    .farmerFruitsId(Util.objectToString(lotWeightDetails[1]))
                    .farmerFullName(Util.objectToString(lotWeightDetails[2]))
                    .raceMasterId(Util.objectToInteger(lotWeightDetails[3]))
                    .farmerVillage(Util.objectToString(lotWeightDetails[4]))
                    .marketName(Util.objectToString(lotWeightDetails[5]))
                    .race(Util.objectToString(lotWeightDetails[6]))
                    .source(Util.objectToString(lotWeightDetails[7]))
                    .tareWeight(Util.objectToFloat(lotWeightDetails[8]))
                    .lotStatus(Util.objectToString(lotWeightDetails[9]))
                    .lotGroupageId(Util.objectToLong(lotWeightDetails[10]))
                    .buyerId(Util.objectToLong(lotWeightDetails[11]))
                    .buyerType(Util.objectToString(lotWeightDetails[12]))
                    .lotWeight(Util.objectToFloat(lotWeightDetails[13]))
                    .amount(Util.objectToFloat(lotWeightDetails[14]))
                    .marketFee(Util.objectToFloat(lotWeightDetails[15]))
                    .soldAmount(Util.objectToFloat(lotWeightDetails[16]))
                    .netWeight(Util.objectToString(lotWeightDetails[17]))
                    .dflLotNumber(Util.objectToString(lotWeightDetails[18]))
                    .raceMasterId(Util.objectToInteger(lotWeightDetails[19]))
                    .lotParentLevel(Util.objectToString(lotWeightDetails[20]))
                    .initialWeighment(Util.objectToFloat(lotWeightDetails[21]))
                    .price(Util.objectToString(lotWeightDetails[22]))
                    .fixationDate(Util.objectToString(lotWeightDetails[23]))
                    .testDate(Util.objectToString(lotWeightDetails[24]))
                    .noOfCocoonTakenForExamination(Util.objectToLong(lotWeightDetails[25]))
                    .noOfDFLFromFc(Util.objectToLong(lotWeightDetails[26]))
                    .noOfCocoonPerKg(Util.objectToLong(lotWeightDetails[29]))
                    .totalNumber(Util.formatToTwoDecimalPlaces(lotWeightDetails[30]))
                    .meltPercentage(Util.objectToString(lotWeightDetails[31]))
                    .pupaCocoonStatus(Util.objectToString(lotWeightDetails[32]))
                    .noOfCocoonExamined(Util.objectToString(lotWeightDetails[33]))
                    .marketAuctionDate(Util.objectToString(lotWeightDetails[34]))
                    .allottedLotId(Util.objectToInteger(lotWeightDetails[35]))
                    .averageYield(Util.objectToString(lotWeightDetails[36]))
                    .dflLotNumber(Util.objectToString(lotWeightDetails[37]))
                    .invoiceNumber(Util.objectToString(lotWeightDetails[38]))
                    .spunFromDate(Util.objectToString(lotWeightDetails[39]))
                    .spunToDate(Util.objectToString(lotWeightDetails[40]))
                    .calculatedAverageYield(Util.objectToString(lotWeightDetails[41]))
                    .remainingCocoonWeight(Util.objectToString(lotWeightDetails[42]))
                    .soldCocoonInKgs(Util.objectToString(lotWeightDetails[43]))
                    .lotWeightAfterWeighment(Util.objectToString(lotWeightDetails[44]))
                    .buyerName(Util.objectToString(lotWeightDetails[45]))
                    .build();

            responses.add(lotDistributeResponse);
        }

        return responses;
    }

    public List<LotDistributeResponse> getLotDistributeDetailsForPermitRSP(LotStatusSeedMarketRequest lotStatusRequest) {
        List<Object[]> lotWeightDetailsList = lotGroupageRepository.getLotDistributeDetailsForPermitRSP(
//                lotStatusRequest.getAllottedLotId(),
                lotStatusRequest.getFromDate(),
                lotStatusRequest.getToDate(),
                lotStatusRequest.getMarketId(),
                lotStatusRequest.getExternalUnitRegistrationId()
        );


        List<LotDistributeResponse> responses = new ArrayList<>();
        int serial = 1; // Initialize counter

        for (Object[] lotWeightDetails : lotWeightDetailsList) {
            LotDistributeResponse lotDistributeResponse = LotDistributeResponse.builder()
                    .serialNumber(serial++) // Set serial number from counter
                    .farmerNumber(Util.objectToString(lotWeightDetails[0]))
                    .farmerFruitsId(Util.objectToString(lotWeightDetails[1]))
                    .farmerFullName(Util.objectToString(lotWeightDetails[2]))
                    .raceMasterId(Util.objectToInteger(lotWeightDetails[3]))
                    .farmerVillage(Util.objectToString(lotWeightDetails[4]))
                    .marketName(Util.objectToString(lotWeightDetails[5]))
                    .race(Util.objectToString(lotWeightDetails[6]))
                    .source(Util.objectToString(lotWeightDetails[7]))
                    .tareWeight(Util.objectToFloat(lotWeightDetails[8]))
                    .lotStatus(Util.objectToString(lotWeightDetails[9]))
                    .lotGroupageId(Util.objectToLong(lotWeightDetails[10]))
                    .buyerId(Util.objectToLong(lotWeightDetails[11]))
                    .buyerType(Util.objectToString(lotWeightDetails[12]))
                    .lotWeight(Util.formatToTwoDecimalPlaces(lotWeightDetails[13]))
                    .totalSoldOutAmount(Util.formatToTwoDecimalPlaces(lotWeightDetails[14]))
                    .totalLotWeight(Util.formatToTwoDecimalPlaces(lotWeightDetails[15]))
                    .amount(Util.formatToTwoDecimalPlaces(lotWeightDetails[16]))
                    .marketFee(Util.objectToFloat(lotWeightDetails[17]))
                    .soldAmount(Util.formatToTwoDecimalPlaces(lotWeightDetails[18]))
                    .netWeight(Util.objectToString(lotWeightDetails[19]))
                    .dflLotNumber(Util.objectToString(lotWeightDetails[20]))
                    .raceMasterId(Util.objectToInteger(lotWeightDetails[21]))
                    .lotParentLevel(Util.objectToString(lotWeightDetails[22]))
                    .initialWeighment(Util.objectToFloat(lotWeightDetails[23]))
                    .price(Util.objectToString(lotWeightDetails[24]))
                    .fixationDate(Util.objectToString(lotWeightDetails[25]))
                    .testDate(Util.objectToString(lotWeightDetails[26]))
                    .noOfCocoonTakenForExamination(Util.objectToLong(lotWeightDetails[27]))
                    .noOfDFLFromFc(Util.objectToLong(lotWeightDetails[28]))
                    .noOfCocoonPerKg(Util.objectToLong(lotWeightDetails[31]))
                    .totalNumber(Util.formatToTwoDecimalPlaces(lotWeightDetails[32]))
                    .meltPercentage(Util.objectToString(lotWeightDetails[33]))
                    .pupaCocoonStatus(Util.objectToString(lotWeightDetails[34]))
                    .noOfCocoonExamined(Util.objectToString(lotWeightDetails[35]))
                    .marketAuctionDate(Util.objectToString(lotWeightDetails[36]))
                    .allottedLotId(Util.objectToInteger(lotWeightDetails[37]))
                    .averageYield(Util.objectToString(lotWeightDetails[38]))
                    .dflLotNumber(Util.objectToString(lotWeightDetails[39]))
                    .invoiceNumber(Util.objectToString(lotWeightDetails[40]))

                    .spunFromDate(Util.objectToString(lotWeightDetails[41]))
                    .spunToDate(Util.objectToString(lotWeightDetails[42]))

                    .calculatedAverageYield(Util.objectToString(lotWeightDetails[43]))
                    .remainingCocoonWeight(Util.objectToString(lotWeightDetails[44]))
                    .soldCocoonInKgs(Util.objectToString(lotWeightDetails[45]))
                    .lotWeightAfterWeighment(Util.objectToString(lotWeightDetails[46]))
                    .buyerName(Util.objectToString(lotWeightDetails[47]))
                    .rspAddress(Util.objectToString(lotWeightDetails[48]))
                    .licenseNo(Util.objectToString(lotWeightDetails[49]))

                    .build();

            responses.add(lotDistributeResponse);
        }

        return responses;
    }

    public List<LotDistributeResponse> getLotDistributeDetailsForMarketReceiptAndCashReceipt(LotStatusSeedMarketRequest lotStatusRequest) {
        List<Object[]> lotWeightDetailsList = lotGroupageRepository.getLotDistributeDetailsForMarketReceiptAndCashReceipt(
//                lotStatusRequest.getAllottedLotId(),
                lotStatusRequest.getAuctionDate(),
                lotStatusRequest.getMarketId(),
                lotStatusRequest.getAllottedLotId()
        );


        List<LotDistributeResponse> responses = new ArrayList<>();
        int serial = 1; // Initialize counter

        for (Object[] lotWeightDetails : lotWeightDetailsList) {
            LotDistributeResponse lotDistributeResponse = LotDistributeResponse.builder()
                    .serialNumber(serial++) // Set serial number from counter
                    .farmerNumber(Util.objectToString(lotWeightDetails[0]))
                    .farmerFruitsId(Util.objectToString(lotWeightDetails[1]))
                    .farmerFullName(Util.objectToString(lotWeightDetails[2]))
                    .fatherNameKan(Util.objectToString(lotWeightDetails[3]))
                    .farmerVillage(Util.objectToString(lotWeightDetails[4]))
                    .marketName(Util.objectToString(lotWeightDetails[5]))
                    .race(Util.objectToString(lotWeightDetails[6]))
                    .source(Util.objectToString(lotWeightDetails[7]))
                    .tareWeight(Util.objectToFloat(lotWeightDetails[8]))
                    .lotStatus(Util.objectToString(lotWeightDetails[9]))
                    .lotWeight(Util.objectToFloat(lotWeightDetails[10]))
                    .amount(Util.objectToFloat(lotWeightDetails[11]))
                    .marketFee(Util.objectToFloat(lotWeightDetails[12]))
                    .soldAmount(Util.objectToFloat(lotWeightDetails[13]))
                    .dflLotNumber(Util.objectToString(lotWeightDetails[14]))
                    .raceMasterId(Util.objectToInteger(lotWeightDetails[15]))
                    .lotParentLevel(Util.objectToString(lotWeightDetails[16]))
                    .initialWeighment(Util.objectToFloat(lotWeightDetails[17]))
                    .price(Util.objectToString(lotWeightDetails[18]))
                    .fixationDate(Util.objectToString(lotWeightDetails[19]))
                    .testDate(Util.objectToString(lotWeightDetails[20]))
                    .noOfCocoonTakenForExamination(Util.objectToLong(lotWeightDetails[21]))
                    .noOfDFLFromFc(Util.objectToLong(lotWeightDetails[22]))
                    .noOfCocoonPerKg(Util.objectToLong(lotWeightDetails[23]))
                    .pupaCocoonStatus(Util.objectToString(lotWeightDetails[24]))
                    .noOfCocoonExamined(Util.objectToString(lotWeightDetails[25]))
                    .marketAuctionDate(Util.objectToString(lotWeightDetails[26]))
                    .allottedLotId(Util.objectToInteger(lotWeightDetails[27]))
                    .averageYield(Util.objectToString(lotWeightDetails[28]))
                    .invoiceNumber(Util.objectToString(lotWeightDetails[29]))
                    .spunFromDate(Util.objectToString(lotWeightDetails[30]))
                    .spunToDate(Util.objectToString(lotWeightDetails[31]))
                    .buyerName(Util.objectToString(lotWeightDetails[32]))
                    .calculatedAverageYield(Util.objectToString(lotWeightDetails[33]))
                    .remainingCocoonWeight(Util.objectToString(lotWeightDetails[34]))
                    .soldCocoonInKgs(Util.objectToString(lotWeightDetails[35]))
                    .lotWeightAfterWeighment(Util.objectToString(lotWeightDetails[36]))
                    .build();

            responses.add(lotDistributeResponse);
        }

        return responses;
    }


    public List<LotDistributeResponse> getLotDistributeResponseForInvoiceAndBonusScheme(LotStatusSeedMarketRequest lotStatusRequest) {
        List<Object[]> lotWeightDetailsList = lotGroupageRepository.getLotDistributeResponseForInvoiceAndBonusScheme(
//                lotStatusRequest.getAllottedLotId(),
                lotStatusRequest.getAuctionDate(),
                lotStatusRequest.getMarketId(),
                lotStatusRequest.getAllottedLotId(),
                lotStatusRequest.getFruitsId()
        );


        List<LotDistributeResponse> responses = new ArrayList<>();
        int serial = 1; // Initialize counter

        for (Object[] lotWeightDetails : lotWeightDetailsList) {
            LotDistributeResponse lotDistributeResponse = LotDistributeResponse.builder()
                    .serialNumber(serial++) // Set serial number from counter
                    .farmerNumber(Util.objectToString(lotWeightDetails[0]))
                    .farmerFruitsId(Util.objectToString(lotWeightDetails[1]))
                    .farmerFullName(Util.objectToString(lotWeightDetails[2]))
                    .farmerVillage(Util.objectToString(lotWeightDetails[3]))
                    .marketName(Util.objectToString(lotWeightDetails[4]))
                    .race(Util.objectToString(lotWeightDetails[5]))
                    .tareWeight(Util.objectToFloat(lotWeightDetails[6]))
                    .lotStatus(Util.objectToString(lotWeightDetails[7]))
                    .lotGroupageId(Util.objectToLong(lotWeightDetails[8]))
                    .buyerId(Util.objectToLong(lotWeightDetails[9]))
                    .buyerType(Util.objectToString(lotWeightDetails[10]))
                    .lotWeight(Util.objectToFloat(lotWeightDetails[11]))
                    .amount(Util.objectToFloat(lotWeightDetails[12]))
                    .marketFee(Util.objectToFloat(lotWeightDetails[13]))
                    .soldAmount(Util.objectToFloat(lotWeightDetails[14]))
                    .dflLotNumber(Util.objectToString(lotWeightDetails[15]))
                    .raceMasterId(Util.objectToInteger(lotWeightDetails[16]))
                    .lotParentLevel(Util.objectToString(lotWeightDetails[17]))
                    .initialWeighment(Util.objectToFloat(lotWeightDetails[18]))
                    .testDate(Util.objectToString(lotWeightDetails[19]))
                    .noOfCocoonTakenForExamination(Util.objectToLong(lotWeightDetails[20]))
                    .noOfDFLFromFc(Util.objectToLong(lotWeightDetails[21]))
                    .noOfCocoonPerKg(Util.objectToLong(lotWeightDetails[22]))
                    .marketAuctionDate(Util.objectToString(lotWeightDetails[23]))
                    .allottedLotId(Util.objectToInteger(lotWeightDetails[24]))
                    .averageYield(Util.objectToString(lotWeightDetails[25]))
                    .invoiceNumber(Util.objectToString(lotWeightDetails[26]))
                    .lotWeightAfterWeighment(Util.objectToString(lotWeightDetails[27]))
                    .buyerName(Util.objectToString(lotWeightDetails[28]))
                    .sumLotWeightRspNssoGovt(Util.objectToFloat(lotWeightDetails[29]))
                    .sumSoldAmountRspNssoGovt(Util.objectToFloat(lotWeightDetails[30]))
                    .sumLotWeightReeling(Util.objectToFloat(lotWeightDetails[31]))
                    .sumSoldAmountReeling(Util.objectToFloat(lotWeightDetails[32]))
                    .totalLotWeight(Util.objectToFloat(lotWeightDetails[33]))
                    .build();

            responses.add(lotDistributeResponse);
        }

        return responses;
    }

    public List<Integer> getAllottedLotIds(LotStatusSeedMarketRequest request) {
        return lotGroupageRepository.getAllottedLotIds(
                request.getAuctionDate(),
                request.getMarketId(),
                request.getFruitsId()
        );
    }


    public ResponseEntity<?> getReelingLotNumberDetails() {

        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        List<LotDistributeResponse> responseList = new ArrayList<>();

        // ✅ Step 1: Get logged-in user's ID from JWT
        Integer marketId = Util.getMarketId(Util.getTokenValues());

        // ✅ Step 3: Fetch all expiring reeler licenses for that TSC
        List<Object[]> applicableList = lotGroupageRepository.getReelingLotNumberDetails(marketId);

        // ✅ Step 4: Convert to response
        reelerResponses(responseList, applicableList);

        rw.setTotalRecords((long) applicableList.size());
        rw.setContent(responseList);
        return ResponseEntity.ok(rw);
    }

    private static void reelerResponses(List<LotDistributeResponse> lotDistributeResponseList, List<Object[]> applicableList) {
//        int serialNumber = pageNumber * pageSize + 1;
        for (Object[] arr : applicableList) {
            LotDistributeResponse lotDistributeResponse;
            lotDistributeResponse = LotDistributeResponse.builder()
//                    .serialNumber(serialNumber++)
                    .lotGroupageId(Util.objectToLong(arr[0]))
                    .buyerType(Util.objectToString(arr[1]))
                    .buyerId(Util.objectToLong(arr[2]))
                    .lotWeight(Util.objectToFloat(arr[3]))
                    .amount(Util.objectToFloat(arr[4]))
                    .marketFee(Util.objectToFloat(arr[5]))
                    .soldAmount(Util.objectToFloat(arr[6]))
                    .allottedLotId(Util.objectToInteger(arr[7]))
                    .marketAuctionDate(Util.objectToString(arr[10]))
                    .averageYield(Util.objectToString(arr[11]))
                    .noOfDFLs(Util.objectToString(arr[12]))
                    .invoiceNumber(Util.objectToString(arr[13]))
                    .lotParentLevel(Util.objectToString(arr[14]))
                    .remainingCocoonWeight(Util.objectToString(arr[15]))
                    .buyerName(Util.objectToString(arr[19]))
                    .farmerFirstName(Util.objectToString(arr[20]))
                    .marketName(Util.objectToString(arr[21]))
                    .farmerFruitsId(Util.objectToString(arr[22]))
                    .allottedLotId(Util.objectToInteger(arr[24]))
                    .build();
            lotDistributeResponseList.add(lotDistributeResponse);
        }
    }


    public List<LotDistributeResponse> getLotDisposalDetails(LotStatusSeedMarketRequest request) {

        List<Object[]> list = lotGroupageRepository.getLotDisposalDetails(
                request.getFruitsId(),
                request.getFitnessCertificateId()
        );

        List<LotDistributeResponse> responses = new ArrayList<>();
        int serial = 1;

        for (Object[] obj : list) {

            LotDistributeResponse response = LotDistributeResponse.builder()
                    .serialNumber(serial++)
                    .lotNumber(Util.objectToString(obj[0]))
                    .numberOfDflsDisposed(Util.objectToLong(obj[1]))
                    .spunToDate(Util.objectToString(obj[2]))
                    .noOfChandies(Util.objectToString(obj[3]))
                    .expectedCocoon(Util.objectToString(obj[4]))
                    .farmerNameKan(Util.objectToString(obj[5]))
                    .fatherNameKan(Util.objectToString(obj[6]))
                    .villageName(Util.objectToString(obj[7]))
                    .fitnessCertificateId(Util.objectToLong(obj[8]))
                    .tscName(Util.objectToString(obj[9]))
                    .spunFromDate(Util.objectToString(obj[10]))
                    .farmerFruitsId(Util.objectToString(obj[11]))
                    .marketAuctionDate(Util.objectToString(obj[12]))
                    .cropStatusId(Util.objectToLong(obj[13]))
                    .cropStatusName(Util.objectToString(obj[14]))
                    .build();

            responses.add(response);
        }

        return responses;
    }


    public List<LotDistributeBuyerWiseResponse> getDetailsForMarketReceipt(
            LotStatusSeedMarketRequest lotStatusRequest) {

        List<Object[]> results = lotGroupageRepository.getDetailsForMarketReceipt(
                lotStatusRequest.getAuctionDate(),
                lotStatusRequest.getMarketId(),
                lotStatusRequest.getAllottedLotId()
        );

        List<LotDistributeBuyerWiseResponse> responses = new ArrayList<>();
        int serial = 1;

        for (Object[] row : results) {

            if (row.length < 33) {
                throw new RuntimeException("Invalid column count: " + row.length);
            }

            LotDistributeBuyerWiseResponse response = LotDistributeBuyerWiseResponse.builder()
                    .serialNumber(serial++)
                    .farmerNumber(Util.objectToString(row[0]))
                    .farmerFruitsId(Util.objectToString(row[1]))
                    .farmerFullName(Util.objectToString(row[2]))
                    .fatherNameKan(Util.objectToString(row[3]))
                    .districtNameKan(Util.objectToString(row[4]))
                    .talukNameKan(Util.objectToString(row[5]))
                    .farmerVillage(Util.objectToString(row[6]))
                    .marketName(Util.objectToString(row[7]))
                    .race(Util.objectToString(row[8]))
                    .source(Util.objectToString(row[9]))
                    .lotParentLevel(Util.objectToString(row[10]))
                    .allottedLotId(Util.objectToInteger(row[11]))
                    .auctionDate(Util.objectToString(row[12]))
                    .buyerName(Util.objectToString(row[13]))
                    .calculatedAverageYield(Util.objectToString(row[14]))

                    // ✅ RSP / NSSO / Grainage
                    .totalRspNssoGrainageLotWeight(Util.objectToString(row[15]))
                    .totalRspNssoGrainageAmount(Util.objectToString(row[16]))
                    .totalRspNssoGrainageSoldAmount(Util.objectToString(row[17]))
                    .totalRspNssoGrainageMarketFee(Util.objectToString(row[18]))

                    // ✅ Reeling
                    .totalReelingLotWeight(Util.objectToString(row[19]))
                    .totalReelingAmount(Util.objectToString(row[20]))
                    .totalReelingSOldAmount(Util.objectToString(row[21]))
                    .totalReelingMarketFee(Util.objectToString(row[22]))

                    // ✅ Totals
                    .totalLotWeight(Util.objectToString(row[23]))
                    .totalAmount(Util.objectToString(row[24] != null ? row[24] : 0))
                    .totalSoldAmount(Util.objectToString(row[25] != null ? row[25] : 0))
                    .totalMarketFee(Util.objectToString(row[26] != null ? row[26] : 0))
                    .reelerName(Util.objectToString(row[27]))
                    .reelerFatherName(Util.objectToString(row[28]))
                    .reelerDistrict(Util.objectToString(row[29]))
                    .reelerTaluk(Util.objectToString(row[30]))
                    .reelerHobli(Util.objectToString(row[31]))
                    .reelerVillage(Util.objectToString(row[32]))

                    .build();

            responses.add(response);
        }

        return responses;
    }

    private Object getSafe(Object[] row, int index) {
        if (row == null || index < 0 || index >= row.length) {
            return null;
        }
        return row[index];
    }

    public List<LotDistributeBuyerWiseResponse> getSeedCocoonDTRReport(
            LotStatusSeedMarketRequest request) {

        List<Object[]> results = lotGroupageRepository.getDetailsForSeedCocoonSeedMarketReport(
                request.getAuctionDate(),
                request.getAllottedLotId(),
                request.getMarketId()
        );

        List<LotDistributeBuyerWiseResponse> responses = new ArrayList<>();
        int serial = 1;

        for (Object[] row : results) {

            if (row == null) continue;

            System.out.println("Row length = " + row.length);

            LotDistributeBuyerWiseResponse response = LotDistributeBuyerWiseResponse.builder()
                    .serialNumber(serial++)

                    .allottedLotId(Util.objectToInteger(getSafe(row, 0)))

                    .farmerFullName(Util.objectToString(getSafe(row, 1)))
                    .fatherNameKan(Util.objectToString(getSafe(row, 2)))
                    .farmerFruitsId(Util.objectToString(getSafe(row, 3)))
                    .farmerVillage(Util.objectToString(getSafe(row, 4)))

                    .parentalLevel(Util.objectToString(getSafe(row, 5)))
                    .noOfDfls(Util.objectToString(getSafe(row, 6)))
                    .fcIssued(Util.objectToInteger(getSafe(row, 7)))

                    .lotWeight(Util.objectToFloat(getSafe(row, 8)))
                    .estimatedWeight(Util.objectToFloat(getSafe(row, 9)))
                    .marketName(Util.objectToString(getSafe(row, 10)))

                    .cocoonsPerKg(Util.objectToInteger(getSafe(row, 11)))
                    .meltPercentage(Util.objectToFloat(getSafe(row, 12)))
                    .totalQuantity(Util.objectToFloat(getSafe(row, 13)))

                    .rspQty(Util.objectToFloat(getSafe(row, 14)))
                    .nssoQty(Util.objectToFloat(getSafe(row, 15)))
                    .govtGrainageQty(Util.objectToFloat(getSafe(row, 16)))
                    .reelingQty(Util.objectToFloat(getSafe(row, 17)))

                    .rspName(Util.objectToString(getSafe(row, 18)))
                    .nssoName(Util.objectToString(getSafe(row, 19)))
                    .govtGrainageName(Util.objectToString(getSafe(row, 20)))
                    .reelingName(Util.objectToString(getSafe(row, 21)))

                    .auctionDate(Util.objectToString(getSafe(row, 22)))
                    .remainingCocoon(Util.objectToFloat(getSafe(row, 23)))

                    // ✅ FIXED (correct mapping)
                    .spunFromDate(Util.objectToString(getSafe(row, 24)))   // spun_date
                    .spunToDate(Util.objectToString(getSafe(row, 25)))     // expected_marker_date

                    .marketFee(Util.objectToFloat(getSafe(row, 26)))
                    .ratePerKg(Util.objectToFloat(getSafe(row, 27)))
                    .soldAmount(Util.objectToFloat(getSafe(row, 28)))

                    .build();

            responses.add(response);
        }

        return responses;
    }

    public List<Map<String, Object>> getLotDetails(LocalDate date, int lotNo) {

        // ✅ ADD THIS VALIDATION
        List<Lot> lots = lotRepository.findByAllottedLotIdAndAuctionDateActiveTrue(lotNo, date);

        if (lots == null || lots.isEmpty()) {
            throw new RuntimeException("Lot not found");
        }

        // Then fetch details
        return lotGroupageRepository.getLotDetails(date, lotNo);
    }
    @Transactional
    public String deleteLot(int lotId, LocalDate date) {

        BigInteger id = BigInteger.valueOf(lotId);
        // ❌ Block delete if distributed or payment done
        List<String> blockedStatuses = List.of(
                LotStatus.DISTRIBUTED.getLabel(),
                LotStatus.PAYMENTFAILED.getLabel()
        );
        int count = lotGroupageRepository.countByLotIdAndStatusIn(id,blockedStatuses);
        if (count > 0) {
            throw new RuntimeException(
                    "You cannot delete this lot as payment already processed"
            );
        }

        Lot lot = lotRepository.findByIdAndAuctionDateAndActiveTrue(id, date)
                .orElseThrow(() -> new RuntimeException("Lot not found"));

        // ✅ Step 3: SAVE AUDIT BEFORE DELETE
        LotDeleteAudit audit = new LotDeleteAudit();
        audit.setLotId(lot.getId());
        audit.setAllottedLotId(lot.getAllottedLotId());
        audit.setAuctionDate(lot.getAuctionDate());
        audit.setMarketAuctionId(lot.getMarketAuctionId());


        lotDeleteAuditRepository.save(audit);

        //  Make Lot inactive
        lot.setActive(false);
        lotRepository.save(lot);

        //  Make LotGroupage inactive
        lotGroupageRepository.updateActiveByLotId(id);

        //  Update MARKET_AUCTION (direct from LOT)
        if (lot.getMarketAuctionId() != null) {
            marketAuctionRepository.updateActiveById(lot.getMarketAuctionId());
        }

        return "Lot deleted successfully";
    }

    public FileInputStream downloadExternalUnitBalance(Long marketId) throws Exception {

        List<Object[]> list = lotGroupageRepository.getExternalUnitBalanceByMarket(marketId);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("External Unit Balance");

        // Header
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Name");
        header.createCell(1).setCellValue("License No");
        header.createCell(2).setCellValue("Virtual Account Number");
        header.createCell(3).setCellValue("Current Balance");
        header.createCell(4).setCellValue("Updated Date & Time");

        int rowNum = 1;

        for (Object[] data : list) {

            Row row = sheet.createRow(rowNum++);

            // Safe mapping
            String name = data[0] != null ? data[0].toString() : "";
            String licenseNo = data[1] != null ? data[1].toString() : "";
            String account = data[2] != null ? data[2].toString() : "";

            double balance = 0;
            if (data[3] != null) {
                try {
                    balance = Double.parseDouble(data[3].toString());
                } catch (Exception e) {
                    balance = 0;
                }
            }

            String date = data[4] != null ? data[4].toString() : "";

            row.createCell(0).setCellValue(name);
            row.createCell(1).setCellValue(licenseNo);
            row.createCell(2).setCellValue(account);
            row.createCell(3).setCellValue(balance);
            row.createCell(4).setCellValue(date);
        }

        // Create temp file
        File file = File.createTempFile("External_Unit_Balance_", ".xlsx");

        try (FileOutputStream fos = new FileOutputStream(file)) {
            workbook.write(fos);
        }

        workbook.close();

        return new FileInputStream(file);
    }

    public FileInputStream downloadReelerBalance(Long marketId) throws Exception {

        List<Object[]> list = lotGroupageRepository.getReelerBalance(marketId);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Reeler Balance");

        // Header
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Reeler Name");
        header.createCell(1).setCellValue("Virtual Account Number");
        header.createCell(2).setCellValue("Current Balance");
        header.createCell(3).setCellValue("Minimum Balance");
        header.createCell(4).setCellValue("Updated Date & Time");

        int rowNum = 1;

        for (Object[] data : list) {

            Row row = sheet.createRow(rowNum++);

            String reelerName = data[0] != null ? data[0].toString() : "";
            String account = data[1] != null ? data[1].toString() : "";

            Double balance = 0.0;
            if (data[2] != null) {
                try {
                    balance = Double.parseDouble(data[2].toString());
                } catch (Exception e) {
                    balance = 0.0;
                }
            }

            Double minBalance = 0.0;
            if (data[3] != null) {
                try {
                    minBalance = Double.parseDouble(data[3].toString());
                } catch (Exception e) {
                    minBalance = 0.0;
                }
            }

            String date = data[4] != null ? data[4].toString() : "";

            row.createCell(0).setCellValue(reelerName);
            row.createCell(1).setCellValue(account);
            row.createCell(2).setCellValue(balance);
            row.createCell(3).setCellValue(minBalance);
            row.createCell(4).setCellValue(date);
        }

        File file = File.createTempFile("Reeler_Balance_", ".xlsx");
        FileOutputStream fos = new FileOutputStream(file);

        workbook.write(fos);
        workbook.close();
        fos.close();

        return new FileInputStream(file);
    }

    public List<Map<String, Object>> getExternalUnitBalanceData(Long marketId) {

        List<Object[]> list = lotGroupageRepository.getExternalUnitBalanceByMarket(marketId);

        List<Map<String, Object>> response = new ArrayList<>();

        for (Object[] data : list) {

            Map<String, Object> map = new HashMap<>();

            map.put("name", data[0]);
            map.put("licenseNo", data[1]);
            map.put("virtualAccountNumber", data[2]);
            map.put("currentBalance", data[3]);
            map.put("updatedDate", data[4]);

            response.add(map);
        }

        return response;
    }

    public List<Map<String, Object>> getReelerBalanceData(Long marketId) {

        List<Object[]> list = lotGroupageRepository.getReelerBalance(marketId);

        List<Map<String, Object>> response = new ArrayList<>();

        for (Object[] data : list) {

            Map<String, Object> map = new HashMap<>();

            map.put("name", data[0]);
            map.put("virtualAccountNumber", data[1]);
            map.put("currentBalance", data[2]);
            map.put("minimumBalance", data[3]);
            map.put("updatedDate", data[4]);

            response.add(map);
        }

        return response;
    }
}
