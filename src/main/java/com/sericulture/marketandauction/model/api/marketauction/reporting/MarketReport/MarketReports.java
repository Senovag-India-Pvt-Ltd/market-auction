package com.sericulture.marketandauction.model.api.marketauction.reporting.MarketReport;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class MarketReports {
    private List<MarketWiseInfo> marketWiseInfos;
}