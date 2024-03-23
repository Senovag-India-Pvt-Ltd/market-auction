package com.sericulture.marketandauction.model.api.marketauction.reporting;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class AverageCocoonYearWise {
    private String year;
    private List<AverageCocoonReport> averageCocoonReports;
}