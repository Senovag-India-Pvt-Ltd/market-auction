package com.sericulture.marketandauction.model.api.marketauction.reporting.DTR;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class DTRResponse {
    private String weight;
    private String minAmount;
    private String maxAmount;
    private String avgAmount;
}