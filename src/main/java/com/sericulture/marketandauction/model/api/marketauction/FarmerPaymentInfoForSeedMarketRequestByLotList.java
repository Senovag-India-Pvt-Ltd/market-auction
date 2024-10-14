package com.sericulture.marketandauction.model.api.marketauction;

import lombok.*;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class FarmerPaymentInfoForSeedMarketRequestByLotList extends FarmerPaymentInfoForSeedMarketRequest{

    private List<Long> allottedLotList;
}
