package com.sericulture.marketandauction.controller;

import com.sericulture.marketandauction.helper.MAConstants;
import com.sericulture.marketandauction.service.AllMasterDataSaverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/addAllMaster")
public class AllMasterDataSaver {

    @Autowired
    AllMasterDataSaverService allMasterDataSaverService;

    @Autowired
    MessageSource messageSource;

    @Operation(summary = "This API allows to added a new market",
            description = "")
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
    @PostMapping("/addMarket")
    public String getEducationDetails(String name, String address, float weight, int lotWeight, int stateId, int districtId, int talukId) {
        allMasterDataSaverService.saveMarketMaster(name,
                address,
                weight,
                lotWeight,
                stateId,
                districtId,
                talukId);
        return "success";
    }

    @Operation(summary = "This API allows to added a new godown to the existing market",
            description = "")
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
    @PostMapping("/addGodown")
    public String addGoDownMaster(String name, int marketId) {
        allMasterDataSaverService.saveGodownMaster(name,
                marketId);
        return "success";
    }

    @Operation(summary = "This API allows to add range of bins to the Market or godown of the market",
            description = "")
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
    @PostMapping("/addBinMaster")
    public String addBinMaster(String type, int marketId, int godownId, int startNumber, int EndNumber) {
        allMasterDataSaverService.saveBinMaster(type, marketId, godownId, startNumber, EndNumber);
        return "success";
    }

    @Operation(summary = "This API allows to added data to the master table which holds the start and end of bins for small and large bins",
            description = "")
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
    @PostMapping("/addBinCounterMaster")
    public String saveBinCounterMaster(int bigBinstart, int bigBinEnd,
                                       int smallBinStart, int smallBinEnd,
                                       int marketId, int godownId) {
        allMasterDataSaverService.saveBinCounterMaster(bigBinstart, bigBinEnd, smallBinStart, smallBinEnd, marketId, godownId);
        return "success";
    }

}
