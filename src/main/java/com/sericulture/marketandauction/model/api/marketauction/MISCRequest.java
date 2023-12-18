package com.sericulture.marketandauction.model.api.marketauction;

import com.sericulture.marketandauction.model.api.RequestBody;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class MISCRequest extends RequestBody {

    @Schema(name = "marketId", example = "1", required = true)
    private int marketId;
    @Schema(name = "godownId", example = "1")
    private int godownId;

    @Schema(name = "allottedLotId", example = "1", required = true)
    private int allottedLotId;

    private String race;

    private float reelerCurrentBalance;

    private float bidAmount;

    private int noOfCrates;

    private double weight;

    private String remarks;
}
