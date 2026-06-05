package com.sericulture.marketandauction.service;

import com.sericulture.marketandauction.helper.MarketAuctionHelper;
import com.sericulture.marketandauction.helper.Util;
import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.entity.*;
import com.sericulture.marketandauction.model.exceptions.ValidationException;
import com.sericulture.marketandauction.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Slf4j
public class ReelerBankTransferService {

    @Autowired
    private ReelerAuctionRepository reelerAuctionRepository;

    @Autowired
    private LotGroupageRepository lotGroupageRepository;

    @Autowired
    private ReelerVidDebitTxnRepository reelerVidDebitTxnRepository;

    @Autowired
    private ReelerBankTransferRepository reelerBankTransferRepository;

    @Autowired
    private TransactionFileGenQueueRepository transactionFileGenQueueRepository;

    @Autowired
    private TransactionFileGenRepository transactionFileGenRepository;

    @Autowired
    private MarketAuctionFileDowndloadService marketAuctionFileDowndloadService;

    @Autowired
    private MarketAuctionHelper marketAuctionHelper;

    // -------------------------------------------------------------------------
    // SEARCH — show current balance and transferable amount
    // -------------------------------------------------------------------------

    /**
     * Reeler column map (getReelerDetailsByLicense):
     *   [0]=reeler_id  [1]=name  [2]=license  [3]=mobile
     *   [4]=virtual_account  [5]=current_balance  [6]=minimum_balance
     *   [7]=bank_account_number  [8]=ifsc_code  [9]=bank_name  [10]=branch_name
     *
     * RSP column map (getExternalUnitDetailsByLicense):
     *   [0]=eu_id  [1]=name  [2]=license  [3]=virtual_account
     *   [4]=current_balance  [5]=minimum_balance
     *   [6]=bank_account_number  [7]=bank_ifsc_code  [8]=bank_name  [9]=bank_branch_name
     *   [10]=eu_market_id
     */
    public ResponseEntity<?> getReelerBalance(String licenseNumber, String buyerType) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(Map.class);

        if ("RSP".equals(buyerType)) {
            Object[][] data = lotGroupageRepository.getExternalUnitDetailsByLicense(licenseNumber);
            if (data == null || data.length == 0) {
                return marketAuctionHelper.retrunIfError(rw, "No RSP found for license number: " + licenseNumber);
            }
            Object[] row = data[0];
            double currentBalance     = Util.objectToFloat(row[4]);
            double minimumBalance     = Util.objectToFloat(row[5]);
            double transferableAmount = Math.max(0, currentBalance - minimumBalance);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("buyerId",              Util.objectToInteger(row[0]));
            result.put("buyerType",            "RSP");
            result.put("name",                 Util.objectToString(row[1]));
            result.put("licenseNumber",        Util.objectToString(row[2]));
            result.put("virtualAccountNumber", Util.objectToString(row[3]));
            result.put("currentBalance",       currentBalance);
            result.put("minimumBalance",       minimumBalance);
            result.put("transferableAmount",   transferableAmount);
            result.put("bankAccountNumber",    Util.objectToString(row[6]));
            result.put("ifscCode",             Util.objectToString(row[7]));
            result.put("bankName",             Util.objectToString(row[8]));
            result.put("branchName",           Util.objectToString(row[9]));
            rw.setContent(result);
            return ResponseEntity.ok(rw);
        }

        // Default: Reeling
        Object[][] data = reelerAuctionRepository.getReelerDetailsByLicense(licenseNumber);
        if (data == null || data.length == 0) {
            return marketAuctionHelper.retrunIfError(rw, "No reeler found for license number: " + licenseNumber);
        }
        Object[] row = data[0];
        double currentBalance     = Util.objectToFloat(row[5]);
        double minimumBalance     = Util.objectToFloat(row[6]);
        double transferableAmount = Math.max(0, currentBalance - minimumBalance);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("buyerId",              Util.objectToInteger(row[0]));
        result.put("buyerType",            "Reeling");
        result.put("name",                 Util.objectToString(row[1]));
        result.put("licenseNumber",        Util.objectToString(row[2]));
        result.put("mobileNumber",         Util.objectToString(row[3]));
        result.put("virtualAccountNumber", Util.objectToString(row[4]));
        result.put("currentBalance",       currentBalance);
        result.put("minimumBalance",       minimumBalance);
        result.put("transferableAmount",   transferableAmount);
        result.put("bankAccountNumber",    Util.objectToString(row[7]));
        result.put("ifscCode",             Util.objectToString(row[8]));
        result.put("bankName",             Util.objectToString(row[9]));
        result.put("branchName",           Util.objectToString(row[10]));
        rw.setContent(result);
        return ResponseEntity.ok(rw);
    }

    // -------------------------------------------------------------------------
    // TRANSFER — debit virtual account, save record, queue CSV generation
    // -------------------------------------------------------------------------

    /**
     * Reeler column map (getReelerBankDetails):
     *   [0]=reeler_id  [1]=name  [2]=license
     *   [3]=virtual_account  [4]=current_balance  [5]=minimum_balance
     *   [6]=bank_account_number  [7]=ifsc_code  [8]=bank_name  [9]=branch_name
     *   [10]=reeler_market_id
     *
     * RSP column map (getExternalUnitBankDetailsById):
     *   [0]=eu_id  [1]=name  [2]=license  [3]=virtual_account
     *   [4]=current_balance  [5]=minimum_balance
     *   [6]=bank_account_number  [7]=bank_ifsc_code  [8]=bank_name  [9]=bank_branch_name
     *   [10]=eu_market_id
     */
    @Transactional
    public ResponseEntity<?> transferToBank(int buyerId, String buyerType, int marketId,
                                            double transferAmount, String fileName, String comment) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(Map.class);
        LocalDate today = Util.getISTLocalDate();

        Object[] rd;
        int buyerMarketId;
        String virtualAccount;
        double currentBalance;
        double minimumBalance;

        if ("RSP".equals(buyerType)) {
            Object[][] data = lotGroupageRepository.getExternalUnitBankDetailsById(buyerId);
            if (data == null || data.length == 0) {
                return marketAuctionHelper.retrunIfError(rw, "RSP details not found for id: " + buyerId);
            }
            rd            = data[0];
            // Fall back to request marketId if eu_virtual_bank_account row is absent
            buyerMarketId = rd[10] != null ? Util.objectToInteger(rd[10]) : marketId;
            virtualAccount = Util.objectToString(rd[3]);
            currentBalance = Util.objectToFloat(rd[4]);
            minimumBalance = Util.objectToFloat(rd[5]);
        } else {
            Object[][] data = reelerAuctionRepository.getReelerBankDetails(buyerId);
            if (data == null || data.length == 0) {
                return marketAuctionHelper.retrunIfError(rw, "Reeler details not found for reelerId: " + buyerId);
            }
            rd            = data[0];
            buyerMarketId = Util.objectToInteger(rd[10]);
            virtualAccount = Util.objectToString(rd[3]);
            currentBalance = Util.objectToFloat(rd[4]);
            minimumBalance = Util.objectToFloat(rd[5]);
        }

        double maxTransferable = currentBalance - minimumBalance;
        if (maxTransferable <= 0) {
            return marketAuctionHelper.retrunIfError(rw,
                    "No transferable amount. Current balance (" + currentBalance
                    + ") is at or below minimum balance (" + minimumBalance + ").");
        }
        if (transferAmount <= 0) {
            return marketAuctionHelper.retrunIfError(rw, "Transfer amount must be greater than 0.");
        }
        if (transferAmount > maxTransferable) {
            return marketAuctionHelper.retrunIfError(rw,
                    "Transfer amount (" + transferAmount + ") exceeds the maximum transferable amount ("
                    + maxTransferable + "). Minimum balance of " + minimumBalance
                    + " must be retained in the virtual account.");
        }

        // 1. Debit virtual account
        reelerVidDebitTxnRepository.save(new ReelerVidDebitTxn(
                0, buyerMarketId, today, buyerId, virtualAccount, transferAmount));

        // 2. Save ReelerBankTransfer record
        ReelerBankTransfer bankTransfer = new ReelerBankTransfer();
        bankTransfer.setBuyerType(buyerType);
        if ("RSP".equals(buyerType)) {
            bankTransfer.setExternalUnitId(buyerId);
        } else {
            bankTransfer.setReelerId(buyerId);
        }
        bankTransfer.setVirtualAccountNumber(virtualAccount);
        bankTransfer.setBankAccountNumber(Util.objectToString(rd[6]));
        bankTransfer.setIfscCode(Util.objectToString(rd[7]));
        bankTransfer.setBankName(Util.objectToString(rd[8]));
        bankTransfer.setBranchName(Util.objectToString(rd[9]));
        bankTransfer.setTransferAmount(transferAmount);
        bankTransfer.setMarketId(buyerMarketId);
        bankTransfer.setAuctionDate(today);
        bankTransfer.setTransferStatus("PENDING");
        bankTransfer.setComment(comment);
        String prefix = "RSP".equals(buyerType) ? "RSP" : "RBT";
        String crn = prefix + today.toString().replace("-", "")
                + String.format("%03d", buyerMarketId)
                + String.format("%04d", buyerId);
        bankTransfer.setCustomerReferenceNumber(crn);
        reelerBankTransferRepository.save(bankTransfer);

        // 3. Queue CSV generation
        if (!transactionFileGenQueueRepository
                .existsTransactionFileGenQueueByMarketIdAndFileName(buyerMarketId, fileName)) {
            TransactionFileGenQueue queue = TransactionFileGenQueue.builder()
                    .marketId(buyerMarketId)
                    .auctionDate(today)
                    .fileName(fileName)
                    .comment("RSP".equals(buyerType) ? "RSP_BANK_TRANSFER" : "REELER_BANK_TRANSFER")
                    .status("requested")
                    .build();
            transactionFileGenQueueRepository.save(queue);
        }

        rw.setContent(Map.of(
                "message",           "Transfer of " + transferAmount + " initiated successfully.",
                "transferAmount",    transferAmount,
                "bankAccountNumber", Util.objectToString(rd[6]),
                "ifscCode",          Util.objectToString(rd[7]),
                "fileName",          fileName
        ));
        return ResponseEntity.ok(rw);
    }

    // -------------------------------------------------------------------------
    // BALANCE BY ID — used after UI selects from buyer dropdown
    // -------------------------------------------------------------------------

    public ResponseEntity<?> getBalanceById(int buyerId, String buyerType) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(Map.class);

        if ("RSP".equals(buyerType)) {
            Object[][] data = lotGroupageRepository.getExternalUnitBankDetailsById(buyerId);
            if (data == null || data.length == 0) {
                return marketAuctionHelper.retrunIfError(rw, "No RSP found for id: " + buyerId);
            }
            Object[] row = data[0];
            double currentBalance     = Util.objectToFloat(row[4]);
            double minimumBalance     = Util.objectToFloat(row[5]);
            double transferableAmount = Math.max(0, currentBalance - minimumBalance);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("buyerId",              Util.objectToInteger(row[0]));
            result.put("buyerType",            "RSP");
            result.put("name",                 Util.objectToString(row[1]));
            result.put("licenseNumber",        Util.objectToString(row[2]));
            result.put("virtualAccountNumber", Util.objectToString(row[3]));
            result.put("currentBalance",       currentBalance);
            result.put("minimumBalance",       minimumBalance);
            result.put("transferableAmount",   transferableAmount);
            result.put("bankAccountNumber",    Util.objectToString(row[6]));
            result.put("ifscCode",             Util.objectToString(row[7]));
            result.put("bankName",             Util.objectToString(row[8]));
            result.put("branchName",           Util.objectToString(row[9]));
            rw.setContent(result);
            return ResponseEntity.ok(rw);
        }

        // Default: Reeling
        Object[][] data = reelerAuctionRepository.getReelerBankDetails(buyerId);
        if (data == null || data.length == 0) {
            return marketAuctionHelper.retrunIfError(rw, "No reeler found for id: " + buyerId);
        }
        Object[] row = data[0];
        double currentBalance     = Util.objectToFloat(row[4]);
        double minimumBalance     = Util.objectToFloat(row[5]);
        double transferableAmount = Math.max(0, currentBalance - minimumBalance);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("buyerId",              Util.objectToInteger(row[0]));
        result.put("buyerType",            "Reeling");
        result.put("name",                 Util.objectToString(row[1]));
        result.put("licenseNumber",        Util.objectToString(row[2]));
        result.put("virtualAccountNumber", Util.objectToString(row[3]));
        result.put("currentBalance",       currentBalance);
        result.put("minimumBalance",       minimumBalance);
        result.put("transferableAmount",   transferableAmount);
        result.put("bankAccountNumber",    Util.objectToString(row[6]));
        result.put("ifscCode",             Util.objectToString(row[7]));
        result.put("bankName",             Util.objectToString(row[8]));
        result.put("branchName",           Util.objectToString(row[9]));
        rw.setContent(result);
        return ResponseEntity.ok(rw);
    }

    // -------------------------------------------------------------------------
    // BUYER LIST — populate dropdown by market + buyerType
    // -------------------------------------------------------------------------

    public ResponseEntity<?> getBuyerList(int marketId, String buyerType) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(Map.class);
        if ("RSP".equals(buyerType)) {
            rw.setContent(lotGroupageRepository.getExternalUnitListByMarket(marketId));
        } else {
            rw.setContent(reelerAuctionRepository.getReelerListByMarket(marketId));
        }
        return ResponseEntity.ok(rw);
    }

    // -------------------------------------------------------------------------
    // STATUS CHECK + DOWNLOAD
    // -------------------------------------------------------------------------

    public ResponseEntity<?> checkCSVStatus(int marketId, String fileName) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(Map.class);
        boolean ready = transactionFileGenRepository.existsByMarketIdAndFileName(marketId, fileName);
        rw.setContent(Map.of(
                "ready",    ready,
                "fileName", fileName,
                "message",  ready
                        ? "CSV is ready for download."
                        : "CSV is not yet generated. Please wait for the bank transaction service to process."
        ));
        return ResponseEntity.ok(rw);
    }

    public ByteArrayInputStream downloadCSV(int marketId, String fileName) {
        if (!transactionFileGenRepository.existsByMarketIdAndFileName(marketId, fileName)) {
            throw new ValidationException(
                    "CSV is not yet generated for file: " + fileName
                    + ". Please wait for the bank transaction service to process.");
        }
        return marketAuctionFileDowndloadService.generateCSV(marketId, fileName);
    }
}