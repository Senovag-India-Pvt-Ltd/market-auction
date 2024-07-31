package com.sericulture.marketandauction.model.api.marketauction;

import com.sericulture.marketandauction.model.api.ResponseBody;
import com.sericulture.marketandauction.model.api.marketauction.reporting.DashboardReportInfo;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class MonthlyDistrictReport extends ResponseBody {
    private String startDate;
    private String endDate;
    private String marketName;
    List<MonthlyDistrictReportInfo> monthlyDistrictReportInfoList;
    List<SumOfMonthlyDistrictReportInfo> sumOfMonthlyDistrictReportInfoList;
}