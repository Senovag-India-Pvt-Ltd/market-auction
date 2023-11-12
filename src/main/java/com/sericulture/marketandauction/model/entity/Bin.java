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

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Bin extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "BIN_SEQ")
    @SequenceGenerator(name = "BIN_SEQ", sequenceName = "BIN_SEQ", allocationSize = 1)
    @Column(name = "BIN_ID")
    private BigInteger id;


    @Column(name = "ALLOTTED_BIN_ID")
    private int allottedBinId;


    @Column(name = "MARKET_AUCTION_ID")
    private BigInteger marketAuctionId;

    @Size( message = "type can be small or big")
    @Column(name = "BIN_TYPE")
    private String type;

    @Temporal(TemporalType.DATE)
    @Column(name = "AUCTION_DATE")
    private LocalDate auctionDate;

    public Bin(int allottedBinId, BigInteger marketAuctionId, String type) {
        this.allottedBinId = allottedBinId;
        this.marketAuctionId = marketAuctionId;
        this.type = type;
    }
}
