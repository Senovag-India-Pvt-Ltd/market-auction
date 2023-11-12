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
public class BinCounterMaster extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "BIN_COUNTER_MASTER_SEQ")
    @SequenceGenerator(name = "BIN_COUNTER_MASTER_SEQ", sequenceName = "BIN_COUNTER_MASTER_SEQ", allocationSize = 1)
    @Column(name = "BIN_COUNTER_MASTER_ID")
    private int id;


    @Column(name = "MARKET_ID")
    private int marketId;


    @Column(name = "GODOWN_ID")
    private int godownId;


    @Column(name = "SMALL_BIN_START")
    private int smallBinStart;


    @Column(name = "BIG_BIN_START")
    private int bigBinStart;


    @Column(name = "SMALL_BIN_END")
    private int smallBinEnd;


    @Column(name = "BIG_BIN_END")
    private int bigBinEnd;

    @Temporal(TemporalType.DATE)
    @Column(name = "AUCTION_DATE")
    private LocalDate auctionDate;
}
