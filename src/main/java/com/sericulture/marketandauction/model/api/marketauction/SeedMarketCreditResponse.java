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
public class SeedMarketCreditResponse {

    private String marketName;

    private String marketId;

    private String postDate;

    private String totalReelerAmount;

    private String totalExternalUnitAmount;

    private String totalAmount;

    private String totalReelerDepositCount;

    private String totalExternalUnitDepositCount;

}