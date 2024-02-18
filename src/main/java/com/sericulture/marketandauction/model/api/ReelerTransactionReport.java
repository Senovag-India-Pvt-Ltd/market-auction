package com.sericulture.marketandauction.model.api;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class ReelerTransactionReport {
    String operationDescription;

    LocalDate transactionDate;

    Double depositAmount;

    Double paymentAmount;

    Double balance;

    String transactionType;
}
