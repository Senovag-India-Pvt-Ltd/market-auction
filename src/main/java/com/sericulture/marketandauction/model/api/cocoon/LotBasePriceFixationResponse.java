package com.sericulture.marketandauction.model.api.cocoon;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class LotBasePriceFixationResponse {
    private long id;
    private int marketId;
    private int pricePerKg;
    private int allottedLotId;
    private LocalDate fixationDate;
    @Schema(name = "error", example = "true")
    Boolean error;

    @Schema(name = "error_description", example = "Username or password is incorrect")
    String error_description;
}
