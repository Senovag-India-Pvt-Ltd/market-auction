package com.sericulture.marketandauction.model.api.marketauction;

import com.sericulture.marketandauction.model.api.RequestBody;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigInteger;
import java.time.LocalDate;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class ReelerBidRequest extends RequestBody {

    @Schema(name = "marketId", example = "1", required = true)
    private int marketId;
    @Schema(name = "godownId", example = "1")
    private int godownId;

    @Schema(name = "allottedLotId", example = "1", required = true)
    private int allottedLotId;

    @Schema(name = "reelerId", example = "1", required = true)
    private BigInteger reelerId;

    @Schema(name = "amount", example = "1", required = true)
    private int amount;

    @Schema(name = "auctionDate", example = "1", required = true)
    private LocalDate auctionDate;

    @Schema(name = "auctionNumber", example = "1", required = true)
    private String auctionNumber;

    @Schema(name = "reelerAuctionId", example = "1", required = true)
    private BigInteger reelerAuctionId;
}
