package com.sericulture.marketandauction.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/** XC-004: one import run of a legacy cocoon market export. Table: V003. */
@Entity
@Table(name = "legacy_cocoon_market_import")
@Getter
@Setter
@NoArgsConstructor
public class LegacyCocoonMarketImport extends BaseEntity implements Serializable {
    public static final String COMPLETED = "COMPLETED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "legacy_cocoon_market_import_id")
    private Long legacyCocoonMarketImportId;

    @Column(name = "source_system", nullable = false)
    private String sourceSystem;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "total_rows", nullable = false)
    private Integer totalRows = 0;

    @Column(name = "inserted_rows", nullable = false)
    private Integer insertedRows = 0;

    @Column(name = "updated_rows", nullable = false)
    private Integer updatedRows = 0;

    @Column(name = "unchanged_rows", nullable = false)
    private Integer unchangedRows = 0;

    @Column(name = "rejected_rows", nullable = false)
    private Integer rejectedRows = 0;

    @Column(name = "already_in_new_system_rows", nullable = false)
    private Integer alreadyInNewSystemRows = 0;

    @Column(name = "message")
    private String message;
}
