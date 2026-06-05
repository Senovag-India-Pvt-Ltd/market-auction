package com.sericulture.marketandauction.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "reeler_bank_transfer")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ReelerBankTransfer extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "REELER_BANK_TRANSFER_SEQ")
    @SequenceGenerator(name = "REELER_BANK_TRANSFER_SEQ", sequenceName = "REELER_BANK_TRANSFER_SEQ", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "buyer_type", nullable = false)
    private String buyerType;

    @Column(name = "reeler_id")
    private Integer reelerId;

    @Column(name = "external_unit_id")
    private Integer externalUnitId;

    @Column(name = "virtual_account_number")
    private String virtualAccountNumber;

    @Column(name = "bank_account_number")
    private String bankAccountNumber;

    @Column(name = "ifsc_code")
    private String ifscCode;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "branch_name")
    private String branchName;

    @Column(name = "transfer_amount", nullable = false)
    private Double transferAmount;

    @Column(name = "market_id", nullable = false)
    private Integer marketId;

    @Column(name = "auction_date", nullable = false)
    private LocalDate auctionDate;

    @Column(name = "transfer_status", nullable = false)
    private String transferStatus;

    @Column(name = "transferred_date")
    private LocalDate transferredDate;

    @Column(name = "customer_reference_number", length = 50)
    private String customerReferenceNumber;

    @Column(name = "comment", length = 500)
    private String comment;
}