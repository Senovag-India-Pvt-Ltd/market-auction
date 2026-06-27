package com.sericulture.marketandauction.model.api.marketauction;

import com.sericulture.marketandauction.model.api.RequestBody;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import lombok.*;

import java.math.BigInteger;
import java.time.LocalDate;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class LotGroupageRequestEdit extends RequestBody {

    @Schema(name = "lotGroupageId", example = "1")
    Long lotGroupageId;

    @Schema(name = "buyerType", example = "Reeler")
    String buyerType;

    @Schema(name = "buyerId", example = "1")
    Long buyerId;

    @Schema(name="lotWeight", example = "1")
    Float lotWeight;

    @Schema(name="amount", example = "1")
    Long amount;

//    @Schema(name="marketFee", example = "1")
//    Long marketFee;

    @Schema(name="soldAmount", example = "1")
    Long soldAmount;

    @Schema(name="allottedLotId", example = "1")
    Long allottedLotId;

//    @Schema(name="marketAuctionId", example = "1")
//    Long marketAuctionId;
//
//
//    @Schema(name="id", example = "1")
//    BigInteger id;

    @Schema(name="auctionDate", example = "1")
    LocalDate auctionDate;

    @Schema(name="dflLotNumber", example = "1")
    Long dflLotNumber;

    @Schema(name="averageYield", example = "1")
    Float averageYield;

    @Schema(name="invoiceNumber", example = "1")
    String invoiceNumber;

    @Schema(name="lotParentLevel", example = "1")
    String lotParentLevel;

    @Schema(name="remainingCocoonWeight", example = "1")
    Float remainingCocoonWeight;

    @Schema(name="userMasterId", example = "1")
    Long userMasterId;

    @Schema(name="externalUnitId", example = "1")
    Long externalUnitId;

    @Schema(name = "fruitsId", example = "Reeler")
    String fruitsId;

    @Schema(name="marketId", example = "1")
    int marketId;

    @Schema(name = "purposeForRejection", example = "true",
            description = "User checkbox indicating the lot's remaining quantity is being flagged for rejection")
    Boolean purposeForRejection;

    @Schema(name = "qtyNos", example = "10")
    private Integer qtyNos;

    @Schema(name = "movingToAnotherMarket", example = "true",
            description = "User checkbox indicating remaining cocoon is being moved to another market")
    private Boolean movingToAnotherMarket;

    @Schema(name = "movingMarketReason", example = "Better price expected at downstream market",
            description = "Reason captured when movingToAnotherMarket is true")
    private String movingMarketReason;

    @Schema(name = "isMarketPaid", example = "0",
            description = "0 = market fee not yet collected, 1 = market fee paid")
    private Integer isMarketPaid;

    @Schema(name = "saleDisposalId", example = "5",
            description = "Chosen sale_and_disposal_of_dfls id when multiple disposal rows match the same "
                    + "fruitsId + lotNumber. Set by the UI after the user selects one; the backend marks "
                    + "only this row as disposed.")
    private Integer saleDisposalId;
}
