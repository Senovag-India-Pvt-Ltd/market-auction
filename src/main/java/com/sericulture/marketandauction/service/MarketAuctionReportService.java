package com.sericulture.marketandauction.service;

import com.sericulture.marketandauction.helper.MarketAuctionHelper;
import com.sericulture.marketandauction.helper.Util;
import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.api.RequestBody;
import com.sericulture.marketandauction.model.api.marketauction.LotHighestBidResponse;
import com.sericulture.marketandauction.repository.LotRepository;
import com.sericulture.marketandauction.repository.ReelerAuctionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class MarketAuctionReportService {

    @Autowired
    private LotRepository lotRepository;

    @Autowired
    private MarketAuctionPrinterService marketAuctionPrinterService;

    @Autowired
    private ReelerAuctionRepository reelerAuctionRepository;

    @Autowired
    private MarketAuctionHelper marketAuctionHelper;

    public static final String getAllHighestBids = """
            SELECT ALLOTTED_LOT_ID,AMOUNT,R.Name  
            FROM REELER_AUCTION RAA INNER JOIN REELER R ON RAA.REELER_ID = R.REELER_ID 
            INNER JOIN (
            select MIN(REELER_AUCTION_ID) ID, RA.ALLOTTED_LOT_ID as AL from REELER_AUCTION RA,
            ( 
            SELECT MAX(AMOUNT) AMT, ALLOTTED_LOT_ID  from REELER_AUCTION ra
            where AUCTION_DATE = :today and ALLOTTED_LOT_ID in ( :lotList) AND MARKET_ID =:marketId GROUP by ALLOTTED_LOT_ID ) as RAB 
            WHERE RAB.AMT=RA.AMOUNT AND  RA.MARKET_ID =:marketId AND RA.ALLOTTED_LOT_ID = RAB.ALLOTTED_LOT_ID AND AUCTION_DATE = :today
            GROUP by  RA.ALLOTTED_LOT_ID ) RA ON RA.ID= RAA.REELER_AUCTION_ID
            """;

    public static final String ALLTTOTED_LOT_LIST_PER_MARKET_ID = """
            SELECT l.allotted_lot_id  from lot l
            INNER JOIN market_auction ma
            on ma.market_auction_id = l.market_auction_id
            where ma.market_auction_date =:auctionDate and ma.market_id = :marketId """;

    public static final String ALLTTOTED_LOT_LIST_PER_MARKET_ID_AND_GODOWNID = ALLTTOTED_LOT_LIST_PER_MARKET_ID + " and ma.godown_id =:godownId";

    public ResponseEntity<?> getLastBiddingSlipByUser(){

        //int allottedLotId = lotRepository.findByMarketIdAndAuctionDateAndCreatedBy();
        return null;

    }

    public ResponseEntity<?> getAllHighestBidsByMarketIdAndOptionalGodownId(RequestBody requestBody){
        List<LotHighestBidResponse> lotHighestBidResponseList = new ArrayList<>();
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        List<Integer> allottedLotList = null;
        if(requestBody.getGodownId()!=0){
            allottedLotList = reelerAuctionRepository.getAllottedLotListByMarketIdAndGoDownId(Util.getISTLocalDate(),requestBody.getMarketId(),requestBody.getGodownId());
        }else {
            allottedLotList = reelerAuctionRepository.getAllottedLotListByMarketId(Util.getISTLocalDate(),requestBody.getMarketId());
        }
        if(Util.isNullOrEmptyList(allottedLotList)){
            marketAuctionHelper.retrunIfError(rw,"No Lots found for the given market:"+requestBody.getMarketId()+" and godownId: "+requestBody.getGodownId());
        }
        List<Integer> highestBidsFoundList = new ArrayList<>();
        Object[][] highestBids = reelerAuctionRepository.getHighestBidAmountForAllLotList(Util.getISTLocalDate(),requestBody.getMarketId(),allottedLotList);

        if(highestBids!=null && highestBids.length>0){
            for(Object[] bids: highestBids){
                highestBidsFoundList.add(Util.objectToInteger(bids[0]));
                LotHighestBidResponse lotHighestBidResponse = new LotHighestBidResponse(
                        Util.objectToInteger(bids[0]),Util.objectToInteger(bids[1]),Util.objectToString(bids[2]));
                lotHighestBidResponseList.add(lotHighestBidResponse);
            }
        }
        allottedLotList.removeAll(highestBidsFoundList);
        if(!allottedLotList.isEmpty()){
            for (Integer lot: allottedLotList){
                lotHighestBidResponseList.add(new LotHighestBidResponse(lot,0,""));
            }
        }
        rw.setContent(lotHighestBidResponseList);
        return ResponseEntity.ok(rw);
    }
}
