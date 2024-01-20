package com.sericulture.marketandauction.model.api.marketauction;

import com.sericulture.marketandauction.model.api.ResponseBody;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Builder
public class DTROnlineReportResponse extends ResponseBody {
    private List<DTROnlineReportUnitDetail> dtrOnlineReportUnitDetailList = new ArrayList<>();
    private int totalLots;
    private double totalFarmerAmount;
    private double totalReelerAmount;
    private double totalReelerMarketFee;
    private double totalFarmerMarketFee;
}
