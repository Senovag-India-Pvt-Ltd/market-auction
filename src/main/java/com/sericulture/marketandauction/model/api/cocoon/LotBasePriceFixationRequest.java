package com.sericulture.marketandauction.model.api.cocoon;

import lombok.*;

import java.time.LocalDate;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class LotBasePriceFixationRequest {
    private int marketId;
    private int pricePerKg;
}
