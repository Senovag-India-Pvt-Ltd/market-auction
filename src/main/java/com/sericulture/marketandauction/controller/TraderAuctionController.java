package com.sericulture.marketandauction.controller;

import com.sericulture.marketandauction.helper.MAConstants;
import com.sericulture.marketandauction.model.api.marketauction.*;
import com.sericulture.marketandauction.service.ReelerAuctionService;
import com.sericulture.marketandauction.service.TraderAuctionService;
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

@RestController
@RequestMapping("/v1/auction/trader")
public class TraderAuctionController {

    @Autowired
    TraderAuctionService traderAuctionService;

    @Operation(summary = "This API allows to submit bid by the reeler",
            description = "This API allows to submit bid for a lot by the reeler. This API allows to submit the bid by the reeler.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok Response", content = {
                    @Content(mediaType = "application/json", schema =
                    @Schema(example = "TODO"))
            }),
            @ApiResponse(responseCode = "400", description = "Bad Request - Has validation errors",
                    content =
                            {
                                    @Content(mediaType = "application/json", schema =
                                    @Schema(example = MAConstants.VALIDATION_MESSAGE_FLEX_TIME))
                            }),
            @ApiResponse(responseCode = "500", description = MAConstants.INTERNAL_SERVER_ERROR_MESSAGE,
                    content =
                            {
                                    @Content(mediaType = "application/json", schema =
                                    @Schema(example = MAConstants.INTERNAL_SERVER_ERROR_OUTPUT_FORMAT))
                            })
    })
    @PostMapping("/submitBid")
    public ResponseEntity<?> submitBid(@RequestBody ReelerBidRequest reelerBidRequest){
        return traderAuctionService.submitbidSP(reelerBidRequest);
    }

    @Operation(summary = "This API allows to submit surrogate bid.",
            description = "This API allows to submit bid on behalf of a trader, For surrogate bids this api needs to be used.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok Response", content = {
                    @Content(mediaType = "application/json", schema =
                    @Schema(example = "TODO"))
            }),
            @ApiResponse(responseCode = "400", description = "Bad Request - Has validation errors",
                    content =
                            {
                                    @Content(mediaType = "application/json", schema =
                                    @Schema(example = MAConstants.VALIDATION_MESSAGE_FLEX_TIME))
                            }),
            @ApiResponse(responseCode = "500", description = MAConstants.INTERNAL_SERVER_ERROR_MESSAGE,
                    content =
                            {
                                    @Content(mediaType = "application/json", schema =
                                    @Schema(example = MAConstants.INTERNAL_SERVER_ERROR_OUTPUT_FORMAT))
                            })
    })
    @PostMapping("/submitSurrogateBid")
    public ResponseEntity<?> submitSurrogateBid(@RequestBody ReelerSurrogateBidRequest reelerBidRequest){
        return  traderAuctionService.submitbid(reelerBidRequest);
    }

    @PostMapping("/getHighestBidPerLot")
    public ResponseEntity<?> getHighestBid(@RequestBody LotStatusRequest lotStatusRequest){
        return traderAuctionService.getHighestBidPerLot(lotStatusRequest);

    }

    @PostMapping("/getHighestBidPerLotDetails")
    public ResponseEntity<?> getHighestBidDetails(@RequestBody LotStatusRequest lotStatusRequest){
        return traderAuctionService.getHighestBidPerLotDetails(lotStatusRequest);

    }


    @PostMapping("/acceptTraderBidForGivenLot")
    public ResponseEntity<?> acceptTraderBidForGivenLot(@RequestBody ReelerBidAcceptRequest reelerBidAcceptRequest){
        return traderAuctionService.acceptTraderBidForGivenLot(reelerBidAcceptRequest);

    }

    @PostMapping("/rejectTraderBidForGivenLot")
    public ResponseEntity<?> rejectTraderBidForGivenLot(@RequestBody ReelerBidAcceptRequest reelerBidAcceptRequest){
        return traderAuctionService.rejectTraderBidForGivenLot(reelerBidAcceptRequest);

    }

    @PostMapping("/getHighestAndCurrentBidByEachLotForTrader")
    public ResponseEntity<?> getTraderLotWithHighestBidDetails(@RequestBody ReelerLotRequest reelerLotRequest){
        return traderAuctionService.getTraderLotWithHighestBidDetails(reelerLotRequest);
    }


    @PostMapping("/removeTraderHighestBid")
    public ResponseEntity<?> removeTraderHighestBid(@RequestBody RemoveReelerHighestBidRequest removeReelerHighestBidRequest){
        return traderAuctionService.removeTraderHighestBid(removeReelerHighestBidRequest);
    }

}
