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
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import jakarta.persistence.Query;
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
public class SilkExchangeMarketService {

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



    public List<MarketAuctionResponse> getAuctionDetailsByReelerForAuctionDate(SearchMarketByFarmerAndAuctionDateRequest searchMarketByFarmerAndAuctionDateRequest){
        List<MarketAuctionResponse> marketAuctionResponseList = new ArrayList<>();
        List<MarketAuction> marketAuctionList = marketAuctionRepository.findAllByReelerIdAndMarketAuctionDate(searchMarketByFarmerAndAuctionDateRequest.getReelerId(), searchMarketByFarmerAndAuctionDateRequest.getAuctionDate());
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
            marketAuctionResponse.setReelerId(marketAuction.getReelerId());
            marketAuctionResponse.setTransactionId(marketAuction.getId());
            marketAuctionResponse.setAllotedBigBinList(binRepository.findAllByMarketAuctionIdAndType(marketAuction.getId(),"big"));
            marketAuctionResponse.setAllotedSmallBinList(binRepository.findAllByMarketAuctionIdAndType(marketAuction.getId(),"small"));
            marketAuctionResponse.setAllotedLotList(lotRepository.findAllAllottedLotsByMarketAuctionId(marketAuction.getId()));
            marketAuctionResponse.setMarketId(marketAuction.getMarketId());
            marketAuctionResponse.setGodownId(marketAuction.getGodownId());
            marketAuctionResponseList.add(marketAuctionResponse);
        }

    }

}

