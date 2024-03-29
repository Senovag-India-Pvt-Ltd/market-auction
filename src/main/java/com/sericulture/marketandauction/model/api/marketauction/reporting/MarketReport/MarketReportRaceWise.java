package com.sericulture.marketandauction.model.api.marketauction.reporting.MarketReport;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class MarketReportRaceWise {
    private String raceName;
    private MarketReportInfo marketReportInfo;
}