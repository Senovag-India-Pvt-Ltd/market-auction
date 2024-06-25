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
public class LotGroupage extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "LOT_GROUPAGE_SEQ")
    @SequenceGenerator(name = "LOT_GROUPAGE_SEQ", sequenceName = "LOT_GROUPAGE_SEQ", allocationSize = 1)
    @Column(name = "LOT_GROUPAGE_ID")
    private Long lotGroupageId;

    @Column(name = "BUYER_TYPE")
    private String buyerType;

    @Column(name = "BUYER_ID")
    private Long buyerId;

    @Column(name = "LOT_WEIGHT")
    private String lotWeight;

    @Column(name = "AMOUNT")
    private Long amount;

    @Column(name = "MARKET_FEE")
    private Long marketFee;

    @Column(name = "SOLD_AMOUNT")
    private Long soldAmount;

    @Column(name = "ALLOTTED_LOT_ID")
    private int allottedLotId;

    @Column(name = "MARKET_AUCTION_ID")
    private BigInteger marketAuctionId;

    @Column(name = "LOT_ID")
    private BigInteger id;

    @Temporal(TemporalType.DATE)
    @Column(name = "AUCTION_DATE")
    private LocalDate auctionDate;


}
