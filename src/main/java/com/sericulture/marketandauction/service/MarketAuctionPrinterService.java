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
            reelerCurrentBalance = Util.objectToFloat(lotDetails[0][33]);

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
                    marketAuctionForPrintResponse.setAuctionDateWithTime((Date)(response[24]));
                    marketAuctionForPrintResponse.setReelerLicense(Util.objectToString(response[25]));
                    marketAuctionForPrintResponse.setReelerName(Util.objectToString(response[26]));
                    marketAuctionForPrintResponse.setReelerAddress(Util.objectToString(response[27]));
                    marketAuctionForPrintResponse.setLotWeight(Util.objectToFloat(response[28]));
                    marketAuctionForPrintResponse.setReelerMarketFee(Util.objectToFloat(response[29]));
                    marketAuctionForPrintResponse.setFarmerMarketFee(Util.objectToFloat(response[30]));
                    marketAuctionForPrintResponse.setLotSoldOutAmount(Util.objectToFloat(response[31]));
                    marketAuctionForPrintResponse.setBidAmount(Util.objectToFloat(response[32]));
                    marketAuctionForPrintResponse.setReelerCurrentBalance(reelerCurrentBalance);
                    marketAuctionForPrintResponse.setReelerNameKannada(Util.objectToString(response[34]));
                    marketAuctionForPrintResponse.setReelerMobileNumber(Util.objectToString(response[35]));
                    marketAuctionForPrintResponse.setFruitsId(Util.objectToString(response[38]));
                    marketAuctionForPrintResponse.setReelerNumber(Util.objectToString(response[36]));
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

                .farmerMobileNumber(Util.objectToString(response[21]))
                .fruitsId(Util.objectToString(response[25]))
                .marketAuctionId((BigDecimal) response[22])
                .fatherNameKan(Util.objectToString(response[23]))
                .auctionDateWithTime((Date)(response[24]))
                .build();
        marketAuctionForPrintResponse.setSmallBinList(binRepository.findAllByMarketAuctionIdAndType(marketAuctionForPrintResponse.getMarketAuctionId().toBigInteger(),"small"));
        marketAuctionForPrintResponse.setBigBinList(binRepository.findAllByMarketAuctionIdAndType(marketAuctionForPrintResponse.getMarketAuctionId().toBigInteger(),"big"));
        marketAuctionForPrintResponse.setLoginName(token.getUsername());
        return marketAuctionForPrintResponse;
    }





    public ResponseEntity<?> getPrintableDataForLotForSeedCocoon(MarketAuctionForPrintRequest marketAuctionForPrintRequest) {

        JwtPayloadData token = marketAuctionHelper.getAuthToken(marketAuctionForPrintRequest);

        ResponseWrapper rw = ResponseWrapper.createWrapper(MarketAuctionForPrintResponse.class);
        MarketAuctionForPrintResponse marketAuctionForPrintResponse = null;

        // Fetch lot details using the provided request parameters
        Object[][] lotDetails = lotRepository.getNewlyCreatedLotDetailsSeedCocoons(
                marketAuctionForPrintRequest.getAuctionDate(),
                marketAuctionForPrintRequest.getMarketId(),
                marketAuctionForPrintRequest.getAllottedLotId()
        );

        if (lotDetails != null && lotDetails.length > 0) {
            for (Object[] response : lotDetails) {
                // Check if lot status is cancelled
                if (LotStatus.CANCELLED.getLabel().equals(Util.objectToString(response[18]))) {
                    return marketAuctionHelper.retrunIfError(rw, "Lot is cancelled and hence cannot be printed");
                }

                BigInteger lotId = BigInteger.valueOf(Long.parseLong(String.valueOf(response[16])));
                marketAuctionForPrintResponse = prepareResponseForLotBaseResponseSeedCocoon(token, response, lotId);

                // Fetch lot weight details and set in the response
                List<Float> lotWeightList = lotWeightDetailRepository.findAllByLotId(lotId);
                if (!Util.isNullOrEmptyList(lotWeightList)) {
                    marketAuctionForPrintResponse.setLotWeightDetail(lotWeightList);
                }
            }
        } else {
            return marketAuctionHelper.retrunIfError(rw, "No Lot found for given request");
        }

        rw.setContent(marketAuctionForPrintResponse);
        return ResponseEntity.ok(rw);
    }


public MarketAuctionForPrintResponse prepareResponseForLotBaseResponseSeedCocoon(
        JwtPayloadData token, Object[] response, BigInteger lotId) {

    float lotSoldOutAmount = Util.objectToFloat(response[28]);
    float farmerMarketFee = Util.objectToFloat(response[27]);
    float reelerMarketFee = Util.objectToFloat(response[26]);

    // Calculate farmerAmount and reelerAmount
    float farmerAmount = lotSoldOutAmount - farmerMarketFee;
    float reelerAmount = lotSoldOutAmount + reelerMarketFee;

    return MarketAuctionForPrintResponse.builder()
            .farmerNumber(Util.objectToString(response[0]))
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
            .tareWeight(Util.objectToFloat(response[15]))  // Corrected parentheses
            .serialNumber(Util.objectToString(response[17]) + lotId)
            .marketNameKannada(Util.objectToString(response[19]))
            .farmerNameKannada(Util.objectToString(response[20]))
            .farmerMobileNumber(Util.objectToString(response[21]))
            .marketAuctionId((BigDecimal) response[22])
            .fatherNameKan(Util.objectToString(response[23]))
            .lotWeight(Util.objectToFloat(response[24]))
            .sadodLotNumber(Util.objectToString(response[25]))
            .reelerMarketFee(reelerMarketFee)
            .farmerMarketFee(farmerMarketFee)
            .lotSoldOutAmount(lotSoldOutAmount)
            .fruitsId(Util.objectToString(response[29]))
            .godownName(Util.objectToString(response[30])) // Updated index for godown_name
            .loginName(token.getUsername())
            .farmerAmount(farmerAmount)  // Added farmerAmount
            .reelerAmount(reelerAmount)  // Added reelerAmount
            .build();
}



    public ResponseEntity<?> getPrintableDataForLotForSilk(MarketAuctionForPrintRequest marketAuctionForPrintRequest) {

        JwtPayloadData token = marketAuctionHelper.getAuthToken(marketAuctionForPrintRequest);

//        ResponseWrapper rw = ResponseWrapper.createWrapper(MarketAuctionForPrintResponse.class);
//        MarketAuctionForPrintResponse marketAuctionForPrintResponse = null;
//        Object[][] lotDetails = lotRepository.getAcceptedLotDetailsForSilk(marketAuctionForPrintRequest.getAuctionDate(), marketAuctionForPrintRequest.getMarketId(), marketAuctionForPrintRequest.getAllottedLotId());
//        float reelerCurrentBalance = 0;
//        boolean foundAcceptedLot = false;
//        if (lotDetails != null && lotDetails.length > 0) {
//            foundAcceptedLot = true;
//            reelerCurrentBalance = Util.objectToFloat(lotDetails[0][41]);
//
//        } else {
//            lotDetails = lotRepository.getNewlyCreatedLotDetailsForSilk(marketAuctionForPrintRequest.getAuctionDate(), marketAuctionForPrintRequest.getMarketId(), marketAuctionForPrintRequest.getAllottedLotId());
//        }
//        if (foundAcceptedLot || (lotDetails != null && lotDetails.length > 0)) {
//            for (Object[] response : lotDetails) {
//                if(LotStatus.CANCELLED.getLabel().equals(Util.objectToString(response[18]))){
//                    return marketAuctionHelper.retrunIfError(rw,"Lot is cancelled and hence cannot be printed");
//                }
//                BigInteger lotId = BigInteger.valueOf(Long.parseLong(String.valueOf(response[16])));
//                marketAuctionForPrintResponse = prepareResponseForLotBaseResponseForSilk(token, response, lotId);
        ResponseWrapper rw = ResponseWrapper.createWrapper(MarketAuctionForPrintResponse.class);
        MarketAuctionForPrintResponse marketAuctionForPrintResponse = null;

// Fetch accepted lot details
        Object[][] lotDetails = lotRepository.getAcceptedLotDetailsForSilk(
                marketAuctionForPrintRequest.getAuctionDate(),
                marketAuctionForPrintRequest.getMarketId(),
                marketAuctionForPrintRequest.getAllottedLotId()
        );

        boolean foundAcceptedLot = lotDetails != null && lotDetails.length > 0;

        if (!foundAcceptedLot) {
            // Fetch newly created lot details if no accepted lot was found
            lotDetails = lotRepository.getNewlyCreatedLotDetailsForSilk(
                    marketAuctionForPrintRequest.getAuctionDate(),
                    marketAuctionForPrintRequest.getMarketId(),
                    marketAuctionForPrintRequest.getAllottedLotId()
            );
        }

        if (foundAcceptedLot || (lotDetails != null && lotDetails.length > 0)) {
            // Iterate over lot details
            for (Object[] response : lotDetails) {
                // Check if the lot is cancelled
                if (LotStatus.CANCELLED.getLabel().equals(Util.objectToString(response[18]))) {
                    return marketAuctionHelper.retrunIfError(rw, "Lot is cancelled and hence cannot be printed");
                }

                // Prepare the response for the given lot
                BigInteger lotId = BigInteger.valueOf(Long.parseLong(String.valueOf(response[16])));
                marketAuctionForPrintResponse = prepareResponseForLotBaseResponseForSilk(token, response, lotId);


        if (foundAcceptedLot) {
                    marketAuctionForPrintResponse.setAuctionDateWithTime((Date)(response[23]));
                    marketAuctionForPrintResponse.setTraderFirstName(Util.objectToString(response[24]));
                    marketAuctionForPrintResponse.setTraderLastName(Util.objectToString(response[25]));
                    marketAuctionForPrintResponse.setTraderFatherName(Util.objectToString(response[26]));
                    marketAuctionForPrintResponse.setTraderAddress(Util.objectToString(response[27]));
                    marketAuctionForPrintResponse.setTraderSilkType(Util.objectToString(response[28]));
                    marketAuctionForPrintResponse.setTraderLicenseFee(Util.objectToFloat(response[29]));
                    marketAuctionForPrintResponse.setTraderMobileNumber(Util.objectToString(response[30]));
                    marketAuctionForPrintResponse.setTraderArnNumber(Util.objectToString(response[31]));
                    marketAuctionForPrintResponse.setTraderLicenseNumber(Util.objectToString(response[32]));
                    marketAuctionForPrintResponse.setTraderApplicationNumber(Util.objectToString(response[33]));
                    marketAuctionForPrintResponse.setTraderLicenseChallanNumber(Util.objectToString(response[34]));
                    marketAuctionForPrintResponse.setLotWeight(Util.objectToFloat(response[35]));
                    marketAuctionForPrintResponse.setReelerMarketFee(Util.objectToFloat(response[36]));
                    marketAuctionForPrintResponse.setTraderMarketFee(Util.objectToFloat(response[37]));
                    marketAuctionForPrintResponse.setLotSoldOutAmount(Util.objectToFloat(response[38]));
                    marketAuctionForPrintResponse.setBidAmount(Util.objectToFloat(response[39]));
                    marketAuctionForPrintResponse.setFruitsId(Util.objectToString(response[41]));
                    marketAuctionForPrintResponse.setTraderAmount(marketAuctionForPrintResponse.getLotSoldOutAmount() - marketAuctionForPrintResponse.getTraderMarketFee());
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

    public MarketAuctionForPrintResponse prepareResponseForLotBaseResponseForSilk(JwtPayloadData token, Object[] response, BigInteger lotId) {
        MarketAuctionForPrintResponse marketAuctionForPrintResponse;
        marketAuctionForPrintResponse = MarketAuctionForPrintResponse.builder().
                reelerLicense(Util.objectToString(response[0]))
                .reelerName(Util.objectToString(response[1]))
                .reelerAddress(Util.objectToString(response[2]))
                .reelerNameKannada(Util.objectToString(response[3]))
                .reelerMobileNumber(Util.objectToString(response[4]))
                .reelerNumber(Util.objectToString(response[5]))
                .reelerBankName(Util.objectToString(response[6]))
                .reelerAccountNumber(Util.objectToString(response[7]))
                .reelerBranchName(Util.objectToString(response[8]))
                .allottedLotId(Integer.parseInt(String.valueOf(response[9])))
                .auctionDate(Util.objectToString(response[10]))
                .farmerEstimatedWeight(Integer.parseInt(String.valueOf(response[11])))
                .marketName(Util.objectToString(response[12]))
                .race(Util.objectToString(response[13]))
                .source(Util.objectToString(response[14]))
                .tareWeight(Util.objectToFloat(response[15]))
                .serialNumber(Util.objectToString(response[17])+ lotId)
                .marketNameKannada(Util.objectToString(response[19]))
                .reelerIfscCode(Util.objectToString(response[20]))

                .marketAuctionId((BigDecimal) response[21])
                .reelerFatherName(Util.objectToString(response[22]))
                .auctionDateWithTime((Date)(response[23]))
                .fruitsId(Util.objectToString(response[24]))
                .build();
        marketAuctionForPrintResponse.setSmallBinList(binRepository.findAllByMarketAuctionIdAndType(marketAuctionForPrintResponse.getMarketAuctionId().toBigInteger(),"small"));
        marketAuctionForPrintResponse.setBigBinList(binRepository.findAllByMarketAuctionIdAndType(marketAuctionForPrintResponse.getMarketAuctionId().toBigInteger(),"big"));
        marketAuctionForPrintResponse.setLoginName(token.getUsername());
        return marketAuctionForPrintResponse;
    }


    public ResponseEntity<?> getPrintableDataForLotForSeedCocoonTriplet(MarketAuctionForPrintRequest marketAuctionForPrintRequest) {

        JwtPayloadData token = marketAuctionHelper.getAuthToken(marketAuctionForPrintRequest);

        ResponseWrapper rw = ResponseWrapper.createWrapper(MarketAuctionForPrintResponse.class);

        MarketAuctionForPrintResponse marketAuctionForPrintResponse = null;

        // Fetch lot details using the provided request parameters
        Object[][] lotDetails = lotRepository.getNewlyCreatedLotDetailsSeedCocoonsTriplet(
                marketAuctionForPrintRequest.getAuctionDate(),
                marketAuctionForPrintRequest.getMarketId(),
                marketAuctionForPrintRequest.getAllottedLotId()
        );

        if (lotDetails != null && lotDetails.length > 0) {
            for (Object[] response : lotDetails) {
                // Check if lot status is cancelled
                if (LotStatus.CANCELLED.getLabel().equals(Util.objectToString(response[18]))) {
                    return marketAuctionHelper.retrunIfError(rw, "Lot is cancelled and hence cannot be printed");
                }

                BigInteger lotId = BigInteger.valueOf(Long.parseLong(String.valueOf(response[16])));
                marketAuctionForPrintResponse = prepareResponseForLotBaseResponseSeedCocoonTriplet(token, response, lotId);

                // Fetch lot weight details and set in the response
                List<Float> lotWeightList = lotWeightDetailRepository.findAllByLotId(lotId);
                if (!Util.isNullOrEmptyList(lotWeightList)) {
                    marketAuctionForPrintResponse.setLotWeightDetail(lotWeightList);
                }
            }
        } else {
            return marketAuctionHelper.retrunIfError(rw, "No Lot found for given request");
        }

        rw.setContent(marketAuctionForPrintResponse);
        return ResponseEntity.ok(rw);
    }


    public MarketAuctionForPrintResponse prepareResponseForLotBaseResponseSeedCocoonTriplet(
            JwtPayloadData token, Object[] response, BigInteger lotId) {

        float lotSoldOutAmount = Util.objectToFloat(response[28]);
        float farmerMarketFee = Util.objectToFloat(response[27]);
        float reelerMarketFee = Util.objectToFloat(response[26]);

        // Calculate farmerAmount and reelerAmount
        float farmerAmount = lotSoldOutAmount - farmerMarketFee;
        float reelerAmount = lotSoldOutAmount + reelerMarketFee;

        return MarketAuctionForPrintResponse.builder()
                .farmerNumber(Util.objectToString(response[0]))
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
                .tareWeight(Util.objectToFloat(response[15]))  // Corrected parentheses
                .serialNumber(Util.objectToString(response[17]) + lotId)
                .marketNameKannada(Util.objectToString(response[19]))
                .farmerNameKannada(Util.objectToString(response[20]))
                .farmerMobileNumber(Util.objectToString(response[21]))
                .marketAuctionId((BigDecimal) response[22])
                .fatherNameKan(Util.objectToString(response[23]))
                .lotWeight(Util.objectToFloat(response[24]))
                .sadodLotNumber(Util.objectToString(response[25]))
                .reelerMarketFee(reelerMarketFee)
                .farmerMarketFee(farmerMarketFee)
                .lotSoldOutAmount(lotSoldOutAmount)
                .fruitsId(Util.objectToString(response[29]))
                .godownName(Util.objectToString(response[30]))
                .lotGroupageId(Util.objectToString(response[31]))
                .lgBuyerId(Util.objectToString(response[32]))
                .lgBuyerType(Util.objectToString(response[33]))
                .lgLotWeight(Util.objectToString(response[34]))
                .lgAmount(Util.objectToString(response[35]))
                .lgMarketFee(Util.objectToString(response[36]))
                .lgSoldOutAmount(Util.objectToString(response[37]))
                .lgAverageYield(Util.objectToString(response[38]))
                .lgNoOfDfl(Util.objectToString(response[39]))
                .lgInvoiceNumber(Util.objectToString(response[40]))
                .lgLotParentLevel(Util.objectToString(response[41]))
                .lgAuctionDate(Util.objectToString(response[42]))
                .lgBuyerName(Util.objectToString(response[43]))


                .loginName(token.getUsername())
                .farmerAmount(farmerAmount)  // Added farmerAmount
                .reelerAmount(reelerAmount)  // Added reelerAmount
                .build();
    }



}
