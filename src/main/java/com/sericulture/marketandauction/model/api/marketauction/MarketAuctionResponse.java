package com.sericulture.marketandauction.model.api.marketauction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sericulture.marketandauction.model.api.ResponseBody;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigInteger;
import java.util.List;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarketAuctionResponse extends ResponseBody {

    @Schema(name="transactionId", example = "1")
    BigInteger transactionId;

    @Schema(name="marketId", example = "1")
    int marketId;

    @Schema(name="godownId", example = "1")
    int godownId;

    @Schema(name="farmerId", example = "1")
    BigInteger farmerId;

    @Schema(name="reelerId", example = "1")
    int reelerId;

    @Schema(name="allotedLotList", example = "[1,2]")
    List<Integer> allotedLotList;

    @Schema(name="allotedSmallBinList", example = "[1,2]")
    List<Integer> allotedSmallBinList;

    @Schema(name="allotedBigBinList", example = "[1,2]")
    List<Integer> allotedBigBinList;
}
