package com.sericulture.marketandauction.model.api.marketauction.reporting;

import lombok.*;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class UnitCounterReportResponse {
    private int allottedLotId;
    private String lotTransactionDate;
    private String reelerLicense;
    private String reelerName;
    private float weight;
    private float lotSoldOutAmount;
    private int bidAmount;
    private double farmerMarketFee;
    private double reelerMarketFee;
}
