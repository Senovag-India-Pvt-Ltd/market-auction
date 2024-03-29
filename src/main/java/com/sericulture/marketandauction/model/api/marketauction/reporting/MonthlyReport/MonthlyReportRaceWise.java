package com.sericulture.marketandauction.model.api.marketauction.reporting.MonthlyReport;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class MonthlyReportRaceWise {
    private String raceName;
    private MonthlyReportInfo thisYearReport;
    private MonthlyReportInfo prevYearReport;
}