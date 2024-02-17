package com.sericulture.marketandauction.controller;

import com.sericulture.marketandauction.model.api.marketauction.DTROnlineReportRequest;
import com.sericulture.marketandauction.model.api.marketauction.FLexTimeRequest;
import com.sericulture.marketandauction.model.api.marketauction.reporting.FarmerTxnReportRequest;
import com.sericulture.marketandauction.model.api.marketauction.reporting.ReportRequest;
import com.sericulture.marketandauction.service.MarketAuctionReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auction/report")
public class MarketAuctionReportController {

    @Autowired
    private MarketAuctionReportService marketAuctionReportService;

    @PostMapping("/getAllHighestBidsByMarketIdAndOptionalGodownId")
    public ResponseEntity<?> getAllHighestBidsByMarketIdAndOptionalGodownId(@RequestBody com.sericulture.marketandauction.model.api.RequestBody requestBody){
        return marketAuctionReportService.getAllHighestBidsByMarketIdAndOptionalGodownId(requestBody);
    }

    @PostMapping("/getDTROnlineReport")
    public ResponseEntity<?> getDTROnlineReport(@RequestBody DTROnlineReportRequest dtrOnlineReportRequest){
        return marketAuctionReportService.getDTROnlineReport(dtrOnlineReportRequest);
    }

    @PostMapping("/getUnitCounterReport")
    public ResponseEntity<?> getUnitCounterReport(@RequestBody ReportRequest reportRequest){
        return marketAuctionReportService.getUnitCounterReport(reportRequest);
    }

    @PostMapping("/getPendingLotReport")
    public ResponseEntity<?> getPendingLotReport(@RequestBody ReportRequest reportRequest){
        return marketAuctionReportService.getPendingLotReport(reportRequest);
    }

    @PostMapping("/getFarmerTxnReport")
    public ResponseEntity<?> getFarmerTxnReport(@RequestBody FarmerTxnReportRequest reportRequest){
        return marketAuctionReportService.getFarmerTxnReport(reportRequest);
    }
}
