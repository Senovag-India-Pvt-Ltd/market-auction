package com.sericulture.marketandauction.service;

import com.sericulture.marketandauction.model.api.marketauction.MarketAuctionRequest;
import com.sericulture.marketandauction.model.api.marketauction.MarketAuctionResponse;
import com.sericulture.marketandauction.model.entity.*;
import com.sericulture.marketandauction.model.mapper.Mapper;
import com.sericulture.marketandauction.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Transactional
    public MarketAuctionResponse saveMarketAuction(MarketAuctionRequest marketAuctionRequest) {

        MarketAuctionResponse marketAuctionResponse = new MarketAuctionResponse();

        MarketAuction marketAuction = mapper.marketAuctionObjectToEntity(marketAuctionRequest, MarketAuction.class);
        validator.validate(marketAuction);
        marketAuction.setMarketAuctionDate(LocalDate.now());
        marketAuction.setStatus("generated");

        marketAuction = marketAuctionRepository.save(marketAuction);

        marketAuctionResponse.setTransactionId(marketAuction.getId());
        marketAuctionResponse.setMarketId(marketAuction.getMarketId());
        marketAuctionResponse.setGodownId(marketAuction.getGodownId());
        marketAuctionResponse.setFarmerId(marketAuction.getFarmerId());


        Map<String, List<Integer>> allotedBins = saveBin(marketAuction.getId(), marketAuction.getNumberOfSmallBin(), marketAuction.getNumberOfBigBin(), marketAuction.getMarketId(), marketAuction.getGodownId());

        marketAuctionResponse.setAllotedBigBinList(allotedBins.get("big"));
        marketAuctionResponse.setAllotedSmallBinList(allotedBins.get("small"));

        List<Integer> lotList = saveLot(marketAuction.getId(), marketAuction.getNumberOfLot(), marketAuction.getMarketId(), marketAuction.getGodownId());
        marketAuctionResponse.setAllotedLotList(lotList);

        return marketAuctionResponse;

    }

    private List<Integer> saveLot(BigInteger id, int numberOfLot, int marketId, int godownId) {
        List<Integer> lotList = new ArrayList<>();
        Integer lotCounter = 0;
        lotCounter = lotRepository.findByMarketIdAndGodownIdAndAuctionDate(marketId, godownId, LocalDate.now());
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
            if (godownId != 0) {
                lot.setGodownId(godownId);
            }
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
        BinCounterMaster binCounterMaster = binCounterMasterRepository.findByMarketIdAndGodownId(marketId, godownId);
        smallBinStart = binCounterMaster.getSmallBinStart();
        bigBinStart = binCounterMaster.getBigBinStart();

        if (bc != null) {
            smallBinStart = bc.getSmallBinNextNumber();
            bigBinStart = bc.getBigBinNextNumber();
        }else{
            smallBinStart--;
            bigBinStart--;
        }

        smallSequenceEnd = saveEachTypeOfBin(marketAuctionId, marketId, godownId, "small", smallBinStart, binCounterMaster.getSmallBinEnd(), numberOfSmallBin, allotedBins);

        bigSequenceEnd = saveEachTypeOfBin(marketAuctionId, marketId, godownId, "big", bigBinStart, binCounterMaster.getBigBinEnd(), numberOfBigBin, allotedBins);

        if(bc==null){
            bc = new BinCounter();
            bc.setMarketId(marketId);
            bc.setGodownId(godownId);
            bc.setAuctionDate(LocalDate.now());
        }
        if(numberOfBigBin!=0)
        bc.setBigBinNextNumber(bigSequenceEnd);
        if(numberOfSmallBin!=0)
        bc.setSmallBinNextNumber(smallSequenceEnd);
        binCounterRepository.save(bc);
        binRepository.saveAll(binList);
        return allotedBins;
    }

    public int saveEachTypeOfBin(BigInteger marketAuctionId, int marketId, int godownId, String type, int binStart, int binEnd, int limit, Map<String, List<Integer>> allotedBins) {
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
            marketAuctionResponse.setAllotedLotList(lotRepository.findAllByMarketAuctionId(marketAuction.getId()));
            marketAuctionResponse.setMarketId(marketAuction.getMarketId());
            marketAuctionResponse.setGodownId(marketAuction.getGodownId());
            marketAuctionResponseList.add(marketAuctionResponse);
        }

    }
}

