package com.sericulture.marketandauction.model.api.marketauction;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class CancelAuctionByLotRequest extends CancelRequest{
    @Schema(name = "allottedLotId", example = "1", required = true)
    private int allottedLotId;

}
