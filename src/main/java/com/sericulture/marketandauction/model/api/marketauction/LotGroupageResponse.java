package com.sericulture.marketandauction.model.api.marketauction;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sericulture.marketandauction.model.api.ResponseBody;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigInteger;
import java.time.LocalDate;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class LotGroupageResponse extends ResponseBody {

    @Schema(name = "lotGroupageId", example = "1")
    Long lotGroupageId;

    @Schema(name = "buyerType", example = "Reeler")
    String buyerType;

    @Schema(name = "buyerId", example = "1")
    Long buyerId;

    @Schema(name="lotWeight", example = "1")
    Long lotWeight;

    @Schema(name="amount", example = "1")
    Long amount;

    @Schema(name="marketFee", example = "1")
    Long marketFee;

    @Schema(name="soldAmount", example = "1")
    Long soldAmount;

    @Schema(name="allottedLotId", example = "1")
    Long allottedLotId;

    @Schema(name="marketAuctionId", example = "1")
    Long marketAuctionId;

    @Schema(name="id", example = "1")
    BigInteger id;

    @Schema(name="auctionDate", example = "1")
    LocalDate auctionDate;
    @Schema(name="dflLotNumber", example = "1")
    Long dflLotNumber;

    @Schema(name="averageYield", example = "1")
    Long averageYield;

    @Schema(name = "error", example = "true")
    Boolean error;

    @Schema(name = "error_description", example = "Username or password is incorrect")
    String error_description;
}
