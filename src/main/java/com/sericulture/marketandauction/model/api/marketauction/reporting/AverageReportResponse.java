package com.sericulture.marketandauction.model.api.marketauction.reporting;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class AverageReportResponse {
    private List<AverageRateYearWise> averageRateYearWiseList;
}