package com.sericulture.marketandauction.controller;

import com.sericulture.marketandauction.model.api.marketauction.CanContinueToWeighmentRequest;
import com.sericulture.marketandauction.model.api.marketauction.CompleteLotWeighmentRequest;
import com.sericulture.marketandauction.model.api.marketauction.LotStatusRequest;
import com.sericulture.marketandauction.service.SilkMarketWeigmentService;
import com.sericulture.marketandauction.service.WeigmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auction/silk-market-weigment")
public class SilkMarketWeighmentController {

    @Autowired
    private SilkMarketWeigmentService silkMarketWeigmentService;

    @PostMapping("/getUpdateSilkWeighmentByLotId")
    public ResponseEntity<?> getLotUpdateWeighmentScreenPrep(@RequestBody LotStatusRequest lotStatusRequest){

        return silkMarketWeigmentService.getWeigmentByLotAndMarketAndAuctionDate(lotStatusRequest);

    }

    @PostMapping("/canContinuetoSilkWeighment")
    public ResponseEntity<?> canContinuetoWeighment(@RequestBody CanContinueToWeighmentRequest canContinueToWeighmentRequest){
        return silkMarketWeigmentService.canContinueToWeighmentProcess(canContinueToWeighmentRequest);
    }

    @PostMapping("/completeSilkWeighmentForLot")
    public ResponseEntity<?> completeWeighMentForLot(@RequestBody CompleteLotWeighmentRequest completeLotWeighmentRequest){
        return silkMarketWeigmentService.completeLotWeighMent(completeLotWeighmentRequest);
    }
}
