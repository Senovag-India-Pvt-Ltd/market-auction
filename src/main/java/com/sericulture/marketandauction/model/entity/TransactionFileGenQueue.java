package com.sericulture.marketandauction.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "TRANSACTION_FILE_GEN_QUEUE")
@Builder
public class TransactionFileGenQueue extends BaseEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "TRANSACTION_FILE_GEN_QUEUE_SEQ")
    @SequenceGenerator(name = "TRANSACTION_FILE_GEN_QUEUE_SEQ", sequenceName = "TRANSACTION_FILE_GEN_QUEUE_SEQ", allocationSize = 1)
    @Column(name = "TRANSACTION_FILE_GEN_QUEUE_ID")
    @Setter(AccessLevel.NONE)
    private Long transFileGenQueueId;
    @Column(name = "TRANSACTION_FILE_GEN_ID")
    @Setter(AccessLevel.NONE)
    private String transactionFileGenId;
    @Column(name = "MARKET_ID")
    private int marketId;
    @Column(name = "AUCTION_DATE")
    private LocalDate auctionDate;
    @Column(name = "COMMENT")
    private String comment;
    @Column(name = "STATUS")
    private String status;
    @Column(name = "RETRY_COUNT")
    private Integer retryCount;
    @Column(name = "FILE_NAME")
    private String fileName;

}