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
public class SearchMarketByStatusAndAuctionDateRequest extends RequestBody {


    @Schema(name = "status", example = "generated")
    private String status;

    @Schema(name = "auctionDate", example = "2023-12-29")
    private LocalDate auctionDate;
}
