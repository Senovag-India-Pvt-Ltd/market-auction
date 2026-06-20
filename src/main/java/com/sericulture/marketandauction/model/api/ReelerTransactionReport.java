package com.sericulture.marketandauction.model.api;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@JsonPropertyOrder({
        "transactionDate",
        "operationDescription",
        "transactionType",
        "depositAmount",
        "lotWeight",
        "ratePerKg",
        "paymentAmount",
        "marketFee",
        "total",
        "refundedAmount",
        "balance",
        "qtyNos"
})
public class ReelerTransactionReport {
    String operationDescription;

    LocalDate transactionDate;

    Double depositAmount;

    Double paymentAmount;

    String transactionType;

    Double lotWeight;

    Double ratePerKg;

    Double marketFee;

    Double total;

    Integer qtyNos;

    Double refundedAmount;

    Double balance;
}
