package com.sericulture.marketandauction.model.api.marketauction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class LotDistributeBuyerWiseResponse {

    private Integer serialNumber;
    private String farmerNumber;
    private String farmerFruitsId;
    private String farmerFullName;
    private String fatherNameKan;
    private String districtNameKan;
    private String talukNameKan;
    private String farmerVillage;
    private String marketName;
    private String race;
    private String source;
    private String lotParentLevel;
    private Integer allottedLotId;
    private String auctionDate;
    private String buyerName;
    private String calculatedAverageYield;
    private String lotWeightAfterWeighment;
    private String totalRspNssoGrainageLotWeight;
    private String totalReelingLotWeight;
    private String totalRspNssoGrainageAmount;
    private String totalReelingAmount;
    private String totalRspNssoGrainageSoldAmount;
    private String totalReelingSOldAmount;
    private String totalRspNssoGrainageMarketFee;
    private String totalReelingMarketFee;
    private String totalLotWeight;
    private String totalAmount;
    private String totalSoldAmount;
    private String totalMarketFee;
    private String price;
}
