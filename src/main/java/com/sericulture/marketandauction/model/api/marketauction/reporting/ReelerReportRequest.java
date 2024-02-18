package com.sericulture.marketandauction.model.api.marketauction.reporting;

import lombok.*;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class ReelerReportRequest extends ReportRequest{
    private String reelerNumber;
}
