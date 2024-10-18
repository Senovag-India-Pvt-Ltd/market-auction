package com.sericulture.marketandauction.controller;

import com.sericulture.marketandauction.model.api.marketauction.MarketAuctionForPrintRequest;
import com.sericulture.marketandauction.service.MarketAuctionPrinterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auction/print")
public class MarketAuctionPrinterController {

    @Autowired
    MarketAuctionPrinterService marketAuctionPrinterService;

    @PostMapping("/getPrintableDataForLot")
    public ResponseEntity<?> getPrintableDataForLot(@RequestBody MarketAuctionForPrintRequest marketAuctionForPrintRequest) {
        return marketAuctionPrinterService.getPrintableDataForLot(marketAuctionForPrintRequest);
    }

    @PostMapping("/getPrintableDataForLotForSilk")
    public ResponseEntity<?> getPrintableDataForLotForSilk(@RequestBody MarketAuctionForPrintRequest marketAuctionForPrintRequest) {
        return marketAuctionPrinterService.getPrintableDataForLotForSilk(marketAuctionForPrintRequest);
    }
}
