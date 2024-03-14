package com.sericulture.marketandauction.model.api.marketauction;

import com.sericulture.marketandauction.model.api.marketauction.reporting.ReportRequest;
import lombok.*;

import java.time.LocalDate;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class ReelerReportForAppRequest extends ReportRequest {
    private LocalDate auctionDate;
    private int marketId;
    private int reelerId;
}