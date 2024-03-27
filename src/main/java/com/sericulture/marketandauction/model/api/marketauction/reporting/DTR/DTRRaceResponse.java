package com.sericulture.marketandauction.model.api.marketauction.reporting.DTR;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class DTRRaceResponse {
    private String raceNameInKannada;
    private List<DTRResponse> dtrResponses;
    private List<DTRResponse> prevResponses;
    private List<DTRResponse> lastYearResponses;
}