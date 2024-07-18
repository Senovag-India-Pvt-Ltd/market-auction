package com.sericulture.marketandauction.helper;


import com.sericulture.authentication.model.JwtPayloadData;
import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.api.RequestBody;
import com.sericulture.marketandauction.model.entity.ExceptionalTime;
import com.sericulture.marketandauction.model.entity.MarketMaster;
import com.sericulture.marketandauction.model.enums.USERTYPE;
import com.sericulture.marketandauction.model.exceptions.MessageLabelType;
import com.sericulture.marketandauction.model.exceptions.ValidationException;
import com.sericulture.marketandauction.model.exceptions.ValidationMessage;
import com.sericulture.marketandauction.repository.ExceptionalTimeRepository;
import com.sericulture.marketandauction.repository.MarketMasterRepository;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.graph.EntityGraphs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@Slf4j
public class MarketAuctionHelper {

    @Autowired
    MarketMasterRepository marketMasterRepository;
    
    @Autowired
    ExceptionalTimeRepository exceptionalTimeRepository;
    @Autowired
    Util util;

    public enum activityType {
        ISSUEBIDSLIP,
        AUCTION,
        AUCTIONACCEPT;

    }


    public boolean canPerformActivity(activityType activity, int marketId,int godownId) {
        ExceptionalTime exceptionalTime = exceptionalTimeRepository.findByMarketIdAndAuctionDate(marketId,Util.getISTLocalDate());
        if(exceptionalTime!=null){
            return canPerformAnyoneActivityExceptionally(exceptionalTime,activity);
        }
        MarketMaster marketMaster = marketMasterRepository.findById(marketId);
        return canPerformAnyoneActivityNormally(marketMaster,activity);
    }

    public int getAuctionNumber(ExceptionalTime exceptionalTime,MarketMaster marketMaster, LocalTime bidTime) {
        if (exceptionalTime != null) {
            if (compareTime(bidTime, exceptionalTime.getAuction1StartTime(), exceptionalTime.getAuction1EndTime())) {
                return 1;
            }
            if (compareTime(bidTime, exceptionalTime.getAuction2StartTime(), exceptionalTime.getAuction2EndTime())) {
                return 2;
            }
            return 3;
        }
        if (compareTime(bidTime, marketMaster.getAuction1StartTime(), marketMaster.getAuction1EndTime())) {
            return 1;
        }
        if (compareTime(bidTime, marketMaster.getAuction2StartTime(), marketMaster.getAuction2EndTime())) {
            return 2;
        }
        return 3;
    }

    public boolean canPerformAnyoneActivityExceptionally(ExceptionalTime exceptionalTime, activityType activity) {
        LocalTime time = Util.getISTLocalTime().truncatedTo(ChronoUnit.SECONDS);
        switch (activity) {
            case ISSUEBIDSLIP:
                return compareTime(time, exceptionalTime.getIssueBidSlipStartTime(), exceptionalTime.getIssueBidSlipEndTime());
            case AUCTION:
                if (compareTime(time, exceptionalTime.getAuction1StartTime(), exceptionalTime.getAuction1EndTime())) {
                    return true;
                }
                if (compareTime(time, exceptionalTime.getAuction2StartTime(), exceptionalTime.getAuction2EndTime())) {
                    return true;
                }
                return compareTime(time, exceptionalTime.getAuction3StartTime(), exceptionalTime.getAuction3EndTime());
            case AUCTIONACCEPT:
                if (compareTime(time, exceptionalTime.getAuction1AcceptStartTime(), exceptionalTime.getAuction1AcceptEndTime())) {
                    return true;
                }
                if (compareTime(time, exceptionalTime.getAuction2AcceptStartTime(), exceptionalTime.getAuction2AcceptEndTime())) {
                    return true;
                }
                return compareTime(time, exceptionalTime.getAuction3AcceptStartTime(), exceptionalTime.getAuction3AcceptEndTime());
        }
        return false;
    }
    public boolean canPerformAnyoneActivityNormally(MarketMaster exceptionalTime, activityType activity) {
        LocalTime time = Util.getISTLocalTime().truncatedTo(ChronoUnit.SECONDS);
        switch (activity) {
            case ISSUEBIDSLIP:
                return compareTime(time, exceptionalTime.getIssueBidSlipStartTime(), exceptionalTime.getIssueBidSlipEndTime());
            case AUCTION:
                if (compareTime(time, exceptionalTime.getAuction1StartTime(), exceptionalTime.getAuction1EndTime())) {
                    return true;
                }
                if (compareTime(time, exceptionalTime.getAuction2StartTime(), exceptionalTime.getAuction2EndTime())) {
                    return true;
                }
                return compareTime(time, exceptionalTime.getAuction3StartTime(), exceptionalTime.getAuction3EndTime());
            case AUCTIONACCEPT:
                if (compareTime(time, exceptionalTime.getAuction1AcceptStartTime(), exceptionalTime.getAuction1AcceptEndTime())) {
                    return true;
                }
                if (compareTime(time, exceptionalTime.getAuction2AcceptStartTime(), exceptionalTime.getAuction2AcceptEndTime())) {
                    return true;
                }
                return compareTime(time, exceptionalTime.getAuction3AcceptStartTime(), exceptionalTime.getAuction3AcceptEndTime());
        }
        return false;
    }
    private boolean compareTime(LocalTime time,LocalTime starTime,LocalTime endTime){
        return (time.isAfter(starTime) && time.isBefore(endTime)) || time.equals(starTime)
                || time.equals(endTime);
    }

    public  ResponseEntity<?> retrunIfError(ResponseWrapper rw, String err){
        ValidationMessage validationMessage = new ValidationMessage(MessageLabelType.NON_LABEL_MESSAGE.name(),
                util.getMessageByCode("MA00002.GEN.FLEXTIME"), "MA00002.GEN.FLEXTIME");
        rw.setErrorCode(-1);
        rw.setErrorMessages(List.of(err));
        return ResponseEntity.ok(rw);
    }

    public JwtPayloadData getAuthToken(int marketId,int userType){
        JwtPayloadData jwtPayloadData = Util.getTokenValues();
        if (jwtPayloadData.getMarketId() != marketId|| jwtPayloadData.getUserType()!= userType) {
            throw new ValidationException(String.format("expected market or usertype is wrong expected market is: %s but found: %s and expected user type is: %s but found %s for the user %s",  jwtPayloadData.getMarketId(),marketId,userType,jwtPayloadData.getUserType(), jwtPayloadData.getUsername()));
        }
        return jwtPayloadData;
    }

    public JwtPayloadData getMOAuthToken(RequestBody requestBody) {
        return getAuthToken(requestBody.getMarketId(),USERTYPE.MO.getType());
    }

    public JwtPayloadData getReelerAuthToken(RequestBody requestBody) {
        JwtPayloadData jwtPayloadData = Util.getTokenValues();
        if (jwtPayloadData.getMarketId() != requestBody.getMarketId() || jwtPayloadData.getUserType()!= USERTYPE.REELER.getType()) {
            throw new ValidationException(String.format("expected market or usertype is wrong expected market is: %s but found: %s and expected user type is: %s but found %s for the user %s",  jwtPayloadData.getMarketId(),requestBody.getMarketId(),USERTYPE.REELER.getType(),jwtPayloadData.getUserType(), jwtPayloadData.getUsername()));
        }
        return jwtPayloadData;
    }

    public int checkCurrentAuction(int marketMasterId) {
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
        ExceptionalTime exceptionalTime = exceptionalTimeRepository.findByMarketIdAndAuctionDate(marketMasterId,Util.getISTLocalDate());

        if(exceptionalTime!=null){
            LocalDateTime now = LocalDateTime.now();
            LocalTime currentTime = now.toLocalTime();

            if (!currentTime.isBefore(exceptionalTime.getAuction1AcceptStartTime()) && currentTime.isBefore(exceptionalTime.getAuction1AcceptEndTime())) {
                return 1;
            } else if (!currentTime.isBefore(exceptionalTime.getAuction2AcceptStartTime()) && currentTime.isBefore(exceptionalTime.getAuction2AcceptEndTime())) {
                return 2;
            } else if (!currentTime.isBefore(exceptionalTime.getAuction3AcceptStartTime()) && currentTime.isBefore(exceptionalTime.getAuction3AcceptEndTime())) {
                return 3;
            } else {
                return 0;
            }
        }

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
    }
}
