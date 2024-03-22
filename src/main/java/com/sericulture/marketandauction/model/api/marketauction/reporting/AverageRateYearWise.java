package com.sericulture.marketandauction.model.api.marketauction.reporting;

import lombok.*;

import java.util.List;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class AverageRateYearWise {
    private String year;
    private List<AverageRateRaceWise> averageRateRaceWiseList;
}