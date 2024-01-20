package com.sericulture.marketandauction.model.api.marketauction;

import com.sericulture.marketandauction.model.api.ResponseBody;
import lombok.*;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Builder
public class LotHighestBidResponse extends ResponseBody {
    private int allottedLotId;
    private int highestBid = 0;
    private String reelerName;
}
