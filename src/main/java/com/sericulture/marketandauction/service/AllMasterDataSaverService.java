package com.sericulture.marketandauction.service;

import com.sericulture.marketandauction.model.entity.BinCounterMaster;
import com.sericulture.marketandauction.model.entity.BinMaster;
import com.sericulture.marketandauction.model.entity.GodownMaster;
import com.sericulture.marketandauction.model.entity.MarketMaster;
import com.sericulture.marketandauction.repository.*;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class AllMasterDataSaverService {

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
    private MarketMasterRepository marketMasterRepository;

    @Autowired
    private GodownMasterRepository godownMasterRepository;

    @Autowired
    private BinCounterMasterRepository binCounterMasterRepository;

    public void saveMarketMaster(String name, String adress, float weight, int lotWeight,int stateId,int districtId,int talukId){
        MarketMaster marketMaster = new MarketMaster();
        marketMaster.setAddress(adress);
        marketMaster.setName(name);
        marketMaster.setBoxWeight(weight);
        marketMaster.setLotWeight(lotWeight);
        marketMaster.setStateId(stateId);
        marketMaster.setDistrictId(districtId);
        marketMaster.setTalukId(talukId);
        marketMasterRepository.save(marketMaster);
    }

    public void saveGodownMaster(String name,int marketId){
        GodownMaster godownMaster = new GodownMaster();
        godownMaster.setName(name);
        godownMaster.setMarketId(marketId);
        godownMasterRepository.save(godownMaster);
    }

    public void saveBinMaster(String type,int marketId,int godownId,int startNumber,int EndNumber){

        List<BinMaster> binMasterList = new ArrayList<>();

        for(int i=startNumber;i<=EndNumber;i++){
            BinMaster binMaster = new BinMaster();
            binMaster.setBinNumber(i);
            binMaster.setType(type);
            binMaster.setStatus("Available");
            binMaster.setMarketId(marketId);
            binMaster.setGodownId(godownId);
            binMasterList.add(binMaster);

        }
        binMasterRepository.saveAll(binMasterList);

    }

    public void saveBinCounterMaster(int bigBinstart,int bigBinEnd,int smallBinStart,int smallBinEnd,int marketId,int godownId){
        BinCounterMaster binCounterMaster = new BinCounterMaster();

        binCounterMaster.setBigBinStart(bigBinstart);
        binCounterMaster.setBigBinEnd(bigBinEnd);
        binCounterMaster.setSmallBinEnd(smallBinEnd);
        binCounterMaster.setSmallBinStart(smallBinStart);
        binCounterMaster.setMarketId(marketId);
        binCounterMaster.setGodownId(godownId);

        binCounterMasterRepository.save(binCounterMaster);
    }
}
