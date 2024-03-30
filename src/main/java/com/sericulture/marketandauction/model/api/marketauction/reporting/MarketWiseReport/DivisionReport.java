package com.sericulture.marketandauction.model.api.marketauction.reporting.MarketWiseReport;

import com.sericulture.marketandauction.model.api.marketauction.reporting.MarketReport.MarketReports;
import com.sericulture.marketandauction.model.api.marketauction.reporting.MarketReport.MarketWiseInfo;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class DivisionReport {
    private String divisionName;
    private List<MarketWiseInfo> marketWiseInfoList;
}