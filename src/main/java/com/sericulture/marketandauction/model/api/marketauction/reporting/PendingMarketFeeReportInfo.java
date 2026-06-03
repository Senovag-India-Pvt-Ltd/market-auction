package com.sericulture.marketandauction.model.api.marketauction.reporting;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PendingMarketFeeReportInfo {
    private int serialNumber;
    private String farmerName;
    private String farmerId;
    private String fruitsId;
    private int lotNo;
    private String marketName;
    private long lotAmount;
    private float marketFee;
    private float paidAmount;
    private float pendingAmount;
    private String currentStatus;
}