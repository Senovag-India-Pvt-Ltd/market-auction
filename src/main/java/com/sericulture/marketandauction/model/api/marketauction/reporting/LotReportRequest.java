package com.sericulture.marketandauction.model.api.marketauction.reporting;

import lombok.*;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class LotReportRequest extends ReportRequest{
    private int lotId;
}
