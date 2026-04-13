package com.sericulture.marketandauction.model.api.marketauction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.Date;

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

    private String parentalLevel;
    private String noOfDfls;
    private Integer fcIssued;

    private Float lotWeight;
    private Float estimatedWeight;

    private Integer cocoonsPerKg;
    private Float meltPercentage;
    private Float totalQuantity;

    private Float rspQty;
    private Float nssoQty;
    private Float govtGrainageQty;
    private Float reelingQty;
    private Float remainingCocoon;


    private String rspName;
    private String nssoName;
    private String govtGrainageName;
    private String reelingName;
    private Date marketAuctionDate;

    private String spunFromDate;
    private String spunToDate;
    private Float amount;
    private Float marketFee;
    private String lgAuctionDate;
    private Float ratePerKg;
    private Float soldAmount;



}
