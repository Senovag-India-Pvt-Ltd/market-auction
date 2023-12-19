package com.sericulture.marketandauction.model.api.marketauction;

import com.sericulture.marketandauction.model.api.ResponseBody;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class CanContinueToWeighmentResponse extends ResponseBody {
    @Schema(name = "weight", example = "25", required = true)
    private int weight;

}
