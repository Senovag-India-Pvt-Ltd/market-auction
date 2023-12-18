package com.sericulture.marketandauction.service;

import com.sericulture.marketandauction.helper.Util;
import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.api.marketauction.LotWeightResponse;
import com.sericulture.marketandauction.model.api.marketauction.MISCRequest;
import com.sericulture.marketandauction.model.entity.CrateMaster;
import com.sericulture.marketandauction.model.entity.Lot;
import com.sericulture.marketandauction.model.exceptions.MessageLabelType;
import com.sericulture.marketandauction.model.exceptions.ValidationMessage;
import com.sericulture.marketandauction.repository.CrateMasterRepository;
import com.sericulture.marketandauction.repository.LotRepository;
import com.sericulture.marketandauction.repository.ReelerVidBlockedAmountRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import jakarta.persistence.Query;
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
    Util util;

    public ResponseEntity<?> getWeigmentByLotAndMarketAndAuctionDate(MISCRequest miscRequest) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(LotWeightResponse.class);
        LotWeightResponse lotWeightResponse = getLotWeightResponse(miscRequest);
        rw.setContent(lotWeightResponse);
        return ResponseEntity.ok(rw);

    }


    public ResponseEntity<?> canContinueToWeighmentProcess(MISCRequest miscRequest, boolean updateWeight) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);

        double reqAmount = getRequiredBidAmountPerLot(miscRequest);

        if (reqAmount < 0) {
            ValidationMessage validationMessage = new ValidationMessage(MessageLabelType.NON_LABEL_MESSAGE.name(),
                    util.getMessageByCode("MA00002.GEN.FLEXTIME"), "MA00002.GEN.FLEXTIME");
            rw.setErrorCode(-1);
            rw.setErrorMessages(List.of("Reeler current balance is not enough and he need " + Math.abs(reqAmount) + " more money"));

        }
        if (updateWeight) {
            Query nativeQuery = entityManager.createNativeQuery("SELECT ra.AMOUNT,rvba.virtual_account_number,r.reeler_id  from lot l , REELER_AUCTION ra , reeler r ,reeler_virtual_bank_account rvba " +
                    " WHERE l.REELER_AUCTION_ID = ra.REELER_AUCTION_ID and r.reeler_id = rvba.reeler_id " +
                    "  and ra.REELER_ID = r.reeler_id and l.allotted_lot_id = ? and l.market_id = ? and l.auction_date = ?");
            nativeQuery.setParameter(1, miscRequest.getAllottedLotId());
            nativeQuery.setParameter(2, miscRequest.getMarketId());
            nativeQuery.setParameter(3, LocalDate.now());
            Object[] result = (Object[]) nativeQuery.getSingleResult();
            double amount = Double.valueOf(String.valueOf(result[0]));

            double amountForBlock = miscRequest.getWeight() * amount * 1.01;

            long count = reelerVidBlockedAmountRepository.findByReelerVirtualAccountNumberAndAuctionDate(String.valueOf(result[1]), LocalDate.now());



            Query insertRVBAQuery = entityManager.createNativeQuery("INSERT INTO REELER_VID_BLOCKED_AMOUNT " +
                    "(MARKET_AUCTION_ID, ALLOTTED_LOT_ID, " +
                    "MARKET_ID, AUCTION_DATE, REELER_ID, AMOUNT, reeler_virtual_account_number, STATUS, CREATED_BY, MODIFIED_BY, CREATED_DATE, MODIFIED_DATE, ACTIVE)" +
                    "SELECT  TOP  1 0, " + miscRequest.getAllottedLotId() + "," + miscRequest.getMarketId() + ",'" + LocalDate.now() + "', '" + String.valueOf(result[2]) + "'," + amountForBlock + ",'" + String.valueOf(result[1]) + "', 'blocked', '', '', CURRENT_TIMESTAMP , CURRENT_TIMESTAMP, 1 from REELER_VID_BLOCKED_AMOUNT " +
                    "WHERE " + count + " = (SELECT COUNT(*) from REELER_VID_BLOCKED_AMOUNT " +
                    "where reeler_virtual_account_number= ? and AUCTION_DATE= ?)");
            insertRVBAQuery.setParameter(1, String.valueOf(result[1]));
            insertRVBAQuery.setParameter(2, LocalDate.now());


            int upodateRows = insertRVBAQuery.executeUpdate();

            if (upodateRows != 1) {
                ValidationMessage validationMessage = new ValidationMessage(MessageLabelType.NON_LABEL_MESSAGE.name(),
                        util.getMessageByCode("MA00002.GEN.FLEXTIME"), "MA00002.GEN.FLEXTIME");
                rw.setErrorCode(-1);
                rw.setErrorMessages(List.of("concurrent modification exception, reeler is trying parellel transaction at same time"));
                entityManager.close();
                return ResponseEntity.ok(rw);
            }

            Lot lot = lotRepository.findByMarketIdAndAllottedLotIdAndAuctionDate(miscRequest.getMarketId(), miscRequest.getAllottedLotId(), LocalDate.now());
            lot.setStatus("inUpdateWeight");
            lot.setNoOfCrates(miscRequest.getNoOfCrates());
            lot.setTotalCratesCapacityWeight(miscRequest.getWeight());
            lot.setRemarks(miscRequest.getRemarks());
            entityManager.merge(lot);
            entityManager.getTransaction().commit();
            entityManager.close();
        }
        return ResponseEntity.ok(rw);
    }

    private double getRequiredBidAmountPerLot(MISCRequest miscRequest) {
        LotWeightResponse lotWeightResponse = getLotWeightResponse(miscRequest);
        CrateMaster crateMaster = crateMasterRepository.findByMarketIdAndGodownIdAndRace(miscRequest.getMarketId(), miscRequest.getGodownId(), miscRequest.getRace());

        float totalCrateCapacityWeight = crateMaster.getApproxWeightPerCrate() * miscRequest.getNoOfCrates();

        double totalAmountPerLot = (totalCrateCapacityWeight * lotWeightResponse.getBidAmount() * 1.01);

        return lotWeightResponse.getReelerCurrentBalance() - totalAmountPerLot;
    }

    private LotWeightResponse getLotWeightResponse(MISCRequest miscRequest){

        EntityManager entityManager = entityManagerFactory.createEntityManager();

        Query nativeQuery = entityManager.createNativeQuery("""
                select f.fruits_id,r.reeling_license_number,f.first_name ,r.name,
                ra.AMOUNT,ma.market_auction_id,rvba.virtual_account_number,fa.address_text,rvcb.CURRENT_BALANCE 
                from FARMER f , reeler r ,
                lot l ,REELER_AUCTION ra ,market_auction ma,reeler_virtual_bank_account rvba,
                REELER_VID_CURRENT_BALANCE rvcb,farmer_address fa
                where l.market_auction_id = ma.market_auction_id 
                and l.REELER_AUCTION_ID = ra.REELER_AUCTION_ID 
                and ra.REELER_ID = r.reeler_id
                and r.reeler_id = rvba.reeler_id 
                and ma.farmer_id = f.FARMER_ID
                and rvcb.reeler_virtual_account_number = rvba.virtual_account_number
                and fa.FARMER_ID = f.FARMER_ID
                and fa.default_address = 1
                and l.allotted_lot_id = ? and l.market_id = ? and l.auction_date = ? """);

        nativeQuery.setParameter(1, miscRequest.getAllottedLotId());
        nativeQuery.setParameter(2, miscRequest.getMarketId());
        nativeQuery.setParameter(3, LocalDate.now());
        Object[] lotWeightDetails = (Object[]) nativeQuery.getSingleResult();
        entityManager.close();

        double currentBalnaceWithoutBlockedAmount =
                getReelerUnblockedReelerAmount(lotWeightDetails[6].toString(), miscRequest.getMarketId(),Float.valueOf(String.valueOf(lotWeightDetails[8])) );
        LotWeightResponse lotWeightResponse = new LotWeightResponse
                (String.valueOf(lotWeightDetails[0]), String.valueOf(lotWeightDetails[1]),
                        String.valueOf(lotWeightDetails[2]), String.valueOf(lotWeightDetails[3]),
                        String.valueOf(lotWeightDetails[7]),
                        currentBalnaceWithoutBlockedAmount,
                        Float.valueOf(String.valueOf(lotWeightDetails[4])));

        return lotWeightResponse;
    }

    private double getReelerUnblockedReelerAmount(String reelerVirtualAccountNumber,int marketId,float reelerCurrentBalance){
        Object blockedReelerAmountObject = reelerVidBlockedAmountRepository.getReelerBlockedAMountPerAuctionDate(reelerVirtualAccountNumber, LocalDate.now(), marketId);
        double blockedReelerAmount = blockedReelerAmountObject == null ? 0.0 : Double.valueOf(String.valueOf(blockedReelerAmountObject));
        double currentBalnaceWithoutBlockedAmount = reelerCurrentBalance - blockedReelerAmount;
        return currentBalnaceWithoutBlockedAmount;
    }

}
