package com.sericulture.marketandauction.service;

import com.sericulture.authentication.model.JwtPayloadData;
import com.sericulture.marketandauction.helper.MarketAuctionHelper;
import com.sericulture.marketandauction.helper.Util;
import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.api.marketauction.*;
import com.sericulture.marketandauction.model.entity.Lot;
import com.sericulture.marketandauction.model.entity.LotWeightDetail;
import com.sericulture.marketandauction.model.entity.MarketMaster;
import com.sericulture.marketandauction.model.entity.ReelerVidDebitTxn;
import com.sericulture.marketandauction.model.enums.LotStatus;
import com.sericulture.marketandauction.model.enums.PAYMENTMODE;
import com.sericulture.marketandauction.model.exceptions.ValidationException;
import com.sericulture.marketandauction.repository.*;
import jakarta.persistence.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class UpdateLotWeightDetailService {

    @PersistenceUnit
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private CrateMasterRepository crateMasterRepository;

    @Autowired
    private ReelerVidBlockedAmountRepository reelerVidBlockedAmountRepository;
    @Autowired
    private LotRepository lotRepository;
    @Autowired
    private LotWeightDetailRepository lotWeightDetailRepository;
    @Autowired
    private MarketMasterRepository marketMasterRepository;
    @Autowired
    private MarketAuctionHelper marketAuctionHelper;

    @Autowired
    Util util;


//    public ResponseEntity<?> getWeightDetailsByLotIdAndMarketAndAuctionDate(GetWeightDetailsRequest getWeightDetailsRequest) {
//        ResponseWrapper rw = ResponseWrapper.createWrapper(LotWeightResponse.class);
//        GetLotWeightDetailResponse updateLotWeightDetailResponse = getLotWeightResponse(getWeightDetailsRequest);
//        rw.setContent(updateLotWeightDetailResponse);
//        return ResponseEntity.ok(rw);
//    }

//    private GetLotWeightDetailResponse getLotWeightResponse(GetWeightDetailsRequest getWeightDetailsRequest) {
//        EntityManager entityManager = entityManagerFactory.createEntityManager();
//        Query nativeQuery = entityManager.createNativeQuery("""
//        select lwd.crate_number, lwd.net_weight
//        from lot_weight_detail lwd
//        JOIN lot l ON lwd.LOT_ID = l.lot_id
//        where l.allotted_lot_id = ? and l.auction_date = ? and l.market_id = ? and l.status = 'weighmentcompleted'""");
//
//        nativeQuery.setParameter(1, getWeightDetailsRequest.getAllottedLotId());
//        nativeQuery.setParameter(2, Util.getISTLocalDate());
//        nativeQuery.setParameter(3, getWeightDetailsRequest.getMarketId());
//
//        Object[] lotWeightDetails = null;
//        try {
//            lotWeightDetails = (Object[]) nativeQuery.getSingleResult();
//            entityManager.close();
//        } catch (NoResultException ex) {
//            entityManager.close();
//            throw new ValidationException(String.format("No data found for the given lot %s, Please check whether it is accepted or not", getWeightDetailsRequest.getAllottedLotId()));
//        }
//
//        int crateNumber = Util.objectToInteger(lotWeightDetails[0]);
//        float netWeight = Util.objectToFloat(lotWeightDetails[1]);
//
//        GetLotWeightDetailResponse updateLotWeightDetailResponse = GetLotWeightDetailResponse.builder()
//                .crateNumber(crateNumber)
//                .netWeight(netWeight)
//                .build();
//
//        return updateLotWeightDetailResponse;
//    }

    public ResponseEntity<?> getWeightDetailsByLotIdAndMarketAndAuctionDate(GetWeightDetailsRequest getWeightDetailsRequest) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        List<GetLotWeightDetailResponse> updateLotWeightDetailResponseList = getLotWeightResponse(getWeightDetailsRequest);
        rw.setContent(updateLotWeightDetailResponseList);
        return ResponseEntity.ok(rw);
    }


    private List<GetLotWeightDetailResponse> getLotWeightResponse(GetWeightDetailsRequest getWeightDetailsRequest) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        Query nativeQuery = entityManager.createNativeQuery("""
        select lwd.crate_number, lwd.net_weight
        from lot_weight_detail lwd
        JOIN lot l ON lwd.LOT_ID = l.lot_id
        where l.allotted_lot_id = ? and l.auction_date = ? and l.market_id = ? and l.status = 'weighmentcompleted'""");

        nativeQuery.setParameter(1, getWeightDetailsRequest.getAllottedLotId());
        nativeQuery.setParameter(2, Util.getISTLocalDate());
        nativeQuery.setParameter(3, getWeightDetailsRequest.getMarketId());

        List<Object[]> lotWeightDetailsList;
        try {
            lotWeightDetailsList = nativeQuery.getResultList();
            entityManager.close();
        } catch (NoResultException ex) {
            entityManager.close();
            throw new ValidationException(String.format("No data found for the given lot %s, Please check whether it is accepted or not", getWeightDetailsRequest.getAllottedLotId()));
        }

        if (lotWeightDetailsList.isEmpty()) {
            throw new ValidationException(String.format("No data found for the given lot %s, Please check whether it is accepted or not", getWeightDetailsRequest.getAllottedLotId()));
        }

        List<GetLotWeightDetailResponse> updateLotWeightDetailResponseList = new ArrayList<>();
        for (Object[] lotWeightDetails : lotWeightDetailsList) {
            int crateNumber = Util.objectToInteger(lotWeightDetails[0]);
            float netWeight = Util.objectToFloat(lotWeightDetails[1]);

            GetLotWeightDetailResponse updateLotWeightDetailResponse = GetLotWeightDetailResponse.builder()
                    .crateNumber(crateNumber)
                    .netWeight(netWeight)
                    .build();

            updateLotWeightDetailResponseList.add(updateLotWeightDetailResponse);
        }

        return updateLotWeightDetailResponseList;
    }

    public ResponseEntity<?> updateNetWeight(UpdateNetWeightRequest updateNetWeightRequest) {
        ResponseWrapper rw = new ResponseWrapper();
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();

        try {
            JwtPayloadData token = marketAuctionHelper.getAuthToken(updateNetWeightRequest);
            MarketMaster marketMaster = marketMasterRepository.findById(updateNetWeightRequest.getMarketId());
            String paymentMode = marketMaster.getPaymentMode();
            entityManager = entityManagerFactory.createEntityManager();
            entityManager.getTransaction().begin();
            Lot lot = lotRepository.findByMarketIdAndAllottedLotIdAndAuctionDate(updateNetWeightRequest.getMarketId(), updateNetWeightRequest.getAllottedLotId(), Util.getISTLocalDate());
//            if (lot.getStatus()==null || !lot.getStatus().equals(LotStatus.ACCEPTED.getLabel())) {
//                //  throw new ValidationException(String.format("expected Lot status is accepted but found: %s for the allottedLotId: %s",lot.getStatus(),lot.getAllottedLotId()));
//                throw new ValidationException(String.format("Lot is accepted. But " + lot.getStatus() + " for lot " +lot.getAllottedLotId()));
//            }
            LotWeightResponse lotWeightResponse = getLotWeightDetailResponse(updateNetWeightRequest);
            if (Util.isNullOrEmptyOrBlank(lotWeightResponse.getReelerVirtualAccountNumber())) {
                throw new ValidationException(String.format("Reeler Virtual account cannot be null:"));
            }

            double reelerCurrentBalance = lotWeightResponse.getReelerCurrentBalance();
            float totalWeightOfAllottedLot = 0;

//            for (Weighment weight : updateNetWeightRequest.getWeighmentList()) {
//                LotWeightDetail lotWeightDetail = new LotWeightDetail(lot.getId(), weight.getCrateNumber(), weight.getGrossWeight(), weight.getNetWeight());
//                entityManager.persist(lotWeightDetail);
//                totalWeightOfAllottedLot += weight.getNetWeight();
//            }
            // Update net weight for each weighment
//            for (Weighment weight : updateNetWeightRequest.getWeighmentList()) {
//                lotWeightDetailRepository.findByAllottedLotIdAndAuctionDateAndCrateNumber(
//                        updateNetWeightRequest.getAllottedLotId(),
//                        Util.getISTLocalDate(),
//                        weight.getCrateNumber()
////                        weight.getNetWeight()
//                );
//                totalWeightOfAllottedLot += weight.getNetWeight();
//            }

            for (Float weight : updateNetWeightRequest.getNetWeight()) {
                LotWeightDetail existingWeightDetail = lotWeightDetailRepository.findByAllottedLotIdAndAuctionDateAndCrateNumber(
                        updateNetWeightRequest.getAllottedLotId(),
                        Util.getISTLocalDate(),
                        updateNetWeightRequest.getCrateNumber()
                );

                if (existingWeightDetail != null) {
                    // Update the total weight based on the difference
                    float oldWeight = existingWeightDetail.getNetWeight();
                    float newWeight = weight;
                    totalWeightOfAllottedLot += (newWeight - oldWeight);

                    // Update the record with new weight
                    existingWeightDetail.setNetWeight(newWeight);
                    lotWeightDetailRepository.save(existingWeightDetail);
                }
            }


            double savedNetWeight = lot.getLotWeightAfterWeighment();
            double weightDifference = totalWeightOfAllottedLot - savedNetWeight;
            double bidAmount = lotWeightResponse.getBidAmount();
            double amountToUpdate = Math.abs(weightDifference * bidAmount);

            if (totalWeightOfAllottedLot > savedNetWeight) {
                // Debit reeler amount
                if (reelerCurrentBalance < amountToUpdate) {
                    throw new ValidationException("Insufficient balance in reeler virtual account.");
                }
                reelerCurrentBalance -= amountToUpdate;
            } else if (totalWeightOfAllottedLot < savedNetWeight) {
                // Add to reeler account
                reelerCurrentBalance += amountToUpdate;
            }

            // Update lot details
            lot.setLotWeightAfterWeighment(totalWeightOfAllottedLot);
            double lotSoldOutAmount = totalWeightOfAllottedLot * bidAmount;

            Object[][] marketBrokarage = marketMasterRepository.getBrokarageInPercentageForMarket(lot.getMarketId());
            double reelerBrokarage = Double.valueOf(String.valueOf(marketBrokarage[0][1]));
            double farmerBrokarage = Double.valueOf(String.valueOf(marketBrokarage[0][2]));
            double reelerMarketFee = Math.round((lotSoldOutAmount * reelerBrokarage) / 100);
            double farmerMarketFee = Math.round((lotSoldOutAmount * farmerBrokarage) / 100);
            double amountDebitedFromReeler = Math.round(lotSoldOutAmount + reelerMarketFee);

            if (paymentMode == null || paymentMode.equals(PAYMENTMODE.ONLINE.getLabel())) {
                ReelerVidDebitTxn reelerVidDebitTxn = new ReelerVidDebitTxn(
                        lot.getAllottedLotId(), lot.getMarketId(), Util.getISTLocalDate(),
                        lotWeightResponse.getReelerId(), lotWeightResponse.getReelerVirtualAccountNumber(),
                        amountDebitedFromReeler
                );
                entityManager.persist(reelerVidDebitTxn);
            }

            lot.setWeighmentCompletedBy(token.getUsername());
            lot.setStatus(LotStatus.WEIGHMENTCOMPLETED.getLabel());
            lot.setLotSoldOutAmount(lotSoldOutAmount);
            lot.setMarketFeeReeler(reelerMarketFee);
            lot.setMarketFeeFarmer(farmerMarketFee);
            entityManager.merge(lot);

            // Update reeler account balance
//            lotWeightResponse.setReelerCurrentBalance(reelerCurrentBalance);
//            reelerAccountRepository.save(lotWeightResponse);

            CompleteLotWeighmentResponse completeLotWeighmentResponse = new CompleteLotWeighmentResponse();
            completeLotWeighmentResponse.setAllottedLotId(updateNetWeightRequest.getAllottedLotId());
            completeLotWeighmentResponse.setTotalAmountDebited(amountDebitedFromReeler);
            rw.setContent(completeLotWeighmentResponse);

            entityManager.getTransaction().commit();
        } catch (Exception ex) {
            if (ex instanceof ValidationException) {
                throw ex;
            }
            log.info("completeLotWeighMent Error while processing for request: " + updateNetWeightRequest + " error:" + ex);
            return marketAuctionHelper.retrunIfError(rw, "Error while processing completeLotWeighMent: " + ex);
        } finally {
            if (entityManager != null && entityManager.isOpen()) {
                entityManager.close();
            }
        }

        return ResponseEntity.ok(rw);
    }

    private LotWeightResponse getLotWeightDetailResponse(UpdateNetWeightRequest updateNetWeightRequest) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        Query nativeQuery = entityManager.createNativeQuery("""
                select  f.farmer_number,f.fruits_id,r.reeling_license_number,f.first_name ,f.middle_name,f.last_name,r.name,
                ra.AMOUNT,ma.RACE_MASTER_ID,v.VILLAGE_NAME ,rvcb.CURRENT_BALANCE,
                isnull( (select sum(amount) from REELER_VID_BLOCKED_AMOUNT b where b.status='blocked' and   
                b.auction_date=ma.market_auction_date  and b.reeler_virtual_account_number=rvcb.reeler_virtual_account_number) ,0) blocked_amount,
                rvba.virtual_account_number,r.reeler_id,mm.market_name,rm.race_name,sm.source_name,mm.box_weight,l.status
                from  
                FARMER f
                INNER JOIN market_auction ma ON ma.farmer_id = f.FARMER_ID  
                INNER JOIN lot l ON l.market_auction_id =ma.market_auction_id  
                INNER JOIN REELER_AUCTION ra ON ra.REELER_AUCTION_ID  = l.REELER_AUCTION_ID
                INNER JOIN reeler r ON r.reeler_id =ra.REELER_ID   
                LEFT JOIN reeler_virtual_bank_account rvba ON rvba.reeler_id =r.reeler_id and rvba.market_master_id = ma.market_id  
                LEFT JOIN REELER_VID_CURRENT_BALANCE rvcb ON rvcb.reeler_virtual_account_number= rvba.virtual_account_number
                LEFT JOIN farmer_address fa ON f.FARMER_ID = fa.FARMER_ID and fa.default_address = 1  
                LEFT JOIN  Village v ON   fa.Village_ID = v.village_id  and f.ACTIVE = 1
                LEFT JOIN market_master mm ON mm.market_master_id = ma.market_id  
                LEFT JOIN race_master rm ON rm.race_id = ma.RACE_MASTER_ID  
                LEFT JOIN source_master sm ON sm.source_id = ma.SOURCE_MASTER_ID  
                where l.allotted_lot_id = ? and l.auction_date = ? and l.market_id = ?
                and f.ACTIVE =1 and ma.active = 1 and r.active =1  """);

        nativeQuery.setParameter(1, updateNetWeightRequest.getAllottedLotId());
        nativeQuery.setParameter(2, Util.getISTLocalDate());
        nativeQuery.setParameter(3, updateNetWeightRequest.getMarketId());

        Object[] lotWeightDetails = null;
        try {
            lotWeightDetails = (Object[]) nativeQuery.getSingleResult();
            entityManager.close();
        }catch (NoResultException ex){
            entityManager.close();
            throw new ValidationException(String.format("No data found for the given lot %s, Please check whether it is accepted or not",updateNetWeightRequest.getAllottedLotId()));
        }
        float reelerCurrentBalance = Util.objectToFloat(lotWeightDetails[10]);
        float blockedAmount =Util.objectToFloat(lotWeightDetails[11]);
        float availableAmount = reelerCurrentBalance - blockedAmount;
        LotWeightResponse lotWeightResponse = LotWeightResponse.builder().
                farmerNumber(Util.objectToString(lotWeightDetails[0]))
                .farmerFruitsId(Util.objectToString(lotWeightDetails[1]))
                .reelerLicense(Util.objectToString(lotWeightDetails[2]))
                .farmerFirstName(Util.objectToString(lotWeightDetails[3]))
                .farmerMiddleName(Util.objectToString(lotWeightDetails[4]))
                .farmerLastName(Util.objectToString(lotWeightDetails[5]))
                .reelerName(Util.objectToString(lotWeightDetails[6]))
                .bidAmount(Util.objectToFloat(lotWeightDetails[7]))
                .raceMasterId(Util.objectToInteger(lotWeightDetails[8]))
                .farmerVillage(Util.objectToString(lotWeightDetails[9]))
                .reelerCurrentBalance(reelerCurrentBalance)
                .blockedAmount(blockedAmount)
                .reelerCurrentAvailableBalance(availableAmount)
                .reelerVirtualAccountNumber(Util.objectToString(lotWeightDetails[12]))
                .reelerId(Util.objectToInteger(lotWeightDetails[13]))
                .marketName(Util.objectToString(lotWeightDetails[14]))
                .race(Util.objectToString(lotWeightDetails[15]))
                .source(Util.objectToString(lotWeightDetails[16]))
                .tareWeight(Util.objectToFloat(lotWeightDetails[17]))
                .lotStatus(Util.objectToString(lotWeightDetails[18]))
                .build();
        return lotWeightResponse;
    }



}
