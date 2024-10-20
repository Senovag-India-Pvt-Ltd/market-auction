package com.sericulture.marketandauction.model.api.marketauction.reporting;

import com.sericulture.marketandauction.model.api.RequestBody;
import lombok.*;

import java.time.LocalDate;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class ReportRequest extends RequestBody {

    private LocalDate reportFromDate;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String reelerNumber;
    private String traderLicenseNumber;



}
