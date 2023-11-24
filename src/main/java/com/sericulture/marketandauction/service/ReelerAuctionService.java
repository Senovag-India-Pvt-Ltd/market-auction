package com.sericulture.marketandauction.service;


import com.sericulture.marketandauction.model.api.marketauction.ReelerAuctionRequest;
import com.sericulture.marketandauction.model.entity.ReelerAuction;
import com.sericulture.marketandauction.model.mapper.Mapper;
import com.sericulture.marketandauction.repository.ReelerAuctionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Slf4j
public class ReelerAuctionService {

    @Autowired
    ReelerAuctionRepository reelerAuctionRepository;

    @Autowired
    Mapper mapper;

    @Autowired
    private CustomValidator validator;

    @Transactional
    public boolean submitbid(ReelerAuctionRequest reelerAuctionRequest){
        try{
            ReelerAuction reelerAuction = mapper.reelerAuctionObjectToEntity(reelerAuctionRequest,ReelerAuction.class);
            validator.validate(reelerAuction);
            reelerAuction.setAuctionDate(LocalDate.now());
            reelerAuctionRepository.save(reelerAuction);
        }catch (Exception ex){
            return false;
        }
        return true;
    }
}
