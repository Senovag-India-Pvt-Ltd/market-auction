package com.sericulture.marketandauction.model.api.marketauction;

import com.sericulture.marketandauction.model.api.RequestBody;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigInteger;
import java.time.LocalDate;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class SearchMarketByFarmerAndAuctionDateRequest extends RequestBody {


    @Schema(name = "farmerId", example = "1")
    private BigInteger farmerId;

    @Schema(name = "reelerId", example = "1")
    private Integer reelerId;

    @Schema(name = "auctionDate", example = "2023-12-29")
    private LocalDate auctionDate;
}
