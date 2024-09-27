package com.sericulture.marketandauction.controller;

import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.api.marketauction.*;
import com.sericulture.marketandauction.service.LotGroupageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping("/updateLotGroupage")
    public ResponseEntity<?> editLotGroupage(@RequestBody LotGroupageDetailsRequestEdit lotGroupageDetailsRequestEdit){
        ResponseWrapper rw = ResponseWrapper.createWrapper(LotGroupageResponse.class);

        rw.setContent(lotGroupageService.editLotGroupage(lotGroupageDetailsRequestEdit));
        return ResponseEntity.ok(rw);

    }
}
