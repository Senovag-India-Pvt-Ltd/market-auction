package com.sericulture.marketandauction.model.api.marketauction;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SeedMFResponse {

    private Integer allottedLotId;
    private String auctionDate;

    private Float totalWeight;
    private Float bidAmount;

    private Float totalSoldAmount;
    private Float totalMarketFee;

    private String licenseNumber;
    private String buyerName;

    private Integer serialNumber;
}