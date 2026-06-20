package com.sericulture.marketandauction.model.api;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class ReelerTransactionReportWrapper {
    List<String> columnHeaders;
    Double openingBalance;
    List<ReelerTransactionReport> reelerTransactionReports;
    Double totalDeposits;
    Double totalLotWeight;
    Double totalPaymentAmount;
    Double totalMarketFee;
    Double totalPurchase;
    Double totalBankTransfers;
    Double closingBalance;
    String name;
    String address;
    String fruitsId;
    String reportType; // "REELER" or "EXTERNal unit"

}
