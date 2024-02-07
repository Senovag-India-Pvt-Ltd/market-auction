package com.sericulture.marketandauction.service;

import com.sericulture.authentication.model.JwtPayloadData;
import com.sericulture.marketandauction.helper.MarketAuctionHelper;
import com.sericulture.marketandauction.helper.Util;
import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.api.marketauction.MarketAuctionForPrintRequest;
import com.sericulture.marketandauction.model.api.marketauction.MarketAuctionForPrintResponse;
import com.sericulture.marketandauction.model.enums.LotStatus;
import com.sericulture.marketandauction.repository.BinRepository;
import com.sericulture.marketandauction.repository.LotRepository;
import com.sericulture.marketandauction.repository.LotWeightDetailRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class MarketAuctionPrinterService {

    @Autowired
    LotRepository lotRepository;

    @Autowired
    LotWeightDetailRepository lotWeightDetailRepository;

    @Autowired
    BinRepository binRepository;

    @Autowired
    MarketAuctionHelper marketAuctionHelper;


    public ResponseEntity<?> getPrintableDataForLot(MarketAuctionForPrintRequest marketAuctionForPrintRequest) {

        JwtPayloadData token = marketAuctionHelper.getAuthToken(marketAuctionForPrintRequest);

        ResponseWrapper rw = ResponseWrapper.createWrapper(MarketAuctionForPrintResponse.class);
        MarketAuctionForPrintResponse marketAuctionForPrintResponse = null;
        Object[][] lotDetails = lotRepository.getAcceptedLotDetails(marketAuctionForPrintRequest.getAuctionDate(), marketAuctionForPrintRequest.getMarketId(), marketAuctionForPrintRequest.getAllottedLotId());
        float reelerCurrentBalance = 0;
        boolean foundAcceptedLot = false;
        if (lotDetails != null && lotDetails.length > 0) {
            foundAcceptedLot = true;
            reelerCurrentBalance = Util.objectToFloat(lotDetails[0][32]);

        } else {
            lotDetails = lotRepository.getNewlyCreatedLotDetails(marketAuctionForPrintRequest.getAuctionDate(), marketAuctionForPrintRequest.getMarketId(), marketAuctionForPrintRequest.getAllottedLotId());
        }
        if (foundAcceptedLot || (lotDetails != null && lotDetails.length > 0)) {
            for (Object[] response : lotDetails) {
                if(LotStatus.CANCELLED.getLabel().equals(Util.objectToString(response[18]))){
                    return marketAuctionHelper.retrunIfError(rw,"Lot is cancelled and hence cannot be printed");
                }
                BigInteger lotId = BigInteger.valueOf(Long.parseLong(String.valueOf(response[16])));
                marketAuctionForPrintResponse = prepareResponseForLotBaseResponse(token, response, lotId);

                if (foundAcceptedLot) {
                    marketAuctionForPrintResponse.setAuctionDateWithTime((Date)(response[23]));
                    marketAuctionForPrintResponse.setReelerLicense(Util.objectToString(response[24]));
                    marketAuctionForPrintResponse.setReelerName(Util.objectToString(response[25]));
                    marketAuctionForPrintResponse.setReelerAddress(Util.objectToString(response[26]));
                    marketAuctionForPrintResponse.setLotWeight(Util.objectToFloat(response[27]));
                    marketAuctionForPrintResponse.setReelerMarketFee(Util.objectToFloat(response[28]));
                    marketAuctionForPrintResponse.setFarmerMarketFee(Util.objectToFloat(response[29]));
                    marketAuctionForPrintResponse.setLotSoldOutAmount(Util.objectToFloat(response[30]));
                    marketAuctionForPrintResponse.setBidAmount(Util.objectToFloat(response[31]));
                    marketAuctionForPrintResponse.setReelerCurrentBalance(reelerCurrentBalance);
                    marketAuctionForPrintResponse.setReelerNameKannada(Util.objectToString(response[33]));
                    marketAuctionForPrintResponse.setReelerMobileNumber(Util.objectToString(response[34]));
                    marketAuctionForPrintResponse.setReelerNumber(Util.objectToString(response[35]));
                    marketAuctionForPrintResponse.setFarmerAmount(marketAuctionForPrintResponse.getLotSoldOutAmount() - marketAuctionForPrintResponse.getFarmerMarketFee());
                    marketAuctionForPrintResponse.setReelerAmount(marketAuctionForPrintResponse.getLotSoldOutAmount() + marketAuctionForPrintResponse.getReelerMarketFee());


                    List<Float> lotWeightList = lotWeightDetailRepository.findAllByLotId(lotId);
                    if(!Util.isNullOrEmptyList(lotWeightList))
                    marketAuctionForPrintResponse.setLotWeightDetail(lotWeightList);

                }
            }
        } else {
            return marketAuctionHelper.retrunIfError(rw, "No Lot found for given request ");
        }
        rw.setContent(marketAuctionForPrintResponse);
        return ResponseEntity.ok(rw);

    }

    public MarketAuctionForPrintResponse prepareResponseForLotBaseResponse(JwtPayloadData token, Object[] response, BigInteger lotId) {
        MarketAuctionForPrintResponse marketAuctionForPrintResponse;
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
                .marketName(Util.objectToString(response[12]))
                .race(Util.objectToString(response[13]))
                .source(Util.objectToString(response[14]))
                .tareWeight(Util.objectToFloat(response[15]))
                .serialNumber(Util.objectToString(response[17])+ lotId)
                .marketNameKannada(Util.objectToString(response[19]))
                .farmerNameKannada(Util.objectToString(response[20]))

                .farmerMobileNumber(Util.objectToString(21))
                .marketAuctionId((BigDecimal) response[22])
                .auctionDateWithTime((Date)(response[23]))
                .build();
        marketAuctionForPrintResponse.setSmallBinList(binRepository.findAllByMarketAuctionIdAndType(marketAuctionForPrintResponse.getMarketAuctionId().toBigInteger(),"small"));
        marketAuctionForPrintResponse.setBigBinList(binRepository.findAllByMarketAuctionIdAndType(marketAuctionForPrintResponse.getMarketAuctionId().toBigInteger(),"big"));
        marketAuctionForPrintResponse.setLoginName(token.getUsername());
        return marketAuctionForPrintResponse;
    }
}
