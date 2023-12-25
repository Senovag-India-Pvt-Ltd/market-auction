package com.sericulture.marketandauction.model.api.marketauction;


import lombok.*;

import java.time.LocalDate;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class MarketAuctionForPrintRequest extends LotStatusRequest{

    private LocalDate auctionDate;
}
