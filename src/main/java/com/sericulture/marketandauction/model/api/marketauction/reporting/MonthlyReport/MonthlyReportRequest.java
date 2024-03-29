package com.sericulture.marketandauction.model.api.marketauction.reporting.MonthlyReport;

import com.sericulture.marketandauction.model.api.RequestBody;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class MonthlyReportRequest extends RequestBody {
    private LocalDate startDate;
    private LocalDate endDate;
}