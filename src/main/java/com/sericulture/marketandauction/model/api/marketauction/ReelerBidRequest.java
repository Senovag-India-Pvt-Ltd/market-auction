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
@ToString
public class ReelerBidRequest extends LotStatusRequest {

    @Schema(name = "reelerId", example = "1", required = true)
    private BigInteger reelerId;

    @Schema(name = "traderLicenseId", example = "1", required = true)
    private BigInteger traderLicenseId;

    @Schema(name = "amount", example = "1", required = true)
    private int amount;

}
