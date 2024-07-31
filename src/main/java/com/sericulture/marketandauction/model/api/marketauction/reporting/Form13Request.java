package com.sericulture.marketandauction.model.api.marketauction.reporting;

import lombok.*;

import java.time.LocalDate;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class Form13Request extends ReportRequest{
    private LocalDate auctionDate;
    private Long districtId;
}