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
public class BinCounter extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "BIN_COUNTER_SEQ")
    @SequenceGenerator(name = "BIN_COUNTER_SEQ", sequenceName = "BIN_COUNTER_SEQ", allocationSize = 1)
    @Column(name = "BIN_COUNTER_ID")
    private BigInteger id;

    @Column(name = "MARKET_ID")
    private int marketId;

    @Column(name = "GODOWN_ID")
    private int godownId;

    @Column(name = "SMALL_BIN_NEXT_NUMBER")
    private int smallBinNextNumber;

    @Column(name = "BIG_BIN_NEXT_NUMBER")
    private int bigBinNextNumber;

    @Temporal(TemporalType.DATE)
    @Column(name = "AUCTION_DATE")
    private LocalDate auctionDate;
}
