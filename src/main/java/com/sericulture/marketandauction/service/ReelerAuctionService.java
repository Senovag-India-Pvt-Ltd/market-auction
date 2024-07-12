package com.sericulture.marketandauction.service;


import com.sericulture.authentication.model.JwtPayloadData;
import com.sericulture.marketandauction.helper.MarketAuctionHelper;
import com.sericulture.marketandauction.helper.Util;
import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.api.ResponseBody;
import com.sericulture.marketandauction.model.api.marketauction.*;
import com.sericulture.marketandauction.model.entity.Lot;
import com.sericulture.marketandauction.model.entity.MarketMaster;
import com.sericulture.marketandauction.model.entity.ReelerAuction;
import com.sericulture.marketandauction.model.enums.LotStatus;
import com.sericulture.marketandauction.model.exceptions.MessageLabelType;
import com.sericulture.marketandauction.model.exceptions.ValidationException;
import com.sericulture.marketandauction.model.exceptions.ValidationMessage;
import com.sericulture.marketandauction.model.mapper.Mapper;
import com.sericulture.marketandauction.repository.LotRepository;
import com.sericulture.marketandauction.repository.MarketMasterRepository;
import com.sericulture.marketandauction.repository.ReelerAuctionRepository;
import jakarta.persistence.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ReelerAuctionService {

    @Autowired
    ReelerAuctionRepository reelerAuctionRepository;

    @Autowired
    MarketMasterRepository marketMasterRepository;

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
        LocalDateTime tStart = LocalDateTime.now();
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        EntityManager entityManager = null;
        try {
            JwtPayloadData token = marketAuctionHelper.getReelerAuthToken(reelerBidRequest);
            entityManager = entityManagerFactory.createEntityManager();
            StoredProcedureQuery procedureQuery = entityManager
                    .createStoredProcedureQuery("SUBMIT_BID_EXCEPTIONAL_SLOT");
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
            procedureQuery.setParameter("UserId", token.getUsername());
            procedureQuery.setParameter("MarketId", reelerBidRequest.getMarketId());
            procedureQuery.setParameter("GodownId",  reelerBidRequest.getGodownId());
            procedureQuery.setParameter("LotId", reelerBidRequest.getAllottedLotId());
            procedureQuery.setParameter("ReelerId", token.getUserTypeId());
            procedureQuery.setParameter("AuctionDate", Util.getISTLocalDate());
            procedureQuery.setParameter("SurrogateBid",0);
            procedureQuery.setParameter("Amount", reelerBidRequest.getAmount());
            procedureQuery.execute();
            String error = (String)procedureQuery.getOutputParameterValue("Error");
            Object success = procedureQuery.getOutputParameterValue("Success");
            System.out.println("Out status: " + success);
            entityManager.getTransaction().commit();
            log.info("total time to complete reelerAuction is: "+ ChronoUnit.MILLIS.between(tStart,LocalDateTime.now()));
            if(StringUtils.isNotEmpty(error)) {
                ValidationMessage validationMessage = new ValidationMessage(MessageLabelType.NON_LABEL_MESSAGE.name(), error, "-1");
                rw.setErrorCode(-1);
                rw.setErrorMessages(List.of(validationMessage));
                return ResponseEntity.ok(rw);
            }
        } catch (Exception ex) {
            if(ex instanceof ValidationException){
                throw ex;
            }
            log.error("Error While submitting the bid for the Request:"+reelerBidRequest+" error id: "+ex);
            return marketAuctionHelper.retrunIfError(rw,"error occurred while submitting bid.");
        }finally {
            if (entityManager != null && entityManager.isOpen()) {
                entityManager.close();
            }
        }
        return ResponseEntity.ok(rw);
    }


    @Transactional
    public ResponseEntity<?> submitbid(ReelerBidRequest reelerBidRequest) {
        LocalDateTime tStart = LocalDateTime.now();
        log.info("Bid submission request:" + reelerBidRequest);
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        try {
            JwtPayloadData token = marketAuctionHelper.getAuthToken(reelerBidRequest);
            boolean canIssue = marketAuctionHelper.canPerformActivity(MarketAuctionHelper.activityType.AUCTION, reelerBidRequest.getMarketId(), reelerBidRequest.getGodownId());
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
            //validator.validate(reelerAuction);
            reelerAuction.setAuctionDate(Util.getISTLocalDate());
            reelerAuction.setReelerId(Math.toIntExact(token.getUserTypeId()));
            reelerAuctionRepository.save(reelerAuction);
            log.info("total time to complete reelerAuction is: "+ ChronoUnit.MILLIS.between(tStart,LocalDateTime.now()));
        } catch (Exception ex) {
            log.error("Error While submitting the bid for the Request:" + reelerBidRequest + " error id: " + ex);
            return marketAuctionHelper.retrunIfError(rw, "error occurred while submitting bid");
        }
        return ResponseEntity.ok(rw);
    }

   /* public ResponseEntity<?> getHighestBidPerLot(LotStatusRequest lotStatusRequest) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(GetHighestBidPerLotResponse.class);
        int currentSession = marketAuctionHelper.checkCurrentAuction(lotStatusRequest.getMarketId());
        ReelerAuction ra = reelerAuctionRepository.getHighestBidForLotAndActive(lotStatusRequest.getAllottedLotId(), lotStatusRequest.getMarketId(), Util.getISTLocalDate(), currentSession);
        GetHighestBidPerLotResponse getHighestBidPerLotResponse = new GetHighestBidPerLotResponse();
        getHighestBidPerLotResponse.setAllottedLotId(ra.getAllottedLotId());
        if (ra != null) {
            getHighestBidPerLotResponse.setHighestBidAmount(ra.getAmount());
        }
        rw.setContent(getHighestBidPerLotResponse);
        return ResponseEntity.ok(rw);

    }*/

    public ResponseEntity<?> getHighestBidPerLot(LotStatusRequest lotStatusRequest) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(GetHighestBidPerLotResponse.class);
//        int currentSession = marketAuctionHelper.checkCurrentAuction(lotStatusRequest.getMarketId());
//        ReelerAuction ra = reelerAuctionRepository.getHighestBidForLotAndActive(lotStatusRequest.getAllottedLotId(), lotStatusRequest.getMarketId(), Util.getISTLocalDate(), currentSession);

        EntityManager entityManager = null;

        try{
            entityManager = entityManagerFactory.createEntityManager();
            StoredProcedureQuery procedureQuery = entityManager
                    .createStoredProcedureQuery("GET_TOP_REELER_AUCTION");
            procedureQuery.registerStoredProcedureParameter("LotId", Integer.class, ParameterMode.IN);
            procedureQuery.registerStoredProcedureParameter("MarketId", Integer.class, ParameterMode.IN);
            procedureQuery.registerStoredProcedureParameter("AuctionDate", LocalDate.class, ParameterMode.IN);
//        procedureQuery.registerStoredProcedureParameter("Error", String.class, ParameterMode.OUT);
//        procedureQuery.registerStoredProcedureParameter("Success", Integer.class, ParameterMode.OUT);
            entityManager.getTransaction().begin();
            procedureQuery.setParameter("LotId",  lotStatusRequest.getAllottedLotId());
            procedureQuery.setParameter("MarketId", lotStatusRequest.getMarketId());
            procedureQuery.setParameter("AuctionDate", Util.getISTLocalDate());
            procedureQuery.execute();
//        String error = (String)procedureQuery.getOutputParameterValue("Error");
//        Object success = procedureQuery.getOutputParameterValue("Success");
            // Object[][] success1 = (Object[][]) procedureQuery.getOutputParameterValue("Success");
            entityManager.getTransaction().commit();

            GetHighestBidPerLotResponse getHighestBidPerLotResponse = new GetHighestBidPerLotResponse();
            List<Object[]> reelerLotHighestAndHisBidList = procedureQuery.getResultList();
            if(reelerLotHighestAndHisBidList.size()>0){
                getHighestBidPerLotResponse.setAllottedLotId(Util.objectToInteger(reelerLotHighestAndHisBidList.get(0)[2]));
                getHighestBidPerLotResponse.setHighestBidAmount(Util.objectToInteger(reelerLotHighestAndHisBidList.get(0)[4]));

            }
            rw.setContent(getHighestBidPerLotResponse);
        }finally {
            if(entityManager!=null && entityManager.isOpen()){
                entityManager.close();
            }
        }

        return ResponseEntity.ok(rw);

    }

    public ResponseEntity<?> getHighestBidPerLotDetails(LotStatusRequest lotStatusRequest) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(ResponseBody.class);
        int currentSession = marketAuctionHelper.checkCurrentAuction(lotStatusRequest.getMarketId());
        //ReelerAuction ra = reelerAuctionRepository.getHighestBidForLotAndActive(lotStatusRequest.getAllottedLotId(), lotStatusRequest.getMarketId(), Util.getISTLocalDate(), currentSession);

        EntityManager entityManager = null;
        try{
            entityManager = entityManagerFactory.createEntityManager();
            StoredProcedureQuery procedureQuery = entityManager
                    .createStoredProcedureQuery("GET_ACCEPT_REELER_AUCTION");
            procedureQuery.registerStoredProcedureParameter("LotId", Integer.class, ParameterMode.IN);
            procedureQuery.registerStoredProcedureParameter("MarketId", Integer.class, ParameterMode.IN);
            procedureQuery.registerStoredProcedureParameter("AuctionDate", LocalDate.class, ParameterMode.IN);
            entityManager.getTransaction().begin();
            procedureQuery.setParameter("LotId",  lotStatusRequest.getAllottedLotId());
            procedureQuery.setParameter("MarketId", lotStatusRequest.getMarketId());
            procedureQuery.setParameter("AuctionDate", Util.getISTLocalDate());
            procedureQuery.execute();
            entityManager.getTransaction().commit();

            List<Object[]> reelerLotHighestAndHisBidList = procedureQuery.getResultList();
            if(reelerLotHighestAndHisBidList.size()>0) {
                Object[] obj = reelerLotHighestAndHisBidList.get(0);
                ReelerAuction ra = reelerAuctionRepository.findById(BigInteger.valueOf(Long.parseLong(Util.objectToString(obj[0]))));

                LotBidDetailResponse lbdr = new LotBidDetailResponse();
                lbdr.setAllottedlotid(lotStatusRequest.getAllottedLotId());
                if (ra != null) {
                    Object[][] reelerDetails = reelerAuctionRepository.getReelerDetailsForHighestBidWithReelingNumber(ra.getId());
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
                    lbdr.setReelingLicenseNumber(reelerDetails[0][2] == null ? "" : String.valueOf(reelerDetails[0][2]));
                    lbdr.setReelerFruitsId(reelerDetails[0][1] == null ? "" : String.valueOf(reelerDetails[0][1]));
                    lbdr.setFarmervillageName(ldrDetails[0][4] == null ? "" : String.valueOf(ldrDetails[0][4]));
                    lbdr.setLotApproxWeightBeforeWeighment(ldrDetails[0][5] == null ? 0 : Integer.valueOf(String.valueOf(ldrDetails[0][5])));
                    lbdr.setStatus(ldrDetails[0][6] == null ? "" : String.valueOf(ldrDetails[0][6]));
                    lbdr.setBidAcceptedBy(ldrDetails[0][7] == null ? "" : String.valueOf(ldrDetails[0][7]));
                    rw.setContent(lbdr);
                } else {
                    marketAuctionHelper.retrunIfError(rw, "No bid found for the given lot please check the input");
                }
            }
        }finally {
            if(entityManager!=null && entityManager.isOpen()){
                entityManager.close();
            }
        }

        return ResponseEntity.ok(rw);

    }

    @Transactional
    public ResponseEntity<?> acceptReelerBidForGivenLot(ReelerBidAcceptRequest lotStatusRequest) {
        log.info("Accept bid received for request: " + lotStatusRequest);
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        EntityManager entityManager = null;
        try {
            JwtPayloadData token = marketAuctionHelper.getAuthToken(lotStatusRequest);

            boolean canIssue = marketAuctionHelper.canPerformActivity(MarketAuctionHelper.activityType.AUCTIONACCEPT, lotStatusRequest.getMarketId(), lotStatusRequest.getGodownId());
            if (!canIssue) {
                ValidationMessage validationMessage = new ValidationMessage(MessageLabelType.NON_LABEL_MESSAGE.name(), "Cannot accept bid as time either over or not started", "-1");
                rw.setErrorCode(-1);
                rw.setErrorMessages(List.of(validationMessage));
                return ResponseEntity.ok(rw);
            }
            Lot lot = lotRepository.findByMarketIdAndAllottedLotIdAndAuctionDate(lotStatusRequest.getMarketId(), lotStatusRequest.getAllottedLotId(), Util.getISTLocalDate());
            if (!Util.isNullOrEmptyOrBlank(lot.getStatus())) {
                throw new ValidationException(String.format("expected Lot status is blank but found:%s for the allottedLotId: %s",lot.getStatus(),lot.getAllottedLotId()));
            }


            entityManager = entityManagerFactory.createEntityManager();
            StoredProcedureQuery procedureQuery = entityManager
                    .createStoredProcedureQuery("GET_ACCEPT_REELER_AUCTION");
            procedureQuery.registerStoredProcedureParameter("LotId", Integer.class, ParameterMode.IN);
            procedureQuery.registerStoredProcedureParameter("MarketId", Integer.class, ParameterMode.IN);
            procedureQuery.registerStoredProcedureParameter("AuctionDate", LocalDate.class, ParameterMode.IN);
            entityManager.getTransaction().begin();
            procedureQuery.setParameter("LotId",  lotStatusRequest.getAllottedLotId());
            procedureQuery.setParameter("MarketId", lotStatusRequest.getMarketId());
            procedureQuery.setParameter("AuctionDate", Util.getISTLocalDate());
            procedureQuery.execute();
            entityManager.getTransaction().commit();

            GetHighestBidPerLotResponse getHighestBidPerLotResponse = new GetHighestBidPerLotResponse();
            List<Object[]> reelerLotHighestAndHisBidList = procedureQuery.getResultList();
            List<ReelerAuction> reelerLotHighestAndHisBidList1 = procedureQuery.getResultList();
            if(reelerLotHighestAndHisBidList.size()>0){
                Object[] obj = reelerLotHighestAndHisBidList.get(0);
                ReelerAuction reelerAuction = reelerAuctionRepository.findById(BigInteger.valueOf(Long.parseLong(Util.objectToString(obj[0]))));
                Lot l = lotRepository.findByMarketIdAndAllottedLotIdAndAuctionDate(Util.objectToInteger(reelerLotHighestAndHisBidList.get(0)[12]), Util.objectToInteger(reelerLotHighestAndHisBidList.get(0)[2]), Util.getISTLocalDate());
                l.setStatus(LotStatus.ACCEPTED.getLabel());
                l.setReelerAuctionId(reelerAuction.getId());
                l.setBidAcceptedBy(token.getUsername());
                reelerAuction.setStatus(LotStatus.ACCEPTED.getLabel());
                lotRepository.save(l);
                reelerAuctionRepository.save(reelerAuction);
            } else {
                throw new ValidationException(String.format("No bids found for the given lot %s",lotStatusRequest.getAllottedLotId()));
            }
            log.info("Accept bid completed for request: " + lotStatusRequest);
            return ResponseEntity.ok(rw);
        } catch (Exception ex) {
            if(ex instanceof ValidationException){
                throw ex;
            }
            log.error("Error while processing request: " + lotStatusRequest + "with error:" + ex);
            return marketAuctionHelper.retrunIfError(rw, "error while processing the request");
        }finally {
            if(entityManager!=null && entityManager.isOpen()){
                entityManager.close();
            }
        }
    }

    @Transactional
    public ResponseEntity<?> rejectReelerBidForGivenLot(ReelerBidAcceptRequest lotStatusRequest) {
        log.info("Reject bid received for request: " + lotStatusRequest);
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        try {
            JwtPayloadData token = marketAuctionHelper.getAuthToken(lotStatusRequest);

            boolean canIssue = marketAuctionHelper.canPerformActivity(MarketAuctionHelper.activityType.AUCTIONACCEPT, lotStatusRequest.getMarketId(), lotStatusRequest.getGodownId());
            if (!canIssue) {
                ValidationMessage validationMessage = new ValidationMessage(MessageLabelType.NON_LABEL_MESSAGE.name(), "Cannot reject bid as time either over or not started", "-1");
                rw.setErrorCode(-1);
                rw.setErrorMessages(List.of(validationMessage));
                return ResponseEntity.ok(rw);
            }
            Lot lot = lotRepository.findByMarketIdAndAllottedLotIdAndAuctionDate(lotStatusRequest.getMarketId(), lotStatusRequest.getAllottedLotId(), Util.getISTLocalDate());
            if (!Util.isNullOrEmptyOrBlank(lot.getStatus())) {
                throw new ValidationException(String.format("expected Lot status is blank but found:%s for the allottedLotId: %s",lot.getStatus(),lot.getAllottedLotId()));
            }
            List<ReelerAuction> reelerAuctionList = reelerAuctionRepository.getBidsForLotAndActiveByMarketIdAndAuctionDate(lotStatusRequest.getAllottedLotId(), lotStatusRequest.getMarketId(), Util.getISTLocalDate());
            if (reelerAuctionList.size()>0) {
                for(ReelerAuction reelerAuction: reelerAuctionList) {
                    reelerAuction.setStatus(LotStatus.REJECTED.getLabel());
                    reelerAuction.setActive(false);
                    reelerAuctionRepository.save(reelerAuction);
                }
            } else {
                throw new ValidationException(String.format("No bids found for the given lot %s",lotStatusRequest.getAllottedLotId()));
            }
            log.info("Reject bid completed for request: " + lotStatusRequest);
            return ResponseEntity.ok(rw);
        } catch (Exception ex) {
            if(ex instanceof ValidationException){
                throw ex;
            }
            log.error("Error while processing request: " + lotStatusRequest + "with error:" + ex);
            return marketAuctionHelper.retrunIfError(rw, "error while processing the request");
        }
    }


    public ResponseEntity<?> getReelerLotWithHighestBidDetails(@RequestBody ReelerLotRequest reelerLotRequest) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        int currentAuction = marketAuctionHelper.checkCurrentAuction(reelerLotRequest.getMarketId());
        List<Integer> reelerLotList = reelerAuctionRepository.findByAuctionDateAndMarketIdAndReelerIdByActive(Util.getISTLocalDate(), reelerLotRequest.getMarketId(), reelerLotRequest.getReelerId());
        Object[][] reelerLotHighestAndHisBidList = reelerAuctionRepository.getHighestAndReelerBidAmountForLotList(Util.getISTLocalDate(), reelerLotRequest.getMarketId(), reelerLotList, reelerLotRequest.getReelerId(), currentAuction);
        Map<Integer, ReelerLotResponse> reelerLotResponseMap = new HashMap<>();

        if (reelerLotHighestAndHisBidList != null && reelerLotHighestAndHisBidList.length > 0) {
            for (Object[] bidDetail : reelerLotHighestAndHisBidList) {
                boolean notFound = true;
                int allottedLot = Integer.parseInt(String.valueOf(bidDetail[2]));
                int bidAmount = Integer.parseInt(String.valueOf(bidDetail[1]));
                BigInteger reelerAuctionId = BigInteger.valueOf(Long.parseLong(String.valueOf(bidDetail[0])));
                List<Object[]> responses = reelerAuctionRepository.getReelerAuctionStatus(reelerAuctionId);
                String status = "N";
                if(responses.get(0) == null){
                    status = "N";
                }else{
                    if(responses.get(0)[0].equals("accepted")){
                        status = "Y";
                    }else{
                        status = "N";
                    }
                }
                ReelerLotResponse reelerLotResponse = reelerLotResponseMap.get(allottedLot);
                if (reelerLotResponse == null) {
                    reelerLotResponse = new ReelerLotResponse();
                    reelerLotResponseMap.put(allottedLot, reelerLotResponse);
                }
                setReelerLotResponse(reelerLotResponse, allottedLot, String.valueOf(bidDetail[3]), bidAmount, reelerAuctionId, status);
            }
        }
        rw.setContent(reelerLotResponseMap.values());
        return ResponseEntity.ok(rw);
    }

  /*  public ResponseEntity<?> getReelerLotWithHighestBidDetails(@RequestBody ReelerLotRequest reelerLotRequest) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
       // int currentAuction = marketAuctionHelper.checkCurrentAuction(reelerLotRequest.getMarketId());
        //List<Integer> reelerLotList = reelerAuctionRepository.findByAuctionDateAndMarketIdAndReelerIdByActive(Util.getISTLocalDate(), reelerLotRequest.getMarketId(), reelerLotRequest.getReelerId());

        EntityManager entityManager = null;
        try{
            entityManager = entityManagerFactory.createEntityManager();
            StoredProcedureQuery procedureQuery = entityManager
                    .createStoredProcedureQuery("GET_AUCTION_DETAILS_1");
            procedureQuery.registerStoredProcedureParameter("today", LocalDate.class, ParameterMode.IN);
            procedureQuery.registerStoredProcedureParameter("marketId", Integer.class, ParameterMode.IN);
            procedureQuery.registerStoredProcedureParameter("reelerId", Integer.class, ParameterMode.IN);
            procedureQuery.registerStoredProcedureParameter("Error", String.class, ParameterMode.OUT);
            procedureQuery.registerStoredProcedureParameter("Success", Integer.class, ParameterMode.OUT);

            entityManager.getTransaction().begin();
            procedureQuery.setParameter("today", Util.getISTLocalDate());
            procedureQuery.setParameter("marketId",  reelerLotRequest.getMarketId());
            procedureQuery.setParameter("reelerId", reelerLotRequest.getReelerId());
            procedureQuery.execute();
            String error = (String)procedureQuery.getOutputParameterValue("Error");
            Object success = procedureQuery.getOutputParameterValue("Success");
            // Object[][] success1 = (Object[][]) procedureQuery.getOutputParameterValue("Success");
            entityManager.getTransaction().commit();
//        Object[][] reelerLotHighestAndHisBidList = reelerAuctionRepository.getHighestAndReelerBidAmountForLotList(Util.getISTLocalDate(), reelerLotRequest.getMarketId(), reelerLotList, reelerLotRequest.getReelerId(), currentAuction);
            Map<Integer, ReelerLotResponse> reelerLotResponseMap = new HashMap<>();
//
//        if (reelerLotHighestAndHisBidList != null && reelerLotHighestAndHisBidList.length > 0) {
            List<Object[]> reelerLotHighestAndHisBidList = procedureQuery.getResultList();
            for (Object[] bidDetail : reelerLotHighestAndHisBidList) {
                boolean notFound = true;
                int allottedLot = Integer.parseInt(String.valueOf(bidDetail[2]));
                int bidAmount = Integer.parseInt(String.valueOf(bidDetail[1]));
                BigInteger reelerAuctionId = BigInteger.valueOf(Long.parseLong(String.valueOf(bidDetail[0])));
                List<Object[]> responses = reelerAuctionRepository.getReelerAuctionStatus(reelerAuctionId);
                String status = "N";
                if(responses.get(0) == null){
                    status = "N";
                }else{
                    if(responses.get(0)[0].equals("accepted")){
                        status = "Y";
                    }else{
                        status = "N";
                    }
                }
                ReelerLotResponse reelerLotResponse = reelerLotResponseMap.get(allottedLot);
                if (reelerLotResponse == null) {
                    reelerLotResponse = new ReelerLotResponse();
                    reelerLotResponseMap.put(allottedLot, reelerLotResponse);
                }
                setReelerLotResponse(reelerLotResponse, allottedLot, String.valueOf(bidDetail[3]), bidAmount, reelerAuctionId, status);
            }
            // }
            rw.setContent(reelerLotResponseMap.values());
        }finally {
            if(entityManager!=null && entityManager.isOpen()){
                entityManager.close();
            }
        }

        return ResponseEntity.ok(rw);
    }
*/
    private void setReelerLotResponse(ReelerLotResponse reelerLotResponse, int allottedLot, String highest, int bidAmount, BigInteger reelerAuctionId, String status) {
        reelerLotResponse.setAllottedLotId(allottedLot);
        reelerLotResponse.setStatus(status);
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
        log.warn("Removing the reeler Bid for:" + removeReelerHighestBidRequest);
        long deletedrows = reelerAuctionRepository.deleteByIdAndMarketIdAndAllottedLotIdAndReelerId(removeReelerHighestBidRequest.getReelerAuctionId(), removeReelerHighestBidRequest.getMarketId(), removeReelerHighestBidRequest.getAllottedLotId(), removeReelerHighestBidRequest.getReelerId());
        return ResponseEntity.ok(rw);
    }

    private void setReelerLotResponse(ReelerLotResponse reelerLotResponse, int allottedLot, String highest, int bidAmount) {
        reelerLotResponse.setAllottedLotId(allottedLot);
        if (highest.equals("h")) {
            reelerLotResponse.setHighestBidAmount(bidAmount);
        } else {
            reelerLotResponse.setMyBidAmount(bidAmount);
        }
    }

    public ResponseEntity<?> getReelerBalance(ReelerBalanceRequest reelerBalanceRequest) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(ReelerBalanceResponse.class);

        Object[][] reelerBalanceRespObject = reelerAuctionRepository.getReelerBalance(reelerBalanceRequest.getReelerId(), reelerBalanceRequest.getMarketId());

        if (reelerBalanceRespObject == null) {
            marketAuctionHelper.retrunIfError(rw, "Reeler data not found for the reelerId and market" + reelerBalanceRequest.getReelerId() + " market: " + reelerBalanceRequest.getMarketId());
        }
        ReelerBalanceResponse reelerBalanceResponse = ReelerBalanceResponse.builder()
                .reelerId(Util.objectToInteger(reelerBalanceRespObject[0][0]))
                .reelerVirtualAccount(Util.objectToString(reelerBalanceRespObject[0][1]))
                .balance(Util.objectToFloat(reelerBalanceRespObject[0][2]))
                .minimumMarketBalance(Util.objectToFloat(reelerBalanceRespObject[0][3]))
                .build();
        rw.setContent(reelerBalanceResponse);
        return ResponseEntity.ok(rw);
    }

   /* public int checkCurrentAuction(int marketMasterId) {
        MarketMaster marketMaster = marketMasterRepository.findById(marketMasterId);

        if (marketMaster != null) {
            LocalDateTime now = LocalDateTime.now();
            LocalTime currentTime = now.toLocalTime();

            if (!currentTime.isBefore(marketMaster.getAuction1StartTime()) && currentTime.isBefore(marketMaster.getAuction1EndTime())) {
                return 1;
            } else if (!currentTime.isBefore(marketMaster.getAuction2StartTime()) && currentTime.isBefore(marketMaster.getAuction2EndTime())) {
                return 2;
            } else if (!currentTime.isBefore(marketMaster.getAuction3StartTime()) && currentTime.isBefore(marketMaster.getAuction3EndTime())) {
                return 3;
            } else {
                return 0;
            }
        } else {
            return -1;
        }
    }

    public int checkCurrentAuctionAccept(int marketMasterId) {
        MarketMaster marketMaster = marketMasterRepository.findById(marketMasterId);

        if (marketMaster != null) {
            LocalDateTime now = LocalDateTime.now();
            LocalTime currentTime = now.toLocalTime();

            if (!currentTime.isBefore(marketMaster.getAuction1AcceptStartTime()) && currentTime.isBefore(marketMaster.getAuction1AcceptEndTime())) {
                return 1;
            } else if (!currentTime.isBefore(marketMaster.getAuction2AcceptStartTime()) && currentTime.isBefore(marketMaster.getAuction2AcceptEndTime())) {
                return 2;
            } else if (!currentTime.isBefore(marketMaster.getAuction3AcceptStartTime()) && currentTime.isBefore(marketMaster.getAuction3AcceptEndTime())) {
                return 3;
            } else {
                return 0;
            }
        } else {
            return -1;
        }
    }*/
}




