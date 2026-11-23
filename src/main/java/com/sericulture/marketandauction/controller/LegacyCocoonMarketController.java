package com.sericulture.marketandauction.controller;

import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.service.legacy.LegacyCocoonMarketImportService;
import com.sericulture.marketandauction.service.legacy.LegacyFileReader;
import com.sericulture.marketandauction.service.legacy.LegacyTraceService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** XC-004: import the pre-project cocoon market export and trace transactions across legacy + new system. */
@RestController
@RequestMapping("/v1/legacy-market")
public class LegacyCocoonMarketController {

    private final LegacyCocoonMarketImportService importService;
    private final LegacyTraceService traceService;

    public LegacyCocoonMarketController(LegacyCocoonMarketImportService importService, LegacyTraceService traceService) {
        this.importService = importService;
        this.traceService = traceService;
    }

    @ExceptionHandler(LegacyCocoonMarketImportService.ImportException.class)
    public ResponseEntity<ResponseWrapper<Object>> handle(LegacyCocoonMarketImportService.ImportException e) {
        ResponseWrapper<Object> rw = new ResponseWrapper<>();
        rw.setErrorCode(-1);
        rw.setErrorMessages(List.of(e.getMessage()));
        rw.setContent(Map.of("error", true, "error_description", e.getMessage()));
        return ResponseEntity.ok(rw);
    }

    @Operation(summary = "Download the import template (CSV)")
    @GetMapping("/template")
    public ResponseEntity<byte[]> template() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"legacy-cocoon-market-template.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(LegacyFileReader.templateCsv());
    }

    @Operation(summary = "Import (or re-import) a legacy cocoon market export; rows are added / updated / skipped by legacy transaction id")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseWrapper<Object>> importFile(@RequestParam("sourceSystem") String sourceSystem,
                                                              @RequestParam("file") MultipartFile file) {
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new LegacyCocoonMarketImportService.ImportException("The file could not be read.");
        }
        return ok(importService.importFile(sourceSystem, file.getOriginalFilename(), bytes));
    }

    @Operation(summary = "Recent import runs")
    @GetMapping("/imports")
    public ResponseEntity<ResponseWrapper<Object>> history() {
        return ok(importService.history());
    }

    @Operation(summary = "Rejected rows of an import run")
    @GetMapping("/imports/{importId}/errors")
    public ResponseEntity<ResponseWrapper<Object>> errors(@PathVariable Long importId) {
        return ok(importService.errors(importId));
    }

    @Operation(summary = "Trace cocoon market transactions across the legacy system and the new system")
    @GetMapping("/trace")
    public ResponseEntity<ResponseWrapper<Object>> trace(
            @RequestParam(value = "fruitsId", required = false) String fruitsId,
            @RequestParam(value = "mobile", required = false) String mobile,
            @RequestParam(value = "lotNumber", required = false) String lotNumber,
            @RequestParam(value = "marketId", required = false) Long marketId,
            @RequestParam(value = "legacyTxnId", required = false) String legacyTxnId,
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ok(traceService.trace(new LegacyTraceService.TraceQuery(fruitsId, mobile, lotNumber, marketId, legacyTxnId, fromDate, toDate)));
    }

    private static ResponseEntity<ResponseWrapper<Object>> ok(Object content) {
        ResponseWrapper<Object> rw = new ResponseWrapper<>();
        rw.setContent(content);
        return ResponseEntity.ok(rw);
    }
}
