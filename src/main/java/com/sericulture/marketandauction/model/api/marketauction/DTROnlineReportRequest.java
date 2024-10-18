package com.sericulture.marketandauction.model.api.marketauction;

import com.sericulture.marketandauction.model.api.RequestBody;
import lombok.*;

import java.time.LocalDate;
@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class DTROnlineReportRequest extends RequestBody {
    private int reelerId;
    private int traderLicenseNumber;
    private LocalDate fromDate;
    private LocalDate toDate;
}
