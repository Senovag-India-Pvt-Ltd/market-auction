package com.sericulture.marketandauction.service;

import com.sericulture.authentication.model.JwtPayloadData;
import com.sericulture.marketandauction.helper.MarketAuctionHelper;
import com.sericulture.marketandauction.helper.Util;
import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.api.marketauction.*;
import com.sericulture.marketandauction.model.entity.*;
import com.sericulture.marketandauction.model.enums.LotStatus;
import com.sericulture.marketandauction.model.enums.PAYMENTMODE;
import com.sericulture.marketandauction.model.exceptions.ValidationException;
import com.sericulture.marketandauction.repository.CrateMasterRepository;
import com.sericulture.marketandauction.repository.LotRepository;
import com.sericulture.marketandauction.repository.MarketMasterRepository;
import com.sericulture.marketandauction.repository.ReelerVidBlockedAmountRepository;
import jakarta.persistence.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@Slf4j
public class SilkMarketWeigmentService {

    @PersistenceUnit
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private CrateMasterRepository crateMasterRepository;

    @Autowired
    private ReelerVidBlockedAmountRepository reelerVidBlockedAmountRepository;
    @Autowired
    private LotRepository lotRepository;
    @Autowired
    private MarketMasterRepository marketMasterRepository;
    @Autowired
    private MarketAuctionHelper marketAuctionHelper;

    @Autowired
    Util util;

    public ResponseEntity<?> getWeigmentByLotAndMarketAndAuctionDate(LotStatusRequest lotStatusRequest) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(LotWeightResponse.class);
        LotWeightResponse lotWeightResponse = getLotWeightResponse(lotStatusRequest);
        rw.setContent(lotWeightResponse);
        return ResponseEntity.ok(rw);

    }


    public ResponseEntity<?> canContinueToWeighmentProcess(CanContinueToWeighmentRequest canContinueToWeighmentRequest) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResponseWrapper rw = ResponseWrapper.createWrapper(CanContinueToWeighmentResponse.class);
        try {
            entityManager.getTransaction().begin();
            CanContinueToWeighmentResponse canContinueToWeighmentResponse = new CanContinueToWeighmentResponse();
            LotWeightResponse lotWeightResponse = getLotWeightResponse(canContinueToWeighmentRequest);
            if (!lotWeightResponse.getLotStatus().equals(LotStatus.ACCEPTED.getLabel())) {
                if (entityManager.isOpen()) {
                    entityManager.close();
                }
                return marketAuctionHelper.retrunIfError(rw, "Lot is accepted. But " + lotWeightResponse.getLotStatus() + " for lot " + canContinueToWeighmentRequest.getAllottedLotId());
                //return marketAuctionHelper.retrunIfError(rw, "expected Lot status is accepted but found: " + lotWeightResponse.getLotStatus() + " for the allottedLotId: " + canContinueToWeighmentRequest.getAllottedLotId());
            }
//            if (util.isNullOrEmptyOrBlank(lotWeightResponse.getReelerVirtualAccountNumber())) {
//                if (entityManager.isOpen()) {
//                    entityManager.close();
//                }
//                return marketAuctionHelper.retrunIfError(rw, "No Reeler Virtual Account found for Reeler " + lotWeightResponse.getReelerId());
//            }
//            CrateMaster crateMaster = crateMasterRepository.findByMarketIdAndGodownIdAndRaceMasterId(canContinueToWeighmentRequest.getMarketId(), canContinueToWeighmentRequest.getGodownId(), lotWeightResponse.getRaceMasterId());
//            int totalCrateCapacityWeight = canContinueToWeighmentRequest.getNoOfCrates() * crateMaster.getApproxWeightPerCrate();
//            double lotSoldOutAmount = totalCrateCapacityWeight * lotWeightResponse.getBidAmount();
//            Object[][] marketBrokarage = marketMasterRepository.getBrokarageInPercentageForMarket(canContinueToWeighmentRequest.getMarketId());
//            double traderBrokarage = Double.valueOf(String.valueOf(marketBrokarage[0][1]));
//            double traderMarketFee = (lotSoldOutAmount * traderBrokarage) / 100;
//            double amountDebitedFromReeler = Util.round(lotSoldOutAmount + traderMarketFee, 2);
//            double hasEnoughMoney = lotWeightResponse.getReelerCurrentAvailableBalance() - amountDebitedFromReeler;
//            canContinueToWeighmentResponse.setWeight(totalCrateCapacityWeight);
            rw.setContent(canContinueToWeighmentResponse);

//            if (hasEnoughMoney < 0) {
//                if (entityManager.isOpen()) {
//                    entityManager.close();
//                }
//                return marketAuctionHelper.retrunIfError(rw, "Reeler current balance is not enough and he need " + Math.abs(hasEnoughMoney) + " more money");
//            }
            entityManager.close();
        }catch (NoResultException ex){
            entityManager.close();
            throw new ValidationException(ex.getMessage());

        }finally {
            if (entityManager != null && entityManager.isOpen()) {
                entityManager.close();
            }
        }
        return ResponseEntity.ok(rw);
    }

    private LotWeightResponse getLotWeightResponse(LotStatusRequest lotStatusRequest) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        Query nativeQuery = entityManager.createNativeQuery("""
                select  r.reeler_number,r.fruits_id,tl.trader_license_number,r.name ,tl.first_name,
                ra.AMOUNT,ma.RACE_MASTER_ID,v.VILLAGE_NAME,tl.trader_license_id,mm.market_name,rm.race_name,sm.source_name,mm.box_weight,l.status
                from
                Reeler r
                INNER JOIN market_auction ma ON ma.reeler_id = r.reeler_id
                INNER JOIN lot l ON l.market_auction_id =ma.market_auction_id
                INNER JOIN REELER_AUCTION ra ON ra.REELER_AUCTION_ID  = l.REELER_AUCTION_ID
                INNER JOIN trader_license tl ON tl.trader_license_id =ra.trader_license_id
                LEFT JOIN  Village v ON   r.Village_ID = v.village_id  and r.ACTIVE = 1
                LEFT JOIN market_master mm ON mm.market_master_id = ma.market_id
                LEFT JOIN race_master rm ON rm.race_id = ma.RACE_MASTER_ID
                LEFT JOIN source_master sm ON sm.source_id = ma.SOURCE_MASTER_ID
                where l.allotted_lot_id = ? and l.auction_date = ? and l.market_id = ?
                and r.ACTIVE =1 and ma.active = 1 and tl.active =1""");

        nativeQuery.setParameter(1, lotStatusRequest.getAllottedLotId());
        nativeQuery.setParameter(2, Util.getISTLocalDate());
        nativeQuery.setParameter(3, lotStatusRequest.getMarketId());

        Object[] lotWeightDetails = null;
        try {
            lotWeightDetails = (Object[]) nativeQuery.getSingleResult();
            entityManager.close();
        }catch (NoResultException ex){
            entityManager.close();
            throw new ValidationException(String.format("No data found for the given lot %s, Please check whether it is accepted or not",lotStatusRequest.getAllottedLotId()));
        }finally {
            if (entityManager != null && entityManager.isOpen()) {
                entityManager.close();
            }
        }
        LotWeightResponse lotWeightResponse = LotWeightResponse.builder().
                reelerNumber(Util.objectToString(lotWeightDetails[0]))
                .reelerFruitsId(Util.objectToString(lotWeightDetails[1]))
                .traderLicenseNumber(Util.objectToString(lotWeightDetails[2]))
                .reelerName(Util.objectToString(lotWeightDetails[3]))
                .traderName(Util.objectToString(lotWeightDetails[4]))
                .bidAmount(Util.objectToFloat(lotWeightDetails[5]))
                .raceMasterId(Util.objectToInteger(lotWeightDetails[6]))
                .reelerVillage(Util.objectToString(lotWeightDetails[7]))
                .traderLicenseId(Util.objectToInteger(lotWeightDetails[8]))
                .marketName(Util.objectToString(lotWeightDetails[9]))
                .race(Util.objectToString(lotWeightDetails[10]))
                .source(Util.objectToString(lotWeightDetails[11]))
                .tareWeight(Util.objectToFloat(lotWeightDetails[12]))
                .lotStatus(Util.objectToString(lotWeightDetails[13]))
                .build();
        return lotWeightResponse;
    }
    public ResponseEntity<?> completeLotWeighMent(CompleteLotWeighmentRequest completeLotWeighmentRequest) {
        log.info("completeLotWeighMent received request: "+completeLotWeighmentRequest);
        EntityManager entityManager = null;
        ResponseWrapper rw = ResponseWrapper.createWrapper(CompleteLotWeighmentResponse.class);
        try {
            JwtPayloadData token = marketAuctionHelper.getAuthToken(completeLotWeighmentRequest);
            MarketMaster marketMaster = marketMasterRepository.findById(completeLotWeighmentRequest.getMarketId());
            String paymentMode = marketMaster.getPaymentMode();
            entityManager = entityManagerFactory.createEntityManager();
            entityManager.getTransaction().begin();
            CompleteLotWeighmentResponse completeLotWeighmentResponse = new CompleteLotWeighmentResponse();
            Lot lot = lotRepository.findByMarketIdAndAllottedLotIdAndAuctionDate(completeLotWeighmentRequest.getMarketId(), completeLotWeighmentRequest.getAllottedLotId(), Util.getISTLocalDate());
            if (lot.getStatus()==null || !lot.getStatus().equals(LotStatus.ACCEPTED.getLabel())) {
                //  throw new ValidationException(String.format("expected Lot status is accepted but found: %s for the allottedLotId: %s",lot.getStatus(),lot.getAllottedLotId()));
                throw new ValidationException(String.format("Lot is accepted. But " + lot.getStatus() + " for lot " +lot.getAllottedLotId()));
            }
            LotWeightResponse lotWeightResponse = getLotWeightResponse(completeLotWeighmentRequest);
//            if (Util.isNullOrEmptyOrBlank(lotWeightResponse.getReelerVirtualAccountNumber())) {
//                throw new ValidationException(String.format("Reeler Virtual account cannot be null:"));
//            }
//            double reelerCurrentBalance = lotWeightResponse.getReelerCurrentBalance();
            float totalWeightOfAllottedLot = 0;
            for (Weighment weight : completeLotWeighmentRequest.getWeighmentList()) {
                LotWeightDetail lotWeightDetail = new LotWeightDetail(lot.getId(), weight.getCrateNumber(), weight.getGrossWeight(), weight.getNetWeight());
                entityManager.persist(lotWeightDetail);
                totalWeightOfAllottedLot += weight.getNetWeight();
            }
            double lotSoldOutAmount = totalWeightOfAllottedLot * lotWeightResponse.getBidAmount();
            Object[][] marketBrokarage = marketMasterRepository.getBrokarageInPercentageForMarket(lot.getMarketId());
            double reelerBrokarage = Double.valueOf(String.valueOf(marketBrokarage[0][1]));
            double farmerBrokarage = Double.valueOf(String.valueOf(marketBrokarage[0][2]));
            double traderBrokarage = Double.valueOf(String.valueOf(marketBrokarage[0][3]));


            double reelerMarketFee = (lotSoldOutAmount * reelerBrokarage) / 100;
            double farmerMarketFee = (lotSoldOutAmount * farmerBrokarage) / 100;
            double traderMarketFee = (lotSoldOutAmount * traderBrokarage) / 100;
            double amountDebitedFromReeler = Math.round(lotSoldOutAmount + reelerMarketFee);
            if(paymentMode==null || paymentMode.equals(PAYMENTMODE.ONLINE.getLabel())){
                ReelerVidDebitTxn reelerVidDebitTxn = new ReelerVidDebitTxn(lot.getAllottedLotId(), lot.getMarketId(), Util.getISTLocalDate(), lotWeightResponse.getReelerId(), lotWeightResponse.getReelerVirtualAccountNumber(), amountDebitedFromReeler);
                entityManager.persist(reelerVidDebitTxn);
            }
            lot.setWeighmentCompletedBy(token.getUsername());
            lot.setStatus(LotStatus.WEIGHMENTCOMPLETED.getLabel());
            lot.setLotWeightAfterWeighment(totalWeightOfAllottedLot);
            lot.setLotSoldOutAmount(lotSoldOutAmount);
            lot.setMarketFeeReeler(reelerMarketFee);
            lot.setMarketFeeFarmer(farmerMarketFee);
            lot.setMarketFeeTrader(traderMarketFee);
            entityManager.merge(lot);
            completeLotWeighmentResponse.setAllottedLotId(completeLotWeighmentRequest.getAllottedLotId());
            completeLotWeighmentResponse.setTotalAmountDebited(amountDebitedFromReeler);
            rw.setContent(completeLotWeighmentResponse);
            entityManager.getTransaction().commit();
        } catch (Exception ex) {
            if(ex instanceof ValidationException){
                throw ex;
            }
            log.info("completeLotWeighMent Error while preocessing for request: "+completeLotWeighmentRequest+" error:"+ex);
            return marketAuctionHelper.retrunIfError(rw,"error while processing completeLotWeighMent: "+ex);
        } finally {
            if (entityManager != null && entityManager.isOpen()) {
                entityManager.close();
            }
        }
        return ResponseEntity.ok(rw);
    }


}
