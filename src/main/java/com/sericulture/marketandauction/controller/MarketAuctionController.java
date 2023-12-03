package com.sericulture.marketandauction.controller;

import com.sericulture.marketandauction.helper.MAConstants;
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

    @Operation(summary = "This API allocates bin and generates a lot",
            description = "Majorly creates a record in market auction, bin and lot. This allowing the farmer to participate in the auction.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok Response", content = {
                    @Content(mediaType = "application/json", schema =
                    @Schema(example = MAConstants.SUCCESS_LOT_ALLOTMENT_OUTPUT))
            }),
            @ApiResponse(responseCode = "400", description = "Bad Request - Has validation errors",
                    content =
                            {
                                    @Content(mediaType = "application/json", schema =
                                    @Schema(example = MAConstants.VALIDATION_ERROR_LOT_ALLOTMENT))
                            }),
            @ApiResponse(responseCode = "500", description = MAConstants.INTERNAL_SERVER_ERROR_MESSAGE,
                    content =
                            {
                                    @Content(mediaType = "application/json", schema =
                                    @Schema(example = MAConstants.INTERNAL_SERVER_ERROR_OUTPUT_FORMAT))
                            })
    })
    @PostMapping("/allot")
    public ResponseEntity<?> allotBidToFarmer(@RequestBody MarketAuctionRequest marketAuctionRequest){

        //customValidator.validate(marketAuctionRequest);
        return marketAuctionService.marketAuctionFacade(marketAuctionRequest);

    }

    @Operation(summary = "This API facilitates search by farmer id and auction date to get all the auction slips",
            description = "Provides all the auction slips generated for the farmer for the auction date entered.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok Response", content = {
                    @Content(mediaType = "application/json", schema =
                    @Schema(example = MAConstants.ALL_SLIPS_MESSAGE_FARMER_AUC_DATE_OUTPUT))
            }),
            @ApiResponse(responseCode = "400", description = "Bad Request - Has validation errors",
                    content =
                            {
                                    @Content(mediaType = "application/json", schema =
                                    @Schema(example = MAConstants.VALIDATION_ERROR_LOT_ALLOTMENT))
                            }),
            @ApiResponse(responseCode = "500", description = MAConstants.INTERNAL_SERVER_ERROR_MESSAGE,
                    content =
                            {
                                    @Content(mediaType = "application/json", schema =
                                    @Schema(example = MAConstants.INTERNAL_SERVER_ERROR_OUTPUT_FORMAT))
                            })
    })
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
    @Operation(summary = "This API facilitates search by status and auction date to get all the auction slips.",
            description = "Provides all the auction slips generated for the status and the auction date provided.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok Response", content = {
                    @Content(mediaType = "application/json", schema =
                    @Schema(example = MAConstants.ALL_SLIPS_STATUS_AND_AUC_DATE_OUTPUT))
            }),
            @ApiResponse(responseCode = "400", description = "Bad Request - Has validation errors",
                    content =
                            {
                                    @Content(mediaType = "application/json", schema =
                                    @Schema(example = MAConstants.VALIDATION_ERROR_LOT_ALLOTMENT))
                            }),
            @ApiResponse(responseCode = "500", description = MAConstants.INTERNAL_SERVER_ERROR_MESSAGE,
                    content =
                            {
                                    @Content(mediaType = "application/json", schema =
                                    @Schema(example = MAConstants.INTERNAL_SERVER_ERROR_OUTPUT_FORMAT))
                            })
    })
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

    @Operation(summary = "This API cancels the market auction slip by farmer Id ",
            description = "Cancels the market auction slip for the farmer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok Response", content = {
                    @Content(mediaType = "application/json", schema =
                    @Schema(example = ""))
            }),
            @ApiResponse(responseCode = "400", description = "Bad Request - Has validation errors",
                    content =
                            {
                                    @Content(mediaType = "application/json", schema =
                                    @Schema(example = MAConstants.VALIDATION_ERROR_LOT_ALLOTMENT))
                            }),
            @ApiResponse(responseCode = "500", description = MAConstants.INTERNAL_SERVER_ERROR_MESSAGE,
                    content =
                            {
                                    @Content(mediaType = "application/json", schema =
                                    @Schema(example = MAConstants.INTERNAL_SERVER_ERROR_OUTPUT_FORMAT))
                            })
    })
    @PostMapping("/cancelfarmerAuction")
    public ResponseEntity<?> cancellFarmerBid(@RequestBody CancelAuctionByFarmerIdRequest cancellationRequest){
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        boolean success = marketAuctionService.cancelBidByFarmerId(cancellationRequest);
            if(!success){
                rw.setErrorCode(-1);
                rw.setErrorMessages(List.of("unable to cancel bid"));
            }
        return ResponseEntity.ok(rw);
    }
    @Operation(summary = "This API cancels the market auction slip by lot Id ",
            description = "Cancels the lot allocated to the farmer by Id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok Response", content = {
                    @Content(mediaType = "application/json", schema =
                    @Schema(example = ""))
            }),
            @ApiResponse(responseCode = "400", description = "Bad Request - Has validation errors",
                    content =
                            {
                                    @Content(mediaType = "application/json", schema =
                                    @Schema(example = MAConstants.VALIDATION_ERROR_LOT_ALLOTMENT))
                            }),
            @ApiResponse(responseCode = "500", description = MAConstants.INTERNAL_SERVER_ERROR_MESSAGE,
                    content =
                            {
                                    @Content(mediaType = "application/json", schema =
                                    @Schema(example = MAConstants.INTERNAL_SERVER_ERROR_OUTPUT_FORMAT))
                            })
    })
    @PostMapping("/cancelLot")
    public ResponseEntity<?> cancelLot(@RequestBody CancelAuctionByLotRequest cancellationRequest){
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        boolean success = marketAuctionService.cancelLot(cancellationRequest);
        if(!success){
            rw.setErrorCode(-1);
            rw.setErrorMessages(List.of("unable to cancel lot"));
        }
        return ResponseEntity.ok(rw);
    }



}
