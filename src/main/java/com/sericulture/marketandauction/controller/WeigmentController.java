package com.sericulture.marketandauction.controller;

import com.sericulture.marketandauction.model.api.marketauction.MISCRequest;
import com.sericulture.marketandauction.service.MISCService;
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
    public ResponseEntity<?> getLotUpdateWeighmentScreenPrep(MISCRequest miscRequest){

       return weigmentService.getWeigmentByLotAndMarketAndAuctionDate(miscRequest);

    }

    @PostMapping("/canContinuetoWeighment")
    public ResponseEntity<?> canContinuetoWeighment(MISCRequest miscRequest){
        return weigmentService.canContinueToWeighmentProcess(miscRequest,false);
    }

    @PostMapping("/updateWeightToContinueToWeighment")
    public ResponseEntity<?> updateWeightToContinueToWeighment(MISCRequest miscRequest){
        return weigmentService.canContinueToWeighmentProcess(miscRequest,true);
    }
}
