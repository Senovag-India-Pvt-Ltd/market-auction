package com.sericulture.marketandauction.model.api.marketauction;

import com.sericulture.marketandauction.model.api.RequestBody;
import lombok.*;

import java.math.BigInteger;
@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class RemoveReelerHighestBidRequest extends LotStatusRequest {
    BigInteger reelerAuctionId;
    BigInteger reelerId;

}
