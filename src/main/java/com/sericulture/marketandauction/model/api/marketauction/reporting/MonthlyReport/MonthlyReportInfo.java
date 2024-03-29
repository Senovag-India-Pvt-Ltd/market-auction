package com.sericulture.marketandauction.model.api.marketauction.reporting.MonthlyReport;

import lombok.*;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class MonthlyReportInfo {
    private String startWeight;
    private String endWeight;
    private String startAmount;
    private String endAmount;
    private String startAvg;
    private String endAvg;
}