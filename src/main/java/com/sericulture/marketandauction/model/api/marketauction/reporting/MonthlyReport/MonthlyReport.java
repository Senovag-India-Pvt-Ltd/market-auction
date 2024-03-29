package com.sericulture.marketandauction.model.api.marketauction.reporting.MonthlyReport;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class MonthlyReport {
    private MonthlyReportResponse monthlyReportResponse;
}