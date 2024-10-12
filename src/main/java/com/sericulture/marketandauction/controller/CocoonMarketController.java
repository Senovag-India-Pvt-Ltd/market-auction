package com.sericulture.marketandauction.controller;

import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.api.cocoon.LotBasePriceFixationRequest;
import com.sericulture.marketandauction.model.api.cocoon.PupaTestAndCocoonAssessmentRequest;
import com.sericulture.marketandauction.model.api.cocoon.PupaTestResultFinderRequest;
import com.sericulture.marketandauction.model.api.marketauction.LotGroupageDetailsRequest;
import com.sericulture.marketandauction.model.api.marketauction.LotGroupageResponse;
import com.sericulture.marketandauction.model.api.marketauction.MarketAuctionRequest;
import com.sericulture.marketandauction.model.entity.LotBasePriceFixation;
import com.sericulture.marketandauction.service.CocoonMarketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/cocoon")
public class CocoonMarketController {

    @Autowired
    CocoonMarketService cocoonMarketService;

    @PostMapping("/reserveLot")
    public ResponseEntity<?> generateCocoonLot(@RequestBody MarketAuctionRequest marketAuctionRequest){
        return cocoonMarketService.marketAuctionFacade(marketAuctionRequest);

    }

    @PostMapping("/saveBasePriceKGLot")
    public ResponseEntity<?> saveBasePriceKGLot(@RequestBody LotBasePriceFixationRequest lotBasePriceFixationRequest){
        return cocoonMarketService.saveLotBasePriceFixation(lotBasePriceFixationRequest);
    }

    @GetMapping("/getLast10daysBasePrice")
    public ResponseEntity<?> getLast10daysBasePrice(){
        return cocoonMarketService.getLast10DaysPrices();
    }

    @PostMapping("/savePupaTestAndCocoonAssessmentResult")
    public ResponseEntity<?> savePupaTestAndCocoonAssessmentResult(@RequestBody PupaTestAndCocoonAssessmentRequest pupaTestAndCocoonAssessmentRequest){
        ResponseWrapper rw = ResponseWrapper.createWrapper(LotGroupageResponse.class);

        rw.setContent(cocoonMarketService.savePupaTestAndCocoonAssessmentResult(pupaTestAndCocoonAssessmentRequest));
        return ResponseEntity.ok(rw);

    }

    @PostMapping("/getPupaTestAndCocoonAssessmentResult")
    public ResponseEntity<?> getPupaTestAndCocoonAssessmentResult(@RequestBody PupaTestResultFinderRequest pupaTestResultFinderRequest){
        return cocoonMarketService.getPupaTestAndCocoonAssessmentResult(pupaTestResultFinderRequest);

    }

}
