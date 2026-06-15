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
                double totalDebitAmount = isReeling ? soldAmount + marketFee : soldAmount;
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
                    double totalDebitDifference = isReeling ? debitDifference + marketFee : (double) debitDifference;
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

    public List<Map<String, String>> getLicenseNumberList(int marketId, String buyerType) {
        List<Map<String, String>> result = new ArrayList<>();
        if ("REELING".equalsIgnoreCase(buyerType)) {
            List<Object[]> rows = reelerAuctionRepository.getReelerListByMarket(marketId);
            for (Object[] row : rows) {
                Map<String, String> map = new java.util.LinkedHashMap<>();
                map.put("licenseNumber", row[2] != null ? row[2].toString() : "");
                map.put("name", row[1] != null ? row[1].toString() : "");
                result.add(map);
            }
        } else if ("EXTERNAL_UNIT".equalsIgnoreCase(buyerType)) {
            List<Object[]> rows = lotGroupageRepository.getExternalUnitListByMarket(marketId);
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
                "ಠೇವಣಿ ಮಾಡಿದ ಮೊತ್ತ / Deposit Amount",
                "ಖರೀದಿಸಿದ ಬಿತ್ತನೆ ರೇಷ್ಮೆ ಗೂಡಿನ ಪರಿಮಾಣ / Quantity of seed cocoon purchased (in kg's)",
                "ದರ (ಪ್ರತಿ ಕಿ.ಗ್ರಾ) / Rate / Kg",
                "ಖರೀದಿಸಿದ ರೇಷ್ಮೆ ಗೂಡಿನ ಮೊತ್ತ / Cocoons Purchase Amount",
                "ಮಾರು ಕಟ್ಟೆ ಶುಲ್ಕ @1% / Market fee @1%",
                "ಒಟ್ಟು ಮೊತ್ತ / Total",
                "ಉಳಿಕೆ ಮೊತ್ತ / Balance Amount"
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
                            "ಠೇವಣಿ ಮಾಡಿದ ಮೊತ್ತ / Deposit Amount",
                            "ಖರೀದಿಸಿದ ಬಿತ್ತನೆ ರೇಷ್ಮೆ ಗೂಡಿನ ಪರಿಮಾಣ / Quantity of seed cocoon purchased(in kg's)",
                            "ಬಿತ್ತನೆ ಗೂಡಿನ ಸಂಖ್ಯೆ (ಪ್ರತಿ ಕೆ.ಜಿಗೆ) / No. of Seed Cocoons per Kg",
                            "ಒಟ್ಟು ಬಿತ್ತನೆ ಗೂಡಿನ ಸಂಖ್ಯೆ / Total No of Seed Cocoons (in No's)",
                            "ದರ ಪ್ರತಿ ಕೆ.ಜಿ.ಗೆ / Rate per Kg",
                            "ಖರೀದಿಸಿದ ರೇಷ್ಮೆ ಗೂಡಿನ ಮೊತ್ತ / Seed Cocoon Purchase Amount",
                            "ಉಳಿಕೆ ಮೊತ್ತ / Balance Amount"
                    ));
                    wrapper.setFruitsId("");
                } else {
                    wrapper.setReportType("REELING");
                    wrapper.setFruitsId(buyerFruitsId);
                }

                // CURRENT BALANCE
                List<Object[]> currentBalanceList =
                        lotGroupageRepository
                                .getSeedMarketCurrentBalance(
                                        virtualAccountNumber);

                if (currentBalanceList == null
                        || currentBalanceList.isEmpty()) {

                    wrapper.setOpeningBalance(0.0);
                    wrapper.setReelerTransactionReports(new ArrayList<>());
                    wrapper.setTotalDeposits(0.0);
                    wrapper.setTotalPurchase(0.0);

                    return wrapper;
                }

                Object[] currentBalanceResult =
                        currentBalanceList.get(0);

                ReportCurrentBalance reportCurrentBalance =
                        new ReportCurrentBalance();

                reportCurrentBalance.setCurrentBalance(
                        ((BigDecimal) currentBalanceResult[0]).doubleValue());

                reportCurrentBalance.setVirtualAccountNumber(
                        (String) currentBalanceResult[1]);

                reportCurrentBalance.setCreatedDate(
                        ((Timestamp) currentBalanceResult[2])
                                .toLocalDateTime());

                // PASSBOOK
                List<Object[]> transactionList =
                        lotGroupageRepository
                                .getSeedMarketTransactionPassBook(
                                        fromDate,
                                        toDate,
                                        reportCurrentBalance.getVirtualAccountNumber(),
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

                double currentBalance =
                        reportCurrentBalance.getCurrentBalance();

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

                        debitSum += rat.getTotal();
                        totalLotWeight += rat.getLotWeight() != null ? rat.getLotWeight() : 0;
                        totalPaymentAmount += rat.getAmount() != null ? rat.getAmount() : 0;
                        totalMarketFee += rat.getMarketFee() != null ? rat.getMarketFee() : 0;

                        report.setPaymentAmount(
                                rat.getAmount());

                        report.setLotWeight(rat.getLotWeight());

                        report.setRatePerKg(rat.getRatePerKg());

                        report.setMarketFee(rat.getMarketFee());

                        report.setTotal(rat.getTotal());

                        report.setQtyNos(rat.getQtyNos());

                        report.setOperationDescription(
                                "Paid to "
                                        + rat.getFarmerName()
                                        + ", for lot "
                                        + rat.getLot());

                    } else {

                        creditSum += rat.getAmount();

                        report.setDepositAmount(
                                rat.getAmount());

                        report.setOperationDescription(
                                "Deposited by " + buyerName);
                    }

                    reports.add(report);
                }

                double openingBalance =
                        currentBalance - creditSum + debitSum;

                double runningBalance =
                        openingBalance;

                for (ReelerTransactionReport report : reports) {

                    if ("D".equalsIgnoreCase(
                            report.getTransactionType())) {

                        runningBalance =
                                runningBalance
                                        - report.getTotal();

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

        final int COLS = 11;

        // ── Fonts ────────────────────────────────────────────────────────────
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        boldFont.setFontHeightInPoints((short) 12);

        Font boldSmall = workbook.createFont();
        boldSmall.setBold(true);
        boldSmall.setFontHeightInPoints((short) 10);

        Font normalFont = workbook.createFont();
        normalFont.setFontHeightInPoints((short) 10);

        // ── Styles ───────────────────────────────────────────────────────────
        CellStyle titleStyle = workbook.createCellStyle();
        titleStyle.setFont(boldFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        titleStyle.setWrapText(true);

        CellStyle labelStyle = workbook.createCellStyle();
        labelStyle.setFont(boldSmall);
        labelStyle.setWrapText(true);

        CellStyle valueStyle = workbook.createCellStyle();
        valueStyle.setFont(normalFont);
        valueStyle.setWrapText(true);

        CellStyle colHeaderStyle = workbook.createCellStyle();
        colHeaderStyle.setFont(boldSmall);
        colHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
        colHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        colHeaderStyle.setWrapText(true);
        colHeaderStyle.setBorderTop(BorderStyle.THIN);
        colHeaderStyle.setBorderBottom(BorderStyle.THIN);
        colHeaderStyle.setBorderLeft(BorderStyle.THIN);
        colHeaderStyle.setBorderRight(BorderStyle.THIN);
        colHeaderStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        colHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setFont(normalFont);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        dataStyle.setAlignment(HorizontalAlignment.LEFT);
        dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        dataStyle.setWrapText(true);

        CellStyle centerDataStyle = workbook.createCellStyle();
        centerDataStyle.setFont(normalFont);
        centerDataStyle.setBorderTop(BorderStyle.THIN);
        centerDataStyle.setBorderBottom(BorderStyle.THIN);
        centerDataStyle.setBorderLeft(BorderStyle.THIN);
        centerDataStyle.setBorderRight(BorderStyle.THIN);
        centerDataStyle.setAlignment(HorizontalAlignment.LEFT);
        centerDataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle numberStyle = workbook.createCellStyle();
        numberStyle.setFont(normalFont);
        numberStyle.setBorderTop(BorderStyle.THIN);
        numberStyle.setBorderBottom(BorderStyle.THIN);
        numberStyle.setBorderLeft(BorderStyle.THIN);
        numberStyle.setBorderRight(BorderStyle.THIN);
        numberStyle.setAlignment(HorizontalAlignment.LEFT);
        numberStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // ── Logo rows 0-1 ────────────────────────────────────────────────────
        Row logoRow0 = sheet.createRow(0);
        logoRow0.setHeightInPoints(36);
        Row logoRow1 = sheet.createRow(1);
        logoRow1.setHeightInPoints(36);
        try {
            org.springframework.core.io.ClassPathResource logoRes =
                    new org.springframework.core.io.ClassPathResource("images/kar_logo.png");
            java.io.InputStream logoStream = logoRes.getInputStream();
            byte[] logoBytes = logoStream.readAllBytes();
            logoStream.close();
            int pictureIdx = workbook.addPicture(logoBytes, org.apache.poi.ss.usermodel.Workbook.PICTURE_TYPE_PNG);
            org.apache.poi.ss.usermodel.Drawing<?> drawing = sheet.createDrawingPatriarch();
            org.apache.poi.ss.usermodel.ClientAnchor anchor =
                    workbook.getCreationHelper().createClientAnchor();
            anchor.setCol1(5); anchor.setRow1(0);
            anchor.setAnchorType(org.apache.poi.ss.usermodel.ClientAnchor.AnchorType.DONT_MOVE_AND_RESIZE);
            org.apache.poi.ss.usermodel.Picture logoPic = drawing.createPicture(anchor, pictureIdx);
            logoPic.resize(0.3);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // ── Helper: merged title row ─────────────────────────────────────────
        int rowIdx = 2;

        // Row 2 — ಕರ್ನಾಟಕ ಸರ್ಕಾರ
        Row r0 = sheet.createRow(rowIdx++);
        r0.setHeightInPoints(22);
        Cell c0 = r0.createCell(0);
        c0.setCellValue("ಕರ್ನಾಟಕ ಸರ್ಕಾರ");
        c0.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(r0.getRowNum(), r0.getRowNum(), 0, COLS - 1));

        // Row 3 — ರೇಷ್ಮೆ, ಇಲಾಖೆ
        Row r1 = sheet.createRow(rowIdx++);
        r1.setHeightInPoints(20);
        Cell c1 = r1.createCell(0);
        c1.setCellValue("ರೇಷ್ಮೆ, ಇಲಾಖೆ");
        c1.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(r1.getRowNum(), r1.getRowNum(), 0, COLS - 1));

        // Row 4 — Market name
        Row r2 = sheet.createRow(rowIdx++);
        r2.setHeightInPoints(20);
        Cell c2 = r2.createCell(0);
        c2.setCellValue("ಸರ್ಕಾರಿ ರೇಷ್ಮೆ ಗೂಡಿನ ಮಾರುಕಟ್ಟೆ, " + marketName);
        c2.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(r2.getRowNum(), r2.getRowNum(), 0, COLS - 1));

        // Row 5 — Report title
        Row r3 = sheet.createRow(rowIdx++);
        r3.setHeightInPoints(20);
        Cell c3 = r3.createCell(0);
        c3.setCellValue("ರೇಷ್ಮೆ ನೂಲು ಬಿಚ್ಚಾಣಿಕೆದಾರರ ವಹಿವಾಟು ವರದಿ");
        c3.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(r3.getRowNum(), r3.getRowNum(), 0, COLS - 1));

        // Row 6 — blank
        sheet.createRow(rowIdx++);

        // Row 7 — License No | value | FRUITS ID | value | Date and Time Stamp | timestamp
        Row r5 = sheet.createRow(rowIdx++);
        r5.setHeightInPoints(18);
        r5.createCell(0).setCellStyle(labelStyle);
        r5.getCell(0).setCellValue("ರೇಷ್ಮೆ ನೂಲು ಬಿಚ್ಚಾಣಿಕೆದಾರರ ರಪದಾರಿ ಸಂಖ್ಯೆ");
        r5.createCell(1).setCellStyle(labelStyle);
        r5.createCell(2).setCellStyle(labelStyle);
        sheet.addMergedRegion(new CellRangeAddress(r5.getRowNum(), r5.getRowNum(), 0, 2));
        r5.createCell(3).setCellStyle(valueStyle);
        r5.getCell(3).setCellValue(request.getLicenseNumber() != null ? request.getLicenseNumber() : "");
        r5.createCell(4).setCellStyle(valueStyle);
        sheet.addMergedRegion(new CellRangeAddress(r5.getRowNum(), r5.getRowNum(), 3, 4));
        r5.createCell(5).setCellStyle(labelStyle);
        r5.getCell(5).setCellValue("FRUITS ID / FID");
        r5.createCell(6).setCellStyle(valueStyle);
        r5.getCell(6).setCellValue(wrapper.getFruitsId() != null ? wrapper.getFruitsId() : "");
        r5.createCell(7).setCellStyle(valueStyle);
        sheet.addMergedRegion(new CellRangeAddress(r5.getRowNum(), r5.getRowNum(), 6, 7));
        r5.createCell(8).setCellStyle(labelStyle);
        r5.getCell(8).setCellValue("Date and Time Stamp");
        String generatedAt = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        r5.createCell(9).setCellStyle(valueStyle);
        r5.getCell(9).setCellValue(generatedAt);
        r5.createCell(10).setCellStyle(valueStyle);
        sheet.addMergedRegion(new CellRangeAddress(r5.getRowNum(), r5.getRowNum(), 9, COLS - 1));

        // Row 8 — Buyer name | Amount in Rs
        Row r6 = sheet.createRow(rowIdx++);
        r6.setHeightInPoints(18);
        r6.createCell(0).setCellStyle(labelStyle);
        r6.getCell(0).setCellValue("ರೇಷ್ಮೆ ನೂಲು ಬಿಚ್ಚಾಣಿಕೆದಾರರ ಹೆಸರು ಮತ್ತು ವಿಳಾಸ:");
        r6.createCell(1).setCellStyle(labelStyle);
        r6.createCell(2).setCellStyle(labelStyle);
        sheet.addMergedRegion(new CellRangeAddress(r6.getRowNum(), r6.getRowNum(), 0, 2));
        String nameAndAddress = (wrapper.getName() != null ? wrapper.getName() : "")
                + (wrapper.getAddress() != null ? wrapper.getAddress() : "");
        r6.createCell(3).setCellStyle(valueStyle);
        r6.getCell(3).setCellValue(nameAndAddress);
        for (int i = 4; i <= 7; i++) r6.createCell(i).setCellStyle(valueStyle);
        sheet.addMergedRegion(new CellRangeAddress(r6.getRowNum(), r6.getRowNum(), 3, 7));
        r6.createCell(8).setCellStyle(labelStyle);
        r6.getCell(8).setCellValue("ಮೊತ್ತ: ರೂ ಗಳಲ್ಲಿ");
        r6.createCell(9).setCellStyle(labelStyle);
        r6.createCell(10).setCellStyle(labelStyle);
        sheet.addMergedRegion(new CellRangeAddress(r6.getRowNum(), r6.getRowNum(), 8, COLS - 1));

        // Row 9 — Transaction period
        Row r7 = sheet.createRow(rowIdx++);
        r7.setHeightInPoints(18);
        r7.createCell(0).setCellStyle(labelStyle);
        r7.getCell(0).setCellValue("ವಹಿವಾಟಿನ ಅವಧಿ :");
        r7.createCell(1).setCellStyle(labelStyle);
        r7.createCell(2).setCellStyle(labelStyle);
        sheet.addMergedRegion(new CellRangeAddress(r7.getRowNum(), r7.getRowNum(), 0, 2));
        String fromStr = request.getFromDate() != null
                ? request.getFromDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
        String toStr = request.getToDate() != null
                ? request.getToDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
        r7.createCell(3).setCellStyle(valueStyle);
        r7.getCell(3).setCellValue(fromStr + " to " + toStr);
        for (int i = 4; i <= COLS - 1; i++) r7.createCell(i).setCellStyle(valueStyle);
        sheet.addMergedRegion(new CellRangeAddress(r7.getRowNum(), r7.getRowNum(), 3, COLS - 1));

        // Row 10 — Opening balance
        Row r8 = sheet.createRow(rowIdx++);
        r8.setHeightInPoints(18);
        r8.createCell(0).setCellStyle(labelStyle);
        r8.getCell(0).setCellValue("ದಿನಾಂಕ " + fromStr + " ರಂದು ಇದ್ದ ಪ್ರಾರಂಭಿಕ ಉಳಿಕೆ ಮೊತ್ತ :");
        r8.createCell(1).setCellStyle(labelStyle);
        r8.createCell(2).setCellStyle(labelStyle);
        sheet.addMergedRegion(new CellRangeAddress(r8.getRowNum(), r8.getRowNum(), 0, 2));
        r8.createCell(3).setCellStyle(numberStyle);
        r8.getCell(3).setCellValue(wrapper.getOpeningBalance() != null ? wrapper.getOpeningBalance() : 0);
        for (int i = 4; i <= COLS - 1; i++) r8.createCell(i).setCellStyle(valueStyle);
        sheet.addMergedRegion(new CellRangeAddress(r8.getRowNum(), r8.getRowNum(), 4, COLS - 1));


        // Row 9 — blank
        sheet.createRow(rowIdx++);

        // Row 10 — Combined Kannada / English column headers
        Row colHeaderRow = sheet.createRow(rowIdx++);
        colHeaderRow.setHeightInPoints(55);
        String[] combinedHeaders = {
                "ಕ್ರಮ ಸಂಖ್ಯೆ / SL No",
                "ವಹಿವಾಟಿನ ದಿನಾಂಕ / Transaction Date",
                "ವಿವರ / Description",
                "ವಹಿವಾಟಿನ ವಿಧ / Transaction Type",
                "ಠೇವಣಿ ಮಾಡಿದ ಮೊತ್ತ / Deposit Amount",
                "ಖರೀದಿಸಿದ ಬಿತ್ತನೆ ರೇಷ್ಮೆ ಗೂಡಿನ ಪರಿಮಾಣ / Quantity of seed cocoon purchased(in kg's)",
                "ದರ (ಪ್ರತಿ ಕಿ.ಗ್ರಾ) / Rate / Kg",
                "ಖರೀದಿಸಿದ ರೇಷ್ಮೆ ಗೂಡಿನ ಮೊತ್ತ / Cocoons Purchase Amount",
                "ಮಾರು ಕಟ್ಟೆ ಶುಲ್ಕ @1% / Market fee @1%",
                "ಒಟ್ಟು ಮೊತ್ತ / Total",
                "ಉಳಿಕೆ ಮೊತ್ತ / Balance Amount"
        };
        for (int i = 0; i < combinedHeaders.length; i++) {
            Cell cell = colHeaderRow.createCell(i);
            cell.setCellValue(combinedHeaders[i]);
            cell.setCellStyle(colHeaderStyle);
        }

        CellStyle totalRowStyle = workbook.createCellStyle();
        totalRowStyle.setFont(boldSmall);
        totalRowStyle.setBorderTop(BorderStyle.THIN);
        totalRowStyle.setBorderBottom(BorderStyle.THIN);
        totalRowStyle.setBorderLeft(BorderStyle.THIN);
        totalRowStyle.setBorderRight(BorderStyle.THIN);
        totalRowStyle.setAlignment(HorizontalAlignment.LEFT);
        totalRowStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        totalRowStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        totalRowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // ── Data rows ────────────────────────────────────────────────────────
        int slNo = 1;
        double sumDeposit = 0, sumLotWeight = 0, sumPayment = 0, sumMarketFee = 0, sumTotal = 0;
        double lastBalance = 0;
        for (ReelerTransactionReport report : wrapper.getReelerTransactionReports()) {
            Row row = sheet.createRow(rowIdx++);
            row.setHeightInPoints(16);

            Cell dc0 = row.createCell(0);
            dc0.setCellValue(slNo++);
            dc0.setCellStyle(centerDataStyle);

            Cell dc1 = row.createCell(1);
            dc1.setCellValue(report.getTransactionDate() != null ? report.getTransactionDate().toString() : "");
            dc1.setCellStyle(centerDataStyle);

            Cell dc2 = row.createCell(2);
            dc2.setCellValue(report.getOperationDescription() != null ? report.getOperationDescription() : "");
            dc2.setCellStyle(dataStyle);

            Cell dc3 = row.createCell(3);
            dc3.setCellValue(report.getTransactionType() != null ? report.getTransactionType() : "");
            dc3.setCellStyle(centerDataStyle);

            double depositVal = report.getDepositAmount() != null ? report.getDepositAmount() : 0;
            Cell dc4 = row.createCell(4);
            dc4.setCellValue(depositVal);
            dc4.setCellStyle(numberStyle);
            sumDeposit += depositVal;

            double lotWeightVal = report.getLotWeight() != null ? report.getLotWeight() : 0;
            Cell dc5 = row.createCell(5);
            dc5.setCellValue(lotWeightVal);
            dc5.setCellStyle(numberStyle);
            sumLotWeight += lotWeightVal;

            Cell dc6 = row.createCell(6);
            dc6.setCellValue(report.getRatePerKg() != null ? report.getRatePerKg() : 0);
            dc6.setCellStyle(numberStyle);

            double paymentVal = report.getPaymentAmount() != null ? report.getPaymentAmount() : 0;
            Cell dc7 = row.createCell(7);
            dc7.setCellValue(paymentVal);
            dc7.setCellStyle(numberStyle);
            sumPayment += paymentVal;

            double marketFeeVal = report.getMarketFee() != null ? report.getMarketFee() : 0;
            Cell dc8 = row.createCell(8);
            dc8.setCellValue(marketFeeVal);
            dc8.setCellStyle(numberStyle);
            sumMarketFee += marketFeeVal;

            double totalVal = report.getTotal() != null ? report.getTotal() : 0;
            Cell dc9 = row.createCell(9);
            dc9.setCellValue(totalVal);
            dc9.setCellStyle(numberStyle);
            sumTotal += totalVal;

            lastBalance = report.getBalance() != null ? report.getBalance() : 0;
            Cell dc10 = row.createCell(10);
            dc10.setCellValue(lastBalance);
            dc10.setCellStyle(numberStyle);
        }

        // ── Totals row ───────────────────────────────────────────────────────
        Row totalRow = sheet.createRow(rowIdx++);
        totalRow.setHeightInPoints(18);
        Cell tc0 = totalRow.createCell(0);
        tc0.setCellValue("ಒಟ್ಟು");
        tc0.setCellStyle(totalRowStyle);
        sheet.addMergedRegion(new CellRangeAddress(totalRow.getRowNum(), totalRow.getRowNum(), 0, 3));
        for (int i = 1; i <= 3; i++) totalRow.createCell(i).setCellStyle(totalRowStyle);

        Cell tc4 = totalRow.createCell(4);
        tc4.setCellValue(sumDeposit);
        tc4.setCellStyle(totalRowStyle);

        Cell tc5 = totalRow.createCell(5);
        tc5.setCellValue(sumLotWeight);
        tc5.setCellStyle(totalRowStyle);

        Cell tc6 = totalRow.createCell(6);
        tc6.setCellValue("");
        tc6.setCellStyle(totalRowStyle);

        Cell tc7 = totalRow.createCell(7);
        tc7.setCellValue(sumPayment);
        tc7.setCellStyle(totalRowStyle);

        Cell tc8 = totalRow.createCell(8);
        tc8.setCellValue(sumMarketFee);
        tc8.setCellStyle(totalRowStyle);

        Cell tc9 = totalRow.createCell(9);
        tc9.setCellValue(sumTotal);
        tc9.setCellStyle(totalRowStyle);

        Cell tc10 = totalRow.createCell(10);
        tc10.setCellValue(lastBalance);
        tc10.setCellStyle(totalRowStyle);

        // ── Column widths: auto-size per content, then clamp to min/max ────────
        int[] minWidths = { 2000, 3500, 8000, 3000, 3500, 4000, 3000, 4500, 3500, 3500, 3500 };
        int[] maxWidths = { 3000, 5000, 13000, 4500, 5500, 6000, 4500, 6000, 5000, 5000, 5000 };
        for (int i = 0; i < COLS; i++) {
            sheet.autoSizeColumn(i);
            int w = sheet.getColumnWidth(i);
            if (w < minWidths[i]) w = minWidths[i];
            if (w > maxWidths[i]) w = maxWidths[i];
            sheet.setColumnWidth(i, w);
        }

        sheet.getPrintSetup().setLandscape(true);
        sheet.getPrintSetup().setPaperSize(org.apache.poi.ss.usermodel.PrintSetup.A3_PAPERSIZE);
        sheet.setFitToPage(true);
        sheet.getPrintSetup().setFitWidth((short) 1);
        sheet.getPrintSetup().setFitHeight((short) 0);

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

        // ── dimensions ───────────────────────────────────────────────────────
        int DPI    = 150;
        int W      = (int)(16.54 * DPI);   // A3 landscape width
        int MARGIN = 40;
        int LOGO_H = 80;
        int INFO_H = 30;
        int HDR_H  = 55;
        int ROW_H  = 32;
        int TITLE_H = 30;
        int titleLines = 4;
        int infoLines  = 4;
        int H = MARGIN + LOGO_H + 8
                + titleLines * TITLE_H + 10
                + infoLines * INFO_H + 10
                + HDR_H
                + reports.size() * ROW_H
                + ROW_H          // totals row
                + MARGIN;

        java.awt.image.BufferedImage logoImg = null;
        try {
            org.springframework.core.io.ClassPathResource logoRes =
                    new org.springframework.core.io.ClassPathResource("images/kar_logo.png");
            logoImg = javax.imageio.ImageIO.read(logoRes.getInputStream());
        } catch (Exception ignored) {}

        // ── column widths ─────────────────────────────────────────────────────
        int usable = W - 2 * MARGIN;
        float[] proportions = {0.04f, 0.10f, 0.16f, 0.06f, 0.08f,
                               0.07f, 0.07f, 0.10f, 0.08f, 0.10f, 0.14f};
        int[] cw = new int[11];
        int allocated = 0;
        for (int i = 0; i < 10; i++) {
            cw[i] = (int)(usable * proportions[i]);
            allocated += cw[i];
        }
        cw[10] = usable - allocated;

        int[] cx = new int[11];
        cx[0] = MARGIN;
        for (int i = 1; i < 11; i++) cx[i] = cx[i - 1] + cw[i - 1];

        // ── fonts ─────────────────────────────────────────────────────────────
        java.awt.Font base = loadKannadaPdfFont(15f);
        java.awt.Font titleFnt  = base.deriveFont(java.awt.Font.BOLD, 18f);
        java.awt.Font labelFnt  = base.deriveFont(java.awt.Font.BOLD, 11f);
        java.awt.Font hdrFnt    = base.deriveFont(java.awt.Font.BOLD, 9f);
        java.awt.Font dataFnt   = base.deriveFont(java.awt.Font.PLAIN, 9f);
        java.awt.Font totalFnt  = base.deriveFont(java.awt.Font.BOLD, 9f);

        java.awt.Color HDR_BG   = new java.awt.Color(0x1a, 0x6f, 0xaf);
        java.awt.Color TOT_BG   = new java.awt.Color(0xd9, 0xe8, 0xf5);
        java.awt.Color BORDER   = new java.awt.Color(0xb0, 0xc4, 0xd8);
        java.awt.Color ALT_BG   = new java.awt.Color(0xea, 0xf4, 0xfb);
        java.awt.Color DARK     = new java.awt.Color(0x1a, 0x1a, 0x1a);

        // ── render ───────────────────────────────────────────────────────────
        java.awt.image.BufferedImage img =
                new java.awt.image.BufferedImage(W, H, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,      java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(java.awt.RenderingHints.KEY_FRACTIONALMETRICS, java.awt.RenderingHints.VALUE_FRACTIONALMETRICS_ON);
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
        y += 10;

        // ── info block ────────────────────────────────────────────────────────
        g.setFont(labelFnt);
        String nameAndAddress = (wrapper.getName() != null ? wrapper.getName() : "")
                + (wrapper.getAddress() != null ? wrapper.getAddress() : "");
        String[][] infoRows = {
                {"ರೇಷ್ಮೆ ನೂಲು ಬಿಚ್ಚಾಣಿಕೆದಾರರ ರಪದಾರಿ ಸಂಖ್ಯೆ",
                 request.getLicenseNumber() != null ? request.getLicenseNumber() : "",
                 "FRUITS ID / FID",
                 wrapper.getFruitsId() != null ? wrapper.getFruitsId() : "",
                 "Date and Time Stamp", generatedAt},
                {"ರೇಷ್ಮೆ ನೂಲು ಬಿಚ್ಚಾಣಿಕೆದಾರರ ಹೆಸರು ಮತ್ತು ವಿಳಾಸ",
                 nameAndAddress, "ಮೊತ್ತ: ರೂ ಗಳಲ್ಲಿ", "", "", ""},
                {"ವಹಿವಾಟಿನ ಅವಧಿ", fromStr + " to " + toStr, "", "", "", ""},
                {"ದಿನಾಂಕ " + fromStr + " ರಂದು ಇದ್ದ ಪ್ರಾರಂಭಿಕ ಉಳಿಕೆ ಮೊತ್ತ",
                 String.format("%.2f", wrapper.getOpeningBalance() != null ? wrapper.getOpeningBalance() : 0),
                 "", "", "", ""}
        };
        for (String[] row : infoRows) {
            int sixth = usable / 6;
            int textY = y + INFO_H / 2 + 4;
            g.setFont(labelFnt); g.setColor(DARK);
            g.drawString(row[0], MARGIN + 4, textY);
            g.setFont(dataFnt);
            g.drawString(row[1], MARGIN + sixth + 4, textY);
            if (!row[2].isEmpty()) {
                g.setFont(labelFnt); g.setColor(DARK);
                g.drawString(row[2], MARGIN + 2 * sixth + 4, textY);
                g.setFont(dataFnt);
                g.drawString(row[3], MARGIN + 3 * sixth + 4, textY);
            }
            if (!row[4].isEmpty()) {
                g.setFont(labelFnt); g.setColor(DARK);
                g.drawString(row[4], MARGIN + 4 * sixth + 4, textY);
                g.setFont(dataFnt);
                g.drawString(row[5], MARGIN + 5 * sixth + 4, textY);
            }
            g.setColor(BORDER);
            g.drawLine(MARGIN, y + INFO_H, W - MARGIN, y + INFO_H);
            y += INFO_H;
        }
        y += 10;

        // ── column headers ────────────────────────────────────────────────────
        String[] hdrs = {
                "ಕ್ರಮ ಸಂಖ್ಯೆ\nSL No",
                "ವಹಿವಾಟಿನ ದಿನಾಂಕ\nDate",
                "ವಿವರ\nDescription",
                "ವಹಿವಾಟಿನ ವಿಧ\nType",
                "ಠೇವಣಿ ಮಾಡಿದ ಮೊತ್ತ\nDeposit",
                "ಬಿತ್ತನೆ ರೇಷ್ಮೆ ಗೂಡಿನ ಪರಿಮಾಣ\nQty (kg)",
                "ದರ (ಪ್ರತಿ ಕಿ.ಗ್ರಾ)\nRate/Kg",
                "ಖರೀದಿಸಿದ ರೇಷ್ಮೆ ಗೂಡಿನ ಮೊತ್ತ\nPurchase Amt",
                "ಮಾರು ಕಟ್ಟೆ ಶುಲ್ಕ @1%\nMarket Fee",
                "ಒಟ್ಟು ಮೊತ್ತ\nTotal",
                "ಉಳಿಕೆ ಮೊತ್ತ\nBalance"
        };
        g.setFont(hdrFnt);
        for (int i = 0; i < 11; i++) {
            g.setColor(HDR_BG);
            g.fillRect(cx[i], y, cw[i], HDR_H);
            g.setColor(BORDER);
            g.drawRect(cx[i], y, cw[i], HDR_H);
            g.setColor(java.awt.Color.WHITE);
            String[] lines = hdrs[i].split("\n");
            int lineH = HDR_H / (lines.length + 1);
            for (int li = 0; li < lines.length; li++) {
                drawCenteredPdf(g, lines[li], cx[i], y + lineH * li, cw[i], lineH);
            }
        }
        y += HDR_H;

        // ── data rows ─────────────────────────────────────────────────────────
        int slNo = 1;
        for (ReelerTransactionReport rep : reports) {
            boolean alt = (slNo % 2 == 0);
            g.setColor(alt ? ALT_BG : java.awt.Color.WHITE);
            g.fillRect(MARGIN, y, usable, ROW_H);
            g.setColor(BORDER);
            g.drawRect(MARGIN, y, usable, ROW_H);
            for (int i = 1; i < 11; i++) {
                g.drawLine(cx[i], y, cx[i], y + ROW_H);
            }
            g.setFont(dataFnt);
            g.setColor(DARK);
            String[] vals = {
                    String.valueOf(slNo++),
                    rep.getTransactionDate() != null ? rep.getTransactionDate().toString() : "",
                    rep.getOperationDescription() != null ? rep.getOperationDescription() : "",
                    rep.getTransactionType() != null ? rep.getTransactionType() : "",
                    fmt(rep.getDepositAmount()),
                    fmt(rep.getLotWeight()),
                    fmt(rep.getRatePerKg()),
                    fmt(rep.getPaymentAmount()),
                    fmt(rep.getMarketFee()),
                    fmt(rep.getTotal()),
                    fmt(rep.getBalance())
            };
            for (int i = 0; i < 11; i++) {
                boolean isNum = i >= 4;
                if (isNum) {
                    drawRightPdf(g, vals[i], cx[i], y, cw[i], ROW_H);
                } else {
                    drawLeftPdf(g, vals[i], cx[i], y, cw[i], ROW_H);
                }
            }
            y += ROW_H;
        }

        // ── totals row ────────────────────────────────────────────────────────
        g.setColor(TOT_BG);
        g.fillRect(MARGIN, y, usable, ROW_H);
        g.setColor(BORDER);
        g.drawRect(MARGIN, y, usable, ROW_H);
        for (int i = 1; i < 11; i++) g.drawLine(cx[i], y, cx[i], y + ROW_H);
        g.setFont(totalFnt);
        g.setColor(DARK);
        drawCenteredPdf(g, "ಒಟ್ಟು / Total",
                cx[0], y, cw[0] + cw[1] + cw[2] + cw[3], ROW_H);
        drawRightPdf(g, fmt2(wrapper.getTotalDeposits()),       cx[4], y, cw[4], ROW_H);
        drawRightPdf(g, fmt2(wrapper.getTotalLotWeight()),      cx[5], y, cw[5], ROW_H);
        drawRightPdf(g, "",                                     cx[6], y, cw[6], ROW_H);
        drawRightPdf(g, fmt2(wrapper.getTotalPaymentAmount()),  cx[7], y, cw[7], ROW_H);
        drawRightPdf(g, fmt2(wrapper.getTotalMarketFee()),      cx[8], y, cw[8], ROW_H);
        drawRightPdf(g, fmt2(wrapper.getTotalPurchase()),       cx[9], y, cw[9], ROW_H);
        drawRightPdf(g, fmt2(wrapper.getClosingBalance()),      cx[10], y, cw[10], ROW_H);

        g.dispose();

        // ── embed in PDFBox ───────────────────────────────────────────────────
        java.io.File pdfFile = java.io.File.createTempFile("Seed_Market_Txn_Report_", ".pdf");
        try (org.apache.pdfbox.pdmodel.PDDocument doc = new org.apache.pdfbox.pdmodel.PDDocument()) {
            org.apache.pdfbox.pdmodel.common.PDRectangle rect =
                    new org.apache.pdfbox.pdmodel.common.PDRectangle(W * 72f / DPI, H * 72f / DPI);
            org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage(rect);
            doc.addPage(page);
            org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject pdImg =
                    org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory.createFromImage(doc, img);
            try (org.apache.pdfbox.pdmodel.PDPageContentStream cs =
                         new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page)) {
                cs.drawImage(pdImg, 0, 0, rect.getWidth(), rect.getHeight());
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

        final int COLS = 11;

        // ── Fonts ──────────────────────────────────────────────────────────────
        Font kannadaFont;
        try {
            java.io.File nirmala = new java.io.File("C:/Windows/Fonts/Nirmala.ttc");
            if (nirmala.exists()) {
                java.awt.Font[] awtFonts = java.awt.Font.createFonts(nirmala);
                java.awt.Font awtFont = null;
                for (java.awt.Font af : awtFonts) {
                    if (af.canDisplay('ಕ')) { awtFont = af; break; }
                }
                if (awtFont != null) {
                    kannadaFont = workbook.createFont();
                    kannadaFont.setFontName(awtFont.getFamily());
                } else { kannadaFont = workbook.createFont(); kannadaFont.setFontName("Arial"); }
            } else { kannadaFont = workbook.createFont(); kannadaFont.setFontName("Arial"); }
        } catch (Exception e) { kannadaFont = workbook.createFont(); kannadaFont.setFontName("Arial"); }
        kannadaFont.setFontHeightInPoints((short) 11);

        Font boldSmall = workbook.createFont();
        boldSmall.setFontName(kannadaFont.getFontName());
        boldSmall.setFontHeightInPoints((short) 9);
        boldSmall.setBold(true);

        Font titleFont = workbook.createFont();
        titleFont.setFontName(kannadaFont.getFontName());
        titleFont.setFontHeightInPoints((short) 13);
        titleFont.setBold(true);

        // ── Styles ─────────────────────────────────────────────────────────────
        CellStyle titleStyle = workbook.createCellStyle();
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle labelStyle = workbook.createCellStyle();
        labelStyle.setFont(boldSmall);
        labelStyle.setBorderBottom(BorderStyle.THIN);
        labelStyle.setBorderTop(BorderStyle.THIN);
        labelStyle.setBorderLeft(BorderStyle.THIN);
        labelStyle.setBorderRight(BorderStyle.THIN);
        labelStyle.setWrapText(true);

        CellStyle valueStyle = workbook.createCellStyle();
        valueStyle.setFont(workbook.createFont());
        valueStyle.setBorderBottom(BorderStyle.THIN);
        valueStyle.setBorderTop(BorderStyle.THIN);
        valueStyle.setBorderLeft(BorderStyle.THIN);
        valueStyle.setBorderRight(BorderStyle.THIN);
        valueStyle.setWrapText(true);

        Font colHdrFont = workbook.createFont();
        colHdrFont.setFontName(kannadaFont.getFontName());
        colHdrFont.setFontHeightInPoints((short) 8);
        colHdrFont.setBold(true);
        colHdrFont.setColor(IndexedColors.WHITE.getIndex());

        CellStyle colHeaderStyle = workbook.createCellStyle();
        colHeaderStyle.setFont(colHdrFont);
        colHeaderStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        colHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        colHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
        colHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        colHeaderStyle.setWrapText(true);
        colHeaderStyle.setBorderBottom(BorderStyle.THIN);
        colHeaderStyle.setBorderTop(BorderStyle.THIN);
        colHeaderStyle.setBorderLeft(BorderStyle.THIN);
        colHeaderStyle.setBorderRight(BorderStyle.THIN);

        CellStyle numberStyle = workbook.createCellStyle();
        numberStyle.setFont(workbook.createFont());
        numberStyle.setAlignment(HorizontalAlignment.RIGHT);
        numberStyle.setBorderBottom(BorderStyle.THIN);
        numberStyle.setBorderTop(BorderStyle.THIN);
        numberStyle.setBorderLeft(BorderStyle.THIN);
        numberStyle.setBorderRight(BorderStyle.THIN);

        CellStyle centerDataStyle = workbook.createCellStyle();
        centerDataStyle.setFont(workbook.createFont());
        centerDataStyle.setAlignment(HorizontalAlignment.CENTER);
        centerDataStyle.setBorderBottom(BorderStyle.THIN);
        centerDataStyle.setBorderTop(BorderStyle.THIN);
        centerDataStyle.setBorderLeft(BorderStyle.THIN);
        centerDataStyle.setBorderRight(BorderStyle.THIN);

        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setFont(workbook.createFont());
        dataStyle.setWrapText(true);
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        // ── Logo rows 0-2 ──────────────────────────────────────────────────────
        Row epLogoRow0 = sheet.createRow(0); epLogoRow0.setHeightInPoints(30);
        Row epLogoRow1 = sheet.createRow(1); epLogoRow1.setHeightInPoints(30);
        Row epLogoRow2 = sheet.createRow(2); epLogoRow2.setHeightInPoints(30);
        try {
            org.springframework.core.io.ClassPathResource logoRes =
                    new org.springframework.core.io.ClassPathResource("images/kar_logo.png");
            java.io.InputStream epLogoStream = logoRes.getInputStream();
            byte[] logoBytes = epLogoStream.readAllBytes();
            epLogoStream.close();
            int pictureIdx = workbook.addPicture(logoBytes, org.apache.poi.ss.usermodel.Workbook.PICTURE_TYPE_PNG);
            org.apache.poi.ss.usermodel.Drawing<?> drawing = sheet.createDrawingPatriarch();
            org.apache.poi.ss.usermodel.ClientAnchor anchor =
                    workbook.getCreationHelper().createClientAnchor();
            anchor.setCol1(5); anchor.setRow1(0);
            anchor.setAnchorType(org.apache.poi.ss.usermodel.ClientAnchor.AnchorType.DONT_MOVE_AND_RESIZE);
            org.apache.poi.ss.usermodel.Picture epLogoPic = drawing.createPicture(anchor, pictureIdx);
            epLogoPic.resize(0.3);
        } catch (Exception e) {
            e.printStackTrace();
        }

        int epRowIdx = 3;

        Row ep0 = sheet.createRow(epRowIdx++); ep0.setHeightInPoints(22);
        Cell epc0 = ep0.createCell(0); epc0.setCellValue("ಕರ್ನಾಟಕ ಸರ್ಕಾರ"); epc0.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(ep0.getRowNum(), ep0.getRowNum(), 0, COLS - 1));

        Row ep1 = sheet.createRow(epRowIdx++); ep1.setHeightInPoints(20);
        Cell epc1 = ep1.createCell(0); epc1.setCellValue("ರೇಷ್ಮೆ, ಇಲಾಖೆ"); epc1.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(ep1.getRowNum(), ep1.getRowNum(), 0, COLS - 1));

        Row ep2 = sheet.createRow(epRowIdx++); ep2.setHeightInPoints(20);
        Cell epc2 = ep2.createCell(0); epc2.setCellValue("ಸರ್ಕಾರಿ ರೇಷ್ಮೆ ಗೂಡಿನ ಮಾರುಕಟ್ಟೆ, " + marketName); epc2.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(ep2.getRowNum(), ep2.getRowNum(), 0, COLS - 1));

        Row ep3 = sheet.createRow(epRowIdx++); ep3.setHeightInPoints(20);
        Cell epc3 = ep3.createCell(0); epc3.setCellValue("ನೊಂದಾಯಿತ ರೇಷ್ಮೆ ಮೊಟ್ಟೆ ಉತ್ಪಾದಕರ ವಹಿವಾಟು ವರದಿ"); epc3.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(ep3.getRowNum(), ep3.getRowNum(), 0, COLS - 1));

        sheet.createRow(epRowIdx++); // blank

        // ── Info row: registration number | Date and Time Stamp ─────────────────
        String epTimestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        String epFrom = request.getFromDate() != null
                ? request.getFromDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
        String epTo   = request.getToDate() != null
                ? request.getToDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
        String epNameAddr = (wrapper.getName() != null ? wrapper.getName() : "")
                + (wrapper.getAddress() != null ? wrapper.getAddress() : "");

        Row ep5 = sheet.createRow(epRowIdx++); ep5.setHeightInPoints(18);
        ep5.createCell(0).setCellStyle(labelStyle);
        ep5.getCell(0).setCellValue("ನೊಂದಾಯಿತ ರೇಷ್ಮೆ ಮೊಟ್ಟೆ ಉತ್ಪಾದಕರ ನೊಂದಣಿ ಸಂಖ್ಯೆ");
        ep5.createCell(1).setCellStyle(labelStyle);
        ep5.createCell(2).setCellStyle(labelStyle);
        sheet.addMergedRegion(new CellRangeAddress(ep5.getRowNum(), ep5.getRowNum(), 0, 2));
        ep5.createCell(3).setCellStyle(valueStyle);
        ep5.getCell(3).setCellValue(request.getLicenseNumber() != null ? request.getLicenseNumber() : "");
        for (int i = 4; i <= 7; i++) ep5.createCell(i).setCellStyle(valueStyle);
        sheet.addMergedRegion(new CellRangeAddress(ep5.getRowNum(), ep5.getRowNum(), 3, 7));
        ep5.createCell(8).setCellStyle(labelStyle);
        ep5.getCell(8).setCellValue("Date and Time Stamp");
        ep5.createCell(9).setCellStyle(valueStyle);
        ep5.getCell(9).setCellValue(epTimestamp);
        ep5.createCell(10).setCellStyle(valueStyle);
        sheet.addMergedRegion(new CellRangeAddress(ep5.getRowNum(), ep5.getRowNum(), 9, 10));

        // ── Info row: name/address | amount in Rs ───────────────────────────────
        Row ep6 = sheet.createRow(epRowIdx++); ep6.setHeightInPoints(18);
        ep6.createCell(0).setCellStyle(labelStyle);
        ep6.getCell(0).setCellValue("ನೊಂದಾಯಿತ ರೇಷ್ಮೆ ಮೊಟ್ಟೆ ಉತ್ಪಾದಕರ ಹೆಸರು ಮತ್ತು ವಿಳಾಸ:");
        ep6.createCell(1).setCellStyle(labelStyle);
        ep6.createCell(2).setCellStyle(labelStyle);
        sheet.addMergedRegion(new CellRangeAddress(ep6.getRowNum(), ep6.getRowNum(), 0, 2));
        ep6.createCell(3).setCellStyle(valueStyle);
        ep6.getCell(3).setCellValue(epNameAddr);
        for (int i = 4; i <= 7; i++) ep6.createCell(i).setCellStyle(valueStyle);
        sheet.addMergedRegion(new CellRangeAddress(ep6.getRowNum(), ep6.getRowNum(), 3, 7));
        ep6.createCell(8).setCellStyle(labelStyle);
        ep6.getCell(8).setCellValue("ಮೊತ್ತ: ರೂ ಗಳಲ್ಲಿ");
        ep6.createCell(9).setCellStyle(labelStyle);
        ep6.createCell(10).setCellStyle(labelStyle);
        sheet.addMergedRegion(new CellRangeAddress(ep6.getRowNum(), ep6.getRowNum(), 8, 10));

        // ── Info row: transaction period ────────────────────────────────────────
        Row ep7 = sheet.createRow(epRowIdx++); ep7.setHeightInPoints(18);
        ep7.createCell(0).setCellStyle(labelStyle);
        ep7.getCell(0).setCellValue("ವಹಿವಾಟಿನ ಅವಧಿ :");
        ep7.createCell(1).setCellStyle(labelStyle);
        ep7.createCell(2).setCellStyle(labelStyle);
        sheet.addMergedRegion(new CellRangeAddress(ep7.getRowNum(), ep7.getRowNum(), 0, 2));
        ep7.createCell(3).setCellStyle(valueStyle);
        ep7.getCell(3).setCellValue(epFrom + " to " + epTo);
        for (int i = 4; i <= 10; i++) ep7.createCell(i).setCellStyle(valueStyle);
        sheet.addMergedRegion(new CellRangeAddress(ep7.getRowNum(), ep7.getRowNum(), 3, 10));

        // ── Info row: opening balance ────────────────────────────────────────────
        Row ep8 = sheet.createRow(epRowIdx++); ep8.setHeightInPoints(18);
        ep8.createCell(0).setCellStyle(labelStyle);
        ep8.getCell(0).setCellValue("ದಿನಾಂಕ " + epFrom + " ರಂದು ಇದ್ದ ಪ್ರಾರಂಭಿಕ ಉಳಿಕೆ ಮೊತ್ತ :");
        ep8.createCell(1).setCellStyle(labelStyle);
        ep8.createCell(2).setCellStyle(labelStyle);
        sheet.addMergedRegion(new CellRangeAddress(ep8.getRowNum(), ep8.getRowNum(), 0, 2));
        ep8.createCell(3).setCellStyle(numberStyle);
        ep8.getCell(3).setCellValue(wrapper.getOpeningBalance() != null ? wrapper.getOpeningBalance() : 0);
        for (int i = 4; i <= 10; i++) ep8.createCell(i).setCellStyle(valueStyle);
        sheet.addMergedRegion(new CellRangeAddress(ep8.getRowNum(), ep8.getRowNum(), 4, 10));

        sheet.createRow(epRowIdx++); // blank

        // ── Column headers ─────────────────────────────────────────────────────
        Row epColHdr = sheet.createRow(epRowIdx++); epColHdr.setHeightInPoints(55);
        String[] epHeaders = {
                "ಕ್ರಮ ಸಂಖ್ಯೆ / SL No",
                "ವಹಿವಾಟಿನ ದಿನಾಂಕ / Transaction Date",
                "ವಿವರ / Description",
                "ವಹಿವಾಟಿನ ವಿಧ / Transaction Type",
                "ಠೇವಣಿ ಮಾಡಿದ ಮೊತ್ತ / Deposit Amount",
                "ಖರೀದಿಸಿದ ಬಿತ್ತನೆ ರೇಷ್ಮೆ ಗೂಡಿನ ಪರಿಮಾಣ / Quantity of seed cocoon purchased(in kg's)",
                "ಬಿತ್ತನೆ ಗೂಡಿನ ಸಂಖ್ಯೆ (ಪ್ರತಿ ಕೆ.ಜಿಗೆ) / No. of Seed Cocoons per Kg",
                "ಒಟ್ಟು ಬಿತ್ತನೆ ಗೂಡಿನ ಸಂಖ್ಯೆ / Total No of Seed Cocoons (in No's)",
                "ದರ ಪ್ರತಿ ಕೆ.ಜಿ.ಗೆ / Rate per Kg",
                "ಖರೀದಿಸಿದ ರೇಷ್ಮೆ ಗೂಡಿನ ಮೊತ್ತ / Seed Cocoon Purchase Amount",
                "ಉಳಿಕೆ ಮೊತ್ತ / Balance Amount"
        };
        for (int i = 0; i < epHeaders.length; i++) {
            Cell c = epColHdr.createCell(i); c.setCellValue(epHeaders[i]); c.setCellStyle(colHeaderStyle);
        }

        CellStyle epTotalStyle = workbook.createCellStyle();
        epTotalStyle.setFont(boldSmall);
        epTotalStyle.setBorderTop(BorderStyle.THIN); epTotalStyle.setBorderBottom(BorderStyle.THIN);
        epTotalStyle.setBorderLeft(BorderStyle.THIN); epTotalStyle.setBorderRight(BorderStyle.THIN);
        epTotalStyle.setAlignment(HorizontalAlignment.LEFT);
        epTotalStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        epTotalStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        epTotalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // ── Data rows ──────────────────────────────────────────────────────────
        int epSlNo = 1;
        double epSumDeposit = 0, epSumLotWeight = 0, epSumTotalCocoons = 0, epSumPayment = 0;
        double epLastBalance = 0;
        double epRunBalance = wrapper.getOpeningBalance() != null ? wrapper.getOpeningBalance() : 0;

        for (ReelerTransactionReport report : wrapper.getReelerTransactionReports()) {
            Row row = sheet.createRow(epRowIdx++); row.setHeightInPoints(16);

            Cell d0 = row.createCell(0); d0.setCellValue(epSlNo++); d0.setCellStyle(centerDataStyle);
            Cell d1 = row.createCell(1); d1.setCellValue(report.getTransactionDate() != null ? report.getTransactionDate().toString() : ""); d1.setCellStyle(centerDataStyle);
            Cell d2 = row.createCell(2); d2.setCellValue(report.getOperationDescription() != null ? report.getOperationDescription() : ""); d2.setCellStyle(dataStyle);
            Cell d3 = row.createCell(3); d3.setCellValue(report.getTransactionType() != null ? report.getTransactionType() : ""); d3.setCellStyle(centerDataStyle);

            double depositVal = report.getDepositAmount() != null ? report.getDepositAmount() : 0;
            Cell d4 = row.createCell(4); d4.setCellValue(depositVal); d4.setCellStyle(numberStyle);
            epSumDeposit += depositVal;

            double lwVal = report.getLotWeight() != null ? report.getLotWeight() : 0;
            Cell d5 = row.createCell(5); d5.setCellValue(lwVal); d5.setCellStyle(numberStyle);
            epSumLotWeight += lwVal;

            int qtyNosVal = report.getQtyNos() != null ? report.getQtyNos() : 0;
            Cell d6 = row.createCell(6); d6.setCellValue(qtyNosVal); d6.setCellStyle(numberStyle);

            double totalCocoons = lwVal * qtyNosVal;
            Cell d7 = row.createCell(7); d7.setCellValue(totalCocoons); d7.setCellStyle(numberStyle);
            epSumTotalCocoons += totalCocoons;

            Cell d8 = row.createCell(8); d8.setCellValue(report.getRatePerKg() != null ? report.getRatePerKg() : 0); d8.setCellStyle(numberStyle);

            double payVal = report.getPaymentAmount() != null ? report.getPaymentAmount() : 0;
            Cell d9 = row.createCell(9); d9.setCellValue(payVal); d9.setCellStyle(numberStyle);
            epSumPayment += payVal;

            if ("C".equalsIgnoreCase(report.getTransactionType())) {
                epRunBalance += depositVal;
            } else {
                epRunBalance -= payVal;
            }
            epLastBalance = epRunBalance;
            Cell d10 = row.createCell(10); d10.setCellValue(epLastBalance); d10.setCellStyle(numberStyle);
        }

        // ── Totals row ─────────────────────────────────────────────────────────
        Row epTotal = sheet.createRow(epRowIdx++); epTotal.setHeightInPoints(18);
        Cell etc0 = epTotal.createCell(0); etc0.setCellValue("ಒಟ್ಟು"); etc0.setCellStyle(epTotalStyle);
        sheet.addMergedRegion(new CellRangeAddress(epTotal.getRowNum(), epTotal.getRowNum(), 0, 3));
        for (int i = 1; i <= 3; i++) epTotal.createCell(i).setCellStyle(epTotalStyle);
        Cell etc4 = epTotal.createCell(4); etc4.setCellValue(epSumDeposit); etc4.setCellStyle(epTotalStyle);
        Cell etc5 = epTotal.createCell(5); etc5.setCellValue(epSumLotWeight); etc5.setCellStyle(epTotalStyle);
        Cell etc6 = epTotal.createCell(6); etc6.setCellValue(""); etc6.setCellStyle(epTotalStyle);
        Cell etc7 = epTotal.createCell(7); etc7.setCellValue(epSumTotalCocoons); etc7.setCellStyle(epTotalStyle);
        Cell etc8 = epTotal.createCell(8); etc8.setCellValue(""); etc8.setCellStyle(epTotalStyle);
        Cell etc9 = epTotal.createCell(9); etc9.setCellValue(epSumPayment); etc9.setCellStyle(epTotalStyle);
        Cell etc10 = epTotal.createCell(10); etc10.setCellValue(epLastBalance); etc10.setCellStyle(epTotalStyle);

        int[] epMin = { 2000, 3500, 8000, 3000, 3500, 4000, 4000, 4500, 3500, 4500, 3500 };
        int[] epMax = { 3000, 5000, 13000, 4500, 5500, 6000, 6000, 6500, 5000, 6000, 5000 };
        for (int i = 0; i < COLS; i++) {
            sheet.autoSizeColumn(i);
            int w = sheet.getColumnWidth(i);
            if (w < epMin[i]) w = epMin[i];
            if (w > epMax[i]) w = epMax[i];
            sheet.setColumnWidth(i, w);
        }
        sheet.getPrintSetup().setLandscape(true);
        sheet.getPrintSetup().setPaperSize(org.apache.poi.ss.usermodel.PrintSetup.A3_PAPERSIZE);
        sheet.setFitToPage(true);
        sheet.getPrintSetup().setFitWidth((short) 1);
        sheet.getPrintSetup().setFitHeight((short) 0);

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

        int DPI = 150;
        int W = (int)(16.54 * DPI);
        int MARGIN = 40;
        int LOGO_H = 80;
        int INFO_H = 30;
        int HDR_H  = 55;
        int ROW_H  = 32;
        int TITLE_H = 30;
        int H = MARGIN + LOGO_H + 8
                + 4 * TITLE_H + 10
                + 4 * INFO_H + 10
                + HDR_H
                + reports.size() * ROW_H
                + ROW_H
                + MARGIN;

        java.awt.image.BufferedImage logoImg = null;
        try {
            org.springframework.core.io.ClassPathResource logoRes =
                    new org.springframework.core.io.ClassPathResource("images/kar_logo.png");
            logoImg = javax.imageio.ImageIO.read(logoRes.getInputStream());
        } catch (Exception ignored) {}

        int usable = W - 2 * MARGIN;
        float[] proportions = {0.04f, 0.10f, 0.16f, 0.06f, 0.08f, 0.07f, 0.08f, 0.08f, 0.09f, 0.10f, 0.14f};
        int[] cw = new int[11];
        int allocated = 0;
        for (int i = 0; i < 10; i++) { cw[i] = (int)(usable * proportions[i]); allocated += cw[i]; }
        cw[10] = usable - allocated;
        int[] cx = new int[11];
        cx[0] = MARGIN;
        for (int i = 1; i < 11; i++) cx[i] = cx[i - 1] + cw[i - 1];

        java.awt.Font base = loadKannadaPdfFont(15f);
        java.awt.Font titleFnt = base.deriveFont(java.awt.Font.BOLD, 18f);
        java.awt.Font labelFnt = base.deriveFont(java.awt.Font.BOLD, 11f);
        java.awt.Font hdrFnt   = base.deriveFont(java.awt.Font.BOLD, 9f);
        java.awt.Font dataFnt  = base.deriveFont(java.awt.Font.PLAIN, 9f);
        java.awt.Font totalFnt = base.deriveFont(java.awt.Font.BOLD, 9f);

        java.awt.Color HDR_BG = new java.awt.Color(0x1a, 0x6f, 0xaf);
        java.awt.Color TOT_BG = new java.awt.Color(0xd9, 0xe8, 0xf5);
        java.awt.Color BORDER = new java.awt.Color(0xb0, 0xc4, 0xd8);
        java.awt.Color ALT_BG = new java.awt.Color(0xea, 0xf4, 0xfb);
        java.awt.Color DARK   = new java.awt.Color(0x1a, 0x1a, 0x1a);

        java.awt.image.BufferedImage img =
                new java.awt.image.BufferedImage(W, H, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,      java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(java.awt.RenderingHints.KEY_FRACTIONALMETRICS, java.awt.RenderingHints.VALUE_FRACTIONALMETRICS_ON);
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
        y += 10;

        g.setFont(labelFnt);
        String epNameAddr = (wrapper.getName() != null ? wrapper.getName() : "")
                + (wrapper.getAddress() != null ? wrapper.getAddress() : "");
        String[][] epInfoRows = {
                {"ನೊಂದಾಯಿತ ರೇಷ್ಮೆ ಮೊಟ್ಟೆ ಉತ್ಪಾದಕರ ನೊಂದಣಿ ಸಂಖ್ಯೆ",
                 request.getLicenseNumber() != null ? request.getLicenseNumber() : "",
                 "Date and Time Stamp", epGenAt, "", ""},
                {"ನೊಂದಾಯಿತ ರೇಷ್ಮೆ ಮೊಟ್ಟೆ ಉತ್ಪಾದಕರ ಹೆಸರು ಮತ್ತು ವಿಳಾಸ",
                 epNameAddr, "ಮೊತ್ತ: ರೂ ಗಳಲ್ಲಿ", "", "", ""},
                {"ವಹಿವಾಟಿನ ಅವಧಿ", epFromStr + " to " + epToStr, "", "", "", ""},
                {"ದಿನಾಂಕ " + epFromStr + " ರಂದು ಇದ್ದ ಪ್ರಾರಂಭಿಕ ಉಳಿಕೆ ಮೊತ್ತ",
                 String.format("%.2f", wrapper.getOpeningBalance() != null ? wrapper.getOpeningBalance() : 0),
                 "", "", "", ""}
        };
        for (String[] infoRow : epInfoRows) {
            int sixth = usable / 6;
            int textY = y + INFO_H / 2 + 4;
            g.setFont(labelFnt); g.setColor(DARK);
            g.drawString(infoRow[0], MARGIN + 4, textY);
            g.setFont(dataFnt);
            g.drawString(infoRow[1], MARGIN + sixth + 4, textY);
            if (!infoRow[2].isEmpty()) {
                g.setFont(labelFnt); g.setColor(DARK);
                g.drawString(infoRow[2], MARGIN + 2 * sixth + 4, textY);
                g.setFont(dataFnt);
                g.drawString(infoRow[3], MARGIN + 3 * sixth + 4, textY);
            }
            if (!infoRow[4].isEmpty()) {
                g.setFont(labelFnt); g.setColor(DARK);
                g.drawString(infoRow[4], MARGIN + 4 * sixth + 4, textY);
                g.setFont(dataFnt);
                g.drawString(infoRow[5], MARGIN + 5 * sixth + 4, textY);
            }
            g.setColor(BORDER); g.drawLine(MARGIN, y + INFO_H, W - MARGIN, y + INFO_H);
            y += INFO_H;
        }
        y += 10;

        String[] epHdrs = {
                "ಕ್ರಮ ಸಂಖ್ಯೆ\nSL No",
                "ವಹಿವಾಟಿನ ದಿನಾಂಕ\nDate",
                "ವಿವರ\nDescription",
                "ವಹಿವಾಟಿನ ವಿಧ\nType",
                "ಠೇವಣಿ ಮೊತ್ತ\nDeposit",
                "ಬಿತ್ತನೆ ಗೂಡಿನ ಪರಿಮಾಣ\nQty (kg)",
                "ಗೂಡಿನ ಸಂಖ್ಯೆ/ಕೆ.ಜಿ\nCocoons/Kg",
                "ಒಟ್ಟು ಗೂಡಿನ ಸಂಖ್ಯೆ\nTotal Cocoons",
                "ದರ ಪ್ರತಿ ಕೆ.ಜಿ\nRate/Kg",
                "ಖರೀದಿ ಮೊತ್ತ\nPayment Amt",
                "ಉಳಿಕೆ ಮೊತ್ತ\nBalance"
        };
        g.setFont(hdrFnt);
        for (int i = 0; i < 11; i++) {
            g.setColor(HDR_BG); g.fillRect(cx[i], y, cw[i], HDR_H);
            g.setColor(BORDER); g.drawRect(cx[i], y, cw[i], HDR_H);
            g.setColor(java.awt.Color.WHITE);
            String[] lines = epHdrs[i].split("\n");
            int lineH = HDR_H / (lines.length + 1);
            for (int li = 0; li < lines.length; li++) {
                drawCenteredPdf(g, lines[li], cx[i], y + lineH * li, cw[i], lineH);
            }
        }
        y += HDR_H;

        int epPdfSlNo = 1;
        double epPdfSumDeposit = 0, epPdfSumLotWeight = 0, epPdfSumTotalCocoons = 0, epPdfSumPayment = 0;
        double epPdfLastBalance = wrapper.getOpeningBalance() != null ? wrapper.getOpeningBalance() : 0;

        for (ReelerTransactionReport rep : reports) {
            boolean alt = (epPdfSlNo % 2 == 0);
            g.setColor(alt ? ALT_BG : java.awt.Color.WHITE); g.fillRect(MARGIN, y, usable, ROW_H);
            g.setColor(BORDER); g.drawRect(MARGIN, y, usable, ROW_H);
            for (int i = 1; i < 11; i++) g.drawLine(cx[i], y, cx[i], y + ROW_H);

            double depV = rep.getDepositAmount() != null ? rep.getDepositAmount() : 0;
            double lwV  = rep.getLotWeight()     != null ? rep.getLotWeight()     : 0;
            int    qnV  = rep.getQtyNos()        != null ? rep.getQtyNos()        : 0;
            double tcV  = lwV * qnV;
            double rpV  = rep.getRatePerKg()     != null ? rep.getRatePerKg()     : 0;
            double pyV  = rep.getPaymentAmount() != null ? rep.getPaymentAmount() : 0;

            if ("C".equalsIgnoreCase(rep.getTransactionType())) {
                epPdfLastBalance += depV;
            } else {
                epPdfLastBalance -= pyV;
            }
            epPdfSumDeposit += depV; epPdfSumLotWeight += lwV;
            epPdfSumTotalCocoons += tcV; epPdfSumPayment += pyV;

            g.setFont(dataFnt); g.setColor(DARK);
            String[] vals = {
                    String.valueOf(epPdfSlNo++),
                    rep.getTransactionDate() != null ? rep.getTransactionDate().toString() : "",
                    rep.getOperationDescription() != null ? rep.getOperationDescription() : "",
                    rep.getTransactionType() != null ? rep.getTransactionType() : "",
                    fmt(depV), fmt(lwV),
                    qnV == 0 ? "" : String.valueOf(qnV),
                    tcV == 0 ? "" : fmt(tcV),
                    rpV == 0 ? "" : fmt(rpV),
                    fmt(pyV), fmt(epPdfLastBalance)
            };
            for (int i = 0; i < 11; i++) {
                if (i >= 4) drawRightPdf(g, vals[i], cx[i], y, cw[i], ROW_H);
                else        drawLeftPdf (g, vals[i], cx[i], y, cw[i], ROW_H);
            }
            y += ROW_H;
        }

        g.setColor(TOT_BG); g.fillRect(MARGIN, y, usable, ROW_H);
        g.setColor(BORDER); g.drawRect(MARGIN, y, usable, ROW_H);
        for (int i = 1; i < 11; i++) g.drawLine(cx[i], y, cx[i], y + ROW_H);
        g.setFont(totalFnt); g.setColor(DARK);
        drawCenteredPdf(g, "ಒಟ್ಟು / Total", cx[0], y, cw[0] + cw[1] + cw[2] + cw[3], ROW_H);
        drawRightPdf(g, fmt2(epPdfSumDeposit),      cx[4], y, cw[4], ROW_H);
        drawRightPdf(g, fmt2(epPdfSumLotWeight),     cx[5], y, cw[5], ROW_H);
        drawRightPdf(g, "",                          cx[6], y, cw[6], ROW_H);
        drawRightPdf(g, fmt2(epPdfSumTotalCocoons),  cx[7], y, cw[7], ROW_H);
        drawRightPdf(g, "",                          cx[8], y, cw[8], ROW_H);
        drawRightPdf(g, fmt2(epPdfSumPayment),       cx[9], y, cw[9], ROW_H);
        drawRightPdf(g, fmt2(epPdfLastBalance),      cx[10], y, cw[10], ROW_H);

        g.dispose();

        java.io.File pdfFile = java.io.File.createTempFile("Egg_Producer_Txn_Report_", ".pdf");
        try (org.apache.pdfbox.pdmodel.PDDocument doc = new org.apache.pdfbox.pdmodel.PDDocument()) {
            org.apache.pdfbox.pdmodel.common.PDRectangle rect =
                    new org.apache.pdfbox.pdmodel.common.PDRectangle(W * 72f / DPI, H * 72f / DPI);
            org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage(rect);
            doc.addPage(page);
            org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject pdImg =
                    org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory.createFromImage(doc, img);
            try (org.apache.pdfbox.pdmodel.PDPageContentStream cs =
                         new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page)) {
                cs.drawImage(pdImg, 0, 0, rect.getWidth(), rect.getHeight());
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
