package com.sericulture.marketandauction.model.api.marketauction;

import com.sericulture.marketandauction.model.api.RequestBody;
import lombok.*;

import java.math.BigInteger;
import java.time.LocalDate;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class ReelerAuctionRequest extends RequestBody {

    private int marketId;

    private int godownId;

    private int allottedLotId;


    private BigInteger reelerId;


    private int amount;


    private String status;


    private LocalDate auctionDate;


    private boolean surrogateBid;

    private String auctionNumber;
}
