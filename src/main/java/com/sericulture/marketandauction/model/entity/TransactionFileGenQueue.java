package com.sericulture.marketandauction.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "TRANSACTION_FILE_GEN_QUEUE")
@Builder
public class TransactionFileGenQueue  implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "TRANSACTION_FILE_GEN_QUEUE_SEQ")
    @SequenceGenerator(name = "TRANSACTION_FILE_GEN_QUEUE_SEQ", sequenceName = "TRANSACTION_FILE_GEN_QUEUE_SEQ", allocationSize = 1)
    @Column(name = "TRANSACTION_FILE_GEN_QUEUE_ID")
    @Setter(AccessLevel.NONE)
    private Long transFileGenQueueId;
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

    @Column(name = "CREATED_BY")
    private String createdBy = "";

    @Column(name = "MODIFIED_BY")
    private String modifiedBy ="";
    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CREATED_DATE", updatable = false)
    private Date createdDate;
    @UpdateTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "MODIFIED_DATE", updatable = true)
    private Date modifiedDate;

    @Getter
    @Setter
    @Column(name = "ACTIVE", columnDefinition = "TINYINT")
    private Boolean active;

    @PrePersist
    public void prePersist() {
        if(active == null)
            active = true;
    }

    @PreUpdate
    public void preUpdate() {
        if(active == null)
            active = true;
    }

}