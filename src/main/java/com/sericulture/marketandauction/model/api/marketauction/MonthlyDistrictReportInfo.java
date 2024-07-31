package com.sericulture.marketandauction.model.api.marketauction;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MonthlyDistrictReportInfo {
    private String serialNumber;
    private String districtName;
    private String talukName;
    private String stateName;
    private String totalLots;
    private String totalWeight;
    private String raceName;
}
