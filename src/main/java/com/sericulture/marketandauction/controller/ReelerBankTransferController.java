package com.sericulture.marketandauction.controller;

import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.exceptions.ValidationException;
import com.sericulture.marketandauction.service.ReelerBankTransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/reelerBankTransfer")
public class ReelerBankTransferController {

    @Autowired
    ReelerBankTransferService reelerBankTransferService;

    /**
     * Search by license number for Reeling or RSP.
     * buyerType: "Reeling" or "RSP"
     * Returns current balance, minimum balance, transferable amount and bank details.
     */
    @GetMapping("/getReelerBalance")
    public ResponseEntity<?> getReelerBalance(
            @RequestParam String licenseNumber,
            @RequestParam String buyerType,
            @RequestParam int marketId) {
        return reelerBankTransferService.getReelerBalance(licenseNumber, buyerType, marketId);
    }

    /**
     * Execute the transfer for Reeling or RSP.
     * buyerType: "Reeling" or "RSP"
     * buyerId: reelerId for Reeling, externalUnitId for RSP
     */
    @PostMapping("/transferToBank")
    public ResponseEntity<?> transferToBank(
            @RequestParam int buyerId,
            @RequestParam String buyerType,
            @RequestParam int marketId,
            @RequestParam double transferAmount,
            @RequestParam String fileName,
            @RequestParam(required = false) String comment) {
        return reelerBankTransferService.transferToBank(buyerId, buyerType, marketId, transferAmount, fileName, comment);
    }

    /**
     * Check if the CSV is ready in TRANSACTION_FILE_GEN.
     * UI polls this endpoint to decide whether to enable the Download button.
     */
    @GetMapping("/checkCSVStatus")
    public ResponseEntity<?> checkCSVStatus(
            @RequestParam int marketId,
            @RequestParam String fileName) {
        return reelerBankTransferService.checkCSVStatus(marketId, fileName);
    }

    @GetMapping("/downloadCSV")
    public ResponseEntity<?> downloadCSV(
            @RequestParam int marketId,
            @RequestParam String fileName) {
        try {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName + ".csv")
                    .contentType(MediaType.parseMediaType("application/csv"))
                    .body(new InputStreamResource(
                            reelerBankTransferService.downloadCSV(marketId, fileName)));
        } catch (ValidationException e) {
            ResponseWrapper rw = ResponseWrapper.createWrapper(String.class);
            rw.setContent(e.getErrorMessages().get(0).getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(rw);
        } catch (Exception e) {
            ResponseWrapper rw = ResponseWrapper.createWrapper(String.class);
            rw.setContent(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(rw);
        }
    }
}