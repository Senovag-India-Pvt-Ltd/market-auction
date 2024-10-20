package com.sericulture.marketandauction.controller;

import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.api.marketauction.DTROnlineReportRequest;
import com.sericulture.marketandauction.model.api.marketauction.FLexTimeRequest;
import com.sericulture.marketandauction.model.api.marketauction.MonthlyDistrictRequest;
import com.sericulture.marketandauction.model.api.marketauction.ReelerReportForAppRequest;
import com.sericulture.marketandauction.model.api.marketauction.reporting.*;
import com.sericulture.marketandauction.model.api.marketauction.reporting.AudioVisual.AudioVisualReportRequest;
import com.sericulture.marketandauction.model.api.marketauction.reporting.MonthlyReport.MonthlyReportRequest;
import com.sericulture.marketandauction.service.MarketAuctionReportService;
import com.sericulture.marketandauction.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
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

    @PostMapping("/getLotAndAmount")
    public ResponseEntity<?> getDisplayLotNos(@RequestBody com.sericulture.marketandauction.model.api.RequestBody requestBody ){
        return marketAuctionReportService.getDisplayLotNos(requestBody);
    }

    @PostMapping("/getDTROnlineReport")
    public ResponseEntity<?> getDTROnlineReport(@RequestBody DTROnlineReportRequest dtrOnlineReportRequest){
        return marketAuctionReportService.getDTROnlineReport(dtrOnlineReportRequest);
    }
    @PostMapping("/getDTROnlineReportForSilkType")
    public ResponseEntity<?> getDTROnlineReportForSilkType(@RequestBody DTROnlineReportRequest dtrOnlineReportRequest){
        return marketAuctionReportService.getDTROnlineReportForSilkType(dtrOnlineReportRequest);
    }

    @PostMapping("/getBlankDTROnlineReport")
    public ResponseEntity<?> getBlankDTROnlineReport(@RequestBody DTROnlineReportRequest dtrOnlineReportRequest){
        return marketAuctionReportService.getBlankDTROnlineReport(dtrOnlineReportRequest);
    }

    @PostMapping("/getBlankDTROnlineReportForSilkType")
    public ResponseEntity<?> getBlankDTROnlineReportForSilkType(@RequestBody DTROnlineReportRequest dtrOnlineReportRequest){
        return marketAuctionReportService.getBlankDTROnlineReportForSilkType(dtrOnlineReportRequest);
    }

    @PostMapping("/getUnitCounterReport")
    public ResponseEntity<?> getUnitCounterReport(@RequestBody ReportRequest reportRequest){
        return marketAuctionReportService.getUnitCounterReport(reportRequest);
    }

    @PostMapping("/getUnitCounterReportSilkType")
    public ResponseEntity<?> getUnitCounterReportSilkType(@RequestBody ReportRequest reportRequest){
        return marketAuctionReportService.getUnitCounterReportSilkType(reportRequest);
    }

    @PostMapping("/getReelerMFReport")
    public ResponseEntity<?> getReelerMFReport(@RequestBody ReportRequest reportRequest){
        return marketAuctionReportService.getReelerMFReport(reportRequest);
    }

    @PostMapping("/getPendingLotReport")
    public ResponseEntity<?> getPendingLotReport(@RequestBody ReportRequest reportRequest){
        return marketAuctionReportService.getPendingLotReport(reportRequest);
    }

    @PostMapping("/getFarmerTxnReport")
    public ResponseEntity<?> getFarmerTxnReport(@RequestBody FarmerTxnReportRequest reportRequest){
        return marketAuctionReportService.getFarmerTxnReport(reportRequest);
    }

    @PostMapping("/getReelerPendingReport")
    public ResponseEntity<?> getReelerPendingReport(@RequestBody com.sericulture.marketandauction.model.api.RequestBody requestBody){
        return marketAuctionReportService.getReelerPendingReport(requestBody);
    }

    @PostMapping("/getReelerReportForApp")
    public ResponseEntity<?> getReelerReportForApp(@RequestBody ReelerReportForAppRequest requestBody){
        return marketAuctionReportService.getReelerReportForMobileApp(requestBody);
    }

    @PostMapping("/getForm13Report")
    public ResponseEntity<?> getForm13Report(@RequestBody Form13Request requestBody){
        return marketAuctionReportService.getForm13Report(requestBody);
    }
    @PostMapping("/getForm13ReportSilk")
    public ResponseEntity<?> getForm13ReportSilk(@RequestBody Form13Request requestBody){
        return marketAuctionReportService.getForm13ReportSilk(requestBody);
    }


    @PostMapping("/getForm13ReportByDist")
    public ResponseEntity<?> getForm13ReportByDist(@RequestBody Form13Request requestBody){
        return marketAuctionReportService.getForm13ReportByDistrict(requestBody);
    }
    @PostMapping("/getForm13ReportByDistrictSilkType")
    public ResponseEntity<?> getForm13ReportByDistrictSilkType(@RequestBody Form13Request requestBody){
        return marketAuctionReportService.getForm13ReportByDistrictSilkType(requestBody);
    }

    @PostMapping("/averageReportForYearsReport")
    public ResponseEntity<?> getAverageReportForYearsReport(@RequestBody AverageReportRequest requestBody){
        return marketAuctionReportService.getAverageReportForYearsReport(requestBody);
    }

    @PostMapping("/averageCocoonReport")
    public ResponseEntity<?> getAverageCocoonReport(@RequestBody AverageReportRequest requestBody){
        return marketAuctionReportService.getAverageCocoonReport(requestBody);
    }

    @PostMapping("/dtrReport")
    public ResponseEntity<?> getDTRReport(@RequestBody Form13Request requestBody){
        return marketAuctionReportService.getDTRReport(requestBody);
    }
    @PostMapping("/audioVisualReport")
    public ResponseEntity<?> getAudioVisualReport(@RequestBody AudioVisualReportRequest requestBody){
        return marketAuctionReportService.getAudioVisualReport(requestBody);
    }

    @PostMapping("/monthlyReport")
    public ResponseEntity<?> getMonthlyReport(@RequestBody MonthlyReportRequest requestBody){
        return marketAuctionReportService.getMonthlyReport(requestBody);
    }

    @PostMapping("/marketReport")
    public ResponseEntity<?> getMarketReport(@RequestBody MonthlyReportRequest requestBody){
        return marketAuctionReportService.getMarketWiseReport(requestBody);
    }

    @PostMapping("/districtWiseReport")
    public ResponseEntity<?> getDistrictWiseReport(@RequestBody MonthlyReportRequest requestBody){
        return marketAuctionReportService.getDistrictWiseReport(requestBody);
    }

    @PostMapping("/27bReport")
    public ResponseEntity<?> get27bReport(@RequestBody MonthlyReportRequest requestBody){
        return marketAuctionReportService.get27bReport(requestBody);
    }

    @PostMapping("/getDashboardReport")
    public ResponseEntity<?> getDashboardReport(@RequestBody DashboardReportRequest reportRequest){
        return marketAuctionReportService.getDashboardReport(reportRequest);
    }
    @PostMapping("/getDashboardReportSilkType")
    public ResponseEntity<?> getDashboardReportSilkType(@RequestBody DashboardReportRequest reportRequest){
        return marketAuctionReportService.getDashboardReportSilkType(reportRequest);
    }


    @PostMapping("/getMonthlyDistrictReport")
    public ResponseEntity<?> getMonthlyDistrictReport(@RequestBody MonthlyDistrictRequest reportRequest){
        return marketAuctionReportService.getMonthlyDistrictReport(reportRequest);
    }

    @PostMapping("/getMonthlyDistrictReportSilkType")
    public ResponseEntity<?> getMonthlyDistrictReportSilkType(@RequestBody MonthlyDistrictRequest reportRequest){
        return marketAuctionReportService.getMonthlyDistrictReportSilkType(reportRequest);
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
