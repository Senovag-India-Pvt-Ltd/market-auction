package com.sericulture.marketandauction.controller;


import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.api.marketauction.FarmerPaymentInfoRequest;
import com.sericulture.marketandauction.model.api.marketauction.FarmerPaymentInfoRequestByLotList;
import com.sericulture.marketandauction.model.enums.LotStatus;
import com.sericulture.marketandauction.service.FarmerPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/v1/auction/fp")
public class FarmerPaymentController {

    @Autowired
    FarmerPaymentService farmerPaymentService;


    @PostMapping("/getWeighMentCompletedList")
    public ResponseEntity<?> getWeighMentCompletedList(@RequestBody com.sericulture.marketandauction.model.api.RequestBody farmerPaymentInfoRequest,
                                                       @RequestParam(defaultValue = "0") final Integer pageNumber,
                                                       @RequestParam(defaultValue = "5") final Integer size
    ) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(Map.class);
        rw.setContent(farmerPaymentService.getWeighmentCompletedTxnByAuctionDateAndMarket(farmerPaymentInfoRequest,PageRequest.of(pageNumber, size)));
        return ResponseEntity.ok(rw);
    }

    @PostMapping("/addSelectedLotlistToReadyForPayment")
    public ResponseEntity<?> updateLotlistToReadyForPayment(@RequestBody FarmerPaymentInfoRequestByLotList farmerPaymentInfoRequestByLotList){
        return farmerPaymentService.updateLotlistByChangingTheStatus(farmerPaymentInfoRequestByLotList,true,LotStatus.WEIGHMENTCOMPLETED.getLabel(),LotStatus.READYFORPAYMENT.getLabel());
    }

    @PostMapping("/bulkSendToReadyForPayment")
    public ResponseEntity<?> updateLotstatusByAuctionToReadyForPayment(@RequestBody FarmerPaymentInfoRequestByLotList farmerPaymentInfoRequestByLotList){
        return farmerPaymentService.updateLotlistByChangingTheStatus(farmerPaymentInfoRequestByLotList,false, LotStatus.WEIGHMENTCOMPLETED.getLabel(),LotStatus.READYFORPAYMENT.getLabel());
    }

    @PostMapping("/getAuctionDateListForBulkSend")
    public ResponseEntity<?> getAllWeighmentCompletedAuctionDatesByMarket(@RequestBody com.sericulture.marketandauction.model.api.RequestBody requestBody){
        return farmerPaymentService.getAllWeighmentCompletedOrReadyForPaymentAuctionDatesByMarket(requestBody,LotStatus.WEIGHMENTCOMPLETED.getLabel());
    }

    @PostMapping("/getAuctionDateListForBankStatement")
    public ResponseEntity<?> getAuctionDateListForBankStatement(@RequestBody com.sericulture.marketandauction.model.api.RequestBody requestBody){
        return farmerPaymentService.getAllWeighmentCompletedOrReadyForPaymentAuctionDatesByMarket(requestBody,LotStatus.READYFORPAYMENT.getLabel());
    }

    @PostMapping("/removeSelectedLotlistfromReadyForPayment")
    public ResponseEntity<?> removeSelectedLotlistfromReadyForPayment(@RequestBody FarmerPaymentInfoRequestByLotList farmerPaymentInfoRequestByLotList){
        return farmerPaymentService.updateLotlistByChangingTheStatus(farmerPaymentInfoRequestByLotList,true,LotStatus.READYFORPAYMENT.getLabel(),LotStatus.WEIGHMENTCOMPLETED.getLabel());
    }

    @GetMapping("/generateCSVFile")
    public ResponseEntity<InputStreamResource> generateCSVFile(@RequestParam int marketId,@RequestParam LocalDate auctionDate,@RequestParam String fileName) {
        InputStreamResource file = new InputStreamResource(farmerPaymentService.generateCSV(marketId,auctionDate));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName+".csv")
                .contentType(MediaType.parseMediaType("application/csv"))
                .body(file);
    }

    @PostMapping("/requestJobToProcessPayment")
    public ResponseEntity<?> requestJobToProcessPayment(@RequestBody FarmerPaymentInfoRequest farmerPaymentInfoRequest){
        return farmerPaymentService.requestJobToProcessPayment(farmerPaymentInfoRequest);
    }

    @PostMapping("/generateBankStatementForAuctionDate")
    public ResponseEntity<?> generateBankStatementForAuctionDate(@RequestBody FarmerPaymentInfoRequest farmerPaymentInfoRequest){
        return farmerPaymentService.generateBankStatementForAuctionDate(farmerPaymentInfoRequest);
    }

}
