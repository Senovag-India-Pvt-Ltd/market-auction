package com.sericulture.marketandauction.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigInteger;
import java.time.LocalDate;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ReelerAuction extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "REELER_AUCTION_SEQ")
    @SequenceGenerator(name = "REELER_AUCTION_SEQ", sequenceName = "REELER_AUCTION_SEQ", allocationSize = 1)
    @Column(name = "REELER_AUCTION_ID")
    private BigInteger id;


    @Column(name = "MARKET_ID")
    private int marketId;

    @Column(name = "ALLOTTED_LOT_ID")
    private int allottedLotId;

    @Column(name = "REELER_ID")
    private BigInteger reelerId;

    @Column(name = "AMOUNT")
    private int amount;

    @Column(name = "STATUS")
    private String status;

    @Temporal(TemporalType.DATE)
    @Column(name = "AUCTION_DATE")
    private LocalDate auctionDate;

    @Column(name = "SURROGATE_BID")
    private boolean surrogateBid;



}
