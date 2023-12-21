package com.sericulture.marketandauction.model.api.marketauction;

import lombok.*;

import java.math.BigInteger;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class ReelerBidAcceptRequest extends LotStatusRequest{


    private String bidAcceptedBy;
}
