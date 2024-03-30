package com.sericulture.marketandauction.model.api.marketauction.reporting.VahivaatuReport;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class DistrictWise {
    private String districtName;
    private List<RaceWiseReport> raceWiseReports;
}