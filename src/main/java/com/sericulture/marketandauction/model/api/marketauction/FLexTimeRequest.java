package com.sericulture.marketandauction.model.api.marketauction;

import com.sericulture.marketandauction.helper.MarketAuctionHelper;
import com.sericulture.marketandauction.model.api.RequestBody;
import lombok.*;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class FLexTimeRequest extends RequestBody {

    private MarketAuctionHelper.activityType activityType;

    private boolean start;

    private int marketId;

    private int godownId;


}
