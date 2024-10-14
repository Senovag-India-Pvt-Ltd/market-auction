package com.sericulture.marketandauction.model.api.cocoon;

import lombok.*;

import java.time.LocalDate;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class PupaTestResultFinderRequest {
    private int marketAuctionId;
    private LocalDate testDate;
    private String pupaTestResult;
    private String cocoonAssessmentResult;
}
