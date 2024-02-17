package com.sericulture.marketandauction.model.api.marketauction.reporting;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class FarmerTxnInfo {
    private int serialNumber;
    private int allottedLotId;
    private String lotTransactionDate;
    private float lotSoldOutAmount;
    private double farmerMarketFee;
    private double farmerAmount;
    private float weight;
    private int bidAmount;
}
