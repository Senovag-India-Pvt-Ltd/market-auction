package com.sericulture.marketandauction.controller;

import com.sericulture.marketandauction.model.api.marketauction.CanContinueToWeighmentRequest;
import com.sericulture.marketandauction.model.api.marketauction.LotStatusRequest;
import com.sericulture.marketandauction.service.WeigmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auction/weigment")
public class WeigmentController {


    @Autowired
    private WeigmentService weigmentService;

    @PostMapping("/getUpdateWeighmentByLotId")
    public ResponseEntity<?> getLotUpdateWeighmentScreenPrep(LotStatusRequest lotStatusRequest){

       return weigmentService.getWeigmentByLotAndMarketAndAuctionDate(lotStatusRequest);

    }

    @PostMapping("/canContinuetoWeighment")
    public ResponseEntity<?> canContinuetoWeighment(CanContinueToWeighmentRequest canContinueToWeighmentRequest){
        return weigmentService.canContinueToWeighmentProcess(canContinueToWeighmentRequest,false);
    }

    @PostMapping("/updateWeightToContinueToWeighment")
    public ResponseEntity<?> updateWeightToContinueToWeighment(CanContinueToWeighmentRequest updateLotWeightRequest){
        return weigmentService.canContinueToWeighmentProcess(updateLotWeightRequest,true);
    }
}
