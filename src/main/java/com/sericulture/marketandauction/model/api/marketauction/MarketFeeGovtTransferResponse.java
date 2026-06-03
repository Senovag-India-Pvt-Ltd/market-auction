package com.sericulture.marketandauction.model.api.marketauction;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@Builder
public class MarketFeeGovtTransferResponse {
    private String fruitsId;
    private String farmerName;
    private double farmerMarketFee;
    private double reelerMarketFee;
    private int lotNo;
}