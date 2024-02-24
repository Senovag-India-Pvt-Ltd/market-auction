package com.sericulture.marketandauction.model.api.marketauction.reporting;

import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;


@Getter
@Setter
public class BinForPendingReport {

    private String type;
    private BigInteger marketAuctionId;
    private int allottedBinId;
}
