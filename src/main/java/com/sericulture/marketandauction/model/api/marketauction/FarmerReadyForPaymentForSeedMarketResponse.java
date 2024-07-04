package com.sericulture.marketandauction.model.api.marketauction;

import com.sericulture.marketandauction.model.api.ResponseBody;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class FarmerReadyForPaymentForSeedMarketResponse extends ResponseBody {

    private List<FarmerReadyPaymentInfoForSeedMarketResponse> farmerReadyPaymentInfoForSeedMarketResponseList;

    private Long soldAmount;

    private String paymentMode;
}
