package com.sericulture.marketandauction.model.api.marketauction;

import lombok.Data;

@Data
public class Buyer {

    private String lgBuyerType;
    private String lgBuyerName;
    private String lgLotWeight;
    private String lgAmount;
    private String lgSoldOutAmount;
    private Long noOfCocoonPerKg;
    private String remainingCocoon;
    private Float farmerAmount;
    private Float lgMarketFee;
    private Float lgMarketFeeForReeling;
    private Float lgMarketFeeForSeed;
    private String lgSoldOutAmountReeling;
    private String lgSoldOutAmountSeed;


}
