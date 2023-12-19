package com.sericulture.marketandauction.model.api.marketauction;

import com.sericulture.marketandauction.model.api.RequestBody;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class CanContinueToWeighmentRequest extends LotStatusRequest {

    @Schema(name = "noOfCrates", example = "2", required = true)
    private int noOfCrates;

    @Schema(name = "remarks", example = "remarks if any", required = true)
    private String remarks;

}
