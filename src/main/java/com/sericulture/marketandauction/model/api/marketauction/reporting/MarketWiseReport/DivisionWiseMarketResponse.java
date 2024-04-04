package com.sericulture.marketandauction.model.api.marketauction.reporting.MarketWiseReport;

import com.sericulture.marketandauction.model.api.marketauction.reporting.MarketReport.MarketReportRaceWise;
import com.sericulture.marketandauction.model.api.marketauction.reporting.MarketReport.MarketReports;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class DivisionWiseMarketResponse {
    private DivisionWiseReport divisionWiseReport;
    private List<MarketReportRaceWise> karnatakaData;
    private List<MarketReportRaceWise> andraData;
    private List<MarketReportRaceWise> tamilNaduData;
    private List<MarketReportRaceWise> maharashtraData;
    private List<MarketReportRaceWise> otherStateData;
    private List<MarketReportRaceWise> otherStateExcKarData;
    private List<MarketReportRaceWise> overAllStateTotal;
}