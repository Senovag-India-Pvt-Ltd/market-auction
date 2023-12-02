package com.sericulture.marketandauction.controller;

import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.api.marketauction.*;
import com.sericulture.marketandauction.service.CustomValidator;
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

    @Autowired
    CustomValidator customValidator;

    @Operation(summary = "Allocates bin and generates a lot", description = "Majorly creates a record in market auction, bin and lot.")
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
    public ResponseEntity<?> allotBidToFarmer(@RequestBody MarketAuctionRequest marketAuctionRequest){

        //customValidator.validate(marketAuctionRequest);
        return marketAuctionService.marketAuctionFacade(marketAuctionRequest);

    }

    @Operation(summary = "Searches by farmer id and auction date", description = "Provides results for the farmer and the auction date provided.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok Response", content = {
                    @Content(mediaType = "application/json", schema =
                    @Schema(example = "{  \"content\": [{\"transactionId\": 1225,\"marketId\": 9,\"godownId\": 0,\"farmerId\": 123,\"allotedLotList\": [],\"allotedSmallBinList\": [],\"allotedBigBinList\": []},{\"transactionId\": 1226,\"marketId\": 9,\"godownId\": 0,\"farmerId\": 123,\"allotedLotList\": [],\"allotedSmallBinList\": [],\"allotedBigBinList\": []}  ],  \"errorMessages\": [],  \"errorCode\": 0}"))
            }),
            @ApiResponse(responseCode = "400", description = "Bad Request - Has validation errors",
                    content =
                            {
                                    @Content(mediaType = "application/json", schema =
                                    @Schema(example = "{\"errorType\":\"VALIDATION\",\"message\":[{\"message\":\"Title should be more than 1 characters.\",\"label\":\"name\",\"locale\":null}]}"))
                            }),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - Error occurred while processing the request.") })
    @PostMapping("/getAllAuctionSlipForFarmerByAuctionDate")
    public ResponseEntity<?> getAuctionDetailsByFarmerForAuctionDate(@RequestBody SearchMarketByFarmerAndAuctionDateRequest searchMarketByFarmerAndAuctionDateRequest){
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        List<MarketAuctionResponse> responses = marketAuctionService.getAuctionDetailsByFarmerForAuctionDate(searchMarketByFarmerAndAuctionDateRequest);
        if(responses.isEmpty()){
            rw.setErrorCode(-1);
            rw.setErrorMessages(List.of("No data found"));
        }
        rw.setContent(responses);

        return ResponseEntity.ok(rw);


    }
    @Operation(summary = "Searches by status and auction date", description = "Provides results for the status and the auction date provided.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok Response", content = {
                    @Content(mediaType = "application/json", schema =
                    @Schema(example = "{\"content\":[{\"transactionId\":95,\"marketId\":1,\"godownId\":0,\"farmerId\":104,\"allotedLotList\":[],\"allotedSmallBinList\":[],\"allotedBigBinList\":[]}],\"errorMessages\":[],\"errorCode\":0}"))
            }),
            @ApiResponse(responseCode = "400", description = "Bad Request - Has validation errors",
                    content =
                            {
                                    @Content(mediaType = "application/json", schema =
                                    @Schema(example = "{\"errorType\":\"VALIDATION\",\"message\":[{\"message\":\"Title should be more than 1 characters.\",\"label\":\"name\",\"locale\":null}]}"))
                            }),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - Error occurred while processing the request.") })
    @PostMapping("/getAllAuctionSlipForStatusByAuctionDate")
    public ResponseEntity<?> getAuctionDetailsByStateForAuctionDate(@RequestBody SearchMarketByStatusAndAuctionDateRequest searchRequest){
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        List<MarketAuctionResponse> responses = marketAuctionService.getAuctionDetailsByStateForAuctionDate(searchRequest);
        if(responses.isEmpty()){
            rw.setErrorCode(-1);
            rw.setErrorMessages(List.of("No data found"));
        }
        rw.setContent(responses);
        return ResponseEntity.ok(rw);
    }

    @Operation(summary = "Searches by status and auction date", description = "Provides results for the status and the auction date provided.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok Response", content = {
                    @Content(mediaType = "application/json", schema =
                    @Schema(example = ""))
            }),
            @ApiResponse(responseCode = "400", description = "Bad Request - Has validation errors",
                    content =
                            {
                                    @Content(mediaType = "application/json", schema =
                                    @Schema(example = "{\"errorType\":\"VALIDATION\",\"message\":[{\"message\":\"Title should be more than 1 characters.\",\"label\":\"name\",\"locale\":null}]}"))
                            }),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - Error occurred while processing the request.") })
    @PostMapping("/cancelfarmerAuction")
    public ResponseEntity<?> cancellFarmerBid(@RequestBody CancellationRequest cancellationRequest){
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        boolean success = marketAuctionService.cancelBidByFarmerId(cancellationRequest);
            if(!success){
                rw.setErrorCode(-1);
                rw.setErrorMessages(List.of("unable to cancel bid"));
            }
        return ResponseEntity.ok(rw);
    }

    @PostMapping("/cancelLot")
    public ResponseEntity<?> cancellLot(@RequestBody CancellationRequest cancellationRequest){
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        boolean success = marketAuctionService.cancelBidByFarmerId(cancellationRequest);
        if(!success){
            rw.setErrorCode(-1);
            rw.setErrorMessages(List.of("unable to cancel lot"));
        }
        return ResponseEntity.ok(rw);
    }



}
