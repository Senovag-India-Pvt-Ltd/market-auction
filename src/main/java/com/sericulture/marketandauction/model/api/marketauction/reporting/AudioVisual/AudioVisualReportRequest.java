package com.sericulture.marketandauction.model.api.marketauction.reporting.AudioVisual;

import com.sericulture.marketandauction.model.api.RequestBody;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class AudioVisualReportRequest extends RequestBody {
    private LocalDate startDate;
    private LocalDate endDate;
    private List<Integer> marketList;
}