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
public class Lot extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "LOT_SEQ")
    @SequenceGenerator(name = "LOT_SEQ", sequenceName = "LOT_SEQ", allocationSize = 1)
    @Column(name = "LOT_ID")
    private BigInteger id;

    @Column(name = "ALLOTTED_LOT_ID")
    private int allottedLotId;

    @Column(name = "MARKET_AUCTION_ID")
    private BigInteger marketAuctionId;

    @Column(name = "STATUS")
    @Size( message = "status of the lot")
    private String status;

    @Column(name = "REJECTED_BY")
    @Size( message = "either market officer or farmer")
    private String rejectedBy;

    @Column(name = "REJECTION_REASON")
    @Size( message = "reason for rejection")
    private String rejectionReason;

    @Column(name = "MARKET_ID")
    private int marketId;

    @Column(name = "GODOWN_ID")
    private int godownId;

    @Temporal(TemporalType.DATE)
    @Column(name = "AUCTION_DATE")
    private LocalDate auctionDate;
}
