package com.sericulture.marketandauction.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.util.Date;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "TRANSACTION_FILE_GEN")
@Builder
public class TransactionFileGeneration extends BaseEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "TRANSACTION_FILE_GEN_SEQ")
    @SequenceGenerator(name = "TRANSACTION_FILE_GEN_SEQ", sequenceName = "TRANSACTION_FILE_GEN_SEQ", allocationSize = 1)
    @Column(name = "TRANSACTION_FILE_GEN_ID")
    private Integer transactionFileGenId;

    @Column(name = "TRANSACTION_FILE_GEN_QUEUE_ID")
    private Integer transactionFileGenQueueId;
    @Column(name = "OBJECT")
    private String object;

    @Column(name = "OBJECT_TYPE")
    private String objectType;

    @Column(name = "STATUS")
    private String status;
    @Column(name = "FILE_NAME")
    private String fileName;

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
