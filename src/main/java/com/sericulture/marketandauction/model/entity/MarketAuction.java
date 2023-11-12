package com.sericulture.marketandauction.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.Date;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
//@Where(clause = "active=1")
public class MarketAuction extends BaseEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "MARKET_AUCTION_SEQ")
    @SequenceGenerator(name = "MARKET_AUCTION_SEQ", sequenceName = "MARKET_AUCTION_SEQ", allocationSize = 1)
    @Column(name = "MARKET_AUCTION_ID")
    private BigInteger id;

    @Temporal(TemporalType.DATE)
    @Column(name = "MARKET_AUCTION_DATE")
    private LocalDate marketAuctionDate;

    @Column(name = "MARKET_ID")
    private int marketId;

    @Column(name = "GODOWN_ID")
    private int godownId;

    @Column(name = "FARMER_ID")
    private BigInteger farmerId;

    @Size( message = "Source of the cocoon")
    @Column(name = "SOURCE")
    private String source;

    @Column(name = "RACE")
    private String race;

    @Column(name = "DFL_COUNT")
    private int dflCount;

    @Column(name = "ESTIMATED_WEIGHT")
    private int estimatedWeight;

    @Column(name = "STATUS")
    private String status;

    @Column(name = "NUMBER_OF_LOT")
    private int numberOfLot;

    @Column(name = "NUMBER_OF_SMALL_BIN")
    private int numberOfSmallBin;

    @Column(name = "NUMBER_OF_BIG_BIN")
    private int numberOfBigBin;

}
