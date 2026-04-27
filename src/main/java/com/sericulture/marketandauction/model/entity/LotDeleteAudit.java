package com.sericulture.marketandauction.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "LOT_DELETE_AUDIT")
@Getter
@Setter
public class LotDeleteAudit extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "lot_delete_audit_seq_gen")
    @SequenceGenerator(
            name = "lot_delete_audit_seq_gen",
            sequenceName = "lot_delete_audit_seq",
            allocationSize = 1
    )
    private Long auditId;

    private BigInteger lotId;

    private Integer allottedLotId;

    private LocalDate auctionDate;

    private BigInteger marketAuctionId;
}
