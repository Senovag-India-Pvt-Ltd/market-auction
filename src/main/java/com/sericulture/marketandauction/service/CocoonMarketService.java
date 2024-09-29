package com.sericulture.marketandauction.service;

import com.sericulture.authentication.model.JwtPayloadData;
import com.sericulture.marketandauction.helper.MarketAuctionHelper;
import com.sericulture.marketandauction.helper.Util;
import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.api.cocoon.LotBasePriceFixationRequest;
import com.sericulture.marketandauction.model.api.cocoon.LotBasePriceFixationResponse;
import com.sericulture.marketandauction.model.api.marketauction.MarketAuctionRequest;
import com.sericulture.marketandauction.model.api.marketauction.MarketAuctionResponse;
import com.sericulture.marketandauction.model.entity.Lot;
import com.sericulture.marketandauction.model.entity.LotBasePriceFixation;
import com.sericulture.marketandauction.model.entity.MarketAuction;
import com.sericulture.marketandauction.model.enums.LotStatus;
import com.sericulture.marketandauction.model.exceptions.MessageLabelType;
import com.sericulture.marketandauction.model.exceptions.ValidationMessage;
import com.sericulture.marketandauction.model.mapper.Mapper;
import com.sericulture.marketandauction.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class CocoonMarketService {
    @Autowired
    private MarketAuctionRepository marketAuctionRepository;

    @Autowired
    private BinCounterRepository binCounterRepository;

    @Autowired
    private BinMasterRepository binMasterRepository;

    @Autowired
    private LotRepository lotRepository;

    @Autowired
    private BinCounterMasterRepository binCounterMasterRepository;

    @Autowired
    Mapper mapper;

    @Autowired
    Util util;
    @Autowired
    private CustomValidator validator;

    @PersistenceUnit
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private MarketAuctionHelper marketAuctionHelper;

    @Autowired
    private LotBasePriceFixationRepository lotBasePriceFixationRepository;

    public ResponseEntity<?> marketAuctionFacade(MarketAuctionRequest marketAuctionRequest) {

        LocalDateTime tStart = LocalDateTime.now();
        ResponseWrapper rw = ResponseWrapper.createWrapper(MarketAuctionResponse.class);
        MarketAuctionResponse marketAuctionResponse = new MarketAuctionResponse();
        MarketAuction marketAuction = null;
        try {
            marketAuction = saveMarketAuction(marketAuctionRequest);
            marketAuctionResponse.setTransactionId(marketAuction.getId());
            marketAuctionResponse.setMarketId(marketAuction.getMarketId());
            marketAuctionResponse.setGodownId(marketAuction.getGodownId());
            marketAuctionResponse.setFarmerId(marketAuction.getFarmerId());
            // saves bin and the lot
            log.info(String.format("marketAuction: processing the Requrest for marketId: %s gowdownId: %s and farmer: %s"
                    ,marketAuction.getMarketId(),marketAuction.getGodownId(),marketAuction.getFarmerId()));
            allotALot(marketAuctionResponse, marketAuction);

            rw.setContent(marketAuctionResponse);
            marketAuction.setStatus("generated");
            log.info(String.format("marketAuction: succesfull generation of market auction Requrest for marketId: %s gowdownId: %s and farmer: %s"
                    ,marketAuction.getMarketId(),marketAuction.getGodownId(),marketAuction.getFarmerId()));
        } catch (Exception e) {
            marketAuction.setStatus("error");
            e.printStackTrace();
            log.error("Error occurred while processing the request %s", marketAuctionRequest);
            throw e;
        } finally {
            if(Objects.nonNull(marketAuction)) {
                marketAuctionRepository.save(marketAuction);
            }
        }
        log.info("total time is: "+ ChronoUnit.MILLIS.between(tStart,LocalDateTime.now()));
        return ResponseEntity.ok(rw);
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private MarketAuction saveMarketAuction(MarketAuctionRequest marketAuctionRequest) {
        MarketAuction marketAuction = mapper.marketAuctionObjectToEntity(marketAuctionRequest, MarketAuction.class);
        validator.validate(marketAuction);
        marketAuction.setMarketAuctionDate(Util.getISTLocalDate());
        marketAuction.setStatus("in creation");

        return marketAuctionRepository.save(marketAuction);
    }

    /** Runs ina single transaction to allocate bins and lot together rollbacks if there are any exception during the process
     *
     * @param marketAuctionResponse
     * @param marketAuction
     */
    private void allotALot(MarketAuctionResponse marketAuctionResponse, MarketAuction marketAuction) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try{
            entityManager.getTransaction().begin();
            LocalDateTime tStart = LocalDateTime.now();
            List<Integer> lotList = saveLot(marketAuction.getId(), marketAuction.getNumberOfLot(), marketAuction.getMarketId(), marketAuction.getGodownId(),marketAuction.getEstimatedWeight(),entityManager);
            marketAuctionResponse.setAllotedLotList(lotList);
            entityManager.getTransaction().commit();
            log.info("lots saved successfully for the request: "+marketAuction);
        }catch (Exception ex){
            entityManager.getTransaction().rollback();
            log.error("Error while creating lots for the request: "+marketAuction);
            throw ex;
        }finally {
            if(entityManager!=null && entityManager.isOpen()){
                entityManager.close();
            }
        }
    }
    private List<Integer> saveLot(BigInteger id, int numberOfLot, int marketId, int godownId, int estimatedWeight, EntityManager entityManager) {
        List<Integer> lotList = new ArrayList<>();
        Integer lotCounter = 0;
        lotCounter = lotRepository.findByMarketIdAndAuctionDate(marketId, Util.getISTLocalDate());
        if (lotCounter == null) {
            lotCounter = 0;
        }
        int approxWeightPerLot = estimatedWeight / numberOfLot;
        List<Lot> lots = new ArrayList<>();
        for (int i = 0; i < numberOfLot; i++) {
            Lot lot = new Lot();
            int allotedLot = lotCounter + 1 + i;
            lot.setAllottedLotId(allotedLot);
            lotList.add(allotedLot);
            lot.setMarketAuctionId(id);
            lot.setMarketId(marketId);
            lot.setAuctionDate(Util.getISTLocalDate());
            lot.setCustomerReferenceNumber(Util.getCRN(Util.getISTLocalDate(),marketId,allotedLot));
            lot.setLotApproxWeightBeforeWeighment(approxWeightPerLot);
            lot.setStatus(LotStatus.ASSESSMENT.getLabel());
            lots.add(lot);
            entityManager.persist(lot);
        }
        return lotList;
    }

    public ResponseEntity<?> saveLotBasePriceFixation(LotBasePriceFixationRequest lotBasePriceFixationRequest){
        ResponseWrapper rw = ResponseWrapper.createWrapper(String.class);
        LotBasePriceFixation lotBasePriceFixation = lotBasePriceFixationRepository.findByMarketIdAndFixationDate(lotBasePriceFixationRequest.getMarketId(),Util.getISTLocalDate());
        if(lotBasePriceFixation!=null){
            rw.setErrorCode(-1);
            rw.setContent("Already price exists first deactivate this and then enter new price");
            return ResponseEntity.ok(rw);

        }
        lotBasePriceFixation = mapper.lotBasePriceFixationObjectToEntity(lotBasePriceFixationRequest,LotBasePriceFixation.class);
        lotBasePriceFixation.setFixationDate(Util.getISTLocalDate());
        lotBasePriceFixationRepository.save(lotBasePriceFixation);
        return ResponseEntity.ok(rw);
    }

    public ResponseEntity<?> getLast10DaysPrices(){
        JwtPayloadData jwtPayloadData = Util.getTokenValues();
        int marketId = Util.getMarketId(jwtPayloadData);
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        List<LotBasePriceFixation> lotBasePriceFixationList = lotBasePriceFixationRepository.findTop10ByMarketIdOrderByIdDesc(marketId);
        List<LotBasePriceFixationResponse> lotBasePriceFixationResponseList = new ArrayList<>();
        for(LotBasePriceFixation lotBasePriceFixation:lotBasePriceFixationList){
            lotBasePriceFixationResponseList.add(mapper.lotBasePriceFixationEntityToObject(lotBasePriceFixation, LotBasePriceFixationResponse.class));
        }
        rw.setContent(lotBasePriceFixationResponseList);
        return ResponseEntity.ok(rw);
    }
}
