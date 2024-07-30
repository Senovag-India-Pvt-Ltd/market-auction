package com.sericulture.marketandauction.controller;

import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.api.marketauction.*;
import com.sericulture.marketandauction.service.UpdateLotWeightDetailService;
import com.sericulture.marketandauction.service.WeigmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auction/updateWeight")
public class UpdateLotWeightDetailController {

    @Autowired
    private UpdateLotWeightDetailService updateLotWeightDetailService;

    @PostMapping("/getWeighmentWeightDetails")
    public ResponseEntity<?> getWeighmentWeightDetails(@RequestBody GetWeightDetailsRequest getWeightDetailsRequest){

        return updateLotWeightDetailService.getWeightDetailsByLotIdAndMarketAndAuctionDate(getWeightDetailsRequest);

    }

    @PostMapping("/updateNetWeight")
    public ResponseEntity<?> updateNetWeight(@RequestBody UpdateNetWeightRequest updateNetWeightRequest){
        ResponseWrapper rw = ResponseWrapper.createWrapper(UpdatedNetWeightResponse.class);

        rw.setContent(updateLotWeightDetailService.updateNetWeight(updateNetWeightRequest));
        return ResponseEntity.ok(rw);

    }
}
