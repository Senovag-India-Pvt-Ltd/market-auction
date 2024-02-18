package com.sericulture.marketandauction.model.api;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class ReelerTransactionReportWrapper {
    Double openingBalance;
    List<ReelerTransactionReport> reelerTransactionReports;
    Double totalDeposits;
    Double totalPurchase;

}
