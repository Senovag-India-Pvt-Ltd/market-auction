package com.sericulture.marketandauction.controller;

import com.sericulture.marketandauction.helper.Util;
import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.api.cocoon.*;
import com.sericulture.marketandauction.model.api.marketauction.LotGroupageDetailsRequest;
import com.sericulture.marketandauction.model.api.marketauction.LotGroupageResponse;
import com.sericulture.marketandauction.model.api.marketauction.LotStatusSeedMarketRequest;
import com.sericulture.marketandauction.model.api.marketauction.MarketAuctionRequest;
import com.sericulture.marketandauction.model.entity.LotBasePriceFixation;
import com.sericulture.marketandauction.model.entity.PupaTestAndCocoonAssessment;
import com.sericulture.marketandauction.service.CocoonMarketService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    @PostMapping("/saveLotWiseBasePriceKGLot")
    public ResponseEntity<?> saveLotWiseBasePriceKGLot(@RequestBody LotBasePriceFixationRequest lotBasePriceFixationRequest){
        return cocoonMarketService.saveLotWiseBasePriceKGLot(lotBasePriceFixationRequest);
    }

    @GetMapping("/getLast10daysBasePrice")
    public ResponseEntity<?> getLast10daysBasePrice(){
        return cocoonMarketService.getLast10DaysPrices();
    }

    @GetMapping("/getPrices")
    public ResponseEntity<?> getPrices(){
        return cocoonMarketService.getPrices();
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

//    @PostMapping("/getPupaCocoonAssessmentList")
//    public ResponseEntity<?> getPupaAndCocoonAssessmentByMarket(@RequestBody com.sericulture.marketandauction.model.api.RequestBody pupaTestAndCocoonAssessmentRequest,
//                                                                @RequestParam(defaultValue = "0") final Integer pageNumber,
//                                                                @RequestParam(defaultValue = "5") final Integer size
//    ) {
//        ResponseWrapper rw = ResponseWrapper.createWrapper(Map.class);
//        rw.setContent(cocoonMarketService.getPupaAndCocoonAssessmentByMarket(pupaCocoonAssessmentRequest, PageRequest.of(pageNumber, size)));
//        return ResponseEntity.ok(rw);
//    }

    @GetMapping("/getPupaCocoonAssessmentList")
    public List<SeedMarketAuctionDetailsResponse> getPupaAndCocoonAssessmentByMarket() {
        return cocoonMarketService.getPupaAndCocoonAssessmentByMarket(Util.getMarketId(Util.getTokenValues()));
    }

    @GetMapping("/get-all-pupa-cocoon-assessment-details-info")
    public List<PupaTestAndCocoonAssessment> getAllPupaTestAndCocoonAssessment() {
        return cocoonMarketService.getAllPupaTestAndCocoonAssessment();
    }
    @GetMapping("/getFinalWeighmentList")
    public List<SeedMarketAuctionDetailsResponse> getFinalWeighmentList() {
        return cocoonMarketService.getFinalWeighmentList(Util.getMarketId(Util.getTokenValues()));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        ResponseWrapper<LotBasePriceFixationResponse> rw =
                ResponseWrapper.createWrapper(LotBasePriceFixationResponse.class);

        rw.setContent(cocoonMarketService.deleteDetails(id));
        return ResponseEntity.ok(rw);
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<?> getById(
            @PathVariable final Integer id
    ) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(LotBasePriceFixationResponse.class);

        rw.setContent(cocoonMarketService.getById(id));
        return ResponseEntity.ok(rw);
    }

    @PostMapping("/getAllottedLotIdsForPrice")
    public ResponseEntity<?> getAllottedLotIdsForPrice() {

        ResponseWrapper rw = ResponseWrapper.createWrapper(Integer.class);
        rw.setContent(cocoonMarketService.getAllottedLotIdsForPrice());

        return ResponseEntity.ok(rw);
    }



}
