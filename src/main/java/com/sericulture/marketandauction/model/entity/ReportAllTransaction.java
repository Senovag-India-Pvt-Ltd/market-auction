package com.sericulture.marketandauction.model.entity;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class ReportAllTransaction {
    String transactionType;
    LocalDateTime createdDate;
    LocalDate dateOn;
    Double amount;
    Long lot;
    String farmerName;
}
