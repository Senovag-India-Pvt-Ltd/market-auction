package com.sericulture.marketandauction.model.api.marketauction.reporting.MarketWiseReport;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class DivisionWiseReport {
    private List<DivisionReport> divisionReportList;
}