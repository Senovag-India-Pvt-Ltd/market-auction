package com.sericulture.marketandauction.model.api.marketauction.reporting;

import com.sericulture.marketandauction.model.api.ResponseBody;
import com.sericulture.marketandauction.model.api.marketauction.DTROnlineReportUnitDetail;
import com.sericulture.marketandauction.model.api.marketauction.reporting.MarketReport.MarketReportRaceWise;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class Form13Response extends ResponseBody {
    private String averageRate;
    private String marketNameKannada;
    List<BreakdownLotStatus> lotsFrom0to351;
    List<BreakdownLotStatus> lotsFrom201to300;
    List<BreakdownLotStatus> averageLotStatus;
    List<BreakDownLotStatusTotalResponse> lotsFrom0to351Total;
    List<GroupLotStatus> totalLotStatus;
    List<GroupLotStatus> stateWiseLotStatus;
    List<GroupLotStatus> genderWiseLotStatus;
    List<GroupLotStatus> raceWiseLotStatus;
    List<Form13TotalResponse> totalStatus;
}