package com.sericulture.marketandauction.model.api.cocoon;

import com.sericulture.marketandauction.model.api.marketauction.FarmerPaymentInfoResponse;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class PupaTestAndCocoonAssessmentResponse {
    private int id;
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
    private List<SeedMarketAuctionDetailsResponse> seedMarketAuctionDetailsResponseList;
}
