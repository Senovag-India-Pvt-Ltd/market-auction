package com.sericulture.marketandauction.model.api.marketauction.reporting;

import lombok.*;

import java.time.LocalDate;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class ReelerTxnReportRequest extends ReelerReportRequest{
    private LocalDate reportToDate;
}
