package com.sericulture.marketandauction.service;

import com.sericulture.marketandauction.helper.Util;
import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.api.marketauction.*;
import com.sericulture.marketandauction.model.entity.LotGroupage;
import com.sericulture.marketandauction.model.exceptions.ValidationException;
import com.sericulture.marketandauction.model.mapper.Mapper;
import com.sericulture.marketandauction.repository.LotGroupageRepository;
import jakarta.persistence.*;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

@Service
@Slf4j
public class LotGroupageService {

    @Autowired
    LotGroupageRepository lotGroupageRepository;

    @Autowired
    Mapper mapper;

    @Autowired
    CustomValidator validator;

    @PersistenceUnit
    private EntityManagerFactory entityManagerFactory;

    @Transactional
//    public LotGroupageResponse saveLotGroupage(LotGroupageDetailsRequest lotGroupageDetailsRequest) {
//        LotGroupageResponse lotGroupageResponse = new LotGroupageResponse();
////Check if lotGroupageRequests is null or empty
//        if (lotGroupageDetailsRequest.getLotGroupageRequests() == null || lotGroupageDetailsRequest.getLotGroupageRequests().isEmpty()) {
//            lotGroupageResponse.setError(true);
//            lotGroupageResponse.setError_description("Lot groupage requests are null or empty.");
//            return lotGroupageResponse;
//        }
//
//        for (int i = 0; i < lotGroupageDetailsRequest.getLotGroupageRequests().size(); i++) {
//            LotGroupage lotGroupage = mapper.lotGroupageObjectToEntity(lotGroupageDetailsRequest.getLotGroupageRequests().get(i), LotGroupage.class);
//            lotGroupageResponse = mapper.lotGroupageEntityToObject(lotGroupageRepository.save(lotGroupage), LotGroupageResponse.class);
//            lotGroupageResponse.setError(false);
//        }
//
//        return lotGroupageResponse;
//    }


//    public LotGroupageResponse saveLotGroupage(LotGroupageDetailsRequest lotGroupageDetailsRequest) {
//        LotGroupageResponse lotGroupageResponse = new LotGroupageResponse();
//
//        // Check if lotGroupageRequests is null or empty
//        if (lotGroupageDetailsRequest.getLotGroupageRequests() == null || lotGroupageDetailsRequest.getLotGroupageRequests().isEmpty()) {
//            lotGroupageResponse.setError(true);
//            lotGroupageResponse.setError_description("Lot groupage requests are null or empty.");
//            return lotGroupageResponse;
//        }
//
//        for (int i = 0; i < lotGroupageDetailsRequest.getLotGroupageRequests().size(); i++) {
//            LotGroupageRequest lotGroupageRequest = lotGroupageDetailsRequest.getLotGroupageRequests().get(i);
//
//            // Debug logging to verify incoming request
//            System.out.println("Received LotGroupageRequest: " + lotGroupageRequest);
//
//            // Check if allottedLotId is null
//            if (lotGroupageRequest.getAllottedLotId() == null) {
//                lotGroupageResponse.setError(true);
//                lotGroupageResponse.setError_description("Allotted Lot ID is null for request " + (i + 1));
//                return lotGroupageResponse;
//            }
//
//            List<Object[]> list = lotGroupageRepository.getMarketAuctionIdByAllottedLotIdAndMarketAuctionDate(
//                    lotGroupageRequest.getAllottedLotId().intValue(),
//                    lotGroupageRequest.getAuctionDate()
//            );
//
//            if (list.isEmpty()) {
//                lotGroupageResponse.setError(true);
//                lotGroupageResponse.setError_description("No market auction found for Allotted Lot ID " + lotGroupageRequest.getAllottedLotId() + " and Auction Date " + lotGroupageRequest.getAuctionDate());
//                return lotGroupageResponse;
//            }
//
//            LotGroupage lotGroupage = new LotGroupage();
//            for (Object[] arr : list) {
//                lotGroupage.setMarketAuctionId(((BigDecimal) arr[0]).toBigIntegerExact());
//                lotGroupage.setId(((BigDecimal) arr[1]).toBigIntegerExact());
//            }
//
//            // Populate LotGroupage entity with request data
//            lotGroupage.setBuyerType(lotGroupageRequest.getBuyerType());
//            lotGroupage.setBuyerId(lotGroupageRequest.getBuyerId());
//            lotGroupage.setLotWeight(lotGroupageRequest.getLotWeight());
//            lotGroupage.setAmount(lotGroupageRequest.getAmount());
//            lotGroupage.setSoldAmount(lotGroupageRequest.getSoldAmount());
//            lotGroupage.setAllottedLotId(lotGroupageRequest.getAllottedLotId());
//            lotGroupage.setAuctionDate(lotGroupageRequest.getAuctionDate());
//
//            // Debug logging before saving
//            System.out.println("LotGroupage entity before saving: " + lotGroupage);
//
//            // Calculate and set market fee based on buyer type
//            if (lotGroupageRequest.getBuyerType() != null) {
//                BigDecimal soldAmount = BigDecimal.valueOf(lotGroupageRequest.getSoldAmount());
//                BigDecimal marketFee = BigDecimal.ZERO;
//
//                switch (lotGroupageRequest.getBuyerType()) {
//                    case "ExternalStakeHolders":
//                        marketFee = soldAmount.add(soldAmount.multiply(BigDecimal.valueOf(0.01)));
//                        break;
//                    case "Reeler":
//                        marketFee = soldAmount.add(soldAmount.multiply(BigDecimal.valueOf(0.02)));
//                        break;
//                    default:
//                        // Handle unknown buyer types if necessary
//                        break;
//                }
//
//                lotGroupage.setMarketFee(marketFee.longValue());
//            }
//
//            lotGroupage = lotGroupageRepository.save(lotGroupage);
//
//            // Debug logging after saving
//            System.out.println("LotGroupage entity after saving: " + lotGroupage);
//
//            // Map saved entity to response
//            lotGroupageResponse = mapper.lotGroupageEntityToObject(lotGroupage, LotGroupageResponse.class);
//            lotGroupageResponse.setError(false);
//
//            // Debug logging for response
//            System.out.println("Generated LotGroupageResponse: " + lotGroupageResponse);
//        }
//
//        return lotGroupageResponse;Changes in Lot group
//    }

    public LotGroupageResponse saveLotGroupage(LotGroupageDetailsRequest lotGroupageDetailsRequest) {
        LotGroupageResponse lotGroupageResponse = new LotGroupageResponse();

        // Check if lotGroupageRequests is null or empty
        if (lotGroupageDetailsRequest.getLotGroupageRequests() == null || lotGroupageDetailsRequest.getLotGroupageRequests().isEmpty()) {
            lotGroupageResponse.setError(true);
            lotGroupageResponse.setError_description("Lot groupage requests are null or empty.");
            return lotGroupageResponse;
        }

        for (int i = 0; i < lotGroupageDetailsRequest.getLotGroupageRequests().size(); i++) {
            LotGroupageRequest lotGroupageRequest = lotGroupageDetailsRequest.getLotGroupageRequests().get(i);
            LotGroupage lotGroupage = mapper.lotGroupageObjectToEntity(lotGroupageRequest, LotGroupage.class);
            List<Object[]> list = lotGroupageRepository.getMarketAuctionIdByAllottedLotIdAndMarketAuctionDate(lotGroupageRequest.getAllottedLotId().intValue(), lotGroupageRequest.getAuctionDate());
            for(Object[] arr :list){
                lotGroupage.setMarketAuctionId(((BigDecimal) arr[0]).toBigIntegerExact());
                lotGroupage.setId(((BigDecimal) arr[1]).toBigIntegerExact());
            }

            // Calculate and set market fee based on buyer type
            if (lotGroupageRequest.getBuyerType() != null) {
                BigDecimal soldAmount = BigDecimal.valueOf(lotGroupageRequest.getSoldAmount());
                BigDecimal marketFee = BigDecimal.ZERO;

                switch (lotGroupageRequest.getBuyerType()) {
                    case "ExternalStakeHolders":
                        marketFee = soldAmount.add(soldAmount.multiply(BigDecimal.valueOf(0.01)));
                        break;
                    case "Reeler":
                        marketFee = soldAmount.add(soldAmount.multiply(BigDecimal.valueOf(0.02)));
                        break;
                    default:
                        // Handle unknown buyer types if necessary
                        break;
                }

                lotGroupage.setMarketFee(marketFee.longValue());
//                lotGroupage.setMarketAuctionId(marketAuctionId);
            }

            lotGroupage = lotGroupageRepository.save(lotGroupage);
            lotGroupageResponse = mapper.lotGroupageEntityToObject(lotGroupage, LotGroupageResponse.class);
            lotGroupageResponse.setError(false);
        }

        return lotGroupageResponse;
    }





    //    public ResponseEntity<?> getLotDistributeDetailsByLotAndMarketAndAuctionDateForSeedMarket(LotStatusSeedMarketRequest lotStatusRequest) {
//        ResponseWrapper rw = ResponseWrapper.createWrapper(LotDistributeResponse.class);
//        LotDistributeResponse lotDistributeResponse = getLotDistributeResponseForSeedMarket(lotStatusRequest);
//        rw.setContent(lotDistributeResponse);
//        return ResponseEntity.ok(rw);
//
//    }
public ResponseEntity<?> getLotDistributeDetailsByLotAndMarketAndAuctionDateForSeedMarket(LotStatusSeedMarketRequest lotStatusRequest) {
    ResponseWrapper rw = ResponseWrapper.createWrapper(LotDistributeResponse.class);
    List<LotDistributeResponse> lotDistributeResponses = getLotDistributeResponseForSeedMarket(lotStatusRequest);
    rw.setContent(lotDistributeResponses);
    return ResponseEntity.ok(rw);
}

//    private LotDistributeResponse getLotDistributeResponseForSeedMarket(LotStatusSeedMarketRequest lotStatusRequest) {
//        EntityManager entityManager = entityManagerFactory.createEntityManager();
//        Query nativeQuery = entityManager.createNativeQuery("""
//                select  f.farmer_number,f.fruits_id,f.first_name ,f.middle_name,f.last_name,
//                ma.RACE_MASTER_ID,v.VILLAGE_NAME,mm.market_name,rm.race_name,sm.source_name,mm.box_weight,l.status,
//                lg.lot_groupage_id,lg.buyer_id,lg.buyer_type,lg.lot_weight,lg.amount,lg.market_fee,lg.sold_amount
//                from
//                FARMER f
//                INNER JOIN market_auction ma ON ma.farmer_id = f.FARMER_ID
//                INNER JOIN lot l ON l.market_auction_id =ma.market_auction_id
//                LEFT JOIN farmer_address fa ON f.FARMER_ID = fa.FARMER_ID and fa.default_address = 1
//                LEFT JOIN  Village v ON   fa.Village_ID = v.village_id  and f.ACTIVE = 1
//                LEFT JOIN market_master mm ON mm.market_master_id = ma.market_id
//                LEFT JOIN race_master rm ON rm.race_id = ma.RACE_MASTER_ID
//                LEFT JOIN source_master sm ON sm.source_id = ma.SOURCE_MASTER_ID
//                LEFT JOIN lot_groupage lg ON ma.market_auction_id =lg.market_auction_id
//                where l.allotted_lot_id = ? and l.auction_date = ? and l.market_id = ?
//                and f.ACTIVE =1 and ma.active = 1  """);
//
//        nativeQuery.setParameter(1, lotStatusRequest.getAllottedLotId());
////        nativeQuery.setParameter(2, Util.getISTLocalDate());
//        nativeQuery.setParameter(2, lotStatusRequest.getAuctionDate());
//        nativeQuery.setParameter(3, lotStatusRequest.getMarketId());
//
//        Object[] lotWeightDetails = null;
//        try {
//            lotWeightDetails = (Object[]) nativeQuery.getSingleResult();
//            entityManager.close();
//        }catch (NoResultException ex){
//            entityManager.close();
//            throw new ValidationException(String.format("No data found for the given lot %s, Please check whether it is accepted or not",lotStatusRequest.getAllottedLotId()));
//        }
//        LotDistributeResponse lotDistributeResponse = LotDistributeResponse.builder().
//                farmerNumber(Util.objectToString(lotWeightDetails[0]))
//                .farmerFruitsId(Util.objectToString(lotWeightDetails[1]))
//                .farmerFirstName(Util.objectToString(lotWeightDetails[2]))
//                .farmerMiddleName(Util.objectToString(lotWeightDetails[3]))
//                .farmerLastName(Util.objectToString(lotWeightDetails[4]))
//                .raceMasterId(Util.objectToInteger(lotWeightDetails[5]))
//                .farmerVillage(Util.objectToString(lotWeightDetails[6]))
//                .marketName(Util.objectToString(lotWeightDetails[7]))
//                .race(Util.objectToString(lotWeightDetails[8]))
//                .source(Util.objectToString(lotWeightDetails[9]))
//                .tareWeight(Util.objectToFloat(lotWeightDetails[10]))
//                .lotStatus(Util.objectToString(lotWeightDetails[11]))
//                .lotGroupageId(Util.objectToLong(lotWeightDetails[12]))
//                .buyerId(Util.objectToLong(lotWeightDetails[13]))
//                .buyerType(Util.objectToString(lotWeightDetails[14]))
//                .lotWeight(Util.objectToString(lotWeightDetails[15]))
//                .amount(Util.objectToLong(lotWeightDetails[16]))
//                .marketFee(Util.objectToLong(lotWeightDetails[17]))
//                .soldAmount(Util.objectToLong(lotWeightDetails[18]))
//                .build();
//        return lotDistributeResponse;
//    }


    public List<LotDistributeResponse> getLotDistributeResponseForSeedMarket(LotStatusSeedMarketRequest lotStatusRequest) {
            EntityManager entityManager = entityManagerFactory.createEntityManager();
            Query nativeQuery = entityManager.createNativeQuery("""
            select  f.farmer_number, f.fruits_id, f.first_name, f.middle_name, f.last_name,
            ma.RACE_MASTER_ID, v.VILLAGE_NAME, mm.market_name, rm.race_name, sm.source_name, mm.box_weight, l.status,
            lg.lot_groupage_id, lg.buyer_id, lg.buyer_type, lg.lot_weight, lg.amount, lg.market_fee, lg.sold_amount
            from FARMER f
            INNER JOIN market_auction ma ON ma.farmer_id = f.FARMER_ID
            INNER JOIN lot l ON l.market_auction_id = ma.market_auction_id
            LEFT JOIN farmer_address fa ON f.FARMER_ID = fa.FARMER_ID and fa.default_address = 1
            LEFT JOIN Village v ON fa.Village_ID = v.village_id and f.ACTIVE = 1
            LEFT JOIN market_master mm ON mm.market_master_id = ma.market_id
            LEFT JOIN race_master rm ON rm.race_id = ma.RACE_MASTER_ID
            LEFT JOIN source_master sm ON sm.source_id = ma.SOURCE_MASTER_ID
            LEFT JOIN lot_groupage lg ON ma.market_auction_id = lg.market_auction_id
            where l.allotted_lot_id = ? and l.auction_date = ? and l.market_id = ?
            and f.ACTIVE = 1 and ma.active = 1""");

            nativeQuery.setParameter(1, lotStatusRequest.getAllottedLotId());
            nativeQuery.setParameter(2, lotStatusRequest.getAuctionDate());
            nativeQuery.setParameter(3, lotStatusRequest.getMarketId());

            List<Object[]> lotWeightDetailsList = nativeQuery.getResultList();
            entityManager.close();

            if (lotWeightDetailsList.isEmpty()) {
                throw new ValidationException(String.format("No data found for the given lot %s, Please check whether it is accepted or not", lotStatusRequest.getAllottedLotId()));
            }

            List<LotDistributeResponse> responses = new ArrayList<>();
            for (Object[] lotWeightDetails : lotWeightDetailsList) {
                LotDistributeResponse lotDistributeResponse = LotDistributeResponse.builder()
                        .farmerNumber(Util.objectToString(lotWeightDetails[0]))
                        .farmerFruitsId(Util.objectToString(lotWeightDetails[1]))
                        .farmerFirstName(Util.objectToString(lotWeightDetails[2]))
                        .farmerMiddleName(Util.objectToString(lotWeightDetails[3]))
                        .farmerLastName(Util.objectToString(lotWeightDetails[4]))
                        .raceMasterId(Util.objectToInteger(lotWeightDetails[5]))
                        .farmerVillage(Util.objectToString(lotWeightDetails[6]))
                        .marketName(Util.objectToString(lotWeightDetails[7]))
                        .race(Util.objectToString(lotWeightDetails[8]))
                        .source(Util.objectToString(lotWeightDetails[9]))
                        .tareWeight(Util.objectToFloat(lotWeightDetails[10]))
                        .lotStatus(Util.objectToString(lotWeightDetails[11]))
                        .lotGroupageId(Util.objectToLong(lotWeightDetails[12]))
                        .buyerId(Util.objectToLong(lotWeightDetails[13]))
                        .buyerType(Util.objectToString(lotWeightDetails[14]))
                        .lotWeight(Util.objectToLong(lotWeightDetails[15]))
                        .amount(Util.objectToLong(lotWeightDetails[16]))
                        .marketFee(Util.objectToLong(lotWeightDetails[17]))
                        .soldAmount(Util.objectToLong(lotWeightDetails[18]))
                        .build();
                responses.add(lotDistributeResponse);
            }

            return responses;
        }


}
