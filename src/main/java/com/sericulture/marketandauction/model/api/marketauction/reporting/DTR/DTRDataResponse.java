package com.sericulture.marketandauction.model.api.marketauction.reporting.DTR;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class DTRDataResponse {
    private List<DTRMarketResponse> dtrMarketResponses;
}