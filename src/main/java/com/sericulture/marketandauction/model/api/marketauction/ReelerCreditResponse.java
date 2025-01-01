package com.sericulture.marketandauction.model.api.marketauction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public class ReelerCreditResponse {
    private String marketName;
    private String postDate;
    private String depositCount;
    private String totalDepositedAmount;
    private String totalReelersCount;
    private String totalOfAllMarketDepositedAmount;

}
