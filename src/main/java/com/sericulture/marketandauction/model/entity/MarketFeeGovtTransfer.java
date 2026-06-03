package com.sericulture.marketandauction.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "market_fee_govt_transfer")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class MarketFeeGovtTransfer extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "MARKET_FEE_GOVT_TRANSFER_SEQ")
    @SequenceGenerator(name = "MARKET_FEE_GOVT_TRANSFER_SEQ", sequenceName = "MARKET_FEE_GOVT_TRANSFER_SEQ", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "lot_groupage_id", nullable = false)
    private Long lotGroupageId;

    @Column(name = "allotted_lot_id", nullable = false)
    private Integer allottedLotId;

    @Column(name = "market_id", nullable = false)
    private Integer marketId;

    @Column(name = "fruits_id")
    private String fruitsId;

    @Column(name = "farmer_market_fee", precision = 18, scale = 2)
    private BigDecimal farmerMarketFee;

    @Column(name = "reeler_market_fee", precision = 18, scale = 2)
    private BigDecimal reelerMarketFee;

    @Column(name = "total_market_fee", precision = 18, scale = 2)
    private BigDecimal totalMarketFee;

    @Column(name = "collection_date", nullable = false)
    private LocalDate collectionDate;

    @Column(name = "transfer_status", nullable = false)
    private String transferStatus;

    @Column(name = "transferred_date")
    private LocalDate transferredDate;

    @Column(name = "govt_account_number")
    private String govtAccountNumber;

    @Column(name = "transaction_reference")
    private String transactionReference;
}
