package com.sericulture.marketandauction.model.api.marketauction;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class CancelAuctionByLotRequest extends CancelAuctionRequest{
    @Schema(name = "lotId", example = "1", required = true)
    private int allottedLotId;
}
