package com.sericulture.marketandauction.model.api.marketauction.reporting.VahivaatuReport;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class VahivaatuInfo {
    private String totalCocoonStarting;
    private String soldOutCocoonStarting;
    private String totalCocoonEnding;
    private String soldOutCocoonEnding;
}