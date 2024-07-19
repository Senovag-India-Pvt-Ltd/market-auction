package com.sericulture.marketandauction.model.api.marketauction.reporting;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class GroupLotStatus {
    private String description;
    private String lot;
    private String weight;
    private String amount;
    private String mf;
    private String min;
    private String max;
    private String avg;

//    private String totalLots;
//    private String totalWeight;
//    private String totalAmount;
//    private String totalMarketFee;
//    private String totalMin;
//    private String totalMax;
//    private String totalAvg;
}