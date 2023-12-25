package com.sericulture.marketandauction.service;

import com.sericulture.marketandauction.helper.MarketAuctionHelper;
import com.sericulture.marketandauction.helper.Util;
import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.api.marketauction.MarketAuctionForPrintRequest;
import com.sericulture.marketandauction.model.api.marketauction.MarketAuctionForPrintResponse;
import com.sericulture.marketandauction.repository.LotRepository;
import com.sericulture.marketandauction.repository.LotWeightDetailRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigInteger;

@Service
@Slf4j
public class MarketAuctionPrinterService {

    @Autowired
    LotRepository lotRepository;

    @Autowired
    LotWeightDetailRepository lotWeightDetailRepository;

    @Autowired
    MarketAuctionHelper marketAuctionHelper;

    public ResponseEntity<?> getPrintableDataForLot(MarketAuctionForPrintRequest marketAuctionForPrintRequest) {

        ResponseWrapper rw = ResponseWrapper.createWrapper(MarketAuctionForPrintResponse.class);
        MarketAuctionForPrintResponse marketAuctionForPrintResponse = null;
        Object[][] lotDetails = lotRepository.getAcceptedLotDetails(marketAuctionForPrintRequest.getAuctionDate(), marketAuctionForPrintRequest.getMarketId(), marketAuctionForPrintRequest.getAllottedLotId());

        boolean foundAcceptedLot = false;
        if (lotDetails != null && lotDetails.length > 0) {
            foundAcceptedLot = true;
        } else {
            lotDetails = lotRepository.getNewlyCreatedLotDetails(marketAuctionForPrintRequest.getAuctionDate(), marketAuctionForPrintRequest.getMarketId(), marketAuctionForPrintRequest.getAllottedLotId());
        }
        if (foundAcceptedLot || (lotDetails != null && lotDetails.length > 0)) {
            for (Object[] response : lotDetails) {
                marketAuctionForPrintResponse = MarketAuctionForPrintResponse.builder().
                        farmerNumber(Util.objectToString(response[0]))
                        .farmerFirstName(Util.objectToString(response[1]))
                        .farmerMiddleName(Util.objectToString(response[2]))
                        .farmerLastName(Util.objectToString(response[3]))
                        .farmerAddress(Util.objectToString(response[4]))
                        .farmerTaluk(Util.objectToString(response[5]))
                        .farmerVillage(Util.objectToString(response[6]))
                        .ifscCode(Util.objectToString(response[7]))
                        .accountNumber(Util.objectToString(response[8]))
                        .allottedLotId(Integer.parseInt(String.valueOf(response[9])))
                        .auctionDate(Util.objectToString(response[10]))
                        .farmerEstimatedWeight(Integer.parseInt(String.valueOf(response[11])))
                        .build();
                if (foundAcceptedLot) {
                    marketAuctionForPrintResponse.setReelerLicense(Util.objectToString(response[12]));
                    marketAuctionForPrintResponse.setReelerName(Util.objectToString(response[13]));
                    marketAuctionForPrintResponse.setReelerAddress(Util.objectToString(response[14]));
                    marketAuctionForPrintResponse.setLotWeight(Util.objectToFloat(response[15]));
                    marketAuctionForPrintResponse.setReelerMarketFee(Util.objectToFloat(response[16]));
                    marketAuctionForPrintResponse.setFarmerMarketFee(Util.objectToFloat(response[17]));
                    marketAuctionForPrintResponse.setLotSoldOutAmount(Util.objectToFloat(response[18]));
                    marketAuctionForPrintResponse.setBidAmount(Util.objectToFloat(response[19]));
                    marketAuctionForPrintResponse.setReelerCurrentBalance(Util.objectToFloat(response[20]));

                    BigInteger lotId = BigInteger.valueOf(Long.parseLong(String.valueOf(response[21])));

                    marketAuctionForPrintResponse.setLotWeightDetail(lotWeightDetailRepository.findAllByLotId(lotId));
                }
            }
        } else {
            return marketAuctionHelper.retrunIfError(rw, "No Lot found for given request ");
        }
        rw.setContent(marketAuctionForPrintResponse);
        return ResponseEntity.ok(rw);

    }


}
