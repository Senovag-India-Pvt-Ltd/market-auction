package com.sericulture.marketandauction.model.api.marketauction.reporting;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class AverageCocoonReport {
    private String month;
    private String lotSoldAmount;
    private String weight;
}