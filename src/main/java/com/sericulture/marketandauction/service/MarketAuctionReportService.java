package com.sericulture.marketandauction.service;

import com.sericulture.marketandauction.repository.LotRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MarketAuctionReportService {

    @Autowired
    private LotRepository lotRepository;

    @Autowired
    private MarketAuctionPrinterService marketAuctionPrinterService;

    public ResponseEntity<?> getLastBiddingSlipByUser(){

        //int allottedLotId = lotRepository.findByMarketIdAndAuctionDateAndCreatedBy();
        return null;

    }
}
