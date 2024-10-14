package com.sericulture.marketandauction.model.api.cocoon;

import lombok.*;

import java.time.LocalDate;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class PupaTestAndCocoonAssessmentRequest {
    private int marketAuctionId;
    private LocalDate testDate;
    private int noOfCocoonTakenForExamination;
    private int noOfDflFromFc;
    private boolean diseaseFree;
    private String diseaseType;
    private int noOfCocoonPerKg;
    private float meltPercentage;
    private String pupaTestResult;
    private String cocoonAssessmentResult;
    private String pupaCocoonStatus;
}
