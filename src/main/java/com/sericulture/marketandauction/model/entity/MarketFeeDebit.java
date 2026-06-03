package com.sericulture.marketandauction.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "market_fee_debit")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class MarketFeeDebit extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "MARKET_FEE_DEBIT_SEQ")
    @SequenceGenerator(name = "MARKET_FEE_DEBIT_SEQ", sequenceName = "MARKET_FEE_DEBIT_SEQ", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "fruits_id")
    private String fruitsId;

    @Column(name = "lot_id")
    private Long lotId;

    @Column(name = "farmer_id")
    private Long farmerId;

    @Column(name = "debit_amount", precision = 18, scale = 2)
    private BigDecimal debitAmount;

    @Column(name = "previous_balance", precision = 18, scale = 2)
    private BigDecimal previousBalance;

    @Column(name = "current_balance", precision = 18, scale = 2)
    private BigDecimal currentBalance;

    // created_by  → inherited from BaseEntity (auto set from logged-in user)
    // created_date → inherited from BaseEntity (auto set timestamp)
    // modified_by  → inherited from BaseEntity
    // modified_date → inherited from BaseEntity
    // active       → inherited from BaseEntity (default true)
}