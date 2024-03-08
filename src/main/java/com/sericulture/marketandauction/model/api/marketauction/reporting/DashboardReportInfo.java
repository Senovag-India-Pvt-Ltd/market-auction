package com.sericulture.marketandauction.model.api.marketauction.reporting;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
@Builder
public class DashboardReportInfo {
    private String raceName;
    private String totalLots;
    private String totalLotsBid;
    private String totalLotsNotBid;
    private String totalBids;
    private String totalReelers;
    private String currentAuctionMaxBid;
    private String accecptedLots;
    private String accecptedLotsMaxBid;
    private String accectedLotsMinBid;
    private String averagRate;
    private String weighedLots;
    private String totalSoldOutAmount;
}
