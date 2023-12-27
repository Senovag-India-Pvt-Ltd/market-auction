package com.sericulture.marketandauction.controller;

import com.sericulture.marketandauction.model.api.marketauction.FarmerPaymentCSVRequest;
import com.sericulture.marketandauction.model.enums.LotStatus;
import com.sericulture.marketandauction.service.MarketAuctionFileDowndloadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/v1/auction/filedownloader")
public class MarketAuctionFileDowndloadController {

    @Autowired
    MarketAuctionFileDowndloadService marketAuctionFileDowndloadService;


    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> getFile(@RequestParam int marketId, @RequestParam String fileName) {

        InputStreamResource file = new InputStreamResource(marketAuctionFileDowndloadService.generateCSV(marketId,fileName));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName+ LocalDate.now()+".csv")
                .contentType(MediaType.parseMediaType("application/csv"))
                .body(file);
    }
}
