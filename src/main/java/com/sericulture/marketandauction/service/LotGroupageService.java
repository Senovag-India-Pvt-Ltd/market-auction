package com.sericulture.marketandauction.service;

import com.sericulture.marketandauction.helper.MarketAuctionHelper;
import com.sericulture.marketandauction.helper.Util;
import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.api.ReelerTransactionReport;
import com.sericulture.marketandauction.model.api.ReelerTransactionReportWrapper;
import com.sericulture.marketandauction.model.api.marketauction.*;
import com.sericulture.marketandauction.model.api.marketauction.reporting.ReelerReportRequest;
import com.sericulture.marketandauction.model.api.marketauction.reporting.ReelerTxnReportRequest;
import com.sericulture.marketandauction.model.api.marketauction.reporting.ReportRequest;
import com.sericulture.marketandauction.model.api.marketauction.reporting.SeedMarketBiddingResponse;
import com.sericulture.marketandauction.model.api.marketauction.MarketFeeGovtTransferRequest;
import com.sericulture.marketandauction.model.api.marketauction.MarketFeeGovtTransferResponse;
import com.sericulture.marketandauction.model.entity.*;
import com.sericulture.marketandauction.model.entity.TransactionFileGenQueue;
import com.sericulture.marketandauction.model.enums.LotStatus;
import com.sericulture.marketandauction.model.enums.PAYMENTMODE;
import com.sericulture.marketandauction.repository.*;
import com.sericulture.marketandauction.model.exceptions.ValidationException;
import com.sericulture.marketandauction.model.mapper.Mapper;
import jakarta.persistence.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.io.FileInputStream;
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
    MarketFeeDebitRepository marketFeeDebitRepository;

    @Autowired
    TransactionFileGenQueueRepository transactionFileGenQueueRepository;

    @Autowired
    MarketAuctionFileDowndloadService marketAuctionFileDowndloadService;

    @Autowired
    MarketFeeGovtTransferRepository marketFeeGovtTransferRepository;

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
                        marketFee = soldAmount.multiply(BigDecimal.valueOf(0.01));
                        break;
                    case "Reeling":
                        marketFee = soldAmount.multiply(BigDecimal.valueOf(0.01));
                        break;
                    default:
                        break;
                }

                double mf = marketFee.setScale(2, RoundingMode.HALF_UP).doubleValue();
                lotGroupage.setMarketFee(mf);
                lotGroupage.setFarmerMarketFee(mf);
                lotGroupage.setReelerMarketFee("Reeling".equals(lotGroupageRequest.getBuyerType()) ? mf : null);
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

            // Persist "Purpose for Rejection" flag explicitly — ModelMapper STRICT
            // mode can silently skip it depending on configuration, so do it directly.
            Boolean purposeForRejection = Boolean.TRUE.equals(lotGroupageRequest.getPurposeForRejection());
            lotGroupage.setPurposeForRejection(purposeForRejection);

            // Persist "Moving to another market" flag + reason explicitly for the same reason.
            Boolean movingToAnotherMarket = Boolean.TRUE.equals(lotGroupageRequest.getMovingToAnotherMarket());
            lotGroupage.setMovingToAnotherMarket(movingToAnotherMarket);
            lotGroupage.setMovingMarketReason(movingToAnotherMarket ? lotGroupageRequest.getMovingMarketReason() : null);

            Float remainingCocoon = lotGroupageRequest.getRemainingCocoonWeight();

            // When the user marks the lot for rejection:
            //   1. Snapshot the leftover (e.g. 100-90 = 10) into rejection_quantity.
            //   2. Zero out remaining_cocoon and mark status = DISTRIBUTED below.
            if (purposeForRejection) {
                BigDecimal rejectionQty = (remainingCocoon != null)
                        ? BigDecimal.valueOf(remainingCocoon).setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                lotGroupage.setRejectionQuantity(rejectionQty);

                remainingCocoon = 0f;
                lotGroupage.setRemainingCocoonWeight(0f);
            } else {
                // Checkbox off → don't carry over a stale rejection quantity.
                lotGroupage.setRejectionQuantity(null);
            }

            // Moving-to-another-market also force-completes the lot: zero out remaining and
            // treat as DISTRIBUTED. Run AFTER rejection branch so both flags can coexist safely.
            if (movingToAnotherMarket) {
                remainingCocoon = 0f;
                lotGroupage.setRemainingCocoonWeight(0f);
            }

            LotStatus status;
            if (movingToAnotherMarket) {
                status = LotStatus.DISTRIBUTED;
            } else if (purposeForRejection) {
                status = LotStatus.DISTRIBUTED;
            } else if (remainingCocoon == null) {
                status = LotStatus.REJECTED;
            } else if (remainingCocoon == 0) {
                status = LotStatus.DISTRIBUTED;
            } else {
                status = LotStatus.REJECTED;
            }
            lotGroupage.setStatus(status.getLabel());

            // Update SaleAndDisposalOfDfls only if remainingCocoon = null or 0.
            // markDisposalEntry handles the multi-row case (a single fruitsId + lotNumber can map
            // to more than one disposal record) using the saleDisposalId the user picked on the UI.
            if (remainingCocoon == null || remainingCocoon == 0) {
                markDisposalEntry(
                        lotGroupageRequest.getFruitsId(),
                        lotGroupageRequest.getLotParentLevel(),
                        lotGroupageRequest.getSaleDisposalId()
                );
            }

            String buyerType = lotGroupageRequest.getBuyerType();
            if ("Govt Grainage".equals(buyerType)) {
                lotGroupage.setIsMarketPaid(0);
            } else if ("Reeling".equals(buyerType) || "RSP".equals(buyerType) || "NSSO".equals(buyerType)) {
                lotGroupage.setIsMarketPaid(1);
            }

            // Save LotGroupage
            lotGroupage = lotGroupageRepository.save(lotGroupage);

            // Set CRN using the generated lotGroupageId so each row gets a unique CRN
            String crn = Util.getCRN(Util.getISTLocalDate(), lotGroupageRequest.getMarketId(), lotGroupage.getLotGroupageId().intValue());
            lotGroupageRepository.updateCustomerReferenceNumber(lotGroupage.getLotGroupageId(), crn);

            // Debit buyer's virtual account (ONLINE mode only) — inserting into REELER_VID_DEBIT_TXN
            // automatically updates REELER_VID_CURRENT_BALANCE (it is a view computed from that table)
            if (marketMaster != null && PAYMENTMODE.ONLINE.getLabel().equalsIgnoreCase(marketMaster.getPaymentMode())
                    && buyerVirtualAccount != null && lotGroupageRequest.getSoldAmount() != null) {
                double soldAmount = lotGroupageRequest.getSoldAmount().doubleValue();
                double marketFee = lotGroupage.getMarketFee() != null ? lotGroupage.getMarketFee() : 0.0;
                boolean isReeling = "Reeling".equals(lotGroupageRequest.getBuyerType());
                double totalDebitAmount = Math.round(isReeling ? soldAmount + marketFee : soldAmount);
                int buyerId = "RSP".equals(lotGroupageRequest.getBuyerType())
                        ? (lotGroupageRequest.getExternalUnitId() != null ? lotGroupageRequest.getExternalUnitId().intValue() : 0)
                        : (lotGroupageRequest.getBuyerId() != null ? lotGroupageRequest.getBuyerId().intValue() : 0);
                ReelerVidDebitTxn debitTxn = new ReelerVidDebitTxn(
                        lotGroupageRequest.getAllottedLotId().intValue(),
                        lotGroupageRequest.getMarketId(),
                        lotGroupageRequest.getAuctionDate(),
                        buyerId,
                        buyerVirtualAccount,
                        totalDebitAmount
                );
                reelerVidDebitTxnRepository.save(debitTxn);

            }

            // NOTE: Govt Grainage market fee is NOT auto-debited here.
            // isMarketPaid = 0 is set above so the lot appears in the
            // "Market Fee Collection" screen. The debit happens only when
            // the market officer clicks Submit on that screen, which calls
            // markLotAsMarketPaid().

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
                ROUND((l.LOT_WEIGHT_AFTER_WEIGHMENT * 100) / NULLIF(ma.dfl_lot_number, 0), 2) AS calculatedAverageYield,
                ROUND(lg.remaining_cocoon, 2) AS remaining_cocoon,
                SUM(lg.lot_weight) OVER (PARTITION BY l.lot_id) AS soldCocoonInKgs,
                ROUND(l.LOT_WEIGHT_AFTER_WEIGHMENT, 2) AS LOT_WEIGHT_AFTER_WEIGHMENT,
                CASE
                    WHEN lg.buyer_type = 'RSP' THEN es.license_number
                    WHEN lg.buyer_type = 'NSSO' THEN es.address
                    WHEN lg.buyer_type = 'Govt Grainage' THEN gm.grainage_master_name
                    WHEN lg.buyer_type = 'Reeling' THEN r.name
                    ELSE NULL
                END AS buyer_name,
                lg.purpose_for_rejection,
                lg.rejection_quantity,
                lg.moving_to_another_market,
                lg.moving_market_reason
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
                    // MySQL TINYINT(1) is returned as Boolean by the JDBC driver, but other
                    // numeric columns come back as Number — accept either shape.
                    .purposeForRejection(toBoolean(lotWeightDetails[45]))
                    .rejectionQuantity(lotWeightDetails[46] != null
                            ? (lotWeightDetails[46] instanceof BigDecimal
                                ? (BigDecimal) lotWeightDetails[46]
                                : BigDecimal.valueOf(((Number) lotWeightDetails[46]).doubleValue()))
                            : null)
                    .movingToAnotherMarket(toBoolean(lotWeightDetails[47]))
                    .movingMarketReason(Util.objectToString(lotWeightDetails[48]))
                    .build();
            responses.add(lotDistributeResponse);
        }

        return responses;
    }

    private static boolean toBoolean(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).intValue() != 0;
        return Boolean.parseBoolean(value.toString());
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
            Double existingFarmerMarketFee = lotGroupage.getFarmerMarketFee();
            Double existingReelerMarketFee = lotGroupage.getReelerMarketFee();
            Integer existingIsMarketPaid = lotGroupage.getIsMarketPaid();

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

            // Restore marketFee and isMarketPaid for existing records; new records calculate below
            if (lotGroupageRequestEdit.getLotGroupageId() != null) {
                lotGroupage.setMarketFee(existingMarketFee);
                lotGroupage.setFarmerMarketFee(existingFarmerMarketFee);
                lotGroupage.setReelerMarketFee(existingReelerMarketFee);
                lotGroupage.setIsMarketPaid(existingIsMarketPaid);
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
                    String editBuyerType = lotGroupageRequestEdit.getBuyerType();
                    boolean isRspOrNsso = "RSP".equals(editBuyerType) || "NSSO".equals(editBuyerType);
                    int newBuyerIntId = isRspOrNsso
                            ? (lotGroupageRequestEdit.getExternalUnitId() != null ? lotGroupageRequestEdit.getExternalUnitId().intValue() : 0)
                            : (lotGroupageRequestEdit.getBuyerId() != null ? lotGroupageRequestEdit.getBuyerId().intValue() : 0);
                    boolean isReeling = "Reeling".equals(editBuyerType);
                    double marketFee = lotGroupage.getMarketFee() != null ? lotGroupage.getMarketFee() : 0.0;
                    double totalDebitDifference = Math.round(isReeling ? debitDifference + marketFee : (double) debitDifference);
                    ReelerVidDebitTxn editDebitTxn = new ReelerVidDebitTxn(
                            lotGroupageRequestEdit.getAllottedLotId().intValue(),
                            lotGroupageRequestEdit.getMarketId(),
                            lotGroupageRequestEdit.getAuctionDate(),
                            newBuyerIntId,
                            newVirtualAccount,
                            totalDebitDifference
                    );
                    reelerVidDebitTxnRepository.save(editDebitTxn);

                }
            }

            if (lotGroupageRequestEdit.getLotGroupageId() == null) {
                String editBuyerType = lotGroupageRequestEdit.getBuyerType();
                if ("Govt Grainage".equals(editBuyerType)) {
                    lotGroupage.setIsMarketPaid(0);
                } else if ("Reeling".equals(editBuyerType) || "RSP".equals(editBuyerType) || "NSSO".equals(editBuyerType)) {
                    lotGroupage.setIsMarketPaid(1);
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
                        marketFee = soldAmount.multiply(BigDecimal.valueOf(0.01));
                        break;
                    case "Reeling":
                        marketFee = soldAmount.multiply(BigDecimal.valueOf(0.01));
                        break;
                    default:
                        break;
                }

                double editMf = marketFee.setScale(2, RoundingMode.HALF_UP).doubleValue();
                lotGroupage.setMarketFee(editMf);
                lotGroupage.setFarmerMarketFee(editMf);
                lotGroupage.setReelerMarketFee("Reeling".equals(lotGroupageRequestEdit.getBuyerType()) ? editMf : null);
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

            // Persist "Purpose for Rejection" explicitly (mirrors save-path behavior).
            Boolean editPurposeForRejection = Boolean.TRUE.equals(lotGroupageRequestEdit.getPurposeForRejection());
            lotGroupage.setPurposeForRejection(editPurposeForRejection);

            // Persist "Moving to another market" explicitly (mirrors save-path behavior).
            Boolean editMovingToAnotherMarket = Boolean.TRUE.equals(lotGroupageRequestEdit.getMovingToAnotherMarket());
            lotGroupage.setMovingToAnotherMarket(editMovingToAnotherMarket);
            lotGroupage.setMovingMarketReason(editMovingToAnotherMarket ? lotGroupageRequestEdit.getMovingMarketReason() : null);

            Float remainingCocoon = lotGroupageRequestEdit.getRemainingCocoonWeight();

            // Rejection check-box force-completes the lot:
            //   1. Snapshot leftover into rejection_quantity.
            //   2. Zero remaining_cocoon + status = DISTRIBUTED (below).
            if (editPurposeForRejection) {
                BigDecimal editRejectionQty = (remainingCocoon != null)
                        ? BigDecimal.valueOf(remainingCocoon).setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                lotGroupage.setRejectionQuantity(editRejectionQty);

                remainingCocoon = 0f;
                lotGroupage.setRemainingCocoonWeight(0f);
            } else {
                lotGroupage.setRejectionQuantity(null);
            }

            // Moving-to-another-market also force-completes the lot.
            if (editMovingToAnotherMarket) {
                remainingCocoon = 0f;
                lotGroupage.setRemainingCocoonWeight(0f);
            }

            LotStatus editStatus;
            if (editMovingToAnotherMarket) {
                editStatus = LotStatus.DISTRIBUTED;
            } else if (editPurposeForRejection) {
                editStatus = LotStatus.DISTRIBUTED;
            } else if (remainingCocoon == null) {
                editStatus = LotStatus.REJECTED;
            } else if (remainingCocoon == 0) {
                editStatus = LotStatus.DISTRIBUTED;
            } else {
                editStatus = LotStatus.REJECTED;
            }
            lotGroupage.setStatus(editStatus.getLabel());

            // Update SaleAndDisposalOfDfls only if remainingCocoon = null or 0.
            // markDisposalEntry handles the multi-row case (a single fruitsId + lotNumber can map
            // to more than one disposal record) using the saleDisposalId the user picked on the UI.
            if (remainingCocoon == null || remainingCocoon == 0) {
                markDisposalEntry(
                        lotGroupageRequestEdit.getFruitsId(),
                        lotGroupageRequestEdit.getLotParentLevel(),
                        lotGroupageRequestEdit.getSaleDisposalId()
                );
            }

            // Save LotGroupage
            lotGroupage = lotGroupageRepository.save(lotGroupage);

            // Set CRN for newly created records (remaining lot distribution path)
            if (lotGroupageRequestEdit.getLotGroupageId() == null) {
                String crn = Util.getCRN(Util.getISTLocalDate(), lotGroupageRequestEdit.getMarketId(), lotGroupage.getLotGroupageId().intValue());
                lotGroupageRepository.updateCustomerReferenceNumber(lotGroupage.getLotGroupageId(), crn);
            }

            // Map the saved lotGroupage to the response object
            LotGroupageResponse singleResponse = mapper.lotGroupageEntityToObject(lotGroupage, LotGroupageResponse.class);
            singleResponse.setError(false);
            responses.add(singleResponse);  // Collect each response
        }

        return responses; // Return the list of responses
    }

    /**
     * Marks the matching sale_and_disposal_of_dfls row as disposed (is_disposed = 1).
     *
     * A single fruitsId + lotNumber can map to MORE THAN ONE disposal row. The old single-result
     * finder threw IncorrectResultSizeDataAccessException in that case. This method instead:
     *   - uses the user-selected {@code saleDisposalId} when provided (the UI sends it after the
     *     user picks one of the candidates returned by {@link #getSaleDisposalCandidates}),
     *   - auto-marks the row when exactly one matches,
     *   - raises a clear ValidationException (HTTP 400, not a 500) when several match and no
     *     selection was sent, so the UI can show the options and let the user choose.
     */
    private void markDisposalEntry(String fruitsId, String lotNumber, Integer saleDisposalId) {
        // Explicit user selection wins — mark exactly the chosen row.
        if (saleDisposalId != null) {
            SaleAndDisposalOfDfls chosen = saleAndDisposalOfDflsRepository
                    .findByIdAndActive(saleDisposalId, true)
                    .orElseThrow(() -> new ValidationException(
                            "Selected disposal record (saleDisposalId " + saleDisposalId + ") was not found."));
            chosen.setIsDisposed(1);
            saleAndDisposalOfDflsRepository.save(chosen);
            return;
        }

        // No explicit selection — look up all matching rows by fruitsId + lotNumber. The List finder
        // avoids the non-unique-result crash when more than one row exists.
        List<SaleAndDisposalOfDfls> entries = saleAndDisposalOfDflsRepository
                .findAllByFruitsIdAndLotNumberAndIsVerifiedAndActive(
                        fruitsId, lotNumber, 1, true);

        if (entries == null || entries.isEmpty()) {
            return; // nothing to mark
        }

        if (entries.size() == 1) {
            SaleAndDisposalOfDfls entry = entries.get(0);
            entry.setIsDisposed(1);
            saleAndDisposalOfDflsRepository.save(entry);
            return;
        }

        // Multiple matches and no saleDisposalId chosen — ambiguous.
        // The UI must call getSaleDisposalCandidates, let the user pick, then resend with saleDisposalId.
        throw new ValidationException(
                "Multiple disposal records (" + entries.size() + ") found for fruitsId " + fruitsId
                        + " and lot number " + lotNumber
                        + ". Please select which disposal record to mark as disposed.");
    }

    /**
     * Returns every disposal row matching a fruitsId + lotNumber so the UI can display them and let
     * the user pick which one to mark as disposed. Used to resolve the ambiguity that
     * {@link #markDisposalEntry} reports. Keyed on fruitsId + lotNumber only (see repository note).
     */
    public List<SaleDisposalCandidateResponse> getSaleDisposalCandidates(SaleDisposalLookupRequest request) {
        List<SaleAndDisposalOfDfls> entries = saleAndDisposalOfDflsRepository
                .findAllByFruitsIdAndLotNumberAndIsVerifiedAndActive(
                        request.getFruitsId(),
                        request.getLotParentLevel(),
                        1,
                        true);

        List<SaleDisposalCandidateResponse> candidates = new ArrayList<>();
        if (entries == null) {
            return candidates;
        }
        for (SaleAndDisposalOfDfls entry : entries) {
            candidates.add(SaleDisposalCandidateResponse.builder()
                    .saleDisposalId(entry.getId())
                    .fruitsId(entry.getFruitsId())
                    .lotNumber(entry.getLotNumber())
                    .numberOfDflsDisposed(entry.getNumberOfDflsDisposed())
                    .eggSheetNumbers(entry.getEggSheetNumbers())
                    .raceId(entry.getRaceId())
                    .releaseDate(entry.getReleaseDate())
                    .dateOfDisposal(entry.getDateOfDisposal())
                    .nameAndAddressOfTheFarm(entry.getNameAndAddressOfTheFarm())
                    .invoiceNumber(entry.getInvoiceNumber())
                    .receiptNo(entry.getReceiptNo())
                    .isDisposed(entry.getIsDisposed())
                    .build());
        }
        return candidates;
    }

    public ResponseEntity<?> validateReelerBalanceForLotGroupage(LotGroupageDetailsRequest lotGroupageDetailsRequest) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);

        if (lotGroupageDetailsRequest.getLotGroupageRequests() == null || lotGroupageDetailsRequest.getLotGroupageRequests().isEmpty()) {
            return marketAuctionHelper.retrunIfError(rw, "Lot groupage requests are null or empty.");
        }

        for (LotGroupageRequest lotGroupageRequest : lotGroupageDetailsRequest.getLotGroupageRequests()) {
            String buyerType = lotGroupageRequest.getBuyerType();

            // Balance check only applies to Reeling, RSP, and NSSO buyer types
            if (!"Reeling".equals(buyerType) && !"RSP".equals(buyerType) && !"NSSO".equals(buyerType)) {
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
            long previousSoldAmount = lotGroupageRequest.getPreviousSoldAmount() != null ? lotGroupageRequest.getPreviousSoldAmount() : 0L;
            long amountToDebit = soldAmount - previousSoldAmount;
            if (amountToDebit > 0 && currentBalance < amountToDebit) {
                return marketAuctionHelper.retrunIfError(rw,
                        "Reeler/RSP current balance is not enough and needs " + (amountToDebit - currentBalance) + " more money");
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
        else if (("RSP".equals(buyerType) || "NSSO".equals(buyerType)) && externalUnitId != null) {
            return lotGroupageRepository
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
        } else if (("RSP".equals(buyerType) || "NSSO".equals(buyerType)) && externalUnitId != null) {
            Object[][] result = lotGroupageRepository.getExternalUnitVirtualAccountBalance(externalUnitId, marketId);
            if (result != null && result.length > 0 && result[0][1] != null) {
                return Util.objectToFloat(result[0][1]);
            }
        }
        // Govt Grainage — no balance check, return 0 (validation skipped)
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
                    .transactionDate(Util.objectToString(lotWeightDetails[46]))
                    .auctionDate(Util.objectToDate(lotWeightDetails[47]))

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
                    .raceName(Util.objectToString(obj[15]))
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

    public List<Map<String, Object>> getGovtGrainageUnpaidLots(LotStatusSeedMarketRequest request) {
        List<Object[]> rows = lotGroupageRepository.getGovtGrainageUnpaidLotsByFruitsId(
                request.getFruitsId(), request.getMarketId());

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> map = new HashMap<>();
            map.put("lotGroupageId",   row[0]);  // needed for markLotAsMarketPaid
            map.put("allottedLotId",   row[1]);  // Lot No
            map.put("auctionDate",     row[2]);
            map.put("lotAmount",       row[3]);  // sold_amount
            map.put("marketFeeAmount", row[4]);  // farmer_market_fee
            map.put("isMarketPaid",    row[5]);
            map.put("fruitsId",        row[6]);
            map.put("farmerName",      row[7]);  // Farmer Name
            map.put("currentBalance",  row[8]);  // Current Balance
            map.put("pendingAmount",   row[9]);  // Pending Amount
            result.add(map);
        }
        return result;
    }

    @Transactional
    public Map<String, Object> markLotAsMarketPaid(Long lotGroupageId, int marketId) {
        LotGroupage lotGroupage = lotGroupageRepository
                .findByLotGroupageIdAndActiveIn(lotGroupageId, Set.of(true))
                .orElseThrow(() -> new RuntimeException("Lot groupage not found for id: " + lotGroupageId));

        // Guard: already paid — don't debit again
        if (Integer.valueOf(1).equals(lotGroupage.getIsMarketPaid())) {
            throw new RuntimeException("Market fee already paid for lot groupage id: " + lotGroupageId);
        }

        // Guard: farmerMarketFee must be set
        if (lotGroupage.getFarmerMarketFee() == null || lotGroupage.getFarmerMarketFee() <= 0) {
            throw new RuntimeException("No market fee to collect for lot groupage id: " + lotGroupageId);
        }

        String farmerVirtualAccount = lotGroupageRepository.getFarmerVirtualAccount(
                lotGroupage.getFruitsId(), marketId);

        if (farmerVirtualAccount == null) {
            throw new RuntimeException("Farmer has not linked a virtual account for this market. " +
                    "Please ask the farmer to deposit funds before collecting market fee.");
        }

        // Guard: farmer must have sufficient balance
        List<Object[]> farmerBalanceList = lotGroupageRepository.getSeedMarketCurrentBalance(farmerVirtualAccount);
        if (farmerBalanceList == null || farmerBalanceList.isEmpty() || farmerBalanceList.get(0)[0] == null) {
            throw new RuntimeException("Farmer's virtual account has no balance. Please ask the farmer to deposit funds.");
        }

        // Capture previous balance BEFORE debit
        BigDecimal previousBalance = (BigDecimal) farmerBalanceList.get(0)[0];
        BigDecimal debitAmount     = BigDecimal.valueOf(lotGroupage.getFarmerMarketFee());

        if (previousBalance.compareTo(debitAmount) < 0) {
            throw new RuntimeException(
                    String.format("Insufficient balance. Required: %.2f, Available: %.2f",
                            debitAmount, previousBalance));
        }

        Long farmerId = lotGroupageRepository.getFarmerIdByFruitsId(lotGroupage.getFruitsId());

        // Step 1: Debit the farmer's virtual account (updates REELER_VID_CURRENT_BALANCE view)
        reelerVidDebitTxnRepository.save(new ReelerVidDebitTxn(
                lotGroupage.getAllottedLotId().intValue(),
                marketId,
                lotGroupage.getAuctionDate(),
                farmerId != null ? farmerId.intValue() : 0,
                farmerVirtualAccount,
                lotGroupage.getFarmerMarketFee()
        ));

        // Step 2: Insert into market_fee_debit (new audit table per requirement)
        BigDecimal currentBalanceAfterDebit = previousBalance.subtract(debitAmount);

        MarketFeeDebit marketFeeDebit = new MarketFeeDebit();
        marketFeeDebit.setFruitsId(lotGroupage.getFruitsId());
        marketFeeDebit.setLotId(lotGroupage.getAllottedLotId());
        marketFeeDebit.setFarmerId(farmerId);
        marketFeeDebit.setDebitAmount(debitAmount);
        marketFeeDebit.setPreviousBalance(previousBalance);
        marketFeeDebit.setCurrentBalance(currentBalanceAfterDebit);
        // created_by, created_date, active → auto handled by BaseEntity
        marketFeeDebitRepository.save(marketFeeDebit);

        // Step 3: Update lot_groupage ismarketpaid = 1
        lotGroupageRepository.markAsMarketPaid(lotGroupageId);

        Map<String, Object> response = new HashMap<>();
        response.put("lotGroupageId", lotGroupageId);
        response.put("isMarketPaid", 1);
        response.put("previousBalance", previousBalance);
        response.put("currentBalance", currentBalanceAfterDebit);
        response.put("debitAmount", debitAmount);
        response.put("message", "Market fee collected successfully");
        return response;
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

    public ResponseEntity<?> getSeedMFReport(ReportRequest reportRequest) {

        List<SeedMFResponse> responseList = new ArrayList<>();
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);

        List<Object[]> resultSet;

        // 🔥 IMPORTANT LOGIC (LIKE REELER MF)
        if (reportRequest.getLicenseNumber() == null || reportRequest.getLicenseNumber().trim().isEmpty()) {

            // 👉 WITHOUT LICENSE
            resultSet = lotGroupageRepository.getSeedMFReportWithoutLicense(
                    reportRequest.getFromDate(),
                    reportRequest.getToDate(),
                    reportRequest.getMarketId()
            );

        } else {

            // 👉 WITH LICENSE
            resultSet = lotGroupageRepository.getSeedMFReportWithLicense(
                    reportRequest.getFromDate(),
                    reportRequest.getToDate(),
                    reportRequest.getMarketId(),
                    reportRequest.getLicenseNumber().trim()
            );
        }

        // ❌ NO DATA
        if (Util.isNullOrEmptyList(resultSet)) {
            marketAuctionHelper.retrunIfError(rw, "No data found");
            return ResponseEntity.ok(rw);
        }

        // 🔥 MAPPING
        for (Object[] row : resultSet) {

            SeedMFResponse response = SeedMFResponse.builder()
                    .allottedLotId(Util.objectToInteger(row[0]))
                    .auctionDate(Util.objectToString(row[1]))

                    .totalWeight(Util.objectToFloat(row[2]))
                    .bidAmount(Util.objectToFloat(row[3]))

                    .totalSoldAmount(Util.objectToFloat(row[4]))
                    .totalMarketFee(Util.objectToFloat(row[5]))

                    .licenseNumber(Util.objectToString(row[6]))
                    .buyerName(Util.objectToString(row[7]))

                    .serialNumber(Util.objectToInteger(row[8]))
                    .build();

            responseList.add(response);
        }

        rw.setContent(responseList);
        return ResponseEntity.ok(rw);
    }

    public FileInputStream downloadSeedMFReport(ReportRequest request) throws Exception {

        List<Object[]> list;

        // SAME LOGIC AS YOUR GET METHOD
        if (request.getLicenseNumber() == null || request.getLicenseNumber().trim().isEmpty()) {

            list = lotGroupageRepository.getSeedMFReportWithoutLicense(
                    request.getFromDate(),
                    request.getToDate(),
                    request.getMarketId()
            );

        } else {

            list = lotGroupageRepository.getSeedMFReportWithLicense(
                    request.getFromDate(),
                    request.getToDate(),
                    request.getMarketId(),
                    request.getLicenseNumber().trim()
            );
        }

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Seed MF Report");

        // HEADER
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("SL No");
        header.createCell(1).setCellValue("Lot No");
        header.createCell(2).setCellValue("Date");
        header.createCell(3).setCellValue("License");
        header.createCell(4).setCellValue("Buyer Name");
        header.createCell(5).setCellValue("Bid Amount");
        header.createCell(6).setCellValue("Weight");
        header.createCell(7).setCellValue("Amount");
        header.createCell(8).setCellValue("MF Amount");

        int rowNum = 1;

        for (Object[] data : list) {

            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(rowNum - 1);
            row.createCell(1).setCellValue(data[0] != null ? data[0].toString() : "");
            row.createCell(2).setCellValue(data[1] != null ? data[1].toString() : "");
            row.createCell(3).setCellValue(data[6] != null ? data[6].toString() : "");
            row.createCell(4).setCellValue(data[7] != null ? data[7].toString() : "");

            row.createCell(5).setCellValue(data[3] != null ? Double.parseDouble(data[3].toString()) : 0);
            row.createCell(6).setCellValue(data[2] != null ? Double.parseDouble(data[2].toString()) : 0);
            row.createCell(7).setCellValue(data[4] != null ? Double.parseDouble(data[4].toString()) : 0);
            row.createCell(8).setCellValue(data[5] != null ? Double.parseDouble(data[5].toString()) : 0);
        }

        File file = File.createTempFile("Seed_MF_Report_", ".xlsx");
        FileOutputStream fos = new FileOutputStream(file);

        workbook.write(fos);
        workbook.close();
        fos.close();

        return new FileInputStream(file);
    }

    public ResponseEntity<?> getSeedMarketBiddingReport(
            ReelerReportRequest reportRequest) {

        ResponseWrapper rw =
                ResponseWrapper.createWrapper(List.class);

        List<Object[]> responses;

        // WITH LICENSE NUMBER
        if(reportRequest.getLicenseNumber() != null
                && !reportRequest.getLicenseNumber().trim().isEmpty()) {

            responses = lotGroupageRepository.getSeedMarketBiddingReport(
                    reportRequest.getMarketId(),
                    reportRequest.getReportFromDate(),
                    reportRequest.getLicenseNumber().trim()
            );

        } else {

            // WITHOUT LICENSE NUMBER
            responses = lotGroupageRepository.getSeedMarketBiddingReportWithoutLicense(reportRequest.getMarketId(), reportRequest.getReportFromDate());
        }

        List<SeedMarketBiddingResponse> responseList = new ArrayList<>();

        for (Object[] row : responses) {

            SeedMarketBiddingResponse res = new SeedMarketBiddingResponse();
            res.setAllottedLotId(row[0] != null ? row[0].toString() : "");
            res.setAuctionDate(row[1] != null ? row[1].toString() : "");
            res.setBuyerType(row[2] != null ? row[2].toString() : "");
            res.setBidderName(row[3] != null ? row[3].toString() : "");
            res.setLicenseNumber(row[4] != null ? row[4].toString() : "");
            res.setLotWeight(row[5] != null ? row[5].toString() : "0");
            res.setAmount(row[6] != null ? row[6].toString() : "0");
            res.setSoldAmount(row[7] != null ? row[7].toString() : "0");
            res.setMarketFee(row[8] != null ? row[8].toString() : "0");
            res.setMarketName(row[9] != null ? row[9].toString() : "");
            res.setCreatedDate(row[10] != null ? row[10].toString() : "");
            res.setLotGroupageId(row[11] != null ? row[11].toString() : "");
            responseList.add(res);
        }

        rw.setContent(responseList);

        return ResponseEntity.ok(rw);
    }

    public FileInputStream downloadSeedMarketBiddingReport(
            ReelerReportRequest request) throws Exception {

        List<Object[]> list;

        // WITH LICENSE
        if (request.getLicenseNumber() != null
                && !request.getLicenseNumber().trim().isEmpty()) {

            list = lotGroupageRepository.getSeedMarketBiddingReport(
                    request.getMarketId(),
                    request.getReportFromDate(),
                    request.getLicenseNumber().trim()
            );

        } else {

            // WITHOUT LICENSE
            list = lotGroupageRepository
                    .getSeedMarketBiddingReportWithoutLicense(
                            request.getMarketId(),
                            request.getReportFromDate()
                    );
        }

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Seed Market Bidding Report");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("SL No");
        header.createCell(1).setCellValue("Lot Number");
        header.createCell(2).setCellValue("Auction Date");
        header.createCell(3).setCellValue("Buyer Type");
        header.createCell(4).setCellValue("Bidder Name");
        header.createCell(5).setCellValue("License Number");
        header.createCell(6).setCellValue("Lot Weight");
        header.createCell(7).setCellValue("Bid Amount");
        header.createCell(8).setCellValue("Sold Amount");
        header.createCell(9).setCellValue("Market Fee");
        header.createCell(10).setCellValue("Market Name");

        int rowNum = 1;

        for (Object[] data : list) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(rowNum - 1);
            row.createCell(1).setCellValue(data[0] != null ? data[0].toString() : "");
            row.createCell(2).setCellValue(data[1] != null ? data[1].toString() : "");
            row.createCell(3).setCellValue(data[2] != null ? data[2].toString() : "");
            row.createCell(4).setCellValue(data[3] != null ? data[3].toString() : "");
            row.createCell(5).setCellValue(data[4] != null ? data[4].toString() : "");

            row.createCell(6).setCellValue(data[5] != null ? Double.parseDouble(data[5].toString()) : 0);

            row.createCell(7).setCellValue(data[6] != null ? Double.parseDouble(data[6].toString()) : 0);

            row.createCell(8).setCellValue(data[7] != null ? Double.parseDouble(data[7].toString()) : 0);

            row.createCell(9).setCellValue(data[8] != null ? Double.parseDouble(data[8].toString()) : 0);

            row.createCell(10).setCellValue(data[9] != null ? data[9].toString() : "");
        }

        File file = File.createTempFile(
                "Seed_Market_Bidding_Report_", ".xlsx");

        FileOutputStream fos = new FileOutputStream(file);
        workbook.write(fos);
        workbook.close();
        fos.close();
        return new FileInputStream(file);
    }

    public List<Map<String, String>> getLicenseNumberList(String buyerType, Integer marketId) {
        List<Map<String, String>> result = new ArrayList<>();
        if ("REELING".equalsIgnoreCase(buyerType)) {
            List<Object[]> rows = (marketId != null)
                    ? reelerAuctionRepository.getReelerListByMarket(marketId)
                    : reelerAuctionRepository.getAllReelers();
            for (Object[] row : rows) {
                Map<String, String> map = new java.util.LinkedHashMap<>();
                map.put("licenseNumber", row[2] != null ? row[2].toString() : "");
                map.put("name", row[1] != null ? row[1].toString() : "");
                result.add(map);
            }
        } else if ("EXTERNAL_UNIT".equalsIgnoreCase(buyerType)) {
            List<Object[]> rows = (marketId != null)
                    ? lotGroupageRepository.getExternalUnitListByMarket(marketId)
                    : lotGroupageRepository.getAllExternalUnits();
            for (Object[] row : rows) {
                Map<String, String> map = new java.util.LinkedHashMap<>();
                map.put("licenseNumber", row[2] != null ? row[2].toString() : "");
                map.put("name", row[1] != null ? row[1].toString() : "");
                result.add(map);
            }
        }
        return result;
    }

    public ReelerTransactionReportWrapper getSeedMarketTxnReport(
            int marketId,
            String licenseNumber,
            LocalDate fromDate,
            LocalDate toDate,
            String buyerType) {

        ReelerTransactionReportWrapper wrapper =
                new ReelerTransactionReportWrapper();

        wrapper.setColumnHeaders(java.util.Arrays.asList(
                "ಕ್ರಮ ಸಂಖ್ಯೆ / SL No",
                "ವಹಿವಾಟಿನ ದಿನಾಂಕ / Transaction Date",
                "ವಿವರ / Description",
                "ವಹಿವಾಟಿನ ವಿಧ / Transaction Type",
                "ಠೇವಣಿ ಮಾಡಿದ ಮೊತ್ತ (ರೂ) / Deposit Amount (Rs)",
                "ಖರೀದಿಸಿದ ಬಿತ್ತನೆ ರೇಷ್ಮೆ ಗೂಡಿನ ಪರಿಮಾಣ / Quantity of seed cocoon purchased (in kg's)",
                "ದರ ಪ್ರತಿ ಕೆ.ಜಿ.ಗೆ (ರೂ)/ Rate per Kg (Rs)",
                "ಖರೀದಿಸಿದ ರೇಷ್ಮೆ ಗೂಡಿನ ಮೊತ್ತ(ರೂ) / Cocoons Purchase Amount (Rs)",
                "ಮಾರು ಕಟ್ಟೆ ಶುಲ್ಕ @1% (ರೂ) / Market fee @1% (Rs)",
                "ಒಟ್ಟು ಮೊತ್ತ (ರೂ)/ Total (Rs)",
                "ಮರುಪಾವತಿಸಿದ ಮೊತ್ತ (ರೂ) / Refunded Amount (Rs)",
                "ಉಳಿಕೆ ಮೊತ್ತ (ರೂ) / Balance Amount (Rs)"
        ));

        try {

            MarketMaster marketMaster =
                    marketMasterRepository.findById(marketId);

            // ONLINE
            if (!marketMaster.getPaymentMode().equalsIgnoreCase("cash")) {
                List<Object[]> buyerResultList;

                try {

                    buyerResultList = lotGroupageRepository
                            .getBuyerDetails(
                                    marketId,
                                    licenseNumber);

                } catch (Exception e) {

                    return new ReelerTransactionReportWrapper();
                }

                if (buyerResultList == null
                        || buyerResultList.isEmpty()) {

                    return new ReelerTransactionReportWrapper();
                }

                Object[] buyerResult =
                        buyerResultList.get(0);

                String buyerName =
                        buyerResult[0].toString();

                String virtualAccountNumber =
                        buyerResult[1].toString();

                String buyerAddress =
                        buyerResult[2] != null ? buyerResult[2].toString() : "";

                String buyerFruitsId =
                        buyerResult[3] != null ? buyerResult[3].toString() : "";

                String buyerSource =
                        buyerResult[4] != null ? buyerResult[4].toString() : "";

                boolean isExternalUnit = "EXTERNAL_UNIT".equalsIgnoreCase(buyerType);

                if (isExternalUnit && "REELING".equalsIgnoreCase(buyerSource)) {
                    throw new com.sericulture.marketandauction.model.exceptions.ValidationException(
                            "License number belongs to a Reeler. Please select 'Reeling' as buyer type.");
                }
                if (!isExternalUnit && "EXTERNAL_UNIT".equalsIgnoreCase(buyerSource)) {
                    throw new com.sericulture.marketandauction.model.exceptions.ValidationException(
                            "License number belongs to an External Unit. Please select 'External Unit' as buyer type.");
                }

                if (isExternalUnit) {
                    wrapper.setReportType("EXTERNAL_UNIT");
                    wrapper.setColumnHeaders(java.util.Arrays.asList(
                            "ಕ್ರಮ ಸಂಖ್ಯೆ / SL No",
                            "ವಹಿವಾಟಿನ ದಿನಾಂಕ / Transaction Date",
                            "ವಿವರ / Description",
                            "ವಹಿವಾಟಿನ ವಿಧ / Transaction Type",
                            "ಠೇವಣಿ ಮಾಡಿದ ಮೊತ್ತ (ರೂ) / Deposit Amount (Rs)",
                            "ಖರೀದಿಸಿದ ಬಿತ್ತನೆ ರೇಷ್ಮೆ ಗೂಡಿನ ಪರಿಮಾಣ / Quantity of seed cocoon purchased(in kg's)",
                            "ಬಿತ್ತನೆ ಗೂಡಿನ ಸಂಖ್ಯೆ (ಪ್ರತಿ ಕೆ.ಜಿಗೆ) / No. of Seed Cocoons per Kg ",
                            "ಒಟ್ಟು ಬಿತ್ತನೆ ಗೂಡಿನ ಸಂಖ್ಯೆ / Total No of Seed Cocoons (in No's)",
                            "ದರ ಪ್ರತಿ ಕೆ.ಜಿ.ಗೆ (ರೂ)/ Rate per Kg (Rs)",
                            "ಖರೀದಿಸಿದ ರೇಷ್ಮೆ ಗೂಡಿನ ಮೊತ್ತ (ರೂ) / Seed Cocoon Purchase Amount (Rs)",
                            "ಮರುಪಾವತಿಸಿದ ಮೊತ್ತ (ರೂ) / Refunded Amount (Rs)",
                            "ಉಳಿಕೆ ಮೊತ್ತ (ರೂ) / Balance Amount (Rs)"
                    ));
                    wrapper.setFruitsId("");
                } else {
                    wrapper.setReportType("REELING");
                    wrapper.setFruitsId(buyerFruitsId);
                }

                // PASSBOOK — use virtualAccountNumber directly.
                // REELER_VID_CURRENT_BALANCE is skipped because external units
                // may not have a row there, causing an early return and leaving
                // the UI stuck on the previous search result.
                List<Object[]> transactionList =
                        lotGroupageRepository
                                .getSeedMarketTransactionPassBook(
                                        fromDate,
                                        toDate,
                                        virtualAccountNumber,
                                        marketId
                                );

                List<ReportAllTransaction> reportAllTransactions =
                        new ArrayList<>();

                for (Object[] obj : transactionList) {

                    ReportAllTransaction rat =
                            new ReportAllTransaction();

                    rat.setTransactionType(obj[0] + "");

                    rat.setAmount(
                            ((Number) obj[1]).doubleValue());

                    if (obj[2] != null) {

                        if (obj[2] instanceof java.sql.Timestamp) {

                            rat.setCreatedDate(
                                    ((java.sql.Timestamp) obj[2])
                                            .toLocalDateTime());

                        } else if (obj[2] instanceof java.sql.Date) {

                            rat.setCreatedDate(
                                    ((java.sql.Date) obj[2])
                                            .toLocalDate()
                                            .atStartOfDay());
                        }
                    }

                    rat.setLot(
                            ((Number) obj[3]).longValue());

                    rat.setDateOn(
                            ((java.sql.Date) obj[4]).toLocalDate());

                    rat.setFarmerName((String) obj[5]);

                    rat.setLotWeight(obj[6] != null
                            ? ((Number) obj[6]).doubleValue()
                            : 0.0);

                    rat.setRatePerKg(obj[7] != null
                            ? ((Number) obj[7]).doubleValue()
                            : 0.0);

                    rat.setMarketFee(obj[8] != null
                            ? ((Number) obj[8]).doubleValue()
                            : 0.0);

                    rat.setTotal(obj[9] != null
                            ? ((Number) obj[9]).doubleValue()
                            : 0.0);

                    rat.setQtyNos(obj[10] != null
                            ? ((Number) obj[10]).intValue()
                            : 0);

                    reportAllTransactions.add(rat);
                }

                double debitSum = 0.0;
                double creditSum = 0.0;
                double totalLotWeight = 0.0;
                double totalPaymentAmount = 0.0;
                double totalMarketFee = 0.0;
                double totalBankTransfers = 0.0;

                Double fetchedOpening = lotGroupageRepository.getSeedMarketOpeningBalance(virtualAccountNumber, fromDate);
                double openingBalance = fetchedOpening != null ? fetchedOpening : 0.0;

                List<ReelerTransactionReport> reports =
                        new ArrayList<>();

                for (ReportAllTransaction rat : reportAllTransactions) {

                    ReelerTransactionReport report =
                            new ReelerTransactionReport();

                    report.setTransactionType(
                            rat.getTransactionType());

                    report.setTransactionDate(
                            rat.getCreatedDate().toLocalDate());

                    if ("D".equalsIgnoreCase(
                            rat.getTransactionType())) {

                        if (rat.getLot() == 0) {

                            totalBankTransfers += rat.getAmount() != null ? rat.getAmount() : 0;

                            report.setRefundedAmount(rat.getAmount());

                            report.setOperationDescription(
                                    "Transfer Back to " + buyerName);

                        } else {

                            double roundedTotal = Math.round(rat.getTotal() != null ? rat.getTotal() : 0.0);
                            report.setTotal(roundedTotal);
                            debitSum += roundedTotal;
                            totalLotWeight += rat.getLotWeight() != null ? rat.getLotWeight() : 0;
                            totalPaymentAmount += rat.getAmount() != null ? rat.getAmount() : 0;
                            totalMarketFee += rat.getMarketFee() != null ? rat.getMarketFee() : 0;

                            report.setPaymentAmount(rat.getAmount());
                            report.setLotWeight(rat.getLotWeight());
                            report.setRatePerKg(rat.getRatePerKg());
                            report.setMarketFee(rat.getMarketFee());
                            report.setQtyNos(rat.getQtyNos());

                            report.setOperationDescription(
                                    "Paid to "
                                            + rat.getFarmerName()
                                            + ", for lot "
                                            + rat.getLot());
                        }

                    } else {

                        creditSum += rat.getAmount();

                        report.setDepositAmount(
                                rat.getAmount());

                        report.setOperationDescription(
                                "Deposited by " + buyerName);
                    }

                    reports.add(report);
                }

                double runningBalance =
                        openingBalance;

                for (ReelerTransactionReport report : reports) {

                    if ("D".equalsIgnoreCase(
                            report.getTransactionType())) {

                        double deduction = report.getTotal() != null ? report.getTotal()
                                : (report.getRefundedAmount() != null ? report.getRefundedAmount() : 0.0);
                        runningBalance = runningBalance - deduction;

                    } else {

                        runningBalance =
                                runningBalance
                                        + report.getDepositAmount();
                    }

                    report.setBalance(runningBalance);
                }

                wrapper.setReelerTransactionReports(reports);
                wrapper.setOpeningBalance(openingBalance);
                wrapper.setTotalDeposits(creditSum);
                wrapper.setTotalLotWeight(totalLotWeight);
                wrapper.setTotalPaymentAmount(totalPaymentAmount);
                wrapper.setTotalMarketFee(totalMarketFee);
                wrapper.setTotalPurchase(debitSum);
                wrapper.setTotalBankTransfers(totalBankTransfers);
                wrapper.setClosingBalance(runningBalance);
                wrapper.setName(buyerName);
                wrapper.setAddress(buyerAddress);
                if (!isExternalUnit) {
                    wrapper.setFruitsId(buyerFruitsId);
                }

            }

            // CASH
            else {

                List<Object[]> objectList;

                if (licenseNumber != null
                        && !licenseNumber.trim().isEmpty()) {

                    objectList =
                            lotGroupageRepository
                                    .getCashBalanceWithLicense(
                                            fromDate,
                                            toDate,
                                            marketId,
                                            licenseNumber
                                    );

                } else {

                    objectList =
                            lotGroupageRepository
                                    .getCashBalance(
                                            fromDate,
                                            toDate,
                                            marketId
                                    );
                }

                List<ReelerTransactionReport> reports =
                        new ArrayList<>();

                Double totalPurchase = 0.0;

                String buyerName = "";

                for (Object[] obj : objectList) {

                    totalPurchase =
                            ((BigDecimal) obj[9]).doubleValue();

                    buyerName =
                            (String) obj[6];

                    ReelerTransactionReport report =
                            new ReelerTransactionReport();

                    report.setTransactionType("Cash");

                    report.setPaymentAmount(
                            ((BigDecimal) obj[0]).doubleValue());

                    report.setTransactionDate(
                            ((java.sql.Date) obj[2]).toLocalDate());

                    int lotId =
                            ((Number) obj[1]).intValue();

                    report.setOperationDescription(
                            "Paid to "
                                    + obj[3] + " "
                                    + obj[4] + " "
                                    + obj[5]
                                    + ", for lot "
                                    + lotId);

                    reports.add(report);
                }

                wrapper.setReelerTransactionReports(reports);

                wrapper.setOpeningBalance(0.0);

                wrapper.setTotalDeposits(0.0);

                wrapper.setTotalPurchase(totalPurchase);

                wrapper.setName(buyerName);
                wrapper.setAddress("");
            }

        } catch (Exception ex) {

            throw ex;
        }

        return wrapper;
    }

    public FileInputStream downloadSeedMarketTxnReport(
            ReelerTxnReportRequest request) throws Exception {

        ReelerTransactionReportWrapper wrapper =
                getSeedMarketTxnReport(
                        request.getMarketId(),
                        request.getLicenseNumber(),
                        request.getFromDate(),
                        request.getToDate(),
                        request.getBuyerType()
                );

        MarketMaster marketMaster = marketMasterRepository.findById(request.getMarketId());
        String marketName = (marketMaster != null && marketMaster.getName() != null)
                ? marketMaster.getName() : "";

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Seed Market Transaction Report");
        final int COLS = 12;

        // ── Number formats ────────────────────────────────────────────────────
        DataFormat dataFmt = workbook.createDataFormat();
        short numFmt2dp = dataFmt.getFormat("#,##0.00");

        // ── Fonts ─────────────────────────────────────────────────────────────
        Font fTitle = workbook.createFont(); fTitle.setBold(true); fTitle.setFontHeightInPoints((short) 14);
        Font fSubTitle = workbook.createFont(); fSubTitle.setBold(true); fSubTitle.setFontHeightInPoints((short) 12);
        Font fLabel = workbook.createFont(); fLabel.setBold(true); fLabel.setFontHeightInPoints((short) 9);
        Font fValue = workbook.createFont(); fValue.setFontHeightInPoints((short) 9);
        Font fHdr = workbook.createFont(); fHdr.setBold(true); fHdr.setFontHeightInPoints((short) 9); fHdr.setColor(IndexedColors.WHITE.getIndex());
        Font fTotal = workbook.createFont(); fTotal.setBold(true); fTotal.setFontHeightInPoints((short) 9);

        // ── Styles ────────────────────────────────────────────────────────────
        CellStyle sTitle = workbook.createCellStyle();
        sTitle.setFont(fTitle); sTitle.setAlignment(HorizontalAlignment.CENTER);
        sTitle.setVerticalAlignment(VerticalAlignment.CENTER); sTitle.setWrapText(true);

        CellStyle sSubTitle = workbook.createCellStyle();
        sSubTitle.setFont(fSubTitle); sSubTitle.setAlignment(HorizontalAlignment.CENTER);
        sSubTitle.setVerticalAlignment(VerticalAlignment.CENTER); sSubTitle.setWrapText(true);

        // Info label: bold, light-yellow bg, thin borders, wrap
        CellStyle sIL = workbook.createCellStyle();
        sIL.setFont(fLabel); sIL.setWrapText(true); sIL.setVerticalAlignment(VerticalAlignment.CENTER);
        sIL.setFillForegroundColor(IndexedColors.LEMON_CHIFFON.getIndex()); sIL.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        sIL.setBorderTop(BorderStyle.THIN); sIL.setBorderBottom(BorderStyle.THIN); sIL.setBorderLeft(BorderStyle.THIN); sIL.setBorderRight(BorderStyle.THIN);

        // Info value: normal, thin borders, wrap
        CellStyle sIV = workbook.createCellStyle();
        sIV.setFont(fValue); sIV.setWrapText(true); sIV.setVerticalAlignment(VerticalAlignment.CENTER);
        sIV.setBorderTop(BorderStyle.THIN); sIV.setBorderBottom(BorderStyle.THIN); sIV.setBorderLeft(BorderStyle.THIN); sIV.setBorderRight(BorderStyle.THIN);

        // Info numeric value: left-aligned (value starts at left of cell), formatted, borders
        CellStyle sIN = workbook.createCellStyle();
        sIN.setFont(fValue); sIN.setAlignment(HorizontalAlignment.LEFT); sIN.setVerticalAlignment(VerticalAlignment.CENTER);
        sIN.setDataFormat(numFmt2dp);
        sIN.setBorderTop(BorderStyle.THIN); sIN.setBorderBottom(BorderStyle.THIN); sIN.setBorderLeft(BorderStyle.THIN); sIN.setBorderRight(BorderStyle.THIN);

        // Column header: bold white on dark blue, center, wrap
        CellStyle sColHdr = workbook.createCellStyle();
        sColHdr.setFont(fHdr); sColHdr.setAlignment(HorizontalAlignment.CENTER);
        sColHdr.setVerticalAlignment(VerticalAlignment.CENTER); sColHdr.setWrapText(true);
        sColHdr.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex()); sColHdr.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        sColHdr.setBorderTop(BorderStyle.THIN); sColHdr.setBorderBottom(BorderStyle.THIN); sColHdr.setBorderLeft(BorderStyle.THIN); sColHdr.setBorderRight(BorderStyle.THIN);

        // Data: center, borders, wrap
        CellStyle sData = workbook.createCellStyle();
        sData.setFont(fValue); sData.setAlignment(HorizontalAlignment.CENTER);
        sData.setVerticalAlignment(VerticalAlignment.CENTER); sData.setWrapText(true);
        sData.setBorderTop(BorderStyle.THIN); sData.setBorderBottom(BorderStyle.THIN); sData.setBorderLeft(BorderStyle.THIN); sData.setBorderRight(BorderStyle.THIN);

        // Description: left-align, wrap
        CellStyle sDesc = workbook.createCellStyle();
        sDesc.setFont(fValue); sDesc.setAlignment(HorizontalAlignment.LEFT);
        sDesc.setVerticalAlignment(VerticalAlignment.CENTER); sDesc.setWrapText(true);
        sDesc.setBorderTop(BorderStyle.THIN); sDesc.setBorderBottom(BorderStyle.THIN); sDesc.setBorderLeft(BorderStyle.THIN); sDesc.setBorderRight(BorderStyle.THIN);

        // Numeric data: right-align, 2dp format
        CellStyle sNum = workbook.createCellStyle();
        sNum.setFont(fValue); sNum.setAlignment(HorizontalAlignment.CENTER);
        sNum.setVerticalAlignment(VerticalAlignment.CENTER); sNum.setDataFormat(numFmt2dp);
        sNum.setBorderTop(BorderStyle.THIN); sNum.setBorderBottom(BorderStyle.THIN); sNum.setBorderLeft(BorderStyle.THIN); sNum.setBorderRight(BorderStyle.THIN);

        // Total label: bold, grey bg, medium top/bottom borders
        CellStyle sTotalL = workbook.createCellStyle();
        sTotalL.setFont(fTotal); sTotalL.setAlignment(HorizontalAlignment.CENTER); sTotalL.setVerticalAlignment(VerticalAlignment.CENTER);
        sTotalL.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex()); sTotalL.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        sTotalL.setBorderTop(BorderStyle.MEDIUM); sTotalL.setBorderBottom(BorderStyle.MEDIUM); sTotalL.setBorderLeft(BorderStyle.THIN); sTotalL.setBorderRight(BorderStyle.THIN);

        // Total number: bold, right, 2dp, grey bg, medium top/bottom borders
        CellStyle sTotalN = workbook.createCellStyle();
        sTotalN.setFont(fTotal); sTotalN.setAlignment(HorizontalAlignment.CENTER);
        sTotalN.setVerticalAlignment(VerticalAlignment.CENTER); sTotalN.setDataFormat(numFmt2dp);
        sTotalN.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex()); sTotalN.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        sTotalN.setBorderTop(BorderStyle.MEDIUM); sTotalN.setBorderBottom(BorderStyle.MEDIUM); sTotalN.setBorderLeft(BorderStyle.THIN); sTotalN.setBorderRight(BorderStyle.THIN);

        // ── Rows 0-1: Logo centered across cols 4-7 ───────────────────────────
        sheet.createRow(0).setHeightInPoints(45);
        sheet.createRow(1).setHeightInPoints(45);
        try {
            org.springframework.core.io.ClassPathResource logoRes =
                    new org.springframework.core.io.ClassPathResource("images/kar_logo.png");
            byte[] logoBytes = logoRes.getInputStream().readAllBytes();
            int picIdx = workbook.addPicture(logoBytes, org.apache.poi.ss.usermodel.Workbook.PICTURE_TYPE_PNG);
            org.apache.poi.ss.usermodel.Drawing<?> drawing = sheet.createDrawingPatriarch();
            org.apache.poi.ss.usermodel.ClientAnchor anchor = workbook.getCreationHelper().createClientAnchor();
            anchor.setCol1(4); anchor.setRow1(0); anchor.setCol2(7); anchor.setRow2(2);
            anchor.setAnchorType(org.apache.poi.ss.usermodel.ClientAnchor.AnchorType.MOVE_AND_RESIZE);
            drawing.createPicture(anchor, picIdx);
        } catch (Exception ignored) {}

        // ── Rows 2-5: Title section ───────────────────────────────────────────
        int rowIdx = 2;
        Row rT0 = sheet.createRow(rowIdx++); rT0.setHeightInPoints(28);
        Cell cT0 = rT0.createCell(0); cT0.setCellValue("ಕರ್ನಾಟಕ ಸರ್ಕಾರ"); cT0.setCellStyle(sTitle);
        for (int i = 1; i < COLS; i++) rT0.createCell(i).setCellStyle(sTitle);
        sheet.addMergedRegion(new CellRangeAddress(rT0.getRowNum(), rT0.getRowNum(), 0, COLS - 1));

        Row rT1 = sheet.createRow(rowIdx++); rT1.setHeightInPoints(24);
        Cell cT1 = rT1.createCell(0); cT1.setCellValue("ರೇಷ್ಮೆ, ಇಲಾಖೆ"); cT1.setCellStyle(sSubTitle);
        for (int i = 1; i < COLS; i++) rT1.createCell(i).setCellStyle(sSubTitle);
        sheet.addMergedRegion(new CellRangeAddress(rT1.getRowNum(), rT1.getRowNum(), 0, COLS - 1));

        Row rT2 = sheet.createRow(rowIdx++); rT2.setHeightInPoints(22);
        Cell cT2 = rT2.createCell(0); cT2.setCellValue("ಸರ್ಕಾರಿ ರೇಷ್ಮೆ ಗೂಡಿನ ಮಾರುಕಟ್ಟೆ, " + marketName); cT2.setCellStyle(sSubTitle);
        for (int i = 1; i < COLS; i++) rT2.createCell(i).setCellStyle(sSubTitle);
        sheet.addMergedRegion(new CellRangeAddress(rT2.getRowNum(), rT2.getRowNum(), 0, COLS - 1));

        Row rT3 = sheet.createRow(rowIdx++); rT3.setHeightInPoints(22);
        Cell cT3 = rT3.createCell(0); cT3.setCellValue("ರೇಷ್ಮೆ ನೂಲು ಬಿಚ್ಚಾಣಿಕೆದಾರರ ವಹಿವಾಟು ವರದಿ"); cT3.setCellStyle(sSubTitle);
        for (int i = 1; i < COLS; i++) rT3.createCell(i).setCellStyle(sSubTitle);
        sheet.addMergedRegion(new CellRangeAddress(rT3.getRowNum(), rT3.getRowNum(), 0, COLS - 1));

        // ── Row 6: Spacer ─────────────────────────────────────────────────────
        sheet.createRow(rowIdx++).setHeightInPoints(6);

        // ── Date strings ──────────────────────────────────────────────────────
        String generatedAt = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
        String fromStr = request.getFromDate() != null
                ? request.getFromDate().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy")) : "";
        String toStr = request.getToDate() != null
                ? request.getToDate().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy")) : "";
        String nameAndAddress = (wrapper.getName() != null ? wrapper.getName() : "")
                + (wrapper.getAddress() != null && !wrapper.getAddress().isEmpty() ? ", " + wrapper.getAddress() : "");

        // ── Rows 7-11: Info block (5 rows) ───────────────────────────────────
        // Pattern: label [0-3] | value starts at col 4 — consistent across all rows

        // Row 7: FID label [0-3] | FID value [4-5] | empty [6-10]
        Row rI0 = sheet.createRow(rowIdx++); rI0.setHeightInPoints(22);
        rI0.createCell(0).setCellValue("FRUITS ID / FID:"); rI0.getCell(0).setCellStyle(sIL);
        rI0.createCell(1).setCellStyle(sIL); rI0.createCell(2).setCellStyle(sIL); rI0.createCell(3).setCellStyle(sIL);
        sheet.addMergedRegion(new CellRangeAddress(rI0.getRowNum(), rI0.getRowNum(), 0, 3));
        rI0.createCell(4).setCellValue(wrapper.getFruitsId() != null ? wrapper.getFruitsId() : ""); rI0.getCell(4).setCellStyle(sIV);
        rI0.createCell(5).setCellStyle(sIV);
        sheet.addMergedRegion(new CellRangeAddress(rI0.getRowNum(), rI0.getRowNum(), 4, 5));
        for (int i = 6; i <= 10; i++) rI0.createCell(i).setCellStyle(sIV);
        sheet.addMergedRegion(new CellRangeAddress(rI0.getRowNum(), rI0.getRowNum(), 6, 10));

        // Row 8: License label [0-3] | Lic value [4-5] | Date label [6-7] | Timestamp [8-10]
        Row rI1 = sheet.createRow(rowIdx++); rI1.setHeightInPoints(28);
        rI1.createCell(0).setCellValue("ರೇಷ್ಮೆ ನೂಲು ಬಿಚ್ಚಾಣಿಕೆದಾರರ ರಪದಾರಿ ಸಂಖ್ಯೆ:"); rI1.getCell(0).setCellStyle(sIL);
        rI1.createCell(1).setCellStyle(sIL); rI1.createCell(2).setCellStyle(sIL); rI1.createCell(3).setCellStyle(sIL);
        sheet.addMergedRegion(new CellRangeAddress(rI1.getRowNum(), rI1.getRowNum(), 0, 3));
        rI1.createCell(4).setCellValue(request.getLicenseNumber() != null ? request.getLicenseNumber() : ""); rI1.getCell(4).setCellStyle(sIV);
        rI1.createCell(5).setCellStyle(sIV);
        sheet.addMergedRegion(new CellRangeAddress(rI1.getRowNum(), rI1.getRowNum(), 4, 5));
        rI1.createCell(6).setCellValue("Date and Time Stamp:"); rI1.getCell(6).setCellStyle(sIL);
        rI1.createCell(7).setCellStyle(sIL);
        sheet.addMergedRegion(new CellRangeAddress(rI1.getRowNum(), rI1.getRowNum(), 6, 7));
        rI1.createCell(8).setCellValue(generatedAt); rI1.getCell(8).setCellStyle(sIV);
        rI1.createCell(9).setCellStyle(sIV); rI1.createCell(10).setCellStyle(sIV);
        sheet.addMergedRegion(new CellRangeAddress(rI1.getRowNum(), rI1.getRowNum(), 8, 10));

        // Row 9: Name label [0-3] | Name/Address value [4-10] — label and value on SAME row
        Row rI2 = sheet.createRow(rowIdx++); rI2.setHeightInPoints(28);
        rI2.createCell(0).setCellValue("ರೇಷ್ಮೆ ನೂಲು ಬಿಚ್ಚಾಣಿಕೆದಾರರ ಹೆಸರು ಮತ್ತು ವಿಳಾಸ:"); rI2.getCell(0).setCellStyle(sIL);
        rI2.createCell(1).setCellStyle(sIL); rI2.createCell(2).setCellStyle(sIL); rI2.createCell(3).setCellStyle(sIL);
        sheet.addMergedRegion(new CellRangeAddress(rI2.getRowNum(), rI2.getRowNum(), 0, 3));
        rI2.createCell(4).setCellValue(nameAndAddress); rI2.getCell(4).setCellStyle(sIV);
        for (int i = 5; i <= 10; i++) rI2.createCell(i).setCellStyle(sIV);
        sheet.addMergedRegion(new CellRangeAddress(rI2.getRowNum(), rI2.getRowNum(), 4, 10));

        // Row 10: Period label [0-3] | Period value [4-10]
        Row rI3 = sheet.createRow(rowIdx++); rI3.setHeightInPoints(22);
        rI3.createCell(0).setCellValue("ವಹಿವಾಟಿನ ಅವಧಿ:"); rI3.getCell(0).setCellStyle(sIL);
        rI3.createCell(1).setCellStyle(sIL); rI3.createCell(2).setCellStyle(sIL); rI3.createCell(3).setCellStyle(sIL);
        sheet.addMergedRegion(new CellRangeAddress(rI3.getRowNum(), rI3.getRowNum(), 0, 3));
        rI3.createCell(4).setCellValue(fromStr + " to " + toStr); rI3.getCell(4).setCellStyle(sIV);
        for (int i = 5; i <= 10; i++) rI3.createCell(i).setCellStyle(sIV);
        sheet.addMergedRegion(new CellRangeAddress(rI3.getRowNum(), rI3.getRowNum(), 4, 10));

        // Row 11: Balance label [0-3] | Balance value [4-5] | empty [6-7] | ಮೊತ್ತ: ರೂ ಗಳಲ್ಲಿ [8-10]
        Row rI4 = sheet.createRow(rowIdx++); rI4.setHeightInPoints(25);
        rI4.createCell(0).setCellValue("ದಿನಾಂಕ " + fromStr + " ರಂದು ಇದ್ದ ಪ್ರಾರಂಭಿಕ ಉಳಿಕೆ ಮೊತ್ತ:"); rI4.getCell(0).setCellStyle(sIL);
        rI4.createCell(1).setCellStyle(sIL); rI4.createCell(2).setCellStyle(sIL); rI4.createCell(3).setCellStyle(sIL);
        sheet.addMergedRegion(new CellRangeAddress(rI4.getRowNum(), rI4.getRowNum(), 0, 3));
        rI4.createCell(4).setCellValue(wrapper.getOpeningBalance() != null ? wrapper.getOpeningBalance() : 0); rI4.getCell(4).setCellStyle(sIN);
        rI4.createCell(5).setCellStyle(sIN);
        sheet.addMergedRegion(new CellRangeAddress(rI4.getRowNum(), rI4.getRowNum(), 4, 5));
        rI4.createCell(6).setCellStyle(sIV); rI4.createCell(7).setCellStyle(sIV);
        sheet.addMergedRegion(new CellRangeAddress(rI4.getRowNum(), rI4.getRowNum(), 6, 7));
        rI4.createCell(8).setCellValue("ಮೊತ್ತ: ರೂ ಗಳಲ್ಲಿ"); rI4.getCell(8).setCellStyle(sIL);
        rI4.createCell(9).setCellStyle(sIL); rI4.createCell(10).setCellStyle(sIL);
        sheet.addMergedRegion(new CellRangeAddress(rI4.getRowNum(), rI4.getRowNum(), 8, 10));

        // ── Row 11: Spacer ────────────────────────────────────────────────────
        sheet.createRow(rowIdx++).setHeightInPoints(6);

        // ── Row 12: Column headers ────────────────────────────────────────────
        Row rHdr = sheet.createRow(rowIdx++); rHdr.setHeightInPoints(85);
        String[] hdrs = {
                "ಕ್ರಮ ಸಂಖ್ಯೆ\nSL No",
                "ವಹಿವಾಟಿನ ದಿನಾಂಕ\nTransaction Date",
                "ವಿವರ\nDescription",
                "ವಹಿವಾಟಿನ ವಿಧ\nTransaction Type",
                "ಠೇವಣಿ ಮಾಡಿದ ಮೊತ್ತ \nDeposit Amount ",
                "ಖರೀದಿಸಿದ ಬಿತ್ತನೆ ರೇಷ್ಮೆ ಗೂಡಿನ ಪರಿಮಾಣ\nQuantity of Seed Cocoon Purchased (Kg)",
                "ದರ ಪ್ರತಿ ಕೆ.ಜಿ.ಗೆ \nRate per Kg ",
                "ಖರೀದಿಸಿದ ರೇಷ್ಮೆ ಗೂಡಿನ ಮೊತ್ತ \nCocoons Purchase Amount ",
                "ಮಾರು ಕಟ್ಟೆ ಶುಲ್ಕ @1% \nMarket Fee @1% ",
                "ಒಟ್ಟು ಮೊತ್ತ \nTotal ",
                "ಮರುಪಾವತಿಸಿದ ಮೊತ್ತ \nRefunded Amount ",
                "ಉಳಿಕೆ ಮೊತ್ತ \nBalance Amount "
        };
        for (int i = 0; i < hdrs.length; i++) {
            Cell hc = rHdr.createCell(i); hc.setCellValue(hdrs[i]); hc.setCellStyle(sColHdr);
        }

        // ── Data rows ─────────────────────────────────────────────────────────
        int slNo = 1;
        double sumDeposit = 0, sumLotWeight = 0, sumPayment = 0, sumMarketFee = 0, sumTotal = 0, sumRefunded = 0;
        double lastBalance = 0;
        for (ReelerTransactionReport report : wrapper.getReelerTransactionReports()) {
            Row row = sheet.createRow(rowIdx++); row.setHeightInPoints(20);
            Cell dc0 = row.createCell(0); dc0.setCellValue(slNo++); dc0.setCellStyle(sData);
            Cell dc1 = row.createCell(1);
            dc1.setCellValue(report.getTransactionDate() != null ? report.getTransactionDate().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy")) : "");
            dc1.setCellStyle(sData);
            Cell dc2 = row.createCell(2); dc2.setCellValue(report.getOperationDescription() != null ? report.getOperationDescription() : ""); dc2.setCellStyle(sDesc);
            Cell dc3 = row.createCell(3); dc3.setCellValue(report.getTransactionType() != null ? report.getTransactionType() : ""); dc3.setCellStyle(sData);
            double depositVal = report.getDepositAmount() != null ? report.getDepositAmount() : 0;
            Cell dc4 = row.createCell(4); dc4.setCellValue(depositVal); dc4.setCellStyle(sNum); sumDeposit += depositVal;
            double lotWeightVal = report.getLotWeight() != null ? report.getLotWeight() : 0;
            Cell dc5 = row.createCell(5); dc5.setCellValue(lotWeightVal); dc5.setCellStyle(sNum); sumLotWeight += lotWeightVal;
            Cell dc6 = row.createCell(6); dc6.setCellValue(report.getRatePerKg() != null ? report.getRatePerKg() : 0); dc6.setCellStyle(sNum);
            double paymentVal = report.getPaymentAmount() != null ? report.getPaymentAmount() : 0;
            Cell dc7 = row.createCell(7); dc7.setCellValue(paymentVal); dc7.setCellStyle(sNum); sumPayment += paymentVal;
            double marketFeeVal = report.getMarketFee() != null ? report.getMarketFee() : 0;
            Cell dc8 = row.createCell(8); dc8.setCellValue(marketFeeVal); dc8.setCellStyle(sNum); sumMarketFee += marketFeeVal;
            double totalVal = report.getTotal() != null ? report.getTotal() : 0;
            Cell dc9 = row.createCell(9); dc9.setCellValue(totalVal); dc9.setCellStyle(sNum); sumTotal += totalVal;
            double refundedVal = report.getRefundedAmount() != null ? report.getRefundedAmount() : 0;
            Cell dc10 = row.createCell(10); dc10.setCellValue(refundedVal); dc10.setCellStyle(sNum); sumRefunded += refundedVal;
            lastBalance = report.getBalance() != null ? report.getBalance() : 0;
            Cell dc11 = row.createCell(11); dc11.setCellValue(lastBalance); dc11.setCellStyle(sNum);
        }

        // ── Totals row ────────────────────────────────────────────────────────
        Row rTot = sheet.createRow(rowIdx++); rTot.setHeightInPoints(22);
        Cell tc0 = rTot.createCell(0); tc0.setCellValue("ಒಟ್ಟು / Total"); tc0.setCellStyle(sTotalL);
        for (int i = 1; i <= 3; i++) rTot.createCell(i).setCellStyle(sTotalL);
        sheet.addMergedRegion(new CellRangeAddress(rTot.getRowNum(), rTot.getRowNum(), 0, 3));
        Cell tc4 = rTot.createCell(4); tc4.setCellValue(sumDeposit); tc4.setCellStyle(sTotalN);
        Cell tc5 = rTot.createCell(5); tc5.setCellValue(sumLotWeight); tc5.setCellStyle(sTotalN);
        Cell tc6 = rTot.createCell(6); tc6.setCellValue(""); tc6.setCellStyle(sTotalL);
        Cell tc7 = rTot.createCell(7); tc7.setCellValue(sumPayment); tc7.setCellStyle(sTotalN);
        Cell tc8 = rTot.createCell(8); tc8.setCellValue(sumMarketFee); tc8.setCellStyle(sTotalN);
        Cell tc9 = rTot.createCell(9); tc9.setCellValue(sumTotal); tc9.setCellStyle(sTotalN);
        Cell tc10 = rTot.createCell(10); tc10.setCellValue(sumRefunded); tc10.setCellStyle(sTotalN);
        Cell tc11 = rTot.createCell(11); tc11.setCellValue(lastBalance); tc11.setCellStyle(sTotalN);

        // ── Column widths: Transaction Type widened to 3000; total 34 500 POI ─
        // Fits A4 landscape (~38 220 POI at 0.4" margins) — no unwanted scaling
        int[] colWidths = { 1200, 3200, 5500, 3000, 3200, 4000, 2800, 3200, 3000, 3000, 3000, 3200 };
        for (int i = 0; i < COLS; i++) sheet.setColumnWidth(i, colWidths[i]);

        // ── Print: A4 landscape, fit 1 page wide ─────────────────────────────
        sheet.setMargin(org.apache.poi.ss.usermodel.Sheet.LeftMargin,   0.4);
        sheet.setMargin(org.apache.poi.ss.usermodel.Sheet.RightMargin,  0.4);
        sheet.setMargin(org.apache.poi.ss.usermodel.Sheet.TopMargin,    0.4);
        sheet.setMargin(org.apache.poi.ss.usermodel.Sheet.BottomMargin, 0.4);
        sheet.getPrintSetup().setLandscape(true);
        sheet.getPrintSetup().setPaperSize(org.apache.poi.ss.usermodel.PrintSetup.A4_PAPERSIZE);
        sheet.setFitToPage(true);
        sheet.getPrintSetup().setFitWidth((short) 1);
        sheet.getPrintSetup().setFitHeight((short) 0);
        sheet.setAutobreaks(true);

        File file = File.createTempFile("Seed_Market_Transaction_Report_", ".xlsx");
        FileOutputStream fos = new FileOutputStream(file);
        workbook.write(fos);
        workbook.close();
        fos.close();

        return new FileInputStream(file);
    }

    public java.io.FileInputStream downloadSeedMarketTxnReportPDF(ReelerTxnReportRequest request) throws Exception {

        ReelerTransactionReportWrapper wrapper = getSeedMarketTxnReport(
                request.getMarketId(), request.getLicenseNumber(),
                request.getFromDate(), request.getToDate(),
                request.getBuyerType());

        MarketMaster marketMaster = marketMasterRepository.findById(request.getMarketId());
        String marketName = (marketMaster != null && marketMaster.getName() != null)
                ? marketMaster.getName() : "";

        java.util.List<ReelerTransactionReport> reports =
                wrapper.getReelerTransactionReports() != null
                        ? wrapper.getReelerTransactionReports()
                        : java.util.Collections.emptyList();

        // ── dimensions (fixed A4 landscape at 300 DPI) ───────────────────────
        int DPI    = 300;
        float PDF_W = 842f;                              // A4 landscape width  (pts)
        float PDF_H = 595f;                              // A4 landscape height (pts)
        int W      = Math.round(PDF_W * DPI / 72f);     // 3508 px
        int H      = Math.round(PDF_H * DPI / 72f);     // 2479 px  (fixed – no gaps when printed)
        int MARGIN  = Math.round(15f * DPI / 72f);      // 63 px  = 15 pt
        int LOGO_H  = Math.round(40f * DPI / 72f);      // 167 px = 40 pt
        int INFO_H  = Math.round(15f * DPI / 72f);      // 63 px  = 15 pt
        int HDR_H   = Math.round(55f * DPI / 72f);      // 229 px = 55 pt
        int ROW_H   = Math.round(15f * DPI / 72f);      // 63 px  = 15 pt
        int TITLE_H = Math.round(11f * DPI / 72f);      // 46 px  = 11 pt

        java.awt.image.BufferedImage logoImg = null;
        try {
            org.springframework.core.io.ClassPathResource logoRes =
                    new org.springframework.core.io.ClassPathResource("images/kar_logo.png");
            logoImg = javax.imageio.ImageIO.read(logoRes.getInputStream());
        } catch (Exception ignored) {}

        // ── column widths ─────────────────────────────────────────────────────
        int usable = W - 2 * MARGIN;
        float[] proportions = {0.04f, 0.06f, 0.15f, 0.08f, 0.08f,
                               0.10f, 0.05f, 0.09f, 0.07f, 0.08f, 0.06f, 0.14f};
        int[] cw = new int[12];
        int allocated = 0;
        for (int i = 0; i < 11; i++) {
            cw[i] = (int)(usable * proportions[i]);
            allocated += cw[i];
        }
        cw[11] = usable - allocated;

        int[] cx = new int[12];
        cx[0] = MARGIN;
        for (int i = 1; i < 12; i++) cx[i] = cx[i - 1] + cw[i - 1];

        // ── fonts (sized in pixels = pt × DPI/72 so they print at correct pt size) ──
        java.awt.Font base     = loadKannadaPdfFont(Math.round(9f * DPI / 72f));             // 37 px = 9 pt
        java.awt.Font titleFnt = base.deriveFont(java.awt.Font.BOLD,  (float)Math.round(9f  * DPI / 72f));  // 37 px = 9 pt
        java.awt.Font labelFnt = base.deriveFont(java.awt.Font.BOLD,  (float)Math.round(9f  * DPI / 72f));  // 37 px = 9 pt
        java.awt.Font hdrFnt   = base.deriveFont(java.awt.Font.BOLD,  (float)Math.round(8f  * DPI / 72f));  // 33 px = 8 pt
        java.awt.Font dataFnt  = base.deriveFont(java.awt.Font.PLAIN, (float)Math.round(9f  * DPI / 72f));  // 37 px = 9 pt
        java.awt.Font totalFnt = base.deriveFont(java.awt.Font.BOLD,  (float)Math.round(9f  * DPI / 72f));  // 37 px = 9 pt

        java.awt.Color HDR_BG   = new java.awt.Color(0x1a, 0x6f, 0xaf);
        java.awt.Color TOT_BG   = new java.awt.Color(0xd9, 0xe8, 0xf5);
        java.awt.Color BORDER   = new java.awt.Color(0xb0, 0xc4, 0xd8);
        java.awt.Color ALT_BG   = new java.awt.Color(0xea, 0xf4, 0xfb);
        java.awt.Color DARK     = new java.awt.Color(0x1a, 0x1a, 0x1a);

        // ── pre-measure description font for dynamic row heights ──────────────
        java.awt.image.BufferedImage _mImg = new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D _mG = _mImg.createGraphics();
        _mG.setFont(dataFnt);
        java.awt.FontMetrics dataFm = _mG.getFontMetrics();
        _mG.dispose();
        int dataLineH = dataFm.getHeight();
        int descColW  = cw[2] - 8;
        int[] rowHeights = new int[reports.size()];
        for (int k = 0; k < reports.size(); k++) {
            String dk = reports.get(k).getOperationDescription();
            int nl = (dk != null && !dk.isEmpty()) ? wrapTextPdf(dataFm, dk, descColW).size() : 1;
            rowHeights[k] = Math.max(ROW_H, nl * dataLineH + 8);
        }

        // ── render ───────────────────────────────────────────────────────────
        java.awt.image.BufferedImage img =
                new java.awt.image.BufferedImage(W, H, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,      java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g.setRenderingHint(java.awt.RenderingHints.KEY_FRACTIONALMETRICS, java.awt.RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,         java.awt.RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,     java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, W, H);

        int y = MARGIN;

        // ── logo ─────────────────────────────────────────────────────────────
        if (logoImg != null) {
            int logoW = (int)(LOGO_H * (double) logoImg.getWidth() / logoImg.getHeight());
            int logoX = (W - logoW) / 2;
            g.drawImage(logoImg, logoX, y, logoW, LOGO_H, null);
        }
        y += LOGO_H + 8;

        // ── title block ───────────────────────────────────────────────────────
        String fromStr = request.getFromDate() != null
                ? request.getFromDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
        String toStr = request.getToDate() != null
                ? request.getToDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
        String generatedAt = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

        g.setFont(titleFnt);
        g.setColor(new java.awt.Color(0x1a, 0x3c, 0x5e));
        String[] titleLines2 = {
                "ಕರ್ನಾಟಕ ಸರ್ಕಾರ",
                "ರೇಷ್ಮೆ, ಇಲಾಖೆ",
                "ಸರ್ಕಾರಿ ರೇಷ್ಮೆ ಗೂಡಿನ ಮಾರುಕಟ್ಟೆ, " + marketName,
                "ರೇಷ್ಮೆ ನೂಲು ಬಿಚ್ಚಾಣಿಕೆದಾರರ ವಹಿವಾಟು ವರದಿ"
        };
        for (String line : titleLines2) {
            drawCenteredPdf(g, line, MARGIN, y, usable, TITLE_H);
            y += TITLE_H;
        }
        y += Math.round(18f * DPI / 72f);  // gap between title and farmer details

        // ── info block ────────────────────────────────────────────────────────
        g.setFont(labelFnt);
        String nameAndAddress = (wrapper.getName() != null ? wrapper.getName() : "")
                + (wrapper.getAddress() != null ? wrapper.getAddress() : "");
        String[][] infoRows = {
                {"ರೇಷ್ಮೆ ನೂಲು ಬಿಚ್ಚಾಣಿಕೆದಾರರ ರಪದಾರಿ ಸಂಖ್ಯೆ :",
                 request.getLicenseNumber() != null ? request.getLicenseNumber() : "",
                 "FRUITS ID / FID :",
                 wrapper.getFruitsId() != null ? wrapper.getFruitsId() : "",
                 "Date and Time Stamp :", generatedAt},
                {"ರೇಷ್ಮೆ ನೂಲು ಬಿಚ್ಚಾಣಿಕೆದಾರರ ಹೆಸರು ಮತ್ತು ವಿಳಾಸ:",
                 nameAndAddress, "", "", "ಮೊತ್ತ: ರೂ ಗಳಲ್ಲಿ", ""},
                {"ವಹಿವಾಟಿನ ಅವಧಿ :", fromStr + " to " + toStr, "", "", "", ""},
                {"ದಿನಾಂಕ " + fromStr + " ರಂದು ಇದ್ದ ಪ್ರಾರಂಭಿಕ ಉಳಿಕೆ ಮೊತ್ತ :",
                 String.format("%.2f", wrapper.getOpeningBalance() != null ? wrapper.getOpeningBalance() : 0),
                 "", "", "", ""}
        };
        int infoValGap = Math.round(10f * DPI / 72f);  // space between label and its value
        for (String[] row : infoRows) {
            int third = usable / 3;
            int textY = y + INFO_H / 2 + 4;

            // Zone 0 — first label:value pair
            g.setFont(labelFnt); g.setColor(DARK);
            int x0 = MARGIN + 4;
            g.drawString(row[0], x0, textY);
            int lw0 = g.getFontMetrics(labelFnt).stringWidth(row[0]);
            g.setFont(dataFnt); g.setColor(DARK);
            g.drawString(row[1], x0 + lw0 + infoValGap, textY);

            // Zone 1 — second label:value pair
            if (!row[2].isEmpty()) {
                int x2 = MARGIN + third + 4;
                g.setFont(labelFnt); g.setColor(DARK);
                g.drawString(row[2], x2, textY);
                int lw2 = g.getFontMetrics(labelFnt).stringWidth(row[2]);
                g.setFont(dataFnt); g.setColor(DARK);
                g.drawString(row[3], x2 + lw2 + infoValGap, textY);
            }

            // Zone 2 — third label:value pair
            if (!row[4].isEmpty()) {
                int x4 = MARGIN + 2 * third + 4;
                g.setFont(labelFnt); g.setColor(DARK);
                g.drawString(row[4], x4, textY);
                int lw4 = g.getFontMetrics(labelFnt).stringWidth(row[4]);
                g.setFont(dataFnt); g.setColor(DARK);
                g.drawString(row[5], x4 + lw4 + infoValGap, textY);
            }

            g.setColor(BORDER);
            g.drawLine(MARGIN, y + INFO_H, W - MARGIN, y + INFO_H);
            y += INFO_H;
        }
        y += Math.round(12f * DPI / 72f);  // gap between farmer details and table
        g.setStroke(new java.awt.BasicStroke(4f));

        // ── column headers ────────────────────────────────────────────────────
        String[] hdrs = {
                "ಕ್ರಮ ಸಂಖ್ಯೆ\nSL No",
                "ವಹಿವಾಟಿನ ದಿನಾಂಕ\nTransaction Date",
                "ವಿವರ\nDescription",
                "ವಹಿವಾಟಿನ ವಿಧ\nTransaction Type",
                "ಠೇವಣಿ ಮಾಡಿದ ಮೊತ್ತ \nDeposit Amount ",
                "ಖರೀದಿಸಿದ ಬಿತ್ತನೆ ರೇಷ್ಮೆ ಗೂಡಿನ ಪರಿಮಾಣ\nQuantity of seed cocoon purchased (in kg's)",
                "ದರ ಪ್ರತಿ ಕೆ.ಜಿ.ಗೆ \nRate per Kg ",
                "ಖರೀದಿಸಿದ ರೇಷ್ಮೆ ಗೂಡಿನ ಮೊತ್ತ \nCocoons Purchase Amount ",
                "ಮಾರು ಕಟ್ಟೆ ಶುಲ್ಕ @1% \nMarket fee @1% ",
                "ಒಟ್ಟು ಮೊತ್ತ \nTotal ",
                "ಮರುಪಾವತಿಸಿದ ಮೊತ್ತ \nRefunded Amount ",
                "ಉಳಿಕೆ ಮೊತ್ತ \nBalance Amount "
        };
        g.setFont(hdrFnt);
        java.awt.FontMetrics hdrFm = g.getFontMetrics(hdrFnt);
        for (int i = 0; i < 12; i++) {
            g.setColor(HDR_BG);
            g.fillRect(cx[i], y, cw[i], HDR_H);
            g.setColor(BORDER);
            g.drawRect(cx[i], y, cw[i], HDR_H);
            g.setColor(java.awt.Color.WHITE);
            java.util.List<String> wrLines = new java.util.ArrayList<>();
            for (String part : hdrs[i].split("\n"))
                wrLines.addAll(wrapTextPdf(hdrFm, part, cw[i] - 4));
            int lineH = HDR_H / (wrLines.size() + 1);
            java.awt.Shape prevClip = g.getClip();
            g.setClip(cx[i] + 1, y + 1, cw[i] - 2, HDR_H - 2);
            for (int li = 0; li < wrLines.size(); li++)
                drawCenteredPdf(g, wrLines.get(li), cx[i], y + lineH * li, cw[i], lineH);
            g.setClip(prevClip);
        }
        y += HDR_H;

        // ── pagination: split by cumulative row heights ───────────────────────
        int firstPageDataStartY = y;
        int availFirstPage  = H - firstPageDataStartY - ROW_H;
        int availSubseqPage = H - MARGIN - HDR_H - ROW_H;

        java.util.List<java.util.List<ReelerTransactionReport>> pageSlices = new java.util.ArrayList<>();
        {
            java.util.List<ReelerTransactionReport> cur = new java.util.ArrayList<>();
            int usedH = 0;
            boolean onFirstPage = true;
            for (int k = 0; k < reports.size(); k++) {
                int avail = onFirstPage ? availFirstPage : availSubseqPage;
                if (!cur.isEmpty() && usedH + rowHeights[k] > avail) {
                    pageSlices.add(cur);
                    cur = new java.util.ArrayList<>();
                    usedH = 0;
                    onFirstPage = false;
                }
                cur.add(reports.get(k));
                usedH += rowHeights[k];
            }
            if (!cur.isEmpty()) pageSlices.add(cur);
        }

        // page 1 image already has the header; subsequent pages are created below
        java.util.List<java.awt.image.BufferedImage> pageImages = new java.util.ArrayList<>();
        pageImages.add(img);

        int globalSlNo = 1;
        int globalRowIdx = 0;

        for (int pi = 0; pi < pageSlices.size(); pi++) {
            java.util.List<ReelerTransactionReport> slice = pageSlices.get(pi);
            boolean isLastPage  = (pi == pageSlices.size() - 1);
            boolean isFirstPage = (pi == 0);

            java.awt.Graphics2D pg;
            int py;

            if (isFirstPage) {
                pg = g;
                py = firstPageDataStartY;
            } else {
                // new page: white background + column headers only
                java.awt.image.BufferedImage newImg =
                        new java.awt.image.BufferedImage(W, H, java.awt.image.BufferedImage.TYPE_INT_RGB);
                java.awt.Graphics2D ng = newImg.createGraphics();
                ng.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,      java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                ng.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
                ng.setRenderingHint(java.awt.RenderingHints.KEY_FRACTIONALMETRICS, java.awt.RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                ng.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,         java.awt.RenderingHints.VALUE_RENDER_QUALITY);
                ng.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,     java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                ng.setColor(java.awt.Color.WHITE);
                ng.fillRect(0, 0, W, H);
                pg = ng;
                py = MARGIN;
                pg.setStroke(new java.awt.BasicStroke(4f));

                // column headers
                pg.setFont(hdrFnt);
                java.awt.FontMetrics hdrFmN = pg.getFontMetrics(hdrFnt);
                for (int i = 0; i < 12; i++) {
                    pg.setColor(HDR_BG);  pg.fillRect(cx[i], py, cw[i], HDR_H);
                    pg.setColor(BORDER);  pg.drawRect(cx[i], py, cw[i], HDR_H);
                    pg.setColor(java.awt.Color.WHITE);
                    java.util.List<String> wl = new java.util.ArrayList<>();
                    for (String part : hdrs[i].split("\n"))
                        wl.addAll(wrapTextPdf(hdrFmN, part, cw[i] - 4));
                    int lhN = HDR_H / (wl.size() + 1);
                    java.awt.Shape pc2 = pg.getClip();
                    pg.setClip(cx[i] + 1, py + 1, cw[i] - 2, HDR_H - 2);
                    for (int li = 0; li < wl.size(); li++)
                        drawCenteredPdf(pg, wl.get(li), cx[i], py + lhN * li, cw[i], lhN);
                    pg.setClip(pc2);
                }
                py += HDR_H;
                pageImages.add(newImg);
            }

            // ── data rows for this page ───────────────────────────────────────
            for (ReelerTransactionReport rep : slice) {
                int curRowH = rowHeights[globalRowIdx++];
                boolean alt = (globalSlNo % 2 == 0);
                pg.setColor(alt ? ALT_BG : java.awt.Color.WHITE);
                pg.fillRect(MARGIN, py, usable, curRowH);
                pg.setColor(BORDER);
                pg.drawRect(MARGIN, py, usable, curRowH);
                for (int i = 1; i < 12; i++) pg.drawLine(cx[i], py, cx[i], py + curRowH);
                pg.setFont(dataFnt);
                pg.setColor(DARK);
                String[] vals = {
                        String.valueOf(globalSlNo++),
                        rep.getTransactionDate() != null ? rep.getTransactionDate().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy")) : "",
                        rep.getOperationDescription() != null ? rep.getOperationDescription() : "",
                        rep.getTransactionType() != null ? rep.getTransactionType() : "",
                        fmt(rep.getDepositAmount()),
                        fmt(rep.getLotWeight()),
                        fmt(rep.getRatePerKg()),
                        fmt(rep.getPaymentAmount()),
                        fmt(rep.getMarketFee()),
                        fmt(rep.getTotal()),
                        fmt(rep.getRefundedAmount()),
                        fmt(rep.getBalance())
                };
                for (int i = 0; i < 12; i++) {
                    java.awt.Shape prevClip = pg.getClip();
                    pg.setClip(cx[i] + 1, py + 1, cw[i] - 2, curRowH - 2);
                    if (i == 2) {
                        java.util.List<String> dLines = wrapTextPdf(dataFm, vals[2], descColW);
                        int totalTH = dLines.size() * dataLineH;
                        int textY = py + Math.max(4, (curRowH - totalTH) / 2);
                        for (String dl : dLines) {
                            pg.drawString(dl, cx[2] + 4, textY + dataFm.getAscent());
                            textY += dataLineH;
                        }
                    } else {
                        drawCenteredPdf(pg, vals[i], cx[i], py, cw[i], curRowH);
                    }
                    pg.setClip(prevClip);
                }
                py += curRowH;
            }

            // ── totals row on last page only ──────────────────────────────────
            if (isLastPage) {
                pg.setColor(TOT_BG);
                pg.fillRect(MARGIN, py, usable, ROW_H);
                pg.setColor(BORDER);
                pg.drawRect(MARGIN, py, usable, ROW_H);
                for (int i = 1; i < 12; i++) pg.drawLine(cx[i], py, cx[i], py + ROW_H);
                pg.setFont(totalFnt);
                pg.setColor(DARK);
                drawCenteredPdf(pg, "ಒಟ್ಟು / Total",
                        cx[0], py, cw[0] + cw[1] + cw[2] + cw[3], ROW_H);
                drawCenteredPdf(pg, fmt2(wrapper.getTotalDeposits()),       cx[4], py, cw[4], ROW_H);
                drawCenteredPdf(pg, fmt2(wrapper.getTotalLotWeight()),      cx[5], py, cw[5], ROW_H);
                drawCenteredPdf(pg, "",                                     cx[6], py, cw[6], ROW_H);
                drawCenteredPdf(pg, fmt2(wrapper.getTotalPaymentAmount()),  cx[7], py, cw[7], ROW_H);
                drawCenteredPdf(pg, fmt2(wrapper.getTotalMarketFee()),      cx[8], py, cw[8], ROW_H);
                drawCenteredPdf(pg, fmt2(wrapper.getTotalPurchase()),       cx[9], py, cw[9], ROW_H);
                drawCenteredPdf(pg, fmt2(wrapper.getTotalBankTransfers()),  cx[10], py, cw[10], ROW_H);
                drawCenteredPdf(pg, fmt2(wrapper.getClosingBalance()),      cx[11], py, cw[11], ROW_H);
            }

            if (!isFirstPage) pg.dispose();
        }

        g.dispose();

        // ── embed in PDFBox (one page per image) ──────────────────────────────
        java.io.File pdfFile = java.io.File.createTempFile("Seed_Market_Txn_Report_", ".pdf");
        try (org.apache.pdfbox.pdmodel.PDDocument doc = new org.apache.pdfbox.pdmodel.PDDocument()) {
            org.apache.pdfbox.pdmodel.common.PDRectangle rect =
                    new org.apache.pdfbox.pdmodel.common.PDRectangle(PDF_W, PDF_H);
            for (java.awt.image.BufferedImage pageImg : pageImages) {
                org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage(rect);
                doc.addPage(page);
                org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject pdImg =
                        org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory.createFromImage(doc, pageImg);
                try (org.apache.pdfbox.pdmodel.PDPageContentStream cs =
                             new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page)) {
                    cs.drawImage(pdImg, 0, 0, rect.getWidth(), rect.getHeight());
                }
            }
            doc.save(pdfFile);
        }
        return new java.io.FileInputStream(pdfFile);
    }

    // ── Egg Producer Transaction Report ──────────────────────────────────────

    public FileInputStream downloadEggProducerTxnReport(ReelerTxnReportRequest request) throws Exception {

        ReelerTransactionReportWrapper wrapper = getSeedMarketTxnReport(
                request.getMarketId(), request.getLicenseNumber(),
                request.getFromDate(), request.getToDate(),
                request.getBuyerType());

        MarketMaster marketMaster = marketMasterRepository.findById(request.getMarketId());
        String marketName = (marketMaster != null && marketMaster.getName() != null)
                ? marketMaster.getName() : "";

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Egg Producer Transaction Report");
        final int COLS = 12;

        // ── Number formats ────────────────────────────────────────────────────
        DataFormat epDataFmt = workbook.createDataFormat();
        short epNumFmt2dp = epDataFmt.getFormat("#,##0.00");

        // ── Fonts ─────────────────────────────────────────────────────────────
        Font epFTitle = workbook.createFont(); epFTitle.setBold(true); epFTitle.setFontHeightInPoints((short) 14);
        Font epFSubTitle = workbook.createFont(); epFSubTitle.setBold(true); epFSubTitle.setFontHeightInPoints((short) 12);
        Font epFLabel = workbook.createFont(); epFLabel.setBold(true); epFLabel.setFontHeightInPoints((short) 9);
        Font epFValue = workbook.createFont(); epFValue.setFontHeightInPoints((short) 9);
        Font epFHdr = workbook.createFont(); epFHdr.setBold(true); epFHdr.setFontHeightInPoints((short) 9); epFHdr.setColor(IndexedColors.WHITE.getIndex());
        Font epFTotal = workbook.createFont(); epFTotal.setBold(true); epFTotal.setFontHeightInPoints((short) 9);

        // ── Styles ────────────────────────────────────────────────────────────
        CellStyle epSTitle = workbook.createCellStyle();
        epSTitle.setFont(epFTitle); epSTitle.setAlignment(HorizontalAlignment.CENTER);
        epSTitle.setVerticalAlignment(VerticalAlignment.CENTER); epSTitle.setWrapText(true);

        CellStyle epSSubTitle = workbook.createCellStyle();
        epSSubTitle.setFont(epFSubTitle); epSSubTitle.setAlignment(HorizontalAlignment.CENTER);
        epSSubTitle.setVerticalAlignment(VerticalAlignment.CENTER); epSSubTitle.setWrapText(true);

        CellStyle epSIL = workbook.createCellStyle();
        epSIL.setFont(epFLabel); epSIL.setWrapText(true); epSIL.setVerticalAlignment(VerticalAlignment.CENTER);
        epSIL.setFillForegroundColor(IndexedColors.LEMON_CHIFFON.getIndex()); epSIL.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        epSIL.setBorderTop(BorderStyle.THIN); epSIL.setBorderBottom(BorderStyle.THIN); epSIL.setBorderLeft(BorderStyle.THIN); epSIL.setBorderRight(BorderStyle.THIN);

        CellStyle epSIV = workbook.createCellStyle();
        epSIV.setFont(epFValue); epSIV.setWrapText(true); epSIV.setVerticalAlignment(VerticalAlignment.CENTER);
        epSIV.setBorderTop(BorderStyle.THIN); epSIV.setBorderBottom(BorderStyle.THIN); epSIV.setBorderLeft(BorderStyle.THIN); epSIV.setBorderRight(BorderStyle.THIN);

        CellStyle epSIN = workbook.createCellStyle();
        epSIN.setFont(epFValue); epSIN.setAlignment(HorizontalAlignment.LEFT); epSIN.setVerticalAlignment(VerticalAlignment.CENTER);
        epSIN.setDataFormat(epNumFmt2dp);
        epSIN.setBorderTop(BorderStyle.THIN); epSIN.setBorderBottom(BorderStyle.THIN); epSIN.setBorderLeft(BorderStyle.THIN); epSIN.setBorderRight(BorderStyle.THIN);

        CellStyle epSColHdr = workbook.createCellStyle();
        epSColHdr.setFont(epFHdr); epSColHdr.setAlignment(HorizontalAlignment.CENTER);
        epSColHdr.setVerticalAlignment(VerticalAlignment.CENTER); epSColHdr.setWrapText(true);
        epSColHdr.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex()); epSColHdr.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        epSColHdr.setBorderTop(BorderStyle.THIN); epSColHdr.setBorderBottom(BorderStyle.THIN); epSColHdr.setBorderLeft(BorderStyle.THIN); epSColHdr.setBorderRight(BorderStyle.THIN);

        CellStyle epSData = workbook.createCellStyle();
        epSData.setFont(epFValue); epSData.setAlignment(HorizontalAlignment.CENTER);
        epSData.setVerticalAlignment(VerticalAlignment.CENTER); epSData.setWrapText(true);
        epSData.setBorderTop(BorderStyle.THIN); epSData.setBorderBottom(BorderStyle.THIN); epSData.setBorderLeft(BorderStyle.THIN); epSData.setBorderRight(BorderStyle.THIN);

        CellStyle epSDesc = workbook.createCellStyle();
        epSDesc.setFont(epFValue); epSDesc.setAlignment(HorizontalAlignment.LEFT);
        epSDesc.setVerticalAlignment(VerticalAlignment.CENTER); epSDesc.setWrapText(true);
        epSDesc.setBorderTop(BorderStyle.THIN); epSDesc.setBorderBottom(BorderStyle.THIN); epSDesc.setBorderLeft(BorderStyle.THIN); epSDesc.setBorderRight(BorderStyle.THIN);

        CellStyle epSNum = workbook.createCellStyle();
        epSNum.setFont(epFValue); epSNum.setAlignment(HorizontalAlignment.CENTER);
        epSNum.setVerticalAlignment(VerticalAlignment.CENTER); epSNum.setDataFormat(epNumFmt2dp);
        epSNum.setBorderTop(BorderStyle.THIN); epSNum.setBorderBottom(BorderStyle.THIN); epSNum.setBorderLeft(BorderStyle.THIN); epSNum.setBorderRight(BorderStyle.THIN);

        CellStyle epSTotalL = workbook.createCellStyle();
        epSTotalL.setFont(epFTotal); epSTotalL.setAlignment(HorizontalAlignment.CENTER); epSTotalL.setVerticalAlignment(VerticalAlignment.CENTER);
        epSTotalL.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex()); epSTotalL.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        epSTotalL.setBorderTop(BorderStyle.MEDIUM); epSTotalL.setBorderBottom(BorderStyle.MEDIUM); epSTotalL.setBorderLeft(BorderStyle.THIN); epSTotalL.setBorderRight(BorderStyle.THIN);

        CellStyle epSTotalN = workbook.createCellStyle();
        epSTotalN.setFont(epFTotal); epSTotalN.setAlignment(HorizontalAlignment.CENTER);
        epSTotalN.setVerticalAlignment(VerticalAlignment.CENTER); epSTotalN.setDataFormat(epNumFmt2dp);
        epSTotalN.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex()); epSTotalN.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        epSTotalN.setBorderTop(BorderStyle.MEDIUM); epSTotalN.setBorderBottom(BorderStyle.MEDIUM); epSTotalN.setBorderLeft(BorderStyle.THIN); epSTotalN.setBorderRight(BorderStyle.THIN);

        // ── Rows 0-1: Logo centered cols 4-7 ─────────────────────────────────
        sheet.createRow(0).setHeightInPoints(45);
        sheet.createRow(1).setHeightInPoints(45);
        try {
            org.springframework.core.io.ClassPathResource logoRes =
                    new org.springframework.core.io.ClassPathResource("images/kar_logo.png");
            byte[] logoBytes = logoRes.getInputStream().readAllBytes();
            int picIdx = workbook.addPicture(logoBytes, org.apache.poi.ss.usermodel.Workbook.PICTURE_TYPE_PNG);
            org.apache.poi.ss.usermodel.Drawing<?> drawing = sheet.createDrawingPatriarch();
            org.apache.poi.ss.usermodel.ClientAnchor anchor = workbook.getCreationHelper().createClientAnchor();
            anchor.setCol1(4); anchor.setRow1(0); anchor.setCol2(7); anchor.setRow2(2);
            anchor.setAnchorType(org.apache.poi.ss.usermodel.ClientAnchor.AnchorType.MOVE_AND_RESIZE);
            drawing.createPicture(anchor, picIdx);
        } catch (Exception ignored) {}

        // ── Rows 2-5: Title section ───────────────────────────────────────────
        int epRowIdx = 2;
        Row ep0 = sheet.createRow(epRowIdx++); ep0.setHeightInPoints(28);
        Cell epc0 = ep0.createCell(0); epc0.setCellValue("ಕರ್ನಾಟಕ ಸರ್ಕಾರ"); epc0.setCellStyle(epSTitle);
        for (int i = 1; i < COLS; i++) ep0.createCell(i).setCellStyle(epSTitle);
        sheet.addMergedRegion(new CellRangeAddress(ep0.getRowNum(), ep0.getRowNum(), 0, COLS - 1));

        Row ep1 = sheet.createRow(epRowIdx++); ep1.setHeightInPoints(24);
        Cell epc1 = ep1.createCell(0); epc1.setCellValue("ರೇಷ್ಮೆ, ಇಲಾಖೆ"); epc1.setCellStyle(epSSubTitle);
        for (int i = 1; i < COLS; i++) ep1.createCell(i).setCellStyle(epSSubTitle);
        sheet.addMergedRegion(new CellRangeAddress(ep1.getRowNum(), ep1.getRowNum(), 0, COLS - 1));

        Row ep2 = sheet.createRow(epRowIdx++); ep2.setHeightInPoints(22);
        Cell epc2 = ep2.createCell(0); epc2.setCellValue("ಸರ್ಕಾರಿ ರೇಷ್ಮೆ ಗೂಡಿನ ಮಾರುಕಟ್ಟೆ, " + marketName); epc2.setCellStyle(epSSubTitle);
        for (int i = 1; i < COLS; i++) ep2.createCell(i).setCellStyle(epSSubTitle);
        sheet.addMergedRegion(new CellRangeAddress(ep2.getRowNum(), ep2.getRowNum(), 0, COLS - 1));

        Row ep3 = sheet.createRow(epRowIdx++); ep3.setHeightInPoints(22);
        Cell epc3 = ep3.createCell(0); epc3.setCellValue("ನೊಂದಾಯಿತ ರೇಷ್ಮೆ ಮೊಟ್ಟೆ ಉತ್ಪಾದಕರ ವಹಿವಾಟು ವರದಿ"); epc3.setCellStyle(epSSubTitle);
        for (int i = 1; i < COLS; i++) ep3.createCell(i).setCellStyle(epSSubTitle);
        sheet.addMergedRegion(new CellRangeAddress(ep3.getRowNum(), ep3.getRowNum(), 0, COLS - 1));

        // ── Row 6: Spacer ─────────────────────────────────────────────────────
        sheet.createRow(epRowIdx++).setHeightInPoints(6);

        // ── Date strings ──────────────────────────────────────────────────────
        String epTimestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
        String epFrom = request.getFromDate() != null
                ? request.getFromDate().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy")) : "";
        String epTo = request.getToDate() != null
                ? request.getToDate().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy")) : "";
        String epNameAddr = (wrapper.getName() != null ? wrapper.getName() : "")
                + (wrapper.getAddress() != null && !wrapper.getAddress().isEmpty() ? ", " + wrapper.getAddress() : "");

        // ── Rows 7-10: Info block (4 rows) ───────────────────────────────────

        // Row 7: Registration label [0-3] | value [4-5] | Date label [6-7] | Timestamp [8-10]
        Row epI1 = sheet.createRow(epRowIdx++); epI1.setHeightInPoints(28);
        epI1.createCell(0).setCellValue("ನೊಂದಾಯಿತ ರೇಷ್ಮೆ ಮೊಟ್ಟೆ ಉತ್ಪಾದಕರ ನೊಂದಣಿ ಸಂಖ್ಯೆ:"); epI1.getCell(0).setCellStyle(epSIL);
        epI1.createCell(1).setCellStyle(epSIL); epI1.createCell(2).setCellStyle(epSIL); epI1.createCell(3).setCellStyle(epSIL);
        sheet.addMergedRegion(new CellRangeAddress(epI1.getRowNum(), epI1.getRowNum(), 0, 3));
        epI1.createCell(4).setCellValue(request.getLicenseNumber() != null ? request.getLicenseNumber() : ""); epI1.getCell(4).setCellStyle(epSIV);
        epI1.createCell(5).setCellStyle(epSIV);
        sheet.addMergedRegion(new CellRangeAddress(epI1.getRowNum(), epI1.getRowNum(), 4, 5));
        epI1.createCell(6).setCellValue("Date and Time Stamp:"); epI1.getCell(6).setCellStyle(epSIL);
        epI1.createCell(7).setCellStyle(epSIL);
        sheet.addMergedRegion(new CellRangeAddress(epI1.getRowNum(), epI1.getRowNum(), 6, 7));
        epI1.createCell(8).setCellValue(epTimestamp); epI1.getCell(8).setCellStyle(epSIV);
        epI1.createCell(9).setCellStyle(epSIV); epI1.createCell(10).setCellStyle(epSIV);
        sheet.addMergedRegion(new CellRangeAddress(epI1.getRowNum(), epI1.getRowNum(), 8, 10));

        // Row 9: Name label [0-3] | Name/Address value [4-10]
        Row epI2 = sheet.createRow(epRowIdx++); epI2.setHeightInPoints(28);
        epI2.createCell(0).setCellValue("ನೊಂದಾಯಿತ ರೇಷ್ಮೆ ಮೊಟ್ಟೆ ಉತ್ಪಾದಕರ ಹೆಸರು ಮತ್ತು ವಿಳಾಸ:"); epI2.getCell(0).setCellStyle(epSIL);
        epI2.createCell(1).setCellStyle(epSIL); epI2.createCell(2).setCellStyle(epSIL); epI2.createCell(3).setCellStyle(epSIL);
        sheet.addMergedRegion(new CellRangeAddress(epI2.getRowNum(), epI2.getRowNum(), 0, 3));
        epI2.createCell(4).setCellValue(epNameAddr); epI2.getCell(4).setCellStyle(epSIV);
        for (int i = 5; i <= 10; i++) epI2.createCell(i).setCellStyle(epSIV);
        sheet.addMergedRegion(new CellRangeAddress(epI2.getRowNum(), epI2.getRowNum(), 4, 10));

        // Row 10: Period label [0-3] | Period value [4-10]
        Row epI3 = sheet.createRow(epRowIdx++); epI3.setHeightInPoints(22);
        epI3.createCell(0).setCellValue("ವಹಿವಾಟಿನ ಅವಧಿ:"); epI3.getCell(0).setCellStyle(epSIL);
        epI3.createCell(1).setCellStyle(epSIL); epI3.createCell(2).setCellStyle(epSIL); epI3.createCell(3).setCellStyle(epSIL);
        sheet.addMergedRegion(new CellRangeAddress(epI3.getRowNum(), epI3.getRowNum(), 0, 3));
        epI3.createCell(4).setCellValue(epFrom + " to " + epTo); epI3.getCell(4).setCellStyle(epSIV);
        for (int i = 5; i <= 10; i++) epI3.createCell(i).setCellStyle(epSIV);
        sheet.addMergedRegion(new CellRangeAddress(epI3.getRowNum(), epI3.getRowNum(), 4, 10));

        // Row 11: Balance label [0-3] | Balance value [4-5] | empty [6-7] | ಮೊತ್ತ: ರೂ ಗಳಲ್ಲಿ [8-10]
        Row epI4 = sheet.createRow(epRowIdx++); epI4.setHeightInPoints(25);
        epI4.createCell(0).setCellValue("ದಿನಾಂಕ " + epFrom + " ರಂದು ಇದ್ದ ಪ್ರಾರಂಭಿಕ ಉಳಿಕೆ ಮೊತ್ತ:"); epI4.getCell(0).setCellStyle(epSIL);
        epI4.createCell(1).setCellStyle(epSIL); epI4.createCell(2).setCellStyle(epSIL); epI4.createCell(3).setCellStyle(epSIL);
        sheet.addMergedRegion(new CellRangeAddress(epI4.getRowNum(), epI4.getRowNum(), 0, 3));
        epI4.createCell(4).setCellValue(wrapper.getOpeningBalance() != null ? wrapper.getOpeningBalance() : 0); epI4.getCell(4).setCellStyle(epSIN);
        epI4.createCell(5).setCellStyle(epSIN);
        sheet.addMergedRegion(new CellRangeAddress(epI4.getRowNum(), epI4.getRowNum(), 4, 5));
        epI4.createCell(6).setCellStyle(epSIV); epI4.createCell(7).setCellStyle(epSIV);
        sheet.addMergedRegion(new CellRangeAddress(epI4.getRowNum(), epI4.getRowNum(), 6, 7));
        epI4.createCell(8).setCellValue("ಮೊತ್ತ: ರೂ ಗಳಲ್ಲಿ"); epI4.getCell(8).setCellStyle(epSIL);
        epI4.createCell(9).setCellStyle(epSIL); epI4.createCell(10).setCellStyle(epSIL);
        sheet.addMergedRegion(new CellRangeAddress(epI4.getRowNum(), epI4.getRowNum(), 8, 10));

        // ── Row 12: Spacer ────────────────────────────────────────────────────
        sheet.createRow(epRowIdx++).setHeightInPoints(6);

        // ── Row 13: Column headers ────────────────────────────────────────────
        Row epColHdr = sheet.createRow(epRowIdx++); epColHdr.setHeightInPoints(85);
        String[] epHeaders = {
                "ಕ್ರಮ ಸಂಖ್ಯೆ\nSL No",
                "ವಹಿವಾಟಿನ ದಿನಾಂಕ\nTransaction Date",
                "ವಿವರ\nDescription",
                "ವಹಿವಾಟಿನ ವಿಧ\nTransaction Type",
                "ಠೇವಣಿ ಮಾಡಿದ ಮೊತ್ತ \nDeposit Amount ",
                "ಖರೀದಿಸಿದ ಬಿತ್ತನೆ ರೇಷ್ಮೆ ಗೂಡಿನ ಪರಿಮಾಣ\nQuantity of Seed Cocoon Purchased (Kg)",
                "ಬಿತ್ತನೆ ಗೂಡಿನ ಸಂಖ್ಯೆ (ಪ್ರತಿ ಕೆ.ಜಿಗೆ)\nSeed Cocoons per Kg",
                "ಒಟ್ಟು ಬಿತ್ತನೆ ಗೂಡಿನ ಸಂಖ್ಯೆ\nTotal Seed Cocoons (Nos)",
                "ದರ ಪ್ರತಿ ಕೆ.ಜಿ.ಗೆ \nRate per Kg ",
                "ಖರೀದಿಸಿದ ರೇಷ್ಮೆ ಗೂಡಿನ ಮೊತ್ತ \nSeed Cocoon Purchase Amount ",
                "ಮರುಪಾವತಿಸಿದ ಮೊತ್ತ \nRefunded Amount ",
                "ಉಳಿಕೆ ಮೊತ್ತ \nBalance Amount "
        };
        for (int i = 0; i < epHeaders.length; i++) {
            Cell c = epColHdr.createCell(i); c.setCellValue(epHeaders[i]); c.setCellStyle(epSColHdr);
        }

        // ── Data rows ─────────────────────────────────────────────────────────
        int epSlNo = 1;
        double epSumDeposit = 0, epSumLotWeight = 0, epSumTotalCocoons = 0, epSumPayment = 0, epSumRefunded = 0;

        for (ReelerTransactionReport report : wrapper.getReelerTransactionReports()) {
            Row row = sheet.createRow(epRowIdx++); row.setHeightInPoints(20);
            Cell d0 = row.createCell(0); d0.setCellValue(epSlNo++); d0.setCellStyle(epSData);
            Cell d1 = row.createCell(1);
            d1.setCellValue(report.getTransactionDate() != null ? report.getTransactionDate().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy")) : "");
            d1.setCellStyle(epSData);
            Cell d2 = row.createCell(2); d2.setCellValue(report.getOperationDescription() != null ? report.getOperationDescription() : ""); d2.setCellStyle(epSDesc);
            Cell d3 = row.createCell(3); d3.setCellValue(report.getTransactionType() != null ? report.getTransactionType() : ""); d3.setCellStyle(epSData);
            double depositVal = report.getDepositAmount() != null ? report.getDepositAmount() : 0;
            Cell d4 = row.createCell(4); d4.setCellValue(depositVal); d4.setCellStyle(epSNum); epSumDeposit += depositVal;
            double lwVal = report.getLotWeight() != null ? report.getLotWeight() : 0;
            Cell d5 = row.createCell(5); d5.setCellValue(lwVal); d5.setCellStyle(epSNum); epSumLotWeight += lwVal;
            int qtyNosVal = report.getQtyNos() != null ? report.getQtyNos() : 0;
            Cell d6 = row.createCell(6); d6.setCellValue(qtyNosVal); d6.setCellStyle(epSData);
            double totalCocoons = lwVal * qtyNosVal;
            Cell d7 = row.createCell(7); d7.setCellValue(totalCocoons); d7.setCellStyle(epSNum); epSumTotalCocoons += totalCocoons;
            Cell d8 = row.createCell(8); d8.setCellValue(report.getRatePerKg() != null ? report.getRatePerKg() : 0); d8.setCellStyle(epSNum);
            double payVal = report.getPaymentAmount() != null ? report.getPaymentAmount() : 0;
            Cell d9 = row.createCell(9); d9.setCellValue(payVal); d9.setCellStyle(epSNum); epSumPayment += payVal;
            double refundedVal = report.getRefundedAmount() != null ? report.getRefundedAmount() : 0;
            Cell d10 = row.createCell(10); d10.setCellValue(refundedVal); d10.setCellStyle(epSNum); epSumRefunded += refundedVal;
            double balanceVal = report.getBalance() != null ? report.getBalance() : 0;
            Cell d11 = row.createCell(11); d11.setCellValue(balanceVal); d11.setCellStyle(epSNum);
        }

        // ── Totals row ────────────────────────────────────────────────────────
        Row epTotal = sheet.createRow(epRowIdx++); epTotal.setHeightInPoints(22);
        Cell etc0 = epTotal.createCell(0); etc0.setCellValue("ಒಟ್ಟು / Total"); etc0.setCellStyle(epSTotalL);
        for (int i = 1; i <= 3; i++) epTotal.createCell(i).setCellStyle(epSTotalL);
        sheet.addMergedRegion(new CellRangeAddress(epTotal.getRowNum(), epTotal.getRowNum(), 0, 3));
        Cell etc4 = epTotal.createCell(4); etc4.setCellValue(epSumDeposit); etc4.setCellStyle(epSTotalN);
        Cell etc5 = epTotal.createCell(5); etc5.setCellValue(epSumLotWeight); etc5.setCellStyle(epSTotalN);
        Cell etc6 = epTotal.createCell(6); etc6.setCellValue(""); etc6.setCellStyle(epSTotalL);
        Cell etc7 = epTotal.createCell(7); etc7.setCellValue(epSumTotalCocoons); etc7.setCellStyle(epSTotalN);
        Cell etc8 = epTotal.createCell(8); etc8.setCellValue(""); etc8.setCellStyle(epSTotalL);
        Cell etc9 = epTotal.createCell(9); etc9.setCellValue(epSumPayment); etc9.setCellStyle(epSTotalN);
        Cell etc10 = epTotal.createCell(10); etc10.setCellValue(epSumRefunded); etc10.setCellStyle(epSTotalN);
        Cell etc11 = epTotal.createCell(11); etc11.setCellValue(wrapper.getClosingBalance() != null ? wrapper.getClosingBalance() : 0); etc11.setCellStyle(epSTotalN);

        // ── Column widths (same as Seed Market, total 34 500 POI) ────────────
        int[] epColWidths = { 1200, 3200, 5500, 3000, 3200, 4000, 2800, 3200, 3000, 3000, 3000, 3200 };
        for (int i = 0; i < COLS; i++) sheet.setColumnWidth(i, epColWidths[i]);

        // ── Print: A4 landscape, fit 1 page wide ─────────────────────────────
        sheet.setMargin(org.apache.poi.ss.usermodel.Sheet.LeftMargin,   0.4);
        sheet.setMargin(org.apache.poi.ss.usermodel.Sheet.RightMargin,  0.4);
        sheet.setMargin(org.apache.poi.ss.usermodel.Sheet.TopMargin,    0.4);
        sheet.setMargin(org.apache.poi.ss.usermodel.Sheet.BottomMargin, 0.4);
        sheet.getPrintSetup().setLandscape(true);
        sheet.getPrintSetup().setPaperSize(org.apache.poi.ss.usermodel.PrintSetup.A4_PAPERSIZE);
        sheet.setFitToPage(true);
        sheet.getPrintSetup().setFitWidth((short) 1);
        sheet.getPrintSetup().setFitHeight((short) 0);
        sheet.setAutobreaks(true);

        File epFile = File.createTempFile("Egg_Producer_Transaction_Report_", ".xlsx");
        FileOutputStream epFos = new FileOutputStream(epFile);
        workbook.write(epFos);
        workbook.close();
        epFos.close();
        return new FileInputStream(epFile);
    }

    public java.io.FileInputStream downloadEggProducerTxnReportPDF(ReelerTxnReportRequest request) throws Exception {

        ReelerTransactionReportWrapper wrapper = getSeedMarketTxnReport(
                request.getMarketId(), request.getLicenseNumber(),
                request.getFromDate(), request.getToDate(),
                request.getBuyerType());

        MarketMaster marketMaster = marketMasterRepository.findById(request.getMarketId());
        String marketName = (marketMaster != null && marketMaster.getName() != null)
                ? marketMaster.getName() : "";

        java.util.List<ReelerTransactionReport> reports =
                wrapper.getReelerTransactionReports() != null
                        ? wrapper.getReelerTransactionReports()
                        : java.util.Collections.emptyList();

        // ── dimensions (fixed A4 landscape at 300 DPI) ───────────────────────
        int DPI    = 300;
        float PDF_W = 842f;                              // A4 landscape width  (pts)
        float PDF_H = 595f;                              // A4 landscape height (pts)
        int W      = Math.round(PDF_W * DPI / 72f);     // 3508 px
        int H      = Math.round(PDF_H * DPI / 72f);     // 2479 px  (fixed – no gaps when printed)
        int MARGIN  = Math.round(15f * DPI / 72f);      // 63 px  = 15 pt
        int LOGO_H  = Math.round(40f * DPI / 72f);      // 167 px = 40 pt
        int INFO_H  = Math.round(15f * DPI / 72f);      // 63 px  = 15 pt
        int HDR_H   = Math.round(55f * DPI / 72f);      // 229 px = 55 pt
        int ROW_H   = Math.round(15f * DPI / 72f);      // 63 px  = 15 pt
        int TITLE_H = Math.round(11f * DPI / 72f);      // 46 px  = 11 pt

        java.awt.image.BufferedImage logoImg = null;
        try {
            org.springframework.core.io.ClassPathResource logoRes =
                    new org.springframework.core.io.ClassPathResource("images/kar_logo.png");
            logoImg = javax.imageio.ImageIO.read(logoRes.getInputStream());
        } catch (Exception ignored) {}

        int usable = W - 2 * MARGIN;
        float[] proportions = {0.04f, 0.06f, 0.13f, 0.08f, 0.08f, 0.10f, 0.06f, 0.09f, 0.07f, 0.08f, 0.07f, 0.14f};
        int[] cw = new int[12];
        int allocated = 0;
        for (int i = 0; i < 11; i++) { cw[i] = (int)(usable * proportions[i]); allocated += cw[i]; }
        cw[11] = usable - allocated;
        int[] cx = new int[12];
        cx[0] = MARGIN;
        for (int i = 1; i < 12; i++) cx[i] = cx[i - 1] + cw[i - 1];

        // ── fonts (sized in pixels = pt × DPI/72 so they print at correct pt size) ──
        java.awt.Font base     = loadKannadaPdfFont(Math.round(10f * DPI / 72f));            // 42 px = 10 pt
        java.awt.Font titleFnt = base.deriveFont(java.awt.Font.BOLD,  (float)Math.round(10f * DPI / 72f));  // 42 px = 10 pt
        java.awt.Font labelFnt = base.deriveFont(java.awt.Font.BOLD,  (float)Math.round(10f * DPI / 72f));  // 42 px = 10 pt
        java.awt.Font hdrFnt   = base.deriveFont(java.awt.Font.BOLD,  (float)Math.round(9f  * DPI / 72f));  // 37 px = 9 pt
        java.awt.Font dataFnt  = base.deriveFont(java.awt.Font.PLAIN, (float)Math.round(10f * DPI / 72f));  // 42 px = 10 pt
        java.awt.Font totalFnt = base.deriveFont(java.awt.Font.BOLD,  (float)Math.round(10f * DPI / 72f));  // 42 px = 10 pt

        java.awt.Color HDR_BG = new java.awt.Color(0x1a, 0x6f, 0xaf);
        java.awt.Color TOT_BG = new java.awt.Color(0xd9, 0xe8, 0xf5);
        java.awt.Color BORDER = new java.awt.Color(0xb0, 0xc4, 0xd8);
        java.awt.Color ALT_BG = new java.awt.Color(0xea, 0xf4, 0xfb);
        java.awt.Color DARK   = new java.awt.Color(0x1a, 0x1a, 0x1a);

        // ── pre-measure description font for dynamic row heights ──────────────
        java.awt.image.BufferedImage _mImg = new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D _mG = _mImg.createGraphics();
        _mG.setFont(dataFnt);
        java.awt.FontMetrics dataFm = _mG.getFontMetrics();
        _mG.dispose();
        int dataLineH = dataFm.getHeight();
        int descColW  = cw[2] - 8;
        int[] rowHeights = new int[reports.size()];
        for (int k = 0; k < reports.size(); k++) {
            String dk = reports.get(k).getOperationDescription();
            int nl = (dk != null && !dk.isEmpty()) ? wrapTextPdf(dataFm, dk, descColW).size() : 1;
            rowHeights[k] = Math.max(ROW_H, nl * dataLineH + 8);
        }

        java.awt.image.BufferedImage img =
                new java.awt.image.BufferedImage(W, H, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,      java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g.setRenderingHint(java.awt.RenderingHints.KEY_FRACTIONALMETRICS, java.awt.RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,         java.awt.RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,     java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, W, H);

        int y = MARGIN;

        if (logoImg != null) {
            int logoW = (int)(LOGO_H * (double) logoImg.getWidth() / logoImg.getHeight());
            int logoX = (W - logoW) / 2;
            g.drawImage(logoImg, logoX, y, logoW, LOGO_H, null);
        }
        y += LOGO_H + 8;

        String epFromStr = request.getFromDate() != null
                ? request.getFromDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
        String epToStr = request.getToDate() != null
                ? request.getToDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
        String epGenAt = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

        g.setFont(titleFnt);
        g.setColor(new java.awt.Color(0x1a, 0x3c, 0x5e));
        for (String line : new String[]{
                "ಕರ್ನಾಟಕ ಸರ್ಕಾರ",
                "ರೇಷ್ಮೆ, ಇಲಾಖೆ",
                "ಸರ್ಕಾರಿ ರೇಷ್ಮೆ ಗೂಡಿನ ಮಾರುಕಟ್ಟೆ, " + marketName,
                "ನೊಂದಾಯಿತ ರೇಷ್ಮೆ ಮೊಟ್ಟೆ ಉತ್ಪಾದಕರ ವಹಿವಾಟು ವರದಿ"
        }) {
            drawCenteredPdf(g, line, MARGIN, y, usable, TITLE_H);
            y += TITLE_H;
        }
        y += Math.round(18f * DPI / 72f);  // gap between title and farmer details

        g.setFont(labelFnt);
        String epNameAddr = (wrapper.getName() != null ? wrapper.getName() : "")
                + (wrapper.getAddress() != null ? wrapper.getAddress() : "");
        String[][] epInfoRows = {
                {"ನೊಂದಾಯಿತ ರೇಷ್ಮೆ ಮೊಟ್ಟೆ ಉತ್ಪಾದಕರ ನೊಂದಣಿ ಸಂಖ್ಯೆ :",
                 request.getLicenseNumber() != null ? request.getLicenseNumber() : "",
                 "", "", "Date and Time Stamp :", epGenAt},
                {"ನೊಂದಾಯಿತ ರೇಷ್ಮೆ ಮೊಟ್ಟೆ ಉತ್ಪಾದಕರ ಹೆಸರು ಮತ್ತು ವಿಳಾಸ :",
                 epNameAddr, "", "", "ಮೊತ್ತ: ರೂ ಗಳಲ್ಲಿ", ""},
                {"ವಹಿವಾಟಿನ ಅವಧಿ :", epFromStr + " to " + epToStr, "", "", "", ""},
                {"ದಿನಾಂಕ " + epFromStr + " ರಂದು ಇದ್ದ ಪ್ರಾರಂಭಿಕ ಉಳಿಕೆ ಮೊತ್ತ :",
                 String.format("%.2f", wrapper.getOpeningBalance() != null ? wrapper.getOpeningBalance() : 0),
                 "", "", "", ""}
        };
        int epInfoValGap = Math.round(10f * DPI / 72f);  // space between label and its value
        for (String[] infoRow : epInfoRows) {
            int third = usable / 3;
            int textY = y + INFO_H / 2 + 4;

            int x0 = MARGIN + 4;
            g.setFont(labelFnt); g.setColor(DARK);
            g.drawString(infoRow[0], x0, textY);
            int lw0 = g.getFontMetrics(labelFnt).stringWidth(infoRow[0]);
            g.setFont(dataFnt); g.setColor(DARK);
            g.drawString(infoRow[1], x0 + lw0 + epInfoValGap, textY);

            if (!infoRow[2].isEmpty()) {
                int x2 = MARGIN + third + 4;
                g.setFont(labelFnt); g.setColor(DARK);
                g.drawString(infoRow[2], x2, textY);
                int lw2 = g.getFontMetrics(labelFnt).stringWidth(infoRow[2]);
                g.setFont(dataFnt); g.setColor(DARK);
                g.drawString(infoRow[3], x2 + lw2 + epInfoValGap, textY);
            }

            if (!infoRow[4].isEmpty()) {
                int x4 = MARGIN + 2 * third + 4;
                g.setFont(labelFnt); g.setColor(DARK);
                g.drawString(infoRow[4], x4, textY);
                int lw4 = g.getFontMetrics(labelFnt).stringWidth(infoRow[4]);
                g.setFont(dataFnt); g.setColor(DARK);
                g.drawString(infoRow[5], x4 + lw4 + epInfoValGap, textY);
            }

            g.setColor(BORDER); g.drawLine(MARGIN, y + INFO_H, W - MARGIN, y + INFO_H);
            y += INFO_H;
        }
        y += Math.round(12f * DPI / 72f);  // gap between farmer details and table
        g.setStroke(new java.awt.BasicStroke(4f));

        String[] epHdrs = {
                "ಕ್ರಮ ಸಂಖ್ಯೆ\nSL No",
                "ವಹಿವಾಟಿನ ದಿನಾಂಕ\nTransaction Date",
                "ವಿವರ\nDescription",
                "ವಹಿವಾಟಿನ ವಿಧ\nTransaction Type",
                "ಠೇವಣಿ ಮಾಡಿದ ಮೊತ್ತ \nDeposit Amount ",
                "ಖರೀದಿಸಿದ ಬಿತ್ತನೆ ರೇಷ್ಮೆ ಗೂಡಿನ ಪರಿಮಾಣ\nQuantity of seed cocoon purchased(in kg's)",
                "ಬಿತ್ತನೆ ಗೂಡಿನ ಸಂಖ್ಯೆ (ಪ್ರತಿ ಕೆ.ಜಿಗೆ)\nNo. of Seed Cocoons per Kg",
                "ಒಟ್ಟು ಬಿತ್ತನೆ ಗೂಡಿನ ಸಂಖ್ಯೆ\nTotal No of Seed Cocoons (in No's)",
                "ದರ ಪ್ರತಿ ಕೆ.ಜಿ.ಗೆ \nRate per Kg ",
                "ಖರೀದಿಸಿದ ರೇಷ್ಮೆ ಗೂಡಿನ ಮೊತ್ತ \nSeed Cocoon Purchase Amount ",
                "ಮರುಪಾವತಿಸಿದ ಮೊತ್ತ \nRefunded Amount ",
                "ಉಳಿಕೆ ಮೊತ್ತ \nBalance Amount "
        };
        g.setFont(hdrFnt);
        java.awt.FontMetrics epHdrFm = g.getFontMetrics(hdrFnt);
        for (int i = 0; i < 12; i++) {
            g.setColor(HDR_BG); g.fillRect(cx[i], y, cw[i], HDR_H);
            g.setColor(BORDER); g.drawRect(cx[i], y, cw[i], HDR_H);
            g.setColor(java.awt.Color.WHITE);
            java.util.List<String> wrLines = new java.util.ArrayList<>();
            for (String part : epHdrs[i].split("\n"))
                wrLines.addAll(wrapTextPdf(epHdrFm, part, cw[i] - 4));
            int lineH = HDR_H / (wrLines.size() + 1);
            java.awt.Shape prevClip = g.getClip();
            g.setClip(cx[i] + 1, y + 1, cw[i] - 2, HDR_H - 2);
            for (int li = 0; li < wrLines.size(); li++)
                drawCenteredPdf(g, wrLines.get(li), cx[i], y + lineH * li, cw[i], lineH);
            g.setClip(prevClip);
        }
        y += HDR_H;

        // ── pre-compute EU totals for the last-page totals row ───────────────
        double epPdfSumDeposit = 0, epPdfSumLotWeight = 0, epPdfSumTotalCocoons = 0,
               epPdfSumPayment = 0, epPdfSumRefunded = 0;
        for (ReelerTransactionReport r : reports) {
            double rlw = r.getLotWeight() != null ? r.getLotWeight() : 0;
            int    rqn = r.getQtyNos()    != null ? r.getQtyNos()    : 0;
            epPdfSumDeposit      += r.getDepositAmount()  != null ? r.getDepositAmount()  : 0;
            epPdfSumLotWeight    += rlw;
            epPdfSumTotalCocoons += rlw * rqn;
            epPdfSumPayment      += r.getPaymentAmount()  != null ? r.getPaymentAmount()  : 0;
            epPdfSumRefunded     += r.getRefundedAmount() != null ? r.getRefundedAmount() : 0;
        }

        // ── pagination: split by cumulative row heights ───────────────────────
        int firstPageDataStartY = y;
        int availFirstPage  = H - firstPageDataStartY - ROW_H;
        int availSubseqPage = H - MARGIN - HDR_H - ROW_H;

        java.util.List<java.util.List<ReelerTransactionReport>> pageSlices = new java.util.ArrayList<>();
        {
            java.util.List<ReelerTransactionReport> cur = new java.util.ArrayList<>();
            int usedH = 0;
            boolean onFirstPage = true;
            for (int k = 0; k < reports.size(); k++) {
                int avail = onFirstPage ? availFirstPage : availSubseqPage;
                if (!cur.isEmpty() && usedH + rowHeights[k] > avail) {
                    pageSlices.add(cur);
                    cur = new java.util.ArrayList<>();
                    usedH = 0;
                    onFirstPage = false;
                }
                cur.add(reports.get(k));
                usedH += rowHeights[k];
            }
            if (!cur.isEmpty()) pageSlices.add(cur);
        }

        java.util.List<java.awt.image.BufferedImage> pageImages = new java.util.ArrayList<>();
        pageImages.add(img);

        int globalSlNo = 1;
        int globalRowIdx = 0;

        for (int pi = 0; pi < pageSlices.size(); pi++) {
            java.util.List<ReelerTransactionReport> slice = pageSlices.get(pi);
            boolean isLastPage  = (pi == pageSlices.size() - 1);
            boolean isFirstPage = (pi == 0);

            java.awt.Graphics2D pg;
            int py;

            if (isFirstPage) {
                pg = g;
                py = firstPageDataStartY;
            } else {
                java.awt.image.BufferedImage newImg =
                        new java.awt.image.BufferedImage(W, H, java.awt.image.BufferedImage.TYPE_INT_RGB);
                java.awt.Graphics2D ng = newImg.createGraphics();
                ng.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,      java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                ng.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
                ng.setRenderingHint(java.awt.RenderingHints.KEY_FRACTIONALMETRICS, java.awt.RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                ng.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,         java.awt.RenderingHints.VALUE_RENDER_QUALITY);
                ng.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,     java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                ng.setColor(java.awt.Color.WHITE);
                ng.fillRect(0, 0, W, H);
                pg = ng;
                py = MARGIN;

                // column headers on this page
                pg.setFont(hdrFnt);
                java.awt.FontMetrics epHdrFmN = pg.getFontMetrics(hdrFnt);
                for (int i = 0; i < 12; i++) {
                    pg.setColor(HDR_BG);  pg.fillRect(cx[i], py, cw[i], HDR_H);
                    pg.setColor(BORDER);  pg.drawRect(cx[i], py, cw[i], HDR_H);
                    pg.setColor(java.awt.Color.WHITE);
                    java.util.List<String> wl = new java.util.ArrayList<>();
                    for (String part : epHdrs[i].split("\n"))
                        wl.addAll(wrapTextPdf(epHdrFmN, part, cw[i] - 4));
                    int lhN = HDR_H / (wl.size() + 1);
                    java.awt.Shape pc2 = pg.getClip();
                    pg.setClip(cx[i] + 1, py + 1, cw[i] - 2, HDR_H - 2);
                    for (int li = 0; li < wl.size(); li++)
                        drawCenteredPdf(pg, wl.get(li), cx[i], py + lhN * li, cw[i], lhN);
                    pg.setClip(pc2);
                }
                py += HDR_H;
                pageImages.add(newImg);
            }

            // ── data rows for this page ───────────────────────────────────────
            for (ReelerTransactionReport rep : slice) {
                int curRowH = rowHeights[globalRowIdx++];
                boolean alt = (globalSlNo % 2 == 0);
                pg.setColor(alt ? ALT_BG : java.awt.Color.WHITE);
                pg.fillRect(MARGIN, py, usable, curRowH);
                pg.setColor(BORDER);
                pg.drawRect(MARGIN, py, usable, curRowH);
                for (int i = 1; i < 12; i++) pg.drawLine(cx[i], py, cx[i], py + curRowH);

                double depV = rep.getDepositAmount()  != null ? rep.getDepositAmount()  : 0;
                double lwV  = rep.getLotWeight()      != null ? rep.getLotWeight()      : 0;
                int    qnV  = rep.getQtyNos()         != null ? rep.getQtyNos()         : 0;
                double tcV  = lwV * qnV;
                double rpV  = rep.getRatePerKg()      != null ? rep.getRatePerKg()      : 0;
                double pmtV = rep.getPaymentAmount()  != null ? rep.getPaymentAmount()  : 0;
                double rfV  = rep.getRefundedAmount() != null ? rep.getRefundedAmount() : 0;

                pg.setFont(dataFnt); pg.setColor(DARK);
                String[] vals = {
                        String.valueOf(globalSlNo++),
                        rep.getTransactionDate() != null ? rep.getTransactionDate().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy")) : "-",
                        rep.getOperationDescription() != null ? rep.getOperationDescription() : "-",
                        rep.getTransactionType() != null ? rep.getTransactionType() : "-",
                        fmt(depV), fmt(lwV),
                        qnV == 0 ? "-" : String.valueOf(qnV),
                        tcV == 0 ? "-" : fmt(tcV),
                        rpV == 0 ? "-" : fmt(rpV),
                        fmt(pmtV), fmt(rfV), fmt(rep.getBalance())
                };
                for (int i = 0; i < 12; i++) {
                    java.awt.Shape prevClip = pg.getClip();
                    pg.setClip(cx[i] + 1, py + 1, cw[i] - 2, curRowH - 2);
                    if (i == 2) {
                        java.util.List<String> dLines = wrapTextPdf(dataFm, vals[2], descColW);
                        int totalTH = dLines.size() * dataLineH;
                        int textY = py + Math.max(4, (curRowH - totalTH) / 2);
                        for (String dl : dLines) {
                            pg.drawString(dl, cx[2] + 4, textY + dataFm.getAscent());
                            textY += dataLineH;
                        }
                    } else {
                        drawCenteredPdf(pg, vals[i], cx[i], py, cw[i], curRowH);
                    }
                    pg.setClip(prevClip);
                }
                py += curRowH;
            }

            // ── totals row on last page only ──────────────────────────────────
            if (isLastPage) {
                pg.setColor(TOT_BG); pg.fillRect(MARGIN, py, usable, ROW_H);
                pg.setColor(BORDER); pg.drawRect(MARGIN, py, usable, ROW_H);
                for (int i = 1; i < 12; i++) pg.drawLine(cx[i], py, cx[i], py + ROW_H);
                pg.setFont(totalFnt); pg.setColor(DARK);
                drawCenteredPdf(pg, "ಒಟ್ಟು / Total", cx[0], py, cw[0] + cw[1] + cw[2] + cw[3], ROW_H);
                drawCenteredPdf(pg, fmt2(epPdfSumDeposit),       cx[4],  py, cw[4],  ROW_H);
                drawCenteredPdf(pg, fmt2(epPdfSumLotWeight),     cx[5],  py, cw[5],  ROW_H);
                drawCenteredPdf(pg, "",                          cx[6],  py, cw[6],  ROW_H);
                drawCenteredPdf(pg, fmt2(epPdfSumTotalCocoons),  cx[7],  py, cw[7],  ROW_H);
                drawCenteredPdf(pg, "",                          cx[8],  py, cw[8],  ROW_H);
                drawCenteredPdf(pg, fmt2(epPdfSumPayment),       cx[9],  py, cw[9],  ROW_H);
                drawCenteredPdf(pg, fmt2(epPdfSumRefunded),      cx[10], py, cw[10], ROW_H);
                drawCenteredPdf(pg, fmt2(wrapper.getClosingBalance()), cx[11], py, cw[11], ROW_H);
            }

            if (!isFirstPage) pg.dispose();
        }

        g.dispose();

        java.io.File pdfFile = java.io.File.createTempFile("Egg_Producer_Txn_Report_", ".pdf");
        try (org.apache.pdfbox.pdmodel.PDDocument doc = new org.apache.pdfbox.pdmodel.PDDocument()) {
            org.apache.pdfbox.pdmodel.common.PDRectangle rect =
                    new org.apache.pdfbox.pdmodel.common.PDRectangle(PDF_W, PDF_H);
            for (java.awt.image.BufferedImage pageImg : pageImages) {
                org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage(rect);
                doc.addPage(page);
                org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject pdImg =
                        org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory.createFromImage(doc, pageImg);
                try (org.apache.pdfbox.pdmodel.PDPageContentStream cs =
                             new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page)) {
                    cs.drawImage(pdImg, 0, 0, rect.getWidth(), rect.getHeight());
                }
            }
            doc.save(pdfFile);
        }
        return new java.io.FileInputStream(pdfFile);
    }

    private java.awt.Font loadKannadaPdfFont(float size) {
        String[] systemFonts = {
                "C:/Windows/Fonts/Nirmala.ttc",
                "C:/Windows/Fonts/NirmalaUI.ttf"
        };
        for (String path : systemFonts) {
            java.io.File f = new java.io.File(path);
            if (f.exists()) {
                try {
                    java.awt.Font[] fonts = java.awt.Font.createFonts(f);
                    for (java.awt.Font font : fonts) {
                        if (font.canDisplay('ಕ')) {
                            java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
                            return font.deriveFont(java.awt.Font.PLAIN, size);
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        try {
            org.springframework.core.io.ClassPathResource res =
                    new org.springframework.core.io.ClassPathResource("fonts/NotoSansKannada-Regular.ttf");
            try (java.io.InputStream is = res.getInputStream()) {
                java.awt.Font f = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, is);
                java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(f);
                return f.deriveFont(java.awt.Font.PLAIN, size);
            }
        } catch (Exception e) {
            return new java.awt.Font("SansSerif", java.awt.Font.PLAIN, (int) size);
        }
    }

    private java.util.List<String> wrapTextPdf(java.awt.FontMetrics fm, String text, int maxW) {
        java.util.List<String> out = new java.util.ArrayList<>();
        if (text == null || text.isEmpty()) return out;
        if (fm.stringWidth(text) <= maxW) { out.add(text); return out; }
        String[] words = text.split(" ");
        StringBuilder cur = new StringBuilder();
        for (String w : words) {
            String candidate = cur.length() == 0 ? w : cur + " " + w;
            if (fm.stringWidth(candidate) <= maxW) {
                cur = new StringBuilder(candidate);
            } else {
                if (cur.length() > 0) { out.add(cur.toString()); cur = new StringBuilder(w); }
                else { out.add(w); }
            }
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out;
    }

    private void drawCenteredPdf(java.awt.Graphics2D g, String text, int x, int y, int w, int h) {
        if (text == null || text.isEmpty()) return;
        java.awt.FontMetrics fm = g.getFontMetrics();
        int tx = x + Math.max(0, (w - fm.stringWidth(text)) / 2);
        int ty = y + (h + fm.getAscent() - fm.getDescent()) / 2;
        g.drawString(text, tx, ty);
    }

    private void drawLeftPdf(java.awt.Graphics2D g, String text, int x, int y, int w, int h) {
        if (text == null || text.isEmpty()) return;
        java.awt.FontMetrics fm = g.getFontMetrics();
        int ty = y + (h + fm.getAscent() - fm.getDescent()) / 2;
        g.drawString(text, x + 4, ty);
    }

    private void drawRightPdf(java.awt.Graphics2D g, String text, int x, int y, int w, int h) {
        if (text == null || text.isEmpty()) return;
        java.awt.FontMetrics fm = g.getFontMetrics();
        int tx = x + w - fm.stringWidth(text) - 4;
        int ty = y + (h + fm.getAscent() - fm.getDescent()) / 2;
        g.drawString(text, tx, ty);
    }

    private String fmt(Double v) {
        return (v == null || v == 0) ? "" : String.format("%.2f", v);
    }

    private String fmt2(Double v) {
        return v != null ? String.format("%.2f", v) : "0.00";
    }

    public ResponseEntity<?> getTransferMarketFeeToGovtAccount(MarketFeeGovtTransferRequest request) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        List<Object[]> responses = lotGroupageRepository.getMarketFeeGovtTransferDetails(
                request.getDate(),
                request.getMarketId()
        );
        if (Util.isNullOrEmptyList(responses)) {
            throw new ValidationException("No data found");
        }
        List<MarketFeeGovtTransferResponse> infoList = new ArrayList<>();
        for (Object[] row : responses) {
            infoList.add(MarketFeeGovtTransferResponse.builder()
                    .fruitsId(Util.objectToString(row[1]))
                    .farmerName(Util.objectToString(row[2]))
                    .farmerMarketFee(Util.objectToFloat(row[3]))
                    .reelerMarketFee(Util.objectToFloat(row[4]))
                    .lotNo(Util.objectToInteger(row[6]))
                    .build());
        }
        rw.setContent(infoList);
        return ResponseEntity.ok(rw);
    }

    @Transactional
    public ResponseEntity<?> executeMarketFeeTransfer(MarketFeeGovtTransferRequest request) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(Map.class);

        if (request.getFileName() == null || request.getFileName().isBlank()) {
            throw new ValidationException("File name is required to execute market fee transfer.");
        }

        if (transactionFileGenQueueRepository.existsTransactionFileGenQueueByMarketIdAndFileName(
                request.getMarketId(), request.getFileName())) {
            rw.setContent(Map.of(
                "message", "Transfer already processed for this file name. Use the file name below to download the CSV.",
                "fileName", request.getFileName()
            ));
            return ResponseEntity.ok(rw);
        }

        String govtAccountNumber = marketFeeGovtTransferRepository.getActiveGovtAccountNumber();
        if (govtAccountNumber == null || govtAccountNumber.isBlank()) {
            throw new ValidationException("No active govt account found. Please configure the govt account.");
        }

        List<Object[]> lotDetails = lotGroupageRepository.getMarketFeeGovtTransferDetails(
                request.getDate(),
                request.getMarketId()
        );
        if (Util.isNullOrEmptyList(lotDetails)) {
            throw new ValidationException("No distributed lots found for the given date and market");
        }

        int savedCount = 0;
        for (Object[] row : lotDetails) {
            Long lotGroupageId = Util.objectToLong(row[0]);
            if (marketFeeGovtTransferRepository.existsByLotGroupageIdAndActiveTrue(lotGroupageId)) {
                continue;
            }
            MarketFeeGovtTransfer transfer = new MarketFeeGovtTransfer();
            transfer.setLotGroupageId(lotGroupageId);
            transfer.setFruitsId(Util.objectToString(row[1]));
            transfer.setFarmerMarketFee(BigDecimal.valueOf(Util.objectToFloat(row[3])));
            transfer.setReelerMarketFee(BigDecimal.valueOf(Util.objectToFloat(row[4])));
            transfer.setTotalMarketFee(BigDecimal.valueOf(Util.objectToFloat(row[5])));
            transfer.setAllottedLotId(Util.objectToInteger(row[6]));
            transfer.setMarketId(Util.objectToInteger(row[7]));
            transfer.setCollectionDate(request.getDate());
            transfer.setTransferStatus("TRANSFERRED");
            transfer.setTransferredDate(Util.getISTLocalDate());
            transfer.setGovtAccountNumber(govtAccountNumber);
            marketFeeGovtTransferRepository.save(transfer);
            savedCount++;
        }

        TransactionFileGenQueue queue = TransactionFileGenQueue.builder()
                .marketId(request.getMarketId())
                .auctionDate(request.getDate())
                .fileName(request.getFileName())
                .comment("MARKET_FEE_TRANSFER")
                .status("requested")
                .build();
        transactionFileGenQueueRepository.save(queue);

        rw.setContent(Map.of(
            "message", "Transfer recorded successfully for " + savedCount + " lots. CSV generation queued.",
            "fileName", request.getFileName()
        ));
        return ResponseEntity.ok(rw);
    }

    public ByteArrayInputStream generateMarketFeePreviewCSV(LocalDate date, int marketId) {
        List<Object[]> rows = lotGroupageRepository.getMarketFeeGovtTransferDetails(date, marketId);
        if (Util.isNullOrEmptyList(rows)) {
            throw new ValidationException("No pending lots found for the given date and market");
        }

        String govtAccountNumber = marketFeeGovtTransferRepository.getActiveGovtAccountNumber();
        if (govtAccountNumber == null || govtAccountNumber.isBlank()) {
            throw new ValidationException("No active govt account found. Please configure the govt account.");
        }

        String chequeDate = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        final CSVFormat format = CSVFormat.DEFAULT.withQuoteMode(QuoteMode.MINIMAL);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             CSVPrinter csv = new CSVPrinter(new PrintWriter(out), format)) {

            csv.printRecord(Arrays.asList(
                "Serial Number", "Lot No", "Fruits ID", "Farmer Name",
                "Farmer Market Fee", "Reeler Market Fee", "Total Market Fee",
                "Govt Account Number", "Customer Reference Number"
            ));

            int serial = 1;
            long totalMarketFee = 0;
            for (Object[] row : rows) {
                // row[0]=lot_groupage_id, row[1]=fruits_id, row[2]=farmer_name,
                // row[3]=farmer_market_fee, row[4]=reeler_market_fee, row[5]=total_market_fee,
                // row[6]=allotted_lot_id, row[7]=market_id, row[8]=auction_date, row[9]=crn
                long amount = Math.round(Util.objectToFloat(row[5]));
                totalMarketFee += amount;

                csv.printRecord(Arrays.asList(
                    serial++,
                    Util.objectToString(row[6]),   // allotted_lot_id
                    Util.objectToString(row[1]),   // fruits_id
                    Util.objectToString(row[2]),   // farmer_name
                    Util.objectToFloat(row[3]),    // farmer_market_fee
                    Util.objectToFloat(row[4]),    // reeler_market_fee
                    amount,                        // total_market_fee
                    govtAccountNumber,
                    Util.objectToString(row[9])    // customer_reference_number
                ));
            }

            csv.printRecord("Total Market Fee to be transferred to Govt: " + totalMarketFee);
            csv.flush();
            return new ByteArrayInputStream(out.toByteArray());

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate market fee preview CSV: " + e.getMessage());
        }
    }

    public ByteArrayInputStream downloadMarketFeeTransferCSV(int marketId, String fileName) {
        return marketAuctionFileDowndloadService.generateCSV(marketId, fileName);
    }

}
