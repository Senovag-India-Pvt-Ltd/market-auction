package com.sericulture.marketandauction.model.api.marketauction;

import com.sericulture.marketandauction.model.api.RequestBody;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class LotStatusSeedMarketRequest extends RequestBody {

    @Schema(name = "allottedLotId", example = "1", required = true)
    private int allottedLotId;

    @Schema(name = "auctionDate", example = "1")
    private LocalDate auctionDate;
}
