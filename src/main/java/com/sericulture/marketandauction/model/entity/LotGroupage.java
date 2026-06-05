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
    private Float lotWeight;

    @Column(name = "AMOUNT")
    private Long amount;

    @Column(name = "MARKET_FEE")
    private Double marketFee;

    @Column(name = "FARMER_MARKET_FEE")
    private Double farmerMarketFee;

    @Column(name = "REELER_MARKET_FEE")
    private Double reelerMarketFee;

    @Column(name = "SOLD_AMOUNT")
    private Long soldAmount;

    @Column(name = "ALLOTTED_LOT_ID")
    private Long allottedLotId;

    @Column(name = "MARKET_AUCTION_ID")
    private BigInteger marketAuctionId;

    @Column(name = "LOT_ID")
    private BigInteger id;

    @Temporal(TemporalType.DATE)
    @Column(name = "AUCTION_DATE")
    private LocalDate auctionDate;

    @Column(name = "average_yield")
    private Float averageYield;

    @Column(name = "no_of_dfls")
    private Long dflLotNumber;

    @Column(name = "invoice_number")
    private String invoiceNumber;

    @Column(name = "lot_parental_level")
    private String lotParentLevel;

    @Column(name = "remaining_cocoon")
    private Float remainingCocoonWeight;

    @Column(name = "user_master_id")
    private Long userMasterId;

    @Column(name = "external_unit_id")
    private Long externalUnitId;

    @Column(name = "fruits_id")
    private String fruitsId;

    @Column(name = "is_disposed")
    private Integer isDisposed;

    @Column(name = "status")
    private String status;

    @Column(name = "customer_reference_number")
    private String customerReferenceNumber;

    @Column(name = "payment_comments")
    private String paymentComments;

    /**
     * Set by the "Purpose for Rejection" checkbox on the Lot Distribution screen.
     * When true, the lot's remaining cocoon weight is being flagged for rejection
     * based on the market's REJECTION_PERCENTAGE threshold (compared in the UI).
     */
    @Column(name = "purpose_for_rejection", columnDefinition = "TINYINT")
    private Boolean purposeForRejection;

    @Column(name = "qty_nos")
    private Integer qtyNos;

    /**
     * Quantity (Kg) recorded as rejection when the "Purpose for Rejection" checkbox
     * is ticked. Equals (lotWeightAfterWeighment - distributedQuantity) at save time.
     * Null when the checkbox is not ticked.
     *
     * Example: weighment 100, distributed 90, checkbox on → rejection_quantity = 10.
     */
    @Column(name = "rejection_quantity", precision = 10, scale = 2)
    private java.math.BigDecimal rejectionQuantity;

    /**
     * Set by the "Moving to another market" checkbox on the Lot Distribution screen.
     * When true, the leftover cocoon is being shipped to a different market, so the
     * lot is force-completed: status = DISTRIBUTED and remaining_cocoon = 0.
     */
    @Column(name = "moving_to_another_market", columnDefinition = "TINYINT")
    private Boolean movingToAnotherMarket;

    /**
     * Free-text reason captured when "Moving to another market" is ticked.
     */
    @Column(name = "moving_market_reason", length = 500)
    private String movingMarketReason;

    @Column(name = "is_market_paid", columnDefinition = "BIT DEFAULT 0")
    private Integer isMarketPaid;
}
