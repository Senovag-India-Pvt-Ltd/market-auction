package com.sericulture.marketandauction.model.api.marketauction.reporting.MonthlyReport;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class MonthlyReportResponse {
    private List<MonthlyReportRaceWise> monthlyReportRaceWiseList;
    private MonthlyReportRaceWise karnatakaResponse;
    private MonthlyReportRaceWise tamilNaduResponse;
    private MonthlyReportRaceWise andraPradeshResponse;
    private MonthlyReportRaceWise otherStateResponse;
    private String thisYearDate;
    private String prevYearDate;
}