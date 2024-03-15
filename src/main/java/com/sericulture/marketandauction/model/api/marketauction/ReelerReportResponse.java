package com.sericulture.marketandauction.model.api.marketauction;

import com.sericulture.marketandauction.model.api.ResponseBody;
import com.sericulture.marketandauction.model.api.marketauction.reporting.ReelerPendingInfo;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class ReelerReportResponse extends ResponseBody {
    private float reelerCurrentBalance;
    private float totalAmountDeposited;
    private float approximatePurchase;
    List<ReelerReport> reelerReportList;
}