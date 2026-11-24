package com.sericulture.marketandauction.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;

/** SR-005: an e-Inward transaction (market_auction) brought by a farmer's authorised agent. Table: V005. */
@Entity
@Table(name = "farmer_representative_txn")
@Getter
@Setter
@NoArgsConstructor
public class FarmerRepresentativeTxn extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "farmer_representative_txn_id")
    private Long farmerRepresentativeTxnId;

    @Column(name = "market_auction_id", nullable = false)
    private Long marketAuctionId;

    @Column(name = "farmer_id", nullable = false)
    private Long farmerId;

    @Column(name = "farmer_representative_id", nullable = false)
    private Long farmerRepresentativeId;

    @Column(name = "market_id")
    private Long marketId;

    @Column(name = "auction_date")
    private LocalDate auctionDate;
}
