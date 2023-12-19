package com.sericulture.marketandauction.model.api.marketauction;

import lombok.*;

import java.math.BigInteger;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class ReelerBidAcceptRequest {

    private BigInteger reelerAuctionId;

    private String bidAcceptedBy;
}
