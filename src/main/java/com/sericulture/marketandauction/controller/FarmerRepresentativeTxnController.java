package com.sericulture.marketandauction.controller;

import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.service.FarmerRepresentativeTxnService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** SR-005: link e-Inward transactions to a farmer's authorised agent and trace the agent's transactions. */
@RestController
@RequestMapping("/v1/farmer-agent-txn")
public class FarmerRepresentativeTxnController {

    private final FarmerRepresentativeTxnService service;

    public FarmerRepresentativeTxnController(FarmerRepresentativeTxnService service) {
        this.service = service;
    }

    public record LinkRequest(Long transactionId, Long farmerRepresentativeId) {
    }

    @ExceptionHandler(FarmerRepresentativeTxnService.LinkException.class)
    public ResponseEntity<ResponseWrapper<Object>> handle(FarmerRepresentativeTxnService.LinkException e) {
        ResponseWrapper<Object> rw = new ResponseWrapper<>();
        rw.setErrorCode(-1);
        rw.setErrorMessages(List.of(e.getMessage()));
        rw.setContent(Map.of("error", true, "error_description", e.getMessage()));
        return ResponseEntity.ok(rw);
    }

    @Operation(summary = "Record that an e-Inward transaction was brought by the farmer's authorised agent")
    @PostMapping("/link")
    public ResponseEntity<ResponseWrapper<Object>> link(@RequestBody LinkRequest request) {
        return ok(service.link(request.transactionId(), request.farmerRepresentativeId()));
    }

    @Operation(summary = "Transactions done under a farmer's authorised agent")
    @GetMapping("/trace")
    public ResponseEntity<ResponseWrapper<Object>> trace(
            @RequestParam("farmerRepresentativeId") Long farmerRepresentativeId,
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ok(service.trace(farmerRepresentativeId, fromDate, toDate));
    }

    private static ResponseEntity<ResponseWrapper<Object>> ok(Object content) {
        ResponseWrapper<Object> rw = new ResponseWrapper<>();
        rw.setContent(content);
        return ResponseEntity.ok(rw);
    }
}
