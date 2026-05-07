package com.sericulture.marketandauction.model.api.marketauction.reporting;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SeedMarketBiddingResponse {

    private String allottedLotId;
    private String auctionDate;

    private String buyerType;
    private String bidderName;
    private String licenseNumber;

    private String lotWeight;
    private String amount;
    private String soldAmount;
    private String marketFee;

    private String marketName;

    private String createdDate;
    private String lotGroupageId;
}