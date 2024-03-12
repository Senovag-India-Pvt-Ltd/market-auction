package com.sericulture.marketandauction.model.api.marketauction.reporting;

import com.sericulture.marketandauction.model.api.ResponseBody;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class ReelerPendingReportResponse extends ResponseBody {
    private String grandTotalAmount;
    List<ReelerPendingInfo> reelerPendingInfoList;
}