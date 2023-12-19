package com.sericulture.marketandauction.service;

import com.sericulture.marketandauction.helper.Util;
import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.api.marketauction.CanContinueToWeighmentRequest;
import com.sericulture.marketandauction.model.api.marketauction.LotStatusRequest;
import com.sericulture.marketandauction.model.api.marketauction.LotWeightResponse;
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

    public ResponseEntity<?> getWeigmentByLotAndMarketAndAuctionDate(LotStatusRequest lotStatusRequest) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(LotWeightResponse.class);
        LotWeightResponse lotWeightResponse = getLotWeightResponse(lotStatusRequest);
        rw.setContent(lotWeightResponse);
        return ResponseEntity.ok(rw);

    }


    public ResponseEntity<?> canContinueToWeighmentProcess(CanContinueToWeighmentRequest canContinueToWeighmentRequest, boolean updateWeight) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);

        LotWeightResponse lotWeightResponse = getLotWeightResponse(canContinueToWeighmentRequest);

        CrateMaster crateMaster = crateMasterRepository.findByMarketIdAndGodownIdAndRaceMasterId(canContinueToWeighmentRequest.getMarketId(), canContinueToWeighmentRequest.getGodownId(), lotWeightResponse.getRaceMasterId());


        int totalCrateCapacityWeight = canContinueToWeighmentRequest.getNoOfCrates() * crateMaster.getApproxWeightPerCrate();
        double amountForBlock = totalCrateCapacityWeight * lotWeightResponse.getBidAmount() * 1.01;

        double hasEnoughMoney = lotWeightResponse.getReelerCurrentAvailableBalance() - amountForBlock;


        if (hasEnoughMoney < 0) {
            ValidationMessage validationMessage = new ValidationMessage(MessageLabelType.NON_LABEL_MESSAGE.name(),
                    util.getMessageByCode("MA00002.GEN.FLEXTIME"), "MA00002.GEN.FLEXTIME");
            rw.setErrorCode(-1);
            rw.setErrorMessages(List.of("Reeler current balance is not enough and he need " + Math.abs(hasEnoughMoney) + " more money"));

        }
        if (updateWeight) {

            long count = reelerVidBlockedAmountRepository.findByReelerVirtualAccountNumberAndAuctionDate(lotWeightResponse.getReelerVirtualAccountNumber(), LocalDate.now());

            Query insertRVBAQuery = entityManager.createNativeQuery("INSERT INTO REELER_VID_BLOCKED_AMOUNT " +
                    "(MARKET_AUCTION_ID, ALLOTTED_LOT_ID, " +
                    "MARKET_ID, AUCTION_DATE, REELER_ID, AMOUNT, reeler_virtual_account_number, STATUS, CREATED_BY, MODIFIED_BY, CREATED_DATE, MODIFIED_DATE, ACTIVE)" +
                    "SELECT  TOP  1 0, " + canContinueToWeighmentRequest.getAllottedLotId() + "," + canContinueToWeighmentRequest.getMarketId() + ",'" + LocalDate.now() + "', '" + lotWeightResponse.getReelerId() + "'," + amountForBlock + ",'" + lotWeightResponse.getReelerVirtualAccountNumber() + "', 'blocked', '', '', CURRENT_TIMESTAMP , CURRENT_TIMESTAMP, 1 from REELER_VID_BLOCKED_AMOUNT " +
                    "WHERE " + count + " = (SELECT COUNT(*) from REELER_VID_BLOCKED_AMOUNT " +
                    "where reeler_virtual_account_number= ? and AUCTION_DATE= ?)");
            insertRVBAQuery.setParameter(1, lotWeightResponse.getReelerVirtualAccountNumber());
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

            Lot lot = lotRepository.findByMarketIdAndAllottedLotIdAndAuctionDate(canContinueToWeighmentRequest.getMarketId(), canContinueToWeighmentRequest.getAllottedLotId(), LocalDate.now());
            lot.setStatus("inUpdateWeight");
            lot.setNoOfCrates(canContinueToWeighmentRequest.getNoOfCrates());
            lot.setTotalCratesCapacityWeight(totalCrateCapacityWeight);
            lot.setRemarks(canContinueToWeighmentRequest.getRemarks());
            entityManager.merge(lot);
            entityManager.getTransaction().commit();
            entityManager.close();
        }
        return ResponseEntity.ok(rw);
    }



    private LotWeightResponse getLotWeightResponse(LotStatusRequest lotStatusRequest) {

        EntityManager entityManager = entityManagerFactory.createEntityManager();

        Query nativeQuery = entityManager.createNativeQuery("select  f.farmer_number,f.fruits_id,r.reeling_license_number,f.first_name ,f.middle_name,f.last_name,r.name," +
                " ra.AMOUNT,ma.RACE_MASTER_ID,v.VILLAGE_NAME ,rvcb.CURRENT_BALANCE," +
                " (select sum(amount) from REELER_VID_BLOCKED_AMOUNT b where b.status='blocked' and  b.auction_date=ma.market_auction_date  and b.reeler_virtual_account_number=rvcb.reeler_virtual_account_number) blocked_amount," +
                " rvcb.CURRENT_BALANCE - (select sum(amount) from REELER_VID_BLOCKED_AMOUNT b  where b.status='blocked' and  auction_date=ma.market_auction_date  and reeler_virtual_account_number=rvcb.reeler_virtual_account_number) available_amount," +
                " rvba.virtual_account_number,r.reeler_id " +
                " from " +
                " FARMER f" +
                " INNER JOIN market_auction ma ON ma.farmer_id = f.FARMER_ID " +
                " INNER JOIN lot l ON l.market_auction_id =ma.market_auction_id and l.auction_date = ma.market_auction_date " +
                " INNER JOIN REELER_AUCTION ra ON ra.ALLOTTED_LOT_ID = l.allotted_lot_id and ra.STATUS ='accepted' and ra.AUCTION_DATE =l.auction_date " +
                " INNER JOIN reeler r ON r.reeler_id =ra.REELER_ID  " +
                " LEFT JOIN reeler_virtual_bank_account rvba ON rvba.reeler_id =r.reeler_id and rvba.market_master_id = ma.market_id" +
                " LEFT JOIN REELER_VID_CURRENT_BALANCE rvcb ON rvcb.reeler_virtual_account_number= rvba.virtual_account_number" +
                " LEFT JOIN farmer_address fa ON f.FARMER_ID = fa.FARMER_ID and fa.default_address = 1 " +
                " LEFT JOIN  Village v ON   fa.Village_ID = v.village_id " +
                " where l.allotted_lot_id = ? and l.auction_date = ? and l.market_id = ?");

        nativeQuery.setParameter(1, lotStatusRequest.getAllottedLotId());
        nativeQuery.setParameter(3, lotStatusRequest.getMarketId());
        nativeQuery.setParameter(2, LocalDate.now());
        Object[] lotWeightDetails = (Object[]) nativeQuery.getSingleResult();
        entityManager.close();


        LotWeightResponse lotWeightResponse = new LotWeightResponse
                (String.valueOf(lotWeightDetails[0]), String.valueOf(lotWeightDetails[1]),
                        String.valueOf(lotWeightDetails[2]), String.valueOf(lotWeightDetails[3]),String.valueOf(lotWeightDetails[4]),String.valueOf(lotWeightDetails[5]),String.valueOf(lotWeightDetails[6]),
                        Float.valueOf(String.valueOf(lotWeightDetails[7])),Integer.parseInt(String.valueOf(lotWeightDetails[8])),String.valueOf(lotWeightDetails[9]),
                        Float.valueOf(String.valueOf(lotWeightDetails[10])),Float.valueOf(String.valueOf(lotWeightDetails[11])),Float.valueOf(String.valueOf(lotWeightDetails[12])),String.valueOf(lotWeightDetails[13]),
                        Integer.valueOf(String.valueOf(lotWeightDetails[14])));

        return lotWeightResponse;
    }

    private double getReelerUnblockedReelerAmount(String reelerVirtualAccountNumber, int marketId, float reelerCurrentBalance) {
        Object blockedReelerAmountObject = reelerVidBlockedAmountRepository.getReelerBlockedAMountPerAuctionDate(reelerVirtualAccountNumber, LocalDate.now(), marketId);
        double blockedReelerAmount = blockedReelerAmountObject == null ? 0.0 : Double.valueOf(String.valueOf(blockedReelerAmountObject));
        double currentBalnaceWithoutBlockedAmount = reelerCurrentBalance - blockedReelerAmount;
        return currentBalnaceWithoutBlockedAmount;
    }

}
