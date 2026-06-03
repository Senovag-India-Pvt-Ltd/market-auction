package com.sericulture.marketandauction.controller;

import com.sericulture.marketandauction.helper.Util;
import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.api.marketauction.*;
import com.sericulture.marketandauction.model.api.marketauction.reporting.ReelerReportRequest;
import com.sericulture.marketandauction.model.api.marketauction.reporting.ReelerTxnReportRequest;
import com.sericulture.marketandauction.model.api.marketauction.reporting.ReportRequest;
import com.sericulture.marketandauction.model.entity.Lot;
import com.sericulture.marketandauction.service.LotGroupageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.FileInputStream;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/lotGroupage")
public class LotGroupageController {

    @Autowired
    LotGroupageService lotGroupageService;

    @PostMapping("/validateReelerBalance")
    public ResponseEntity<?> validateReelerBalance(@RequestBody LotGroupageDetailsRequest lotGroupageRequest) {
        return lotGroupageService.validateReelerBalanceForLotGroupage(lotGroupageRequest);
    }

    @PostMapping("/saveLotGroupage")
    public ResponseEntity<?> saveLotGroupage(@RequestBody LotGroupageDetailsRequest lotGroupageRequest){
        ResponseWrapper rw = ResponseWrapper.createWrapper(LotGroupageResponse.class);

        rw.setContent(lotGroupageService.saveLotGroupage(lotGroupageRequest));
        return ResponseEntity.ok(rw);

    }

    @PostMapping("/getUpdateLotDistributeByLotIdForSeedMarket")
    public ResponseEntity<?> getUpdateLotDistributeByLotIdForSeedMarket(@RequestBody LotStatusSeedMarketRequest lotStatusSeedMarketRequest){

        return lotGroupageService.getLotDistributeDetailsByLotAndMarketAndAuctionDateForSeedMarket(lotStatusSeedMarketRequest);

    }

    @PostMapping("/getReelingLotNumberDetails")
    public ResponseEntity<?> getReelingLotNumberDetails() {
        return lotGroupageService.getReelingLotNumberDetails();
    }

    @PostMapping("/updateLotGroupage")
    public ResponseEntity<?> editLotGroupage(@RequestBody LotGroupageDetailsRequestEdit lotGroupageDetailsRequestEdit){
        ResponseWrapper rw = ResponseWrapper.createWrapper(LotGroupageResponse.class);

        rw.setContent(lotGroupageService.editLotGroupage(lotGroupageDetailsRequestEdit));
        return ResponseEntity.ok(rw);

    }

    @PostMapping("/getLotDistributeResponseForInvoiceForSeedMarket")
    public ResponseEntity<?> getLotDistributeResponseForInvoiceForSeedMarket(@RequestBody LotStatusSeedMarketRequest lotStatusSeedMarketRequest){
        ResponseWrapper rw = ResponseWrapper.createWrapper(LotGroupageResponse.class);

        rw.setContent (lotGroupageService.getLotDistributeResponseForInvoiceForSeedMarket(lotStatusSeedMarketRequest));
        return ResponseEntity.ok(rw);
    }

    @PostMapping("/getLotDistributeDetailsForPermitRSP")
    public ResponseEntity<?> getLotDistributeDetailsForPermitRSP(@RequestBody LotStatusSeedMarketRequest lotStatusSeedMarketRequest){
        ResponseWrapper rw = ResponseWrapper.createWrapper(LotGroupageResponse.class);

        rw.setContent (lotGroupageService.getLotDistributeDetailsForPermitRSP(lotStatusSeedMarketRequest));
        return ResponseEntity.ok(rw);
    }

    @PostMapping("/getLotDistributeDetailsForMarketReceiptAndCashReceipt")
    public ResponseEntity<?> getLotDistributeDetailsForMarketReceiptAndCashReceipt(@RequestBody LotStatusSeedMarketRequest lotStatusSeedMarketRequest){
        ResponseWrapper rw = ResponseWrapper.createWrapper(LotGroupageResponse.class);

        rw.setContent (lotGroupageService.getLotDistributeDetailsForMarketReceiptAndCashReceipt(lotStatusSeedMarketRequest));
        return ResponseEntity.ok(rw);
    }

    @PostMapping("/getLotDistributeResponseForInvoiceAndBonusScheme")
    public ResponseEntity<?> getLotDistributeResponseForInvoiceAndBonusScheme(@RequestBody LotStatusSeedMarketRequest lotStatusSeedMarketRequest){
        ResponseWrapper rw = ResponseWrapper.createWrapper(LotGroupageResponse.class);

        rw.setContent (lotGroupageService.getLotDistributeResponseForInvoiceAndBonusScheme(lotStatusSeedMarketRequest));
        return ResponseEntity.ok(rw);
    }

    @PostMapping("/getAllottedLotIds")
    public ResponseEntity<?> getAllottedLotIds(@RequestBody LotStatusSeedMarketRequest request) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(Integer.class);
        rw.setContent(lotGroupageService.getAllottedLotIds(request));
        return ResponseEntity.ok(rw);
    }

//    @PostMapping("/getFitnessCertificateDetails")
//    public ResponseEntity<?> getLotDisposalDetails(@RequestParam String fruitsId,@RequestParam Long  fitnessCertificateId){
//
//        ResponseWrapper rw = ResponseWrapper.createWrapper(LotDistributeResponse.class);
//        rw.setContent(lotGroupageService.getLotDisposalDetails(fruitsId,fitnessCertificateId));
//
//        return ResponseEntity.ok(rw);
//    }

    @PostMapping("/getFitnessCertificateDetails")
    public ResponseEntity<?> getLotDisposalDetails(@RequestBody LotStatusSeedMarketRequest request){

        ResponseWrapper rw = ResponseWrapper.createWrapper(LotDistributeResponse.class);

        rw.setContent(
                lotGroupageService.getLotDisposalDetails(request)
        );

        return ResponseEntity.ok(rw);
    }

    @PostMapping("/getDetailsForMarketReceipt")
    public ResponseEntity<?> getDetailsForMarketReceipt(
            @RequestBody LotStatusSeedMarketRequest lotStatusSeedMarketRequest) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(LotDistributeBuyerWiseResponse.class);
        rw.setContent(lotGroupageService.getDetailsForMarketReceipt(lotStatusSeedMarketRequest));
        return ResponseEntity.ok(rw);
    }
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "Ok Response"),
//            @ApiResponse(responseCode = "400", description = "Bad Request - Has validation errors",
//                    content = {
//                            @Content(mediaType = "application/json", schema =
//                            @Schema(example = "{\"content\":null,\"errorMessages\":[{\"errorType\":\"VALIDATION\",\"message\":[{\"message\":\"Invalid Input\",\"label\":\"NON_LABEL_MESSAGE\",\"locale\":null}]}]}"))
//                    }),
//            @ApiResponse(responseCode = "500", description = "Internal Server Error - Error occurred while processing the request.")
//    })
    @GetMapping("/getdetails")
    public ResponseEntity<?> getLotDetails(
            @RequestParam LocalDate date,
            @RequestParam int lotNo
    ) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(Map.class);

        rw.setContent(
                lotGroupageService.getLotDetails(date, lotNo)
        );

        return ResponseEntity.ok(rw);
    }

    @PostMapping("/getSeedCocoonDTRReport")
    public ResponseEntity<?> getDetailsForSeedCocoonDTRReport(
            @RequestBody LotStatusSeedMarketRequest request) {

        ResponseWrapper rw = ResponseWrapper.createWrapper(LotDistributeBuyerWiseResponse.class);
        rw.setContent(lotGroupageService.getSeedCocoonDTRReport(request));

        return ResponseEntity.ok(rw);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteLot(
            @RequestParam int lotId,
            @RequestParam LocalDate date) {

        String message = lotGroupageService.deleteLot(lotId, date);

        return ResponseEntity.ok(Map.of("content", message));
    }

    @PostMapping("/external-unit-balance-report")
    public ResponseEntity<InputStreamResource> downloadExternalUnitBalance(
            @RequestParam Long marketId) {

        try {

            FileInputStream fis = lotGroupageService.downloadExternalUnitBalance(marketId);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=external_unit_balance_" + Util.getISTLocalDate() + ".xlsx")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(fis));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/reeler-balance-report")
    public ResponseEntity<InputStreamResource> downloadReelerBalance(
            @RequestParam Long marketId) {

        try {

            FileInputStream fis = lotGroupageService.downloadReelerBalance(marketId);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=reeler_balance_" + Util.getISTLocalDate() + ".xlsx")
                    .header(HttpHeaders.CONTENT_TYPE,
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .body(new InputStreamResource(fis));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @PostMapping("/external-unit-balance-data")
    public ResponseEntity<?> getExternalUnitBalanceData(
            @RequestParam Long marketId) {

        return ResponseEntity.ok(
                lotGroupageService.getExternalUnitBalanceData(marketId)
        );
    }

    @PostMapping("/reeler-balance-data")
    public ResponseEntity<?> getReelerBalanceData(
            @RequestParam Long marketId) {

        return ResponseEntity.ok(
                lotGroupageService.getReelerBalanceData(marketId)
        );
    }

    @PostMapping("/getSeedMFReport")
    public ResponseEntity<?> getSeedMFReport(@RequestBody ReportRequest reportRequest) {
        return lotGroupageService.getSeedMFReport(reportRequest);
    }

    @PostMapping("/downloadSeedMFReport")
    public ResponseEntity<?> downloadSeedMFReport(@RequestBody ReportRequest request) {
        try {

            FileInputStream file = lotGroupageService.downloadSeedMFReport(request);

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=SeedMFReport.xlsx")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(file));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error downloading file");
        }
    }

    @PostMapping("/getSeedMarketBiddingReport")
    public ResponseEntity<?> getSeedMarketBiddingReport(
            @RequestBody ReelerReportRequest reportRequest){

        return lotGroupageService
                .getSeedMarketBiddingReport(reportRequest);
    }
    @PostMapping("/downloadSeedMarketBiddingReport")
    public ResponseEntity<?> downloadSeedMarketBiddingReport(
            @RequestBody ReelerReportRequest request) {
        try {
            FileInputStream file = lotGroupageService.downloadSeedMarketBiddingReport(request);
            return ResponseEntity.ok()
                    .header("Content-Disposition",
                            "attachment; filename=SeedMarketBiddingReport.xlsx")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(file));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error downloading file");
        }
    }

    @PostMapping("/getSeedMarketTxnReport")
    public ResponseEntity<?> getSeedMarketTxnReport(
            @RequestBody ReelerTxnReportRequest reelerTxnReportRequest){

        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);

        rw.setContent(
                lotGroupageService.getSeedMarketTxnReport(
                        reelerTxnReportRequest.getMarketId(),
                        reelerTxnReportRequest.getLicenseNumber(),
                        reelerTxnReportRequest.getFromDate(),
                        reelerTxnReportRequest.getToDate()
                )
        );

        return ResponseEntity.ok(rw);
    }

    @PostMapping("/downloadSeedMarketTxnReport")
    public ResponseEntity<?> downloadSeedMarketTxnReport(
            @RequestBody ReelerTxnReportRequest request) {

        try {

            FileInputStream file =
                    lotGroupageService
                            .downloadSeedMarketTxnReport(request);

            return ResponseEntity.ok()
                    .header(
                            "Content-Disposition",
                            "attachment; filename=SeedMarketTransactionReport.xlsx"
                    )
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(file));

        } catch (Exception e) {

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error downloading file");
        }
    }

    @PostMapping("/getGovtGrainageUnpaidLots")
    public ResponseEntity<?> getGovtGrainageUnpaidLots(@RequestBody LotStatusSeedMarketRequest request) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(Map.class);
        rw.setContent(lotGroupageService.getGovtGrainageUnpaidLots(request));
        return ResponseEntity.ok(rw);
    }

    @PostMapping("/markLotAsMarketPaid")
    public ResponseEntity<?> markLotAsMarketPaid(
            @RequestParam Long lotGroupageId,
            @RequestParam int marketId) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(Map.class);
        rw.setContent(lotGroupageService.markLotAsMarketPaid(lotGroupageId, marketId));
        return ResponseEntity.ok(rw);
    }

    @PostMapping("/getTransferMarketFeeToGovtAccount")
    public ResponseEntity<?> getTransferMarketFeeToGovtAccount(
            @RequestBody MarketFeeGovtTransferRequest request) {
        return lotGroupageService.getTransferMarketFeeToGovtAccount(request);
    }

    @PostMapping("/executeMarketFeeTransfer")
    public ResponseEntity<?> executeMarketFeeTransfer(
            @RequestBody MarketFeeGovtTransferRequest request) {
        return lotGroupageService.executeMarketFeeTransfer(request);
    }

    @GetMapping("/generateMarketFeePreviewCSV")
    public ResponseEntity<InputStreamResource> generateMarketFeePreviewCSV(
            @RequestParam int marketId,
            @RequestParam LocalDate date) {

        String fileName = "market_fee_preview_" + marketId + "_" + date + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.parseMediaType("application/csv"))
                .body(new InputStreamResource(lotGroupageService.generateMarketFeePreviewCSV(date, marketId)));
    }

    @GetMapping("/downloadMarketFeeTransferCSV")
    public ResponseEntity<InputStreamResource> downloadMarketFeeTransferCSV(
            @RequestParam int marketId,
            @RequestParam String fileName) {

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName + ".csv")
                .contentType(MediaType.parseMediaType("application/csv"))
                .body(new InputStreamResource(lotGroupageService.downloadMarketFeeTransferCSV(marketId, fileName)));
    }

}
