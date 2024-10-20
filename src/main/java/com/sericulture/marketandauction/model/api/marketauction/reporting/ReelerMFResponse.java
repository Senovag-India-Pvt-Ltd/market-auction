package com.sericulture.marketandauction.model.api.marketauction.reporting;

import lombok.*;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class ReelerMFResponse {
//    private int serialNumber;
//    private int allottedLotId;
//    private String lotTransactionDate;
//    private String reelerLicense;
//    private String reelerName;
//    private float weight;
//    private float lotSoldOutAmount;
//    private int bidAmount;
//    private double farmerMarketFee;
//    private double reelerMarketFee;

    private int serialNumber;
    private int allottedLotId;
    private String lotTransactionDate;
    private String reelerLicense;
    private String reelerName;
    private int bidAmount;
    private float weight;
    private float lotSoldOutAmount;
    private double farmerMarketFee;
    private double reelerMarketFee;

}
