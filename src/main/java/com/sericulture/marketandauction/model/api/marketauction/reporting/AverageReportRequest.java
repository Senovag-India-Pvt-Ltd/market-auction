package com.sericulture.marketandauction.model.api.marketauction.reporting;

import com.sericulture.marketandauction.model.api.RequestBody;
import lombok.*;

import java.time.LocalDate;
@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class AverageReportRequest extends RequestBody {

    private LocalDate startYear;
    private LocalDate endYear;
}