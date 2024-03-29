package com.sericulture.marketandauction.model.api.marketauction.reporting.MarketReport;

import lombok.*;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class MarketReportResponse {
    private MarketReports marketReports;
}