package com.sericulture.marketandauction.model.api.marketauction.reporting.MarketReport;

import lombok.*;
import org.apache.el.stream.Stream;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class MarketWiseInfo {
    private String marketName;
    private List<MarketReportRaceWise> marketReportRaceWises;
    private String totalWeightStarting;
    private String totalWeightEnding;
    private String totalAmountStarting;
    private String totalAmountEnding;
    private String avgAmountStarting;
    private String avgAmountEnding;
    private String marketFeeStarting;
    private String marketFeeEnding;
    private String lotsStarting;
    private String lotsEnding;
}