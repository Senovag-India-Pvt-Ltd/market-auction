package com.sericulture.marketandauction.model.api.marketauction.reporting.VahivaatuReport;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class VahivaatuReport {
    private List<DistrictWise> districtWises;
    private DistrictWise overAllSum;
}