package com.sericulture.marketandauction.controller;

import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.api.marketauction.*;
import com.sericulture.marketandauction.service.LotGroupageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/lotGroupage")
public class LotGroupageController {

    @Autowired
    LotGroupageService lotGroupageService;

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

}
