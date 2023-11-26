package com.sericulture.marketandauction.model.api.marketauction;

import com.sericulture.marketandauction.model.api.RequestBody;
import lombok.*;

import java.math.BigInteger;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class CancellationRequest extends RequestBody {

    private int marketId;

    private int cancellationReason;

    private BigInteger auctionId;

    private int allottedLotId;


}
