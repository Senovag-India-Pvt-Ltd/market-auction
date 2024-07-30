package com.sericulture.marketandauction.model.api.marketauction;

import com.sericulture.marketandauction.model.api.RequestBody;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class UpdateNetWeightRequest extends RequestBody {

    @Schema(name = "allottedLotId", example = "1")
    private int allottedLotId;

    @Schema(name = "auctionDate", example = "1")
    private LocalDate auctionDate;

    @Schema(name = "crateNumber", example = "1")
    private int crateNumber;

    @Schema(name = "netWeight", example = "[10.5, 20.0, 15.75]")
    private List<Float> netWeight;

}
