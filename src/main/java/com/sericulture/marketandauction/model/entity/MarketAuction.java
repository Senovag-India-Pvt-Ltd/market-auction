package com.sericulture.marketandauction.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

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
@ToString
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
    @Column(name = "SOURCE_MASTER_ID")
    private int sourceMasterId;
    @Column(name = "RACE_MASTER_ID")
    private int raceMasterId;
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
    @Column(name = "REASON_FOR_CANCELLATION")
    private int reasonForCancellation;
    @Column(name = "dfl_lot_number")
    private String dflLotNumber;
    @Column(name = "lot_variety")
    private String lotVariety;
    @Column(name = "lot_parental_level")
    private String lotParentalLevel;
    @Column(name = "reeler_id")
    private int reelerId;
}
