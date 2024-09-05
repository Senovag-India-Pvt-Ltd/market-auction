package com.sericulture.marketandauction.service;

import com.sericulture.authentication.model.JwtPayloadData;
import com.sericulture.marketandauction.helper.MarketAuctionHelper;
import com.sericulture.marketandauction.helper.Util;
import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.api.marketauction.*;
import com.sericulture.marketandauction.model.entity.*;
import com.sericulture.marketandauction.model.enums.LotStatus;
import com.sericulture.marketandauction.model.enums.PAYMENTMODE;
import com.sericulture.marketandauction.model.exceptions.MessageLabelType;
import com.sericulture.marketandauction.model.exceptions.ValidationException;
import com.sericulture.marketandauction.model.exceptions.ValidationMessage;
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
import java.util.List;

@Service
@Slf4j
public class WeigmentService {

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
            if (util.isNullOrEmptyOrBlank(lotWeightResponse.getReelerVirtualAccountNumber())) {
                if (entityManager.isOpen()) {
                    entityManager.close();
                }
                return marketAuctionHelper.retrunIfError(rw, "No Reeler Virtual Account found for Reeler " + lotWeightResponse.getReelerId());
            }
            CrateMaster crateMaster = crateMasterRepository.findByMarketIdAndGodownIdAndRaceMasterId(canContinueToWeighmentRequest.getMarketId(), canContinueToWeighmentRequest.getGodownId(), lotWeightResponse.getRaceMasterId());
            int totalCrateCapacityWeight = canContinueToWeighmentRequest.getNoOfCrates() * crateMaster.getApproxWeightPerCrate();
            double lotSoldOutAmount = totalCrateCapacityWeight * lotWeightResponse.getBidAmount();
            Object[][] marketBrokarage = marketMasterRepository.getBrokarageInPercentageForMarket(canContinueToWeighmentRequest.getMarketId());
            double reelerBrokarage = Double.valueOf(String.valueOf(marketBrokarage[0][1]));
            double reelerMarketFee = (lotSoldOutAmount * reelerBrokarage) / 100;
            double amountDebitedFromReeler = Util.round(lotSoldOutAmount + reelerMarketFee, 2);
            double hasEnoughMoney = lotWeightResponse.getReelerCurrentAvailableBalance() - amountDebitedFromReeler;
            canContinueToWeighmentResponse.setWeight(totalCrateCapacityWeight);
            rw.setContent(canContinueToWeighmentResponse);

            if (hasEnoughMoney < 0) {
                if (entityManager.isOpen()) {
                    entityManager.close();
                }
                return marketAuctionHelper.retrunIfError(rw, "Reeler current balance is not enough and he need " + Math.abs(hasEnoughMoney) + " more money");
            }
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
                and f.ACTIVE =1 and ma.active = 1 and r.active =1 and rvba.active =1 """);

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
            if (Util.isNullOrEmptyOrBlank(lotWeightResponse.getReelerVirtualAccountNumber())) {
                throw new ValidationException(String.format("Reeler Virtual account cannot be null:"));
            }
            double reelerCurrentBalance = lotWeightResponse.getReelerCurrentBalance();
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
            double reelerMarketFee = (lotSoldOutAmount * reelerBrokarage) / 100;
            double farmerMarketFee = (lotSoldOutAmount * farmerBrokarage) / 100;
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
