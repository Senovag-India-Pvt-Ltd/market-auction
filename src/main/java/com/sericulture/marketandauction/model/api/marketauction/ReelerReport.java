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
@Builder
public class ReelerReport extends ResponseBody {
    private int serialNumber;
    private int allottedLotId;
    private float bidAmount;
    private float weight;
    private float amount;
    private float marketFee;
}
