package com.sericulture.marketandauction.helper;


import com.sericulture.marketandauction.model.entity.FlexTime;
import com.sericulture.marketandauction.model.entity.MarketMaster;
import com.sericulture.marketandauction.repository.FlexTimeRepository;
import com.sericulture.marketandauction.repository.MarketMasterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

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
        FlexTime flexTime = flexTimeRepository.findByActivityTypeAndMarketId(activity.toString(), marketId);

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
                || time.equals(endTime) || flexTime.isStart();

    }
}
