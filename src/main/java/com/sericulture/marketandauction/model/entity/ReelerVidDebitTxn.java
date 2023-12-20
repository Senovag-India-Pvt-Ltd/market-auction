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
public class ReelerVidDebitTxn extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "REELER_VID_DEBIT_TXN_SEQ")
    @SequenceGenerator(name = "REELER_VID_DEBIT_TXN_SEQ", sequenceName = "REELER_VID_DEBIT_TXN_SEQ", allocationSize = 1)
    @Column(name = "REELER_VID_DEBIT_TXN_ID")
    private BigInteger id;

    @Column(name = "LOT_ID")
    private int allottedLotId;

    @Column(name = "MARKET_ID")
    private int marketId;

    @Column(name = "AUCTION_DATE")
    private LocalDate auctionDate;

    @Column(name = "REELER_ID")
    private int reelerId;

    @Column(name = "VIRTUAL_ACCOUNT")
    private String reelerVirtualAccountNumber;

    @Column(name = "AMOUNT")
    private double amount;

}
