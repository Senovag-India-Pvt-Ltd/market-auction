package com.sericulture.marketandauction.controller;

import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.api.marketauction.DTROnlineReportRequest;
import com.sericulture.marketandauction.model.api.marketauction.FLexTimeRequest;
import com.sericulture.marketandauction.model.api.marketauction.reporting.*;
import com.sericulture.marketandauction.service.MarketAuctionReportService;
import com.sericulture.marketandauction.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/auction/report")
public class MarketAuctionReportController {

    @Autowired
    private MarketAuctionReportService marketAuctionReportService;

    @Autowired
    private ReportService reportService;

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

    @PostMapping("/getReelerTxnReport")
    public ResponseEntity<?> getReelerTxnReport(@RequestBody ReelerTxnReportRequest reelerTxnReportRequest){
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        rw.setContent(reportService.generateReelerReport(reelerTxnReportRequest.getMarketId(),reelerTxnReportRequest.getReelerNumber(),reelerTxnReportRequest.getReportFromDate(),reelerTxnReportRequest.getReportToDate()));
        return ResponseEntity.ok(rw);
    }

    @PostMapping("/getBiddingReport")
    public ResponseEntity<?> getBiddingReport(@RequestBody LotReportRequest reportRequest){
        return marketAuctionReportService.getBiddingReport(reportRequest);
    }

    @PostMapping("/getReelerBiddingReport")
    public ResponseEntity<?> getReelerBiddingReport(@RequestBody ReelerReportRequest reportRequest){
        return marketAuctionReportService.getReelerBiddingReport(reportRequest);
    }
}
