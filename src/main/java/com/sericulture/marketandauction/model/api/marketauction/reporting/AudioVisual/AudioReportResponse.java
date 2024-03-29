package com.sericulture.marketandauction.model.api.marketauction.reporting.AudioVisual;

import com.sericulture.marketandauction.model.api.marketauction.reporting.DTR.DTRMarketResponse;
import lombok.*;

import java.util.List;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class AudioReportResponse {
    private List<MonthWiseReport> monthWiseReports;
}