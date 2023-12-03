package com.sericulture.marketandauction.controller;

import com.sericulture.marketandauction.helper.MAConstants;
import com.sericulture.marketandauction.model.api.marketauction.FLexTimeRequest;
import com.sericulture.marketandauction.service.MISCService;
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
@RequestMapping("/v1/auction/misc")
public class MISCController {

    @Autowired
    MISCService miscService;

    @Operation(summary = "Flex time to increase the time beyond the actual time for the activity",
            description = "This api enables to extend the time for activities like issuing bid slip and auction beyond their cutoff time. " +
                    "This api turns on the additional time for the activity. " +
                    "The activity should be turned off once activated, else it will be on indefinitely. Both these are manual activity and should be handled carefully.")
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
    @PostMapping("/flipFlexTime")
    public ResponseEntity<?> submitBid(@RequestBody FLexTimeRequest fLexTimeRequest){
        return miscService.flipFlexTime(fLexTimeRequest);
    }
}
