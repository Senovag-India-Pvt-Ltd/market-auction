package com.sericulture.marketandauction.helper;


import com.sericulture.marketandauction.model.entity.FlexTime;
import com.sericulture.marketandauction.model.entity.MarketMaster;
import com.sericulture.marketandauction.repository.FlexTimeRepository;
import com.sericulture.marketandauction.repository.MarketMasterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.Date;

@Component
public class MarketAuctionHelper {

    @Autowired
    MarketMasterRepository marketMasterRepository;

    @Autowired
    FlexTimeRepository flexTimeRepository;

    public enum activityType {
        ISSUEBIDSLIP,
        AUCTION1,
        AUCTION2,
        AUCTION3,
        ACCEPTBID
    }


    public boolean canPerformActivity(activityType activity, int marketId) {
        MarketMaster marketMaster = marketMasterRepository.findById(marketId);
        FlexTime flexTime = flexTimeRepository.findByActivityType(activity.toString());

        LocalTime time = LocalTime.now();

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
            case AUCTION2:
                starttime = marketMaster.getAuction2StartTime();
                endTime = marketMaster.getAuction2EndTime();
            case AUCTION3:
                starttime = marketMaster.getAuction3StartTime();
                endTime = marketMaster.getAuction3EndTime();

        }
        return time.equals(starttime) || time.isAfter(starttime)
                || time.equals(endTime) || time.isBefore(endTime) || flexTime.isStart();

    }
}
