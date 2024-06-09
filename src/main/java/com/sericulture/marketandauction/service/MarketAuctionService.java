package com.sericulture.marketandauction.service;

import com.sericulture.authentication.model.JwtPayloadData;
import com.sericulture.marketandauction.helper.MarketAuctionHelper;
import com.sericulture.marketandauction.helper.Util;
import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.api.marketauction.*;
import com.sericulture.marketandauction.model.entity.*;
import com.sericulture.marketandauction.model.enums.LotStatus;
import com.sericulture.marketandauction.model.exceptions.MessageLabelType;
import com.sericulture.marketandauction.model.exceptions.ValidationException;
import com.sericulture.marketandauction.model.exceptions.ValidationMessage;
import com.sericulture.marketandauction.model.mapper.Mapper;
import com.sericulture.marketandauction.repository.*;
import jakarta.persistence.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Slf4j
public class MarketAuctionService {

    @Autowired
    private MarketAuctionRepository marketAuctionRepository;

    @Autowired
    private BinCounterRepository binCounterRepository;

    @Autowired
    private BinMasterRepository binMasterRepository;

    @Autowired
    private BinRepository binRepository;

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
    public  ResponseEntity<?> marketAuctionFacade(MarketAuctionRequest marketAuctionRequest) {

        LocalDateTime tStart = LocalDateTime.now();

        ResponseWrapper rw = ResponseWrapper.createWrapper(MarketAuctionResponse.class);
        MarketAuctionResponse marketAuctionResponse = new MarketAuctionResponse();

        JwtPayloadData token = marketAuctionHelper.getMOAuthToken(marketAuctionRequest);

        int marketId = Util.getMarketId(token);
        int goDownId = Util.getGodownId(token);


        //validator.validate(marketAuctionResponse);
        boolean canIssue = marketAuctionHelper.canPerformActivity(MarketAuctionHelper.activityType.ISSUEBIDSLIP,marketAuctionRequest.getMarketId(), marketAuctionRequest.getGodownId());

        if(!canIssue){
            ValidationMessage validationMessage = new ValidationMessage(MessageLabelType.NON_LABEL_MESSAGE.name(),
                    util.getMessageByCode("MA00002.GEN.FLEXTIME"),"MA00002.GEN.FLEXTIME");
            rw.setErrorCode(-1);
            rw.setErrorMessages(List.of(validationMessage));
            return ResponseEntity.ok(rw);
        }
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
            saveBinAndLot(marketAuctionResponse, marketAuction);

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
    //@Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.SERIALIZABLE,rollbackFor = Exception.class)
    private void saveBinAndLot(MarketAuctionResponse marketAuctionResponse, MarketAuction marketAuction) {

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try{

            entityManager.getTransaction().begin();
            LocalDateTime tStart = LocalDateTime.now();
            Map<String, List<Integer>> allotedBins = saveBin(marketAuction.getId(), marketAuction.getNumberOfSmallBin(), marketAuction.getNumberOfBigBin(), marketAuction.getMarketId(), marketAuction.getGodownId(),entityManager);

            marketAuctionResponse.setAllotedBigBinList(allotedBins.get("big"));
            marketAuctionResponse.setAllotedSmallBinList(allotedBins.get("small"));

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
    private List<Integer> saveLot(BigInteger id, int numberOfLot, int marketId, int godownId,int estimatedWeight,EntityManager entityManager) {
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
            lots.add(lot);
            entityManager.persist(lot);
        }
        //lotRepository.saveAll(lots);

        return lotList;
    }


    private Map<String, List<Integer>> saveBin(BigInteger marketAuctionId, int numberOfSmallBin, int numberOfBigBin, int marketId, int godownId,EntityManager entityManager) {
        BinCounter bc = null;
        Map<String, List<Integer>> allotedBins = new HashMap<>();
        int smallSequenceEnd = 0;
        int bigSequenceEnd = 0;
        int smallBinStart = 0;
        int bigBinStart = 0;
        List<Integer> smallBins = new ArrayList<>();
        List<Integer> smallAlloted = new ArrayList<>();
        List<Bin> binList = new ArrayList<>();
        bc = binCounterRepository.findByMarketIdAndGodownIdAndAuctionDate(marketId, godownId, Util.getISTLocalDate());
        // in case its null its inserts the new record in separate transaction locking the row, thus syncing the process allowing only one row.
        if (bc == null) {
           bc = checkAndInsertForMaster(marketId, godownId);

        }
        // locks the row for that market for all
        LocalDateTime tStart = LocalDateTime.now();
       // bc = binCounterRepository.getByMarketEntryForTheDayLocked(bc.getId().longValue());

       Query q= entityManager.createNativeQuery("select * from bin_counter with(ROWLOCK,XLOCK) where bin_counter_id= ?1",BinCounter.class);
       q.setParameter(1,bc.getId().longValue());

       //List<Object> bc = q.getResultList();
        bc = (BinCounter) q.getSingleResult();

     //  bc = (BinCounter) bcList.get(0);


        log.info("total time  acquire lock "+ ChronoUnit.MILLIS.between(tStart,LocalDateTime.now()));
        smallBinStart = bc.getSmallBinNextNumber();
        bigBinStart = bc.getBigBinNextNumber();

        BinCounterMaster binCounterMaster = binCounterMasterRepository.findByMarketIdAndGodownId(marketId, godownId);
        //Master fetched to get the last count /** NOT REQUIRED TO LOCK THE MASTER FOR NOW */
        //BinCounterMaster binCounterMaster = binCounterMasterRepository.getByMarketIdAndAuction(byMarketIdAndGodownId.getId());


        if(numberOfSmallBin>0){
            smallSequenceEnd = saveEachTypeOfBin(marketAuctionId, marketId, godownId, "small", smallBinStart, binCounterMaster.getSmallBinEnd(), numberOfSmallBin, allotedBins,entityManager);
        }
        if(numberOfBigBin>0){
            bigSequenceEnd = saveEachTypeOfBin(marketAuctionId, marketId, godownId, "big", bigBinStart, binCounterMaster.getBigBinEnd(), numberOfBigBin, allotedBins,entityManager);
        }

        if(bc==null){
            bc = new BinCounter();
            bc.setMarketId(marketId);
            bc.setGodownId(godownId);
            bc.setAuctionDate(Util.getISTLocalDate());
        }

        if(numberOfBigBin>0)
            bc.setBigBinNextNumber(++bigSequenceEnd);
        if(numberOfSmallBin>0)
            bc.setSmallBinNextNumber(++smallSequenceEnd);
        //binCounterRepository.save(bc);

        entityManager.merge(bc);
        entityManager.persist(bc);

        return allotedBins;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    private BinCounter checkAndInsertForMaster( int marketId, int godownId) {
        //fetch pk to lock only that row
        BinCounterMaster byMarketIdAndGodownId = binCounterMasterRepository.findByMarketIdAndGodownId(marketId, godownId);
        //Master fetched to get the last count
        BinCounterMaster binCounterMaster = binCounterMasterRepository.getByMarketIdAndAuction(byMarketIdAndGodownId.getId());
        BinCounter bc = binCounterRepository.findByMarketIdAndGodownIdAndAuctionDate(marketId, godownId, Util.getISTLocalDate());
        if(Objects.isNull(bc)) {
            int smallBinStart = binCounterMaster.getSmallBinStart();
            int bigBinStart = binCounterMaster.getBigBinStart();
            bc = new BinCounter();
            bc.setBigBinNextNumber(smallBinStart);
            bc.setBigBinNextNumber(bigBinStart);
            bc.setMarketId(marketId);
            bc.setGodownId(godownId);
            bc.setAuctionDate(Util.getISTLocalDate());
            binCounterRepository.save(bc);
        }
        return bc;
    }

    private int saveEachTypeOfBin(BigInteger marketAuctionId, int marketId, int godownId, String type, int binStart, int binEnd, int limit, Map<String, List<Integer>> allotedBins,EntityManager entityManager) {
        List<Integer> bins = binMasterRepository.
                findByMarketIdAndGodownIdAndTypeAndStatusAndBinNumber(marketId, godownId, type, "available", binStart, binEnd, limit);
        int nextSequence = 0;
        List<Integer> allotedList = new ArrayList<>();
        List<Bin> binList = new ArrayList<>();
        if(bins==null || bins.isEmpty() || bins.size()!=limit){
            throw new ValidationException("No sufficient bins available");
        }
        for (int i = 0; i < limit; i++) {
            Bin bin = new Bin(bins.get(i), marketAuctionId, type,marketId);
            bin.setAuctionDate(Util.getISTLocalDate());
            binList.add(bin);
            nextSequence = bins.get(i);
            allotedList.add(nextSequence);
            entityManager.persist(bin);
        }
        //binRepository.saveAll(binList);
        allotedBins.put(type, allotedList);
        return nextSequence;
    }

    public List<MarketAuctionResponse> getAuctionDetailsByFarmerForAuctionDate(SearchMarketByFarmerAndAuctionDateRequest searchMarketByFarmerAndAuctionDateRequest){
        List<MarketAuctionResponse> marketAuctionResponseList = new ArrayList<>();
        List<MarketAuction> marketAuctionList = marketAuctionRepository.findAllByFarmerIdAndMarketAuctionDate(searchMarketByFarmerAndAuctionDateRequest.getFarmerId(), searchMarketByFarmerAndAuctionDateRequest.getAuctionDate());
        if(marketAuctionList!=null && !marketAuctionList.isEmpty()){
            prepareMarketResponse(marketAuctionList,marketAuctionResponseList);
        }
        return marketAuctionResponseList;
    }

    public List<MarketAuctionResponse> getAuctionDetailsByStateForAuctionDate(SearchMarketByStatusAndAuctionDateRequest marketAuctionRequest){
        List<MarketAuctionResponse> marketAuctionResponseList = new ArrayList<>();
        List<MarketAuction> marketAuctionList = marketAuctionRepository.findAllByStatusAndMarketAuctionDate(marketAuctionRequest.getStatus(),marketAuctionRequest.getAuctionDate());
        if(marketAuctionList!=null && !marketAuctionList.isEmpty()){
            prepareMarketResponse(marketAuctionList,marketAuctionResponseList);
        }
        return marketAuctionResponseList;
    }

    private void prepareMarketResponse(List<MarketAuction> marketAuctionList,List<MarketAuctionResponse> marketAuctionResponseList){

        for(MarketAuction marketAuction: marketAuctionList){
            MarketAuctionResponse marketAuctionResponse = new MarketAuctionResponse();
            marketAuctionResponse.setFarmerId(marketAuction.getFarmerId());
            marketAuctionResponse.setTransactionId(marketAuction.getId());
            marketAuctionResponse.setAllotedBigBinList(binRepository.findAllByMarketAuctionIdAndType(marketAuction.getId(),"big"));
            marketAuctionResponse.setAllotedSmallBinList(binRepository.findAllByMarketAuctionIdAndType(marketAuction.getId(),"small"));
            marketAuctionResponse.setAllotedLotList(lotRepository.findAllAllottedLotsByMarketAuctionId(marketAuction.getId()));
            marketAuctionResponse.setMarketId(marketAuction.getMarketId());
            marketAuctionResponse.setGodownId(marketAuction.getGodownId());
            marketAuctionResponseList.add(marketAuctionResponse);
        }

    }

    @Transactional
    public boolean cancelBidByFarmerId(CancelAuctionByFarmerIdRequest cancellationRequest){
        try{
            MarketAuction marketAuction = marketAuctionRepository.findById(cancellationRequest.getAuctionId());
            JwtPayloadData token = marketAuctionHelper.getMOAuthToken(cancellationRequest);

            marketAuction.setStatus(LotStatus.CANCELLED.getLabel());
            marketAuction.setReasonForCancellation(cancellationRequest.getCancellationReason());

            marketAuctionRepository.save(marketAuction);
            List<Lot> lotList = lotRepository.findAllByMarketAuctionId(cancellationRequest.getAuctionId());

            for(Lot lot:lotList){
                lot.setStatus(LotStatus.CANCELLED.getLabel());
                lot.setReasonForCancellation(cancellationRequest.getCancellationReason());
                lot.setRejectedBy(token.getUsername());
            }
            lotRepository.saveAll(lotList);

        }catch (Exception ex){
            return false;
        }

        return true;

    }


}

