package com.sericulture.marketandauction.service;


import com.sericulture.marketandauction.helper.MarketAuctionHelper;
import com.sericulture.marketandauction.helper.Util;
import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.api.marketauction.FarmerPaymentInfoRequest;
import com.sericulture.marketandauction.model.api.marketauction.FarmerPaymentInfoRequestByLotList;
import com.sericulture.marketandauction.model.api.marketauction.FarmerPaymentInfoResponse;
import com.sericulture.marketandauction.model.enums.LotStatus;
import com.sericulture.marketandauction.repository.LotRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class FarmerPaymentService {


    @Autowired
    LotRepository lotRepository;

    @PersistenceUnit
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    MarketAuctionHelper marketAuctionHelper;


    public ResponseEntity<?> getWeighmentCompletedTxnByAuctionDateAndMarket(FarmerPaymentInfoRequest farmerPaymentInfoRequest, final Pageable pageable) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        Page<Object[]> paginatedResponse = lotRepository.getWeighmentCompletedTxnByAuctionDateAndMarket(pageable,farmerPaymentInfoRequest.getPaymentDate(),farmerPaymentInfoRequest.getMarketId());

        if (paginatedResponse == null || paginatedResponse.isEmpty()) {
            rw.setErrorCode(-1);
            rw.setErrorMessages(List.of("No lot  found"));
            return ResponseEntity.ok(rw);
        }

        List<FarmerPaymentInfoResponse> farmerPaymentInfoResponseList = new ArrayList<>();
        prepareFarmerPaymentInfoResponseList(paginatedResponse.getContent(), farmerPaymentInfoResponseList);
        rw.setContent(farmerPaymentInfoResponseList);
        return ResponseEntity.ok(rw);
    }

    private void prepareFarmerPaymentInfoResponseList(List<Object[]> paginatedResponse, List<FarmerPaymentInfoResponse> farmerPaymentInfoResponseList) {
        for (Object[] response : paginatedResponse) {
            FarmerPaymentInfoResponse farmerPaymentInfoResponse = new FarmerPaymentInfoResponse
                    (Integer.parseInt(Util.objectToString(response[0])), Integer.parseInt(Util.objectToString(response[2])),
                            Util.objectToString(response[3]), Util.objectToString(response[4]), Util.objectToString(response[5]), Util.objectToString(response[6])
                            , Util.objectToString(response[7]), Util.objectToString(response[8]),
                            Util.objectToString(response[9]), Util.objectToString(response[10]), Util.objectToString(response[11]),
                            Util.objectToString(response[12]), Util.objectToString(response[13]), Util.objectToFloat(response[14]),
                            Util.objectToFloat(response[15]), Util.objectToFloat(response[16]), Long.valueOf(Util.objectToString(response[1])));

            farmerPaymentInfoResponseList.add(farmerPaymentInfoResponse);

        }
    }

    public ResponseEntity<?> updateLotlistToReadyForPayment(FarmerPaymentInfoRequestByLotList farmerPaymentInfoRequestByLotList,boolean selectedLot) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        EntityManager entityManager = null;
        try {
            List<Integer> lotList = farmerPaymentInfoRequestByLotList.getAllottedLotList();
            if (Util.isNullOrEmptyList(lotList)) {
                lotList = null;
                if (selectedLot) {
                    rw.setErrorMessages(List.of("Lot list is empty"));
                    rw.setErrorCode(-1);
                    return ResponseEntity.ok(rw);
                }
            }
            Object[][] paginatedResponse = lotRepository.getWeighmentCompletedTxnByLotList(farmerPaymentInfoRequestByLotList.getPaymentDate(),farmerPaymentInfoRequestByLotList.getMarketId(),lotList);

            if (paginatedResponse == null || paginatedResponse.length == 0) {
                return marketAuctionHelper.retrunIfError(rw, "no lots to update");
            }
            List<Long> lotIds = new ArrayList<>();
            for (Object[] response : paginatedResponse) {
                lotIds.add(Util.objectToLong(response[0]));
            }

            entityManager = entityManagerFactory.createEntityManager();

            entityManager.getTransaction().begin();

            Query nativeQuery = entityManager.createNativeQuery("UPDATE Lot set status = ? where lot_id in ( ? )");
            nativeQuery.setParameter(1, LotStatus.READYFORPAYMENT.getLabel());
            nativeQuery.setParameter(2, lotIds);

            nativeQuery.executeUpdate();

            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            entityManager.getTransaction().rollback();
            return marketAuctionHelper.retrunIfError(rw, "Exception while updating the readyForPayement to list:" + farmerPaymentInfoRequestByLotList + " error: " + ex);

        } finally {
            if (entityManager != null && entityManager.isOpen()) {
                entityManager.close();
            }
        }
        return ResponseEntity.ok(rw);
    }


}
