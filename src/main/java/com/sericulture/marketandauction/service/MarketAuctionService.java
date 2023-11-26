package com.sericulture.marketandauction.service;

import com.sericulture.marketandauction.helper.MarketAuctionHelper;
import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.api.marketauction.CancellationRequest;
import com.sericulture.marketandauction.model.api.marketauction.MarketAuctionRequest;
import com.sericulture.marketandauction.model.api.marketauction.MarketAuctionResponse;
import com.sericulture.marketandauction.model.entity.*;
import com.sericulture.marketandauction.model.mapper.Mapper;
import com.sericulture.marketandauction.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.LocalDate;
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
    private CustomValidator validator;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private MarketAuctionHelper marketAuctionHelper;
    public  ResponseEntity<?> marketAuctionFacade(MarketAuctionRequest marketAuctionRequest) {

        ResponseWrapper rw = ResponseWrapper.createWrapper(MarketAuctionResponse.class);
        MarketAuctionResponse marketAuctionResponse = new MarketAuctionResponse();
        //validator.validate(marketAuctionResponse);
        boolean canIssue = marketAuctionHelper.canPerformActivity(MarketAuctionHelper.activityType.ISSUEBIDSLIP,marketAuctionRequest.getMarketId());

        if(!canIssue){
            rw.setErrorCode(-1);
            rw.setErrorMessages(List.of("Cannot issue slip as time is over"));
            return ResponseEntity.ok(rw);
        }
        boolean hasException = false;
        MarketAuction marketAuction = null;
        try {
            marketAuction = saveMarketAuction(marketAuctionRequest);
            marketAuctionResponse.setTransactionId(marketAuction.getId());
            marketAuctionResponse.setMarketId(marketAuction.getMarketId());
            marketAuctionResponse.setGodownId(marketAuction.getGodownId());
            marketAuctionResponse.setFarmerId(marketAuction.getFarmerId());
            // saves bin and the lot
            saveBinAndLot(marketAuctionResponse, marketAuction);
        } catch (Exception e) {
            hasException = true;
            e.printStackTrace();
            log.error("Error occurred while processing the request %s", marketAuctionRequest);
        } finally {
            if(Objects.nonNull(marketAuction)) {
                if(hasException) {
                    marketAuction.setStatus("error");
                } else {
                    marketAuction.setStatus("generated");
                }
                marketAuctionRepository.save(marketAuction);
            }
        }
        return ResponseEntity.ok(rw);
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private MarketAuction saveMarketAuction(MarketAuctionRequest marketAuctionRequest) {
        MarketAuction marketAuction = mapper.marketAuctionObjectToEntity(marketAuctionRequest, MarketAuction.class);
        validator.validate(marketAuction);
        marketAuction.setMarketAuctionDate(LocalDate.now());
        marketAuction.setStatus("in creation");

        return marketAuctionRepository.save(marketAuction);
    }

    /** Runs ina single transaction to allocate bins and lot together rollbacks if there are any exception during the process
     *
     * @param marketAuctionResponse
     * @param marketAuction
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    private void saveBinAndLot(MarketAuctionResponse marketAuctionResponse, MarketAuction marketAuction) {
        Map<String, List<Integer>> allotedBins = saveBin(marketAuction.getId(), marketAuction.getNumberOfSmallBin(), marketAuction.getNumberOfBigBin(), marketAuction.getMarketId(), marketAuction.getGodownId());

        marketAuctionResponse.setAllotedBigBinList(allotedBins.get("big"));
        marketAuctionResponse.setAllotedSmallBinList(allotedBins.get("small"));

        List<Integer> lotList = saveLot(marketAuction.getId(), marketAuction.getNumberOfLot(), marketAuction.getMarketId(), marketAuction.getGodownId());
        marketAuctionResponse.setAllotedLotList(lotList);

    }
    private List<Integer> saveLot(BigInteger id, int numberOfLot, int marketId, int godownId) {
        List<Integer> lotList = new ArrayList<>();
        Integer lotCounter = 0;
        lotCounter = lotRepository.findByMarketIdAndAuctionDate(marketId, LocalDate.now());
        if (lotCounter == null) {
            lotCounter = 0;
        }
        List<Lot> lots = new ArrayList<>();
        for (int i = 0; i < numberOfLot; i++) {
            Lot lot = new Lot();
            int allotedLot = lotCounter + 1 + i;
            lot.setAllottedLotId(allotedLot);
            lotList.add(allotedLot);
            lot.setMarketAuctionId(id);
            lot.setMarketId(marketId);
            lot.setAuctionDate(LocalDate.now());
            lots.add(lot);
        }
        lotRepository.saveAll(lots);
        return lotList;
    }


    private Map<String, List<Integer>> saveBin(BigInteger marketAuctionId, int numberOfSmallBin, int numberOfBigBin, int marketId, int godownId) {
        BinCounter bc = null;
        Map<String, List<Integer>> allotedBins = new HashMap<>();
        int smallSequenceEnd = 0;
        int bigSequenceEnd = 0;
        int smallBinStart = 0;
        int bigBinStart = 0;
        List<Integer> smallBins = new ArrayList<>();
        List<Integer> smallAlloted = new ArrayList<>();
        List<Bin> binList = new ArrayList<>();
        bc = binCounterRepository.findByMarketIdAndGodownIdAndAuctionDate(marketId, godownId, LocalDate.now());
        // in case its null its inserts the new record in separate transaction locking the row, thus syncing the process allowing only one row.
        if (bc == null) {
            checkAndInsertForMaster(marketId, godownId);

        }
        // locks the row for that market for all
        bc = binCounterRepository.findByMarketIdAndGodownIdAndAuctionDate(marketId, godownId, LocalDate.now());
        bc = binCounterRepository.getByMarketEntryForTheDayLocked(bc.getId().longValue());
        smallBinStart = bc.getSmallBinNextNumber();
        bigBinStart = bc.getBigBinNextNumber();

        BinCounterMaster binCounterMaster = binCounterMasterRepository.findByMarketIdAndGodownId(marketId, godownId);
        //Master fetched to get the last count /** NOT REQUIRED TO LOCK THE MASTER FOR NOW */
        //BinCounterMaster binCounterMaster = binCounterMasterRepository.getByMarketIdAndAuction(byMarketIdAndGodownId.getId());


        smallSequenceEnd = saveEachTypeOfBin(marketAuctionId, marketId, godownId, "small", smallBinStart, binCounterMaster.getSmallBinEnd(), numberOfSmallBin, allotedBins);

        bigSequenceEnd = saveEachTypeOfBin(marketAuctionId, marketId, godownId, "big", bigBinStart, binCounterMaster.getBigBinEnd(), numberOfBigBin, allotedBins);

        if(bc==null){
            bc = new BinCounter();
            bc.setMarketId(marketId);
            bc.setGodownId(godownId);
            bc.setAuctionDate(LocalDate.now());
        }
        if(numberOfBigBin!=0)
            bc.setBigBinNextNumber(++bigSequenceEnd);
        if(numberOfSmallBin!=0)
            bc.setSmallBinNextNumber(++smallSequenceEnd);
        binCounterRepository.save(bc);
        binRepository.saveAll(binList);
        return allotedBins;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    private void checkAndInsertForMaster( int marketId, int godownId) {
        //fetch pk to lock only that row
        BinCounterMaster byMarketIdAndGodownId = binCounterMasterRepository.findByMarketIdAndGodownId(marketId, godownId);
        //Master fetched to get the last count
        BinCounterMaster binCounterMaster = binCounterMasterRepository.getByMarketIdAndAuction(byMarketIdAndGodownId.getId());
        BinCounter bc = binCounterRepository.findByMarketIdAndGodownIdAndAuctionDate(marketId, godownId, LocalDate.now());
        if(Objects.isNull(bc)) {
            int smallBinStart = binCounterMaster.getSmallBinStart();
            int bigBinStart = binCounterMaster.getBigBinStart();
            bc = new BinCounter();
            bc.setBigBinNextNumber(smallBinStart);
            bc.setBigBinNextNumber(bigBinStart);
            bc.setMarketId(marketId);
            bc.setGodownId(godownId);
            bc.setAuctionDate(LocalDate.now());
            binCounterRepository.save(bc);
        }
    }

    private int saveEachTypeOfBin(BigInteger marketAuctionId, int marketId, int godownId, String type, int binStart, int binEnd, int limit, Map<String, List<Integer>> allotedBins) {
        List<Integer> bins = binMasterRepository.
                findByMarketIdAndGodownIdAndTypeAndStatusAndBinNumber(marketId, godownId, type, "available", binStart, binEnd, limit);
        int nextSequence = 0;
        List<Integer> allotedList = new ArrayList<>();
        List<Bin> binList = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            Bin bin = new Bin(bins.get(i), marketAuctionId, type);
            bin.setAuctionDate(LocalDate.now());
            binList.add(bin);
            nextSequence = bins.get(i);
            allotedList.add(nextSequence);
        }
        binRepository.saveAll(binList);
        allotedBins.put(type, allotedList);
        return nextSequence;
    }

    public List<MarketAuctionResponse> getAuctionDetailsByFarmerForAuctionDate(MarketAuctionRequest marketAuctionRequest){
        List<MarketAuctionResponse> marketAuctionResponseList = new ArrayList<>();
        List<MarketAuction> marketAuctionList = marketAuctionRepository.findAllByFarmerIdAndMarketAuctionDate(marketAuctionRequest.getFarmerId(),marketAuctionRequest.getMarketAuctionDate());
        if(marketAuctionList!=null && !marketAuctionList.isEmpty()){
            prepareMarketResponse(marketAuctionList,marketAuctionResponseList);
        }
        return marketAuctionResponseList;
    }

    public List<MarketAuctionResponse> getAuctionDetailsByStateForAuctionDate(MarketAuctionRequest marketAuctionRequest){
        List<MarketAuctionResponse> marketAuctionResponseList = new ArrayList<>();
        List<MarketAuction> marketAuctionList = marketAuctionRepository.findAllByStatusAndMarketAuctionDate(marketAuctionRequest.getStatus(),marketAuctionRequest.getMarketAuctionDate());
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
    public boolean cancelBidByFarmerId(CancellationRequest cancellationRequest){
        try{
            MarketAuction marketAuction = marketAuctionRepository.findById(cancellationRequest.getAuctionId());

            marketAuction.setStatus("cancelled");
            marketAuction.setReasonForCancellation(cancellationRequest.getCancellationReason());

            marketAuctionRepository.save(marketAuction);
            List<Lot> lotList = lotRepository.findAllByMarketAuctionId(cancellationRequest.getAuctionId());

            for(Lot lot:lotList){
                lot.setStatus("cancelled");
                lot.setReasonForCancellation(cancellationRequest.getCancellationReason());
                lot.setRejectedBy("MO");
            }

            lotRepository.saveAll(lotList);

        }catch (Exception ex){
            return false;
        }

        return true;

    }

    @Transactional
    public boolean cancelLot(CancellationRequest cancellationRequest) {
        try {
            Lot lot = lotRepository.findByMarketIdAndAllottedLotId(cancellationRequest.getMarketId(), cancellationRequest.getAllottedLotId());
            lot.setStatus("cancelled");
            lot.setReasonForCancellation(cancellationRequest.getCancellationReason());
            lot.setRejectedBy("farmer");
            lotRepository.save(lot);

        }catch (Exception ex){
            return false;
        }

        return true;

    }
}

