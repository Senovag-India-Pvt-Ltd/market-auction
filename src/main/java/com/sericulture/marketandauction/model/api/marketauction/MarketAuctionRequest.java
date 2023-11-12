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
public class MarketAuctionRequest extends RequestBody {

    @Schema(name = "marketId", example = "1", required = true)
    private int marketId;

    @Schema(name = "gowdownId", example = "1", required = false)
    private int godownId;

    @Schema(name = "farmerId", example = "123", required = true)
    private BigInteger farmerId;

    @Schema(name = "source", example = "private CRC", required = true)
    private String source;

    @Schema(name = "race", example = "FC1 X FC2", required = true)
    private String race;

    @Schema(name = "dflCount", example = "how many dfls", required = true)
    private int dflCount;

    @Schema(name = "estimatedWeight", example = "Estimated weight from farmer", required = true)
    private int estimatedWeight;

    @Schema(name = "status", example = "Generated status field")
    private String status;

    @Schema(name = "numberOfLot", example = "number of lots assigned to this transaction", required = true)
    private int numberOfLot;

    @Schema(name = "numberOfSmallBin", example = "number of small bins assigned to this transaction", required = true)
    private int numberOfSmallBin;

    @Schema(name = "numberOfBigBin", example = "number of big bins assigned to this transaction", required = true)
    private int numberOfBigBin;

    private LocalDate marketAuctionDate;
}
