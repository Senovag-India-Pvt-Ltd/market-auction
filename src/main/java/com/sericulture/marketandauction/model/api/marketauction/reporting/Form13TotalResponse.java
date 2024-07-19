package com.sericulture.marketandauction.model.api.marketauction.reporting;

import com.sericulture.marketandauction.model.api.ResponseBody;
import lombok.*;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class Form13TotalResponse extends ResponseBody {
    private String description;
    private String totalLots;
    private String totalWeight;
    private String totalAmount;
    private String totalMarketFee;
    private String totalMin;
    private String totalMax;
    private String totalAvg;
}
