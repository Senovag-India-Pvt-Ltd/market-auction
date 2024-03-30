package com.sericulture.marketandauction.model.api.marketauction.reporting.VahivaatuReport;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class RaceWiseReport {
    private String raceName;
    private VahivaatuInfo vahivaatuInfo;
}