package com.sericulture.marketandauction.model.api.marketauction.reporting;

import lombok.*;

import java.time.LocalDate;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class FarmerTxnReportRequest extends ReportRequest{
    private LocalDate reportToDate;
    private String farmerNumber;
}
