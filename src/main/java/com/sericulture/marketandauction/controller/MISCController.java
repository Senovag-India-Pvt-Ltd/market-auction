package com.sericulture.marketandauction.controller;

import com.sericulture.marketandauction.model.api.marketauction.FLexTimeRequest;
import com.sericulture.marketandauction.model.api.marketauction.ReelerAuctionRequest;
import com.sericulture.marketandauction.service.MISCService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auction/misc")
public class MISCController {

    @Autowired
    MISCService miscService;


    @PostMapping("/flipFlexTime")
    public ResponseEntity<?> submitBid(@RequestBody FLexTimeRequest fLexTimeRequest){
        return miscService.flipFlexTime(fLexTimeRequest);
    }
}
