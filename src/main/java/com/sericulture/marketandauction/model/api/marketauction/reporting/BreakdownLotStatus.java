package com.sericulture.marketandauction.model.api.marketauction.reporting;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class BreakdownLotStatus {
    private String description;
    private String lot;
    private String weight;
    private String percentage;
}