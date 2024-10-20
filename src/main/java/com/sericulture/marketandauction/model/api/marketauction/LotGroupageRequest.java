package com.sericulture.marketandauction.model.api.marketauction;

import brave.Request;
import com.sericulture.marketandauction.model.api.RequestBody;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class LotGroupageRequest extends RequestBody {

    @Schema(name = "buyerType", example = "Reeler")
    String buyerType;

    @Schema(name = "buyerId", example = "1")
    Long buyerId;

    @Schema(name="lotWeight", example = "1")
    Long lotWeight;

    @Schema(name="amount", example = "1")
    Long amount;

    @Schema(name="dflLotNumber", example = "1")
    Long dflLotNumber;

    @Schema(name="averageYield", example = "1")
    Float averageYield;

//    @Schema(name="marketFee", example = "1")
//    Long marketFee;

    @Schema(name="soldAmount", example = "1")
    Long soldAmount;

    @Schema(name="allottedLotId", example = "1")
    Long allottedLotId;

//    @Schema(name="marketAuctionId", example = "1")
//    BigInteger marketAuctionId;
//
//    @Schema(name="id", example = "1")
//    BigInteger id;

    @Schema(name="auctionDate", example = "1")
    LocalDate auctionDate;

    @Schema(name="invoiceNumber", example = "1")
    String invoiceNumber;

    @Schema(name="lotParentLevel", example = "1")
    String lotParentLevel;




}
