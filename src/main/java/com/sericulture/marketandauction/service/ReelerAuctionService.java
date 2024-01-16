package com.sericulture.marketandauction.service;


import com.sericulture.marketandauction.helper.MarketAuctionHelper;
import com.sericulture.marketandauction.helper.Util;
import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.api.marketauction.*;
import com.sericulture.marketandauction.model.entity.Lot;
import com.sericulture.marketandauction.model.entity.ReelerAuction;
import com.sericulture.marketandauction.model.exceptions.MessageLabelType;
import com.sericulture.marketandauction.model.exceptions.ValidationMessage;
import com.sericulture.marketandauction.model.mapper.Mapper;
import com.sericulture.marketandauction.repository.LotRepository;
import com.sericulture.marketandauction.repository.ReelerAuctionRepository;
import jakarta.persistence.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigInteger;
import java.time.LocalDate;
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
    
    @Autowired
    Util util;

    @PersistenceUnit
    private EntityManagerFactory entityManagerFactory;

    public ResponseEntity<?> submitbidSP(ReelerBidRequest reelerBidRequest) {
        log.info("Bid Submission request:"+reelerBidRequest);
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        try {
             EntityManager entityManager = entityManagerFactory.createEntityManager();
            /* entityManager.getTransaction().begin();
            Object singleResult = entityManager.createNativeQuery("{  CALL REELER_VID_CREDIT_TXN_SP(:UserId, :MarketId, :GodownId, :LotId, :ReelerId,:AuctionDate, :SurrogateBid, :Amount) }")
                    .setParameter("UserId", "")
                    .setParameter("MarketId", reelerBidRequest.getMarketId())
                    .setParameter("GodownId", reelerBidRequest.getGodownId())
                    .setParameter("LotId", reelerBidRequest.getAllottedLotId())
                    .setParameter("ReelerId", reelerBidRequest.getReelerId())
                    .setParameter("AuctionDate", Util.getISTLocalDate())
                    .setParameter("SurrogateBid", 0)
                    .setParameter("Amount", reelerBidRequest.getAmount())
                    .getSingleResult();*/



            StoredProcedureQuery procedureQuery = entityManager
                    .createStoredProcedureQuery("SUBMIT_BID");
            procedureQuery.registerStoredProcedureParameter("UserId", String.class, ParameterMode.IN);
            procedureQuery.registerStoredProcedureParameter("MarketId", Integer.class, ParameterMode.IN);
            procedureQuery.registerStoredProcedureParameter("GodownId", Integer.class, ParameterMode.IN);
            procedureQuery.registerStoredProcedureParameter("LotId", Integer.class, ParameterMode.IN);
            procedureQuery.registerStoredProcedureParameter("ReelerId", Integer.class, ParameterMode.IN);
            procedureQuery.registerStoredProcedureParameter("AuctionDate", LocalDate.class, ParameterMode.IN);
            procedureQuery.registerStoredProcedureParameter("SurrogateBid", Integer.class, ParameterMode.IN);
            procedureQuery.registerStoredProcedureParameter("Amount", Integer.class, ParameterMode.IN);

            procedureQuery.registerStoredProcedureParameter("Error", String.class, ParameterMode.OUT);
            procedureQuery.registerStoredProcedureParameter("Success", Integer.class, ParameterMode.OUT);

            entityManager.getTransaction().begin();
            procedureQuery.setParameter("UserId", "");
            procedureQuery.setParameter("MarketId", reelerBidRequest.getMarketId());
            procedureQuery.setParameter("GodownId",  reelerBidRequest.getGodownId());
            procedureQuery.setParameter("LotId", reelerBidRequest.getAllottedLotId());
            procedureQuery.setParameter("ReelerId", reelerBidRequest.getReelerId());
            procedureQuery.setParameter("AuctionDate", Util.getISTLocalDate());
            procedureQuery.setParameter("SurrogateBid",0);
            procedureQuery.setParameter("Amount", reelerBidRequest.getAmount());
            procedureQuery.execute();
            String error = (String)procedureQuery.getOutputParameterValue("Error");
            Object success = procedureQuery.getOutputParameterValue("Success");
            System.out.println("Out status: " + success);
            entityManager.getTransaction().commit();
            if(StringUtils.isNotEmpty(error)) {
                ValidationMessage validationMessage = new ValidationMessage(MessageLabelType.NON_LABEL_MESSAGE.name(), error, "-1");
                rw.setErrorCode(-1);
                rw.setErrorMessages(List.of(validationMessage));
                return ResponseEntity.ok(rw);
            }
        } catch (Exception ex) {
            log.error("Error While submitting the bid for the Request:"+reelerBidRequest+" error id: "+ex);
            return marketAuctionHelper.retrunIfError(rw,"error occurred while submitting bid.");
        }
        return ResponseEntity.ok(rw);
    }


    @Transactional
    public ResponseEntity<?> submitbid(ReelerBidRequest reelerBidRequest) {
        log.info("Bid submission request:"+reelerBidRequest);
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        try {
            boolean canIssue = marketAuctionHelper.canPerformInAnyOneAuction(reelerBidRequest.getMarketId(), reelerBidRequest.getGodownId());
            if (!canIssue) {
                ValidationMessage validationMessage = new ValidationMessage(MessageLabelType.NON_LABEL_MESSAGE.name(), "Cannot accept bid as time either over or not started", "-1");
                rw.setErrorCode(-1);
                rw.setErrorMessages(List.of(validationMessage));
                return ResponseEntity.ok(rw);
            }
            Lot lot = lotRepository.findByMarketIdAndAllottedLotIdAndAuctionDate(reelerBidRequest.getMarketId(), reelerBidRequest.getAllottedLotId(), Util.getISTLocalDate());
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

            ReelerAuction reelerAuction = mapper.reelerAuctionObjectToEntity(reelerBidRequest, ReelerAuction.class);
            validator.validate(reelerAuction);
            reelerAuction.setAuctionDate(Util.getISTLocalDate());
            reelerAuctionRepository.save(reelerAuction);
        } catch (Exception ex) {
            log.error("Error While submitting the bid for the Request:"+reelerBidRequest+" error id: "+ex);
            return marketAuctionHelper.retrunIfError(rw,"error occurred while submitting bid");
        }
        return ResponseEntity.ok(rw);
    }

    public ResponseEntity<?> getHighestBidPerLot(LotStatusRequest lotStatusRequest) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(GetHighestBidPerLotResponse.class);

        ReelerAuction ra = reelerAuctionRepository.getHighestBidForLot(lotStatusRequest.getAllottedLotId(), lotStatusRequest.getMarketId(), Util.getISTLocalDate());
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
        ReelerAuction ra = reelerAuctionRepository.getHighestBidForLot(lotStatusRequest.getAllottedLotId(), lotStatusRequest.getMarketId(), Util.getISTLocalDate());
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
            Object[][] ldrDetails = reelerAuctionRepository.getLotBidDetailResponse(lotStatusRequest.getAllottedLotId(), Util.getISTLocalDate(), lotStatusRequest.getMarketId());
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
        log.info("Accept bid received for request: "+lotStatusRequest);
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        try {
            boolean canIssue = marketAuctionHelper.canPerformAnyOneAuctionAccept(lotStatusRequest.getMarketId(), lotStatusRequest.getGodownId());
            if (!canIssue) {
                ValidationMessage validationMessage = new ValidationMessage(MessageLabelType.NON_LABEL_MESSAGE.name(), "Cannot accept bid as time either over or not started", "-1");
                rw.setErrorCode(-1);
                rw.setErrorMessages(List.of(validationMessage));
                return ResponseEntity.ok(rw);
            }
            Lot lot = lotRepository.findByMarketIdAndAllottedLotIdAndAuctionDate(lotStatusRequest.getMarketId(), lotStatusRequest.getAllottedLotId(),Util.getISTLocalDate());
            if(!Util.isNullOrEmptyOrBlank(lot.getStatus())){
                return marketAuctionHelper.retrunIfError(rw,"expected Lot status is blank but found: "+lot.getStatus()+" for the allottedLotId: "+lot.getAllottedLotId());
            }
            ReelerAuction reelerAuction = reelerAuctionRepository.getHighestBidForLot(lotStatusRequest.getAllottedLotId(),lotStatusRequest.getMarketId(),Util.getISTLocalDate());
            if (reelerAuction != null) {
                Lot l = lotRepository.findByMarketIdAndAllottedLotIdAndAuctionDate(reelerAuction.getMarketId(), reelerAuction.getAllottedLotId(), Util.getISTLocalDate());
                l.setStatus("accepted");
                l.setReelerAuctionId(reelerAuction.getId());
                l.setBidAcceptedBy(lotStatusRequest.getBidAcceptedBy());
                reelerAuction.setStatus("accepted");
                lotRepository.save(l);
                reelerAuctionRepository.save(reelerAuction);
            } else {
                return marketAuctionHelper.retrunIfError(rw,"no Reeler auction found");
            }
            log.info("Accept bid completed for request: "+lotStatusRequest);
            return ResponseEntity.ok(rw);
        }catch (Exception ex){
            log.error("Error while processing request: "+lotStatusRequest+"with error:"+ex);
            return marketAuctionHelper.retrunIfError(rw,"error while processing the request");
        }
    }


    public ResponseEntity<?> getReelerLotWithHighestBidDetails(@RequestBody ReelerLotRequest reelerLotRequest) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);

        List<Integer> reelerLotList = reelerAuctionRepository.findByAuctionDateAndMarketIdAndReelerId(Util.getISTLocalDate(), reelerLotRequest.getMarketId(), reelerLotRequest.getReelerId());
        Object[][] reelerLotHighestAndHisBidList = reelerAuctionRepository.getHighestAndReelerBidAmountForLotList(Util.getISTLocalDate(), reelerLotRequest.getMarketId(), reelerLotList, reelerLotRequest.getReelerId());
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




