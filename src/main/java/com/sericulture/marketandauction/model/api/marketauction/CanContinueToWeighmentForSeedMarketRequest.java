package com.sericulture.marketandauction.model.api.marketauction;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class CanContinueToWeighmentForSeedMarketRequest extends LotStatusSeedMarketRequest {

    @Schema(name = "noOfCrates", example = "2", required = true)
    private int noOfCrates;

    @Schema(name = "remarks", example = "remarks if any", required = true)
    private String remarks;

}
