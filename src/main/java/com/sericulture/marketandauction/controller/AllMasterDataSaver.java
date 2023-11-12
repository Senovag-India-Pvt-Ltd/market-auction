package com.sericulture.marketandauction.controller;

import com.sericulture.marketandauction.service.AllMasterDataSaverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/addAllMaster")
public class AllMasterDataSaver {

    @Autowired
    AllMasterDataSaverService allMasterDataSaverService;

    @Autowired
    MessageSource messageSource;


    @PostMapping("/addMarket")
    public String getEducationDetails(String name, String adress, float weight, int lotWeight,int stateId,int districtId,int talukId) {
        allMasterDataSaverService.saveMarketMaster(name,adress,weight,lotWeight,stateId,districtId,talukId);
        return "success";
    }

    @PostMapping("/addGodown")
    public String addGoDownMaster(String name,int marketId){
        allMasterDataSaverService.saveGodownMaster(name,marketId);
        return "success";
    }

    @PostMapping("/addBinMaster")
    public String addBinMaster(String type,int marketId,int godownId,int startNumber,int EndNumber){
        allMasterDataSaverService.saveBinMaster(type,marketId,godownId,startNumber,EndNumber);
        return "success";
    }

    @PostMapping("/addBinCounterMaster")
    public String saveBinCounterMaster(int bigBinstart,int bigBinEnd,int smallBinStart,int smallBinEnd,int marketId,int godownId){
        allMasterDataSaverService.saveBinCounterMaster(bigBinstart,bigBinEnd,smallBinStart,smallBinEnd,marketId,godownId);
        return "success";
    }

}
