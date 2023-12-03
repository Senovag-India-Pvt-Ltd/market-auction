package com.sericulture.marketandauction.model.api.marketauction;

import com.sericulture.marketandauction.model.api.RequestBody;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigInteger;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class CancelAuctionRequest extends RequestBody {

    @Schema(name = "marketId", example = "1", required = true)
    private int marketId;
    @Schema(name = "cancellationReason", example = "do not want to participate", required = true)
    private int cancellationReason;

    @Schema(name = "auctionId", example = "1", required = true)
    private BigInteger auctionId;

}
