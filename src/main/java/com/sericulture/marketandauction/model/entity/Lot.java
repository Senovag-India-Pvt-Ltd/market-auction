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

    @Column(name = "REASON_FOR_CANCELLATION")
    private Integer reasonForCancellation;

    @Column(name = "MARKET_ID")
    private int marketId;

    @Temporal(TemporalType.DATE)
    @Column(name = "AUCTION_DATE")
    private LocalDate auctionDate;

    @Column(name = "REELER_AUCTION_ID")
    private BigInteger reelerAuctionId;

    @Column(name = "REELER_AUCTION_ACCEPTED_ID")
    private BigInteger reelerAuctionAcceptedId;


    @Column(name = "NO_OF_CRATES")
    private int noOfCrates;

    @Column(name = "TOTAL_CRATES_CAPACITY_WEIGHT")
    private double totalCratesCapacityWeight;

    @Column(name = "REMARKS")
    private String remarks;

    @Column(name = "LOT_WEIGHT_AFTER_WEIGHMENT")
    private float lotWeightAfterWeighment;

    @Column(name = "LOT_APPROX_WEIGHT_BEFORE_WEIGHMENT")
    private int lotApproxWeightBeforeWeighment;

    @Column(name = "BID_ACCEPTED_BY")
    private String bidAcceptedBy;

    @Column(name = "WEIGHMENT_COMPLETED_BY")
    private String weighmentCompletedBy;

    @Column(name = "LOT_SOLD_OUT_AMOUNT")
    private double lotSoldOutAmount;

    @Column(name = "MARKET_FEE_REELER")
    private double marketFeeReeler;

    @Column(name = "MARKET_FEE_FARMER")
    private double marketFeeFarmer;

    @Column(name = "MARKET_FEE_TRADER")
    private double marketFeeTrader;

    @Column(name = "CUSTOMER_REFERENCE_NUMBER")
    private String customerReferenceNumber;

    @Column(name = "COMMENTS")
    private String comments;


}
