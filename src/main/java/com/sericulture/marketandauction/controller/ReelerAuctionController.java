package com.sericulture.marketandauction.controller;

import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.api.marketauction.ReelerAuctionRequest;
import com.sericulture.marketandauction.service.ReelerAuctionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/auction/reeler")
public class ReelerAuctionController {

    @Autowired
    ReelerAuctionService reelerAuctionService;


    @PostMapping("/submitBid")
    public ResponseEntity<?> submitBid(@RequestBody ReelerAuctionRequest reelerAuctionRequest){
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        boolean success = reelerAuctionService.submitbid(reelerAuctionRequest);
        if(!success){
            rw.setErrorCode(-1);
            rw.setErrorMessages(List.of("unable to submit bid"));
        }
        return ResponseEntity.ok(rw);
    }

    @PostMapping("/submitSurrogateBid")
    public ResponseEntity<?> submitSurrogateBid(@RequestBody ReelerAuctionRequest reelerAuctionRequest){
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        reelerAuctionRequest.setSurrogateBid(true);
        boolean success = reelerAuctionService.submitbid(reelerAuctionRequest);
        if(!success){
            rw.setErrorCode(-1);
            rw.setErrorMessages(List.of("unable to submit bid"));
        }
        return ResponseEntity.ok(rw);
    }
}
