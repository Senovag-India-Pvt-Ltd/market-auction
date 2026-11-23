package com.sericulture.marketandauction.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/** XC-004: one cocoon market transaction of the pre-project (legacy) market system. Table: V003. */
@Entity
@Table(name = "legacy_cocoon_market_txn")
@Getter
@Setter
@NoArgsConstructor
public class LegacyCocoonMarketTxn extends BaseEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "legacy_cocoon_market_txn_id")
    private Long legacyCocoonMarketTxnId;

    @Column(name = "source_system", nullable = false)
    private String sourceSystem;

    @Column(name = "legacy_txn_id", nullable = false)
    private String legacyTxnId;

    @Column(name = "market_master_id", nullable = false)
    private Long marketMasterId;

    @Column(name = "market_name_raw")
    private String marketNameRaw;

    @Column(name = "auction_date", nullable = false)
    private LocalDate auctionDate;

    @Column(name = "lot_number", nullable = false)
    private String lotNumber;

    @Column(name = "farmer_id")
    private Long farmerId;

    @Column(name = "farmer_fruits_id")
    private String farmerFruitsId;

    @Column(name = "farmer_name")
    private String farmerName;

    @Column(name = "farmer_mobile")
    private String farmerMobile;

    @Column(name = "reeler_id")
    private Long reelerId;

    @Column(name = "reeler_licence_no")
    private String reelerLicenceNo;

    @Column(name = "reeler_name")
    private String reelerName;

    @Column(name = "race_master_id")
    private Long raceMasterId;

    @Column(name = "race_name_raw")
    private String raceNameRaw;

    @Column(name = "weight_kg", nullable = false, precision = 12, scale = 3)
    private BigDecimal weightKg;

    @Column(name = "rate_per_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal ratePerKg;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "farmer_market_fee", precision = 12, scale = 2)
    private BigDecimal farmerMarketFee;

    @Column(name = "reeler_market_fee", precision = 12, scale = 2)
    private BigDecimal reelerMarketFee;

    @Column(name = "new_system_lot_id")
    private Long newSystemLotId;

    @Column(name = "row_hash", nullable = false)
    private String rowHash;

    @Column(name = "first_import_id", nullable = false)
    private Long firstImportId;

    @Column(name = "last_import_id", nullable = false)
    private Long lastImportId;
}
