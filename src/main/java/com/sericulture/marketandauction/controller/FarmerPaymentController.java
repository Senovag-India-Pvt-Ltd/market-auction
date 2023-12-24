package com.sericulture.marketandauction.controller;


import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.api.marketauction.FarmerPaymentInfoRequest;
import com.sericulture.marketandauction.model.api.marketauction.FarmerPaymentInfoRequestByLotList;
import com.sericulture.marketandauction.service.FarmerPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/auction/fp")
public class FarmerPaymentController {

    @Autowired
    FarmerPaymentService farmerPaymentService;


    @PostMapping("/getWeighMentCompletedList")
    public ResponseEntity<?> getWeighMentCompletedList(@RequestBody  FarmerPaymentInfoRequest farmerPaymentInfoRequest,
                                                       @RequestParam(defaultValue = "0") final Integer pageNumber,
                                                       @RequestParam(defaultValue = "5") final Integer size
    ) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(Map.class);
        rw.setContent(farmerPaymentService.getWeighmentCompletedTxnByAuctionDateAndMarket(farmerPaymentInfoRequest,PageRequest.of(pageNumber, size)));
        return ResponseEntity.ok(rw);
    }

    @PostMapping("/updateSelectedLotlistToReadyForPayment")
    public ResponseEntity<?> updateLotlistToReadyForPayment(@RequestBody FarmerPaymentInfoRequestByLotList farmerPaymentInfoRequestByLotList){
        return farmerPaymentService.updateLotlistToReadyForPayment(farmerPaymentInfoRequestByLotList,true);
    }

    @PostMapping("/updateLotstatusByAuctionToReadyForPayment")
    public ResponseEntity<?> updateLotstatusByAuctionToReadyForPayment(@RequestBody FarmerPaymentInfoRequestByLotList farmerPaymentInfoRequestByLotList){
        return farmerPaymentService.updateLotlistToReadyForPayment(farmerPaymentInfoRequestByLotList,false);
    }

}
