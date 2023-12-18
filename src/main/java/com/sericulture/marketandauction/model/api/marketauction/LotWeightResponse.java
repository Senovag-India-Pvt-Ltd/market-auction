package com.sericulture.marketandauction.model.api.marketauction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sericulture.marketandauction.model.api.ResponseBody;
import lombok.*;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class LotWeightResponse extends ResponseBody {

    private String farmerFruitsId;

    private String reelerLicense;

    private String farmerName;

    private String reelerName;

    private String farmerAddress;

    private double reelerCurrentBalance;

    private float bidAmount;
}
