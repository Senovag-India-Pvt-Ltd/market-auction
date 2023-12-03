package com.sericulture.marketandauction.model.api.marketauction;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;


@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class ReelerSurrogateBidRequest extends ReelerBidRequest{

    @Schema(name = "surrogateBid", example = "1", required = true)
    private boolean surrogateBid;

}
