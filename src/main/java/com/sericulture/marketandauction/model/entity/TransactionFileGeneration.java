package com.sericulture.marketandauction.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.io.Serializable;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "TRANSACTION_FILE_GEN")
@Builder
public class TransactionFileGeneration extends BaseEntity implements Serializable {
    @Id
    @Column(name = "TRANSACTION_FILE_GEN_ID")
    private String transactionFileGenId;

    @Column(name = "OBJECT")
    private String object;

    @Column(name = "OBJECT_TYPE")
    private String objectType;

    @Column(name = "STATUS")
    private String status;
}
