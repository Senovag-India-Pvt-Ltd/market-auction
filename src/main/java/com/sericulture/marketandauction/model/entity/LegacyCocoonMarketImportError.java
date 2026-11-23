package com.sericulture.marketandauction.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/** XC-004: a rejected row of a legacy import run. Table: V003. */
@Entity
@Table(name = "legacy_cocoon_market_import_error")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LegacyCocoonMarketImportError implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "legacy_cocoon_market_import_error_id")
    private Long legacyCocoonMarketImportErrorId;

    @Column(name = "legacy_cocoon_market_import_id", nullable = false)
    private Long legacyCocoonMarketImportId;

    @Column(name = "row_number_in_file", nullable = false)
    private Integer rowNumberInFile;

    @Column(name = "legacy_txn_id")
    private String legacyTxnId;

    @Column(name = "message", nullable = false)
    private String message;
}
