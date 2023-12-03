package com.sericulture.marketandauction.service;


import com.sericulture.marketandauction.helper.MarketAuctionHelper;
import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.api.marketauction.ReelerBidRequest;
import com.sericulture.marketandauction.model.entity.Lot;
import com.sericulture.marketandauction.model.entity.ReelerAuction;
import com.sericulture.marketandauction.model.exceptions.MessageLabelType;
import com.sericulture.marketandauction.model.exceptions.ValidationMessage;
import com.sericulture.marketandauction.model.mapper.Mapper;
import com.sericulture.marketandauction.repository.LotRepository;
import com.sericulture.marketandauction.repository.ReelerAuctionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
public class ReelerAuctionService {

    @Autowired
    ReelerAuctionRepository reelerAuctionRepository;

    @Autowired
    Mapper mapper;

    @Autowired
    private CustomValidator validator;

    @Autowired
    MarketAuctionHelper marketAuctionHelper;

    @Autowired
    LotRepository lotRepository;

    @Transactional
    public ResponseEntity<?> submitbid(ReelerBidRequest reelerBidRequest) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        try {
            boolean canIssue = marketAuctionHelper.canPerformActivity(MarketAuctionHelper.activityType.valueOf(reelerBidRequest.getAuctionNumber()), reelerBidRequest.getMarketId(), reelerBidRequest.getGodownId());

            if (!canIssue) {
                ValidationMessage validationMessage = new ValidationMessage(MessageLabelType.NON_LABEL_MESSAGE.name(),"Cannot accept bid as time either over or not started","-1");
                rw.setErrorCode(-1);
                rw.setErrorMessages(List.of(validationMessage));
                return ResponseEntity.ok(rw);
            }

            Lot lot = lotRepository.findByMarketIdAndAllottedLotIdAndAuctionDate(reelerBidRequest.getMarketId(), reelerBidRequest.getAllottedLotId(),LocalDate.now());

            if(lot==null){
                ValidationMessage validationMessage = new ValidationMessage(MessageLabelType.NON_LABEL_MESSAGE.name(),"lot not found","-1");
                rw.setErrorCode(-1);
                rw.setErrorMessages(List.of(validationMessage));
                return ResponseEntity.ok(rw);
            }

            if ("accepted".equals(lot.getStatus())) {
                ValidationMessage validationMessage = new ValidationMessage(MessageLabelType.NON_LABEL_MESSAGE.name(),"lot already accepted","-1");
                rw.setErrorCode(-1);
                rw.setErrorMessages(List.of(validationMessage));
                return ResponseEntity.ok(rw);
            }

            if ("cancelled".equals(lot.getStatus())) {
                ValidationMessage validationMessage = new ValidationMessage(MessageLabelType.NON_LABEL_MESSAGE.name(),"lot is cancelled","-1");
                rw.setErrorCode(-1);
                rw.setErrorMessages(List.of(validationMessage));
                return ResponseEntity.ok(rw);
            }

            ReelerAuction reelerAuction = mapper.reelerAuctionObjectToEntity(reelerBidRequest, ReelerAuction.class);
            validator.validate(reelerAuction);
            reelerAuction.setAuctionDate(LocalDate.now());
            reelerAuctionRepository.save(reelerAuction);
        } catch (Exception ex) {
            return ResponseEntity.ok(rw);
        }
        return ResponseEntity.ok(rw);
    }
}
