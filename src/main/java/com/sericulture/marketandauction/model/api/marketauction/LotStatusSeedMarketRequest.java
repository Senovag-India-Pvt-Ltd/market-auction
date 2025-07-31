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

    @Schema(name = "fromDate", example = "1")
    private LocalDate fromDate;

    @Schema(name = "toDate", example = "1")
    private LocalDate toDate;

    @Schema(name = "grainageMasterId", example = "1")
    private Long grainageMasterId;

    @Schema(name = "externalUnitRegistrationId", example = "1")
    private Long externalUnitRegistrationId;
}
