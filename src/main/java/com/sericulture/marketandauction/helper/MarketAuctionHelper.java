package com.sericulture.marketandauction.helper;


import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.entity.FlexTime;
import com.sericulture.marketandauction.model.entity.MarketMaster;
import com.sericulture.marketandauction.model.exceptions.MessageLabelType;
import com.sericulture.marketandauction.model.exceptions.ValidationMessage;
import com.sericulture.marketandauction.repository.FlexTimeRepository;
import com.sericulture.marketandauction.repository.MarketMasterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class MarketAuctionHelper {

    @Autowired
    MarketMasterRepository marketMasterRepository;

    @Autowired
    FlexTimeRepository flexTimeRepository;

    @Autowired
    Util util;

    public enum activityType {
        ISSUEBIDSLIP,
        AUCTION1,
        AUCTION2,
        AUCTION3,
        ACCEPTBID,

    }


    public boolean canPerformActivity(activityType activity, int marketId,int godownId) {
        MarketMaster marketMaster = marketMasterRepository.findById(marketId);
        return canPerformAnyoneActivity(marketMaster,activity,marketId,godownId);

    }

    public boolean canPerformInAnyOneAuction(int marketId,int godownId){
        MarketMaster marketMaster = marketMasterRepository.findById(marketId);
        boolean auction1 = canPerformAnyoneActivity(marketMaster,activityType.AUCTION1,marketId,godownId);
        if(auction1)
            return true;
        boolean auction2 = canPerformAnyoneActivity(marketMaster,activityType.AUCTION2,marketId,godownId);
        if(auction2)
            return true;
        boolean auction3 = canPerformAnyoneActivity(marketMaster,activityType.AUCTION3,marketId,godownId);
        if(auction3)
            return true;
        return false;
    }

    public boolean canPerformAnyoneActivity(MarketMaster marketMaster,activityType activity,int marketId,int godownId){
        FlexTime flexTime = flexTimeRepository.findByActivityTypeAndMarketIdAndGodownId(activity.toString(), marketId,godownId);

        LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);

        LocalTime starttime = null;
        LocalTime endTime = null;

        switch (activity) {
            case ISSUEBIDSLIP:
                starttime = marketMaster.getIssueBidSlipStartTime();
                endTime = marketMaster.getIssueBidSlipEndTime();
                break;
            case AUCTION1:
                starttime = marketMaster.getAuction1StartTime();
                endTime = marketMaster.getAuction1EndTime();
                break;
            case AUCTION2:
                starttime = marketMaster.getAuction2StartTime();
                endTime = marketMaster.getAuction2EndTime();
                break;
            case AUCTION3:
                starttime = marketMaster.getAuction3StartTime();
                endTime = marketMaster.getAuction3EndTime();
                break;

        }
        //in between todo
        return (time.isAfter(starttime) && time.isBefore(endTime)) || time.equals(starttime)
                || time.equals(endTime) || (flexTime==null ? false : flexTime.isStart());
    }


    public  ResponseEntity<?> retrunIfError(ResponseWrapper rw, String err){
        ValidationMessage validationMessage = new ValidationMessage(MessageLabelType.NON_LABEL_MESSAGE.name(),
                util.getMessageByCode("MA00002.GEN.FLEXTIME"), "MA00002.GEN.FLEXTIME");
        rw.setErrorCode(-1);
        rw.setErrorMessages(List.of(err));
        return ResponseEntity.ok(rw);
    }
}
