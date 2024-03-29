package com.sericulture.marketandauction.model.api.marketauction.reporting.AudioVisual;

import com.sericulture.marketandauction.model.api.marketauction.reporting.DTR.DTRMarketResponse;
import com.sericulture.marketandauction.model.api.marketauction.reporting.DTR.DTRResponse;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class MonthWiseReport {
    private String month;
    private List<DTRMarketResponse> dtrMarketResponses;
}