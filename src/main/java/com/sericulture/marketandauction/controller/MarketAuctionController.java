package com.sericulture.marketandauction.controller;

import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.api.marketauction.MarketAuctionRequest;
import com.sericulture.marketandauction.model.api.marketauction.MarketAuctionResponse;
import com.sericulture.marketandauction.service.MarketAuctionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/auction")
public class MarketAuctionController {

    @Autowired
    MarketAuctionService marketAuctionService;

    @Operation(summary = "Insert Caste Details", description = "Creates Caste Details in to DB")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok Response", content = {
                    @Content(mediaType = "application/json", schema =
                    @Schema(example = "{\n" +
                            "    \"content\": {\n" +
                            "        \"transactionId\": 9,\n" +
                            "        \"marketId\": 1,\n" +
                            "        \"godownId\": 0,\n" +
                            "        \"farmerId\": 1,\n" +
                            "        \"allotedLotList\": [\n" +
                            "            1,\n" +
                            "            2,\n" +
                            "            3\n" +
                            "        ],\n" +
                            "        \"allotedSmallBinList\": [\n" +
                            "            5,\n" +
                            "            6,\n" +
                            "            7,\n" +
                            "            8\n" +
                            "        ],\n" +
                            "        \"allotedBigBinList\": [\n" +
                            "            4,\n" +
                            "            5\n" +
                            "        ]\n" +
                            "    },\n" +
                            "    \"errorMessages\": []\n" +
                            "}"))
            }),
            @ApiResponse(responseCode = "400", description = "Bad Request - Has validation errors",
                    content =
                            {
                                    @Content(mediaType = "application/json", schema =
                                    @Schema(example = "{\"errorType\":\"VALIDATION\",\"message\":[{\"message\":\"Title should be more than 1 characters.\",\"label\":\"name\",\"locale\":null}]}"))
                            }),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - Error occurred while processing the request.")
    })
    @PostMapping("/allot")
    public ResponseEntity<?> addCasteDetails(@RequestBody MarketAuctionRequest marketAuctionRequest){
        ResponseWrapper rw = ResponseWrapper.createWrapper(MarketAuctionResponse.class);

        rw.setContent(marketAuctionService.saveMarketAuction(marketAuctionRequest));
        return ResponseEntity.ok(rw);
    }

    @PostMapping("/getAllAuctionSlipForFarmerByAuctionDate")
    public ResponseEntity<?> getAuctionDetailsByFarmerForAuctionDate(@RequestBody MarketAuctionRequest marketAuctionRequest){
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        List<MarketAuctionResponse> responses = marketAuctionService.getAuctionDetailsByFarmerForAuctionDate(marketAuctionRequest);
        if(responses.isEmpty()){
            rw.setErrorCode(-1);
            rw.setErrorMessages(List.of("No data found"));
        }
        rw.setContent(responses);

        return ResponseEntity.ok(rw);


    }

    @PostMapping("/getAllAuctionSlipForStatusByAuctionDate")
    public ResponseEntity<?> getAuctionDetailsByStateForAuctionDate(@RequestBody MarketAuctionRequest marketAuctionRequest){
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        List<MarketAuctionResponse> responses = marketAuctionService.getAuctionDetailsByStateForAuctionDate(marketAuctionRequest);
        if(responses.isEmpty()){
            rw.setErrorCode(-1);
            rw.setErrorMessages(List.of("No data found"));
        }
        rw.setContent(responses);

        return ResponseEntity.ok(rw);


    }



}
