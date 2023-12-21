package com.sericulture.marketandauction.service;


import com.sericulture.marketandauction.helper.MarketAuctionHelper;
import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.api.marketauction.*;
import com.sericulture.marketandauction.model.entity.Lot;
import com.sericulture.marketandauction.model.entity.ReelerAuction;
import com.sericulture.marketandauction.model.exceptions.MessageLabelType;
import com.sericulture.marketandauction.model.exceptions.ValidationMessage;
import com.sericulture.marketandauction.model.mapper.Mapper;
import com.sericulture.marketandauction.repository.LotRepository;
import com.sericulture.marketandauction.repository.ReelerAuctionRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

            Lot lot = lotRepository.findByMarketIdAndAllottedLotIdAndAuctionDate(reelerBidRequest.getMarketId(), reelerBidRequest.getAllottedLotId(), LocalDate.now());

            if (lot == null) {
                ValidationMessage validationMessage = new ValidationMessage(MessageLabelType.NON_LABEL_MESSAGE.name(), "lot not found", "-1");
                rw.setErrorCode(-1);
                rw.setErrorMessages(List.of(validationMessage));
                return ResponseEntity.ok(rw);
            }

            if (lot.getStatus() != null && !lot.getStatus().trim().isEmpty()) {
                ValidationMessage validationMessage = new ValidationMessage(MessageLabelType.NON_LABEL_MESSAGE.name(), "cannot bid as the status is: " + lot.getStatus(), "-1");
                rw.setErrorCode(-1);
                rw.setErrorMessages(List.of(validationMessage));
                return ResponseEntity.ok(rw);
            }

            boolean canIssue = marketAuctionHelper.canPerformInAnyOneAuction(reelerBidRequest.getMarketId(), reelerBidRequest.getGodownId());

            if (!canIssue) {
                ValidationMessage validationMessage = new ValidationMessage(MessageLabelType.NON_LABEL_MESSAGE.name(), "Cannot accept bid as time either over or not started", "-1");
                rw.setErrorCode(-1);
                rw.setErrorMessages(List.of(validationMessage));
                return ResponseEntity.ok(rw);
            }

            ReelerAuction reelerAuction = mapper.reelerAuctionObjectToEntity(reelerBidRequest, ReelerAuction.class);
            validator.validate(reelerAuction);
            reelerAuction.setAuctionDate(LocalDate.now());
            reelerAuctionRepository.save(reelerAuction);
        } catch (Exception ex) {
            ValidationMessage validationMessage = new ValidationMessage(MessageLabelType.NON_LABEL_MESSAGE.name(), "error occurred while submitting bid", "-1");
            rw.setErrorCode(-1);
            rw.setErrorMessages(List.of(validationMessage));

            return ResponseEntity.ok(rw);
        }
        return ResponseEntity.ok(rw);
    }

    public ResponseEntity<?> getHighestBidPerLot(LotStatusRequest lotStatusRequest) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(GetHighestBidPerLotResponse.class);

        ReelerAuction ra = reelerAuctionRepository.getHighestBidForLot(lotStatusRequest.getAllottedLotId(), lotStatusRequest.getMarketId(), LocalDate.now());
        GetHighestBidPerLotResponse getHighestBidPerLotResponse = new GetHighestBidPerLotResponse();
        getHighestBidPerLotResponse.setAllottedLotId(ra.getAllottedLotId());
        if (ra != null) {
            getHighestBidPerLotResponse.setHighestBidAmount(ra.getAmount());
        }
        rw.setContent(getHighestBidPerLotResponse);
        return ResponseEntity.ok(rw);

    }

    public ResponseEntity<?> getHighestBidPerLotDetails(LotStatusRequest lotStatusRequest) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(LotBidDetailResponse.class);

        ReelerAuction ra = reelerAuctionRepository.getHighestBidForLot(lotStatusRequest.getAllottedLotId(), lotStatusRequest.getMarketId(), LocalDate.now());
        LotBidDetailResponse lbdr = new LotBidDetailResponse();
        lbdr.setAllottedlotid(lotStatusRequest.getAllottedLotId());
        if (ra != null) {
            Object[][] reelerDetails = reelerAuctionRepository.getReelerDetailsForHighestBid(ra.getId());
            if (reelerDetails == null || reelerDetails.length == 0) {
                rw.setErrorCode(-1);
                rw.setErrorMessages(List.of("No reeler found please check for allotedLotId:" + lotStatusRequest.getAllottedLotId() + "and market: " + lotStatusRequest.getMarketId()));

                return ResponseEntity.ok(rw);
            }

            lbdr.setAmount(ra.getAmount());
            lbdr.setReelerAuctionId(ra.getId());
            Object[][] ldrDetails = reelerAuctionRepository.getLotBidDetailResponse(lotStatusRequest.getAllottedLotId(), LocalDate.now(), lotStatusRequest.getMarketId());
            if (ldrDetails == null || ldrDetails.length == 0) {
                rw.setErrorCode(-1);
                rw.setErrorMessages(List.of("No farmer details found please check for allotedLotId:" + lotStatusRequest.getAllottedLotId() + "and market: " + lotStatusRequest.getMarketId()));

                return ResponseEntity.ok(rw);
            }
            lbdr.setFarmerFirstName(ldrDetails[0][0] == null ? "" : String.valueOf(ldrDetails[0][0]));
            lbdr.setFarmerMiddleName(ldrDetails[0][1] == null ? "" : String.valueOf(ldrDetails[0][1]));
            lbdr.setFarmerLastName(ldrDetails[0][2] == null ? "" : String.valueOf(ldrDetails[0][2]));
            lbdr.setFarmerNumber(ldrDetails[0][3] == null ? "" : String.valueOf(ldrDetails[0][3]));
            lbdr.setReelerName(reelerDetails[0][0] == null ? "" : String.valueOf(reelerDetails[0][0]));
            lbdr.setReelerFruitsId(reelerDetails[0][1] == null ? "" : String.valueOf(reelerDetails[0][1]));
            lbdr.setFarmervillageName(ldrDetails[0][4] == null ? "" : String.valueOf(ldrDetails[0][4]));
            lbdr.setLotApproxWeightBeforeWeighment(ldrDetails[0][5] == null ? 0 : Integer.valueOf(String.valueOf(ldrDetails[0][5])));
            lbdr.setStatus(ldrDetails[0][6] == null ? "" : String.valueOf(ldrDetails[0][6]));
            lbdr.setBidAcceptedBy(ldrDetails[0][7] == null ? "" : String.valueOf(ldrDetails[0][7]));
        }
        rw.setContent(lbdr);
        return ResponseEntity.ok(rw);

    }

    @Transactional
    public ResponseEntity<?> acceptReelerBidForGivenLot(ReelerBidAcceptRequest lotStatusRequest) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        ReelerAuction reelerAuction = reelerAuctionRepository.getHighestBidForLot(lotStatusRequest.getAllottedLotId(),lotStatusRequest.getMarketId(),LocalDate.now());
        if (reelerAuction != null) {
            Lot l = lotRepository.findByMarketIdAndAllottedLotIdAndAuctionDate(reelerAuction.getMarketId(), reelerAuction.getAllottedLotId(), LocalDate.now());
            l.setStatus("accepted");
            l.setReelerAuctionId(reelerAuction.getId());
            l.setBidAcceptedBy(lotStatusRequest.getBidAcceptedBy());
            reelerAuction.setStatus("accepted");
            lotRepository.save(l);
            reelerAuctionRepository.save(reelerAuction);
        } else {

            ValidationMessage validationMessage = new ValidationMessage(MessageLabelType.NON_LABEL_MESSAGE.name(), "no Reeler auction found", "-1");
            rw.setErrorCode(-1);
            rw.setErrorMessages(List.of(validationMessage));
        }
        return ResponseEntity.ok(rw);
    }


    public ResponseEntity<?> getReelerLotWithHighestBidDetails(@RequestBody ReelerLotRequest reelerLotRequest) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        List<Integer> reelerLotList = reelerAuctionRepository.findByAuctionDateAndMarketIdAndReelerId(LocalDate.now(), reelerLotRequest.getMarketId(), reelerLotRequest.getReelerId());
        Object[][] reelerLotHighestAndHisBidList = reelerAuctionRepository.getHighestAndReelerBidAmountForLotList(LocalDate.now(), reelerLotRequest.getMarketId(), reelerLotList, reelerLotRequest.getReelerId());
        Map<Integer, ReelerLotResponse> reelerLotResponseMap = new HashMap<>();

        if (reelerLotHighestAndHisBidList != null && reelerLotHighestAndHisBidList.length > 0) {
            for (Object[] bidDetail : reelerLotHighestAndHisBidList) {
                boolean notFound = true;
                int allottedLot = Integer.parseInt(String.valueOf(bidDetail[2]));
                int bidAmount = Integer.parseInt(String.valueOf(bidDetail[1]));
                BigInteger reelerAuctionId = BigInteger.valueOf(Long.parseLong(String.valueOf(bidDetail[0])));

                ReelerLotResponse reelerLotResponse = reelerLotResponseMap.get(allottedLot);
                if (reelerLotResponse == null) {
                    reelerLotResponse = new ReelerLotResponse();
                    reelerLotResponseMap.put(allottedLot, reelerLotResponse);
                }
                setReelerLotResponse(reelerLotResponse, allottedLot, String.valueOf(bidDetail[3]), bidAmount, reelerAuctionId);
            }
        }
        rw.setContent(reelerLotResponseMap.values());
        return ResponseEntity.ok(rw);
    }

    private void setReelerLotResponse(ReelerLotResponse reelerLotResponse, int allottedLot, String highest, int bidAmount, BigInteger reelerAuctionId) {
        reelerLotResponse.setAllottedLotId(allottedLot);
        if (reelerLotResponse.getReelerAuctionId() != null && reelerLotResponse.getReelerAuctionId().equals(reelerAuctionId)) {
            reelerLotResponse.setAwarded(true);
        }
        if (highest.equals("HIGHEST")) {
            reelerLotResponse.setHighestBidAmount(bidAmount);
            if (reelerLotResponse.getReelerAuctionId() == null) {
                reelerLotResponse.setReelerAuctionId(reelerAuctionId);
            }
        } else {
            reelerLotResponse.setReelerAuctionId(reelerAuctionId);
            reelerLotResponse.setMyBidAmount(bidAmount);
        }
    }

    @Transactional
    public ResponseEntity<?> removeReelerHighestBid(RemoveReelerHighestBidRequest removeReelerHighestBidRequest) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        log.warn("Removing the reeler Bid for:"+removeReelerHighestBidRequest);
        long deletedrows = reelerAuctionRepository.deleteByIdAndMarketIdAndAllottedLotIdAndReelerId(removeReelerHighestBidRequest.getReelerAuctionId(),removeReelerHighestBidRequest.getMarketId(), removeReelerHighestBidRequest.getAllottedLotId(), removeReelerHighestBidRequest.getReelerId());
        return ResponseEntity.ok(rw);
    }

    private void setReelerLotResponse(ReelerLotResponse reelerLotResponse,int allottedLot,String highest,int bidAmount){
        reelerLotResponse.setAllottedLotId(allottedLot);
        if(highest.equals("h")){
            reelerLotResponse.setHighestBidAmount(bidAmount);
        }else{
            reelerLotResponse.setMyBidAmount(bidAmount);
        }
    }
}




