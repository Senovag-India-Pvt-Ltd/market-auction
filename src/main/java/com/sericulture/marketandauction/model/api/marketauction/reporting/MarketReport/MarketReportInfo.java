package com.sericulture.marketandauction.model.api.marketauction.reporting.MarketReport;

import com.sericulture.marketandauction.model.api.marketauction.reporting.DTR.DTRResponse;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class MarketReportInfo {
    private String startingWeight;
    private String startingAmount;
    private String startingAvg;
    private String endingWeight;
    private String endingAmount;
    private String endingAvg;
}