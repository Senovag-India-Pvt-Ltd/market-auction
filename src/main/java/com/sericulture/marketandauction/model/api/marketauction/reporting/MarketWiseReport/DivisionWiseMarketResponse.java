package com.sericulture.marketandauction.model.api.marketauction.reporting.MarketWiseReport;

import com.sericulture.marketandauction.model.api.marketauction.reporting.MarketReport.MarketReports;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class DivisionWiseMarketResponse {
    private DivisionWiseReport divisionWiseReport;
}