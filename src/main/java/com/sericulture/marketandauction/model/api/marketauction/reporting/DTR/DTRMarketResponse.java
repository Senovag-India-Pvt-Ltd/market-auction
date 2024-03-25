package com.sericulture.marketandauction.model.api.marketauction.reporting.DTR;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class DTRMarketResponse {
    private String marketNameInKannada;
    private List<DTRRaceResponse> dtrRaceResponses;
}