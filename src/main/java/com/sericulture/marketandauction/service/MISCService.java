package com.sericulture.marketandauction.service;

import com.sericulture.authentication.model.JwtPayloadData;
import com.sericulture.marketandauction.helper.MarketAuctionHelper;
import com.sericulture.marketandauction.helper.Util;
import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.api.RequestBody;
import com.sericulture.marketandauction.model.api.marketauction.ExceptionalTimeRequest;
import com.sericulture.marketandauction.model.api.marketauction.FLexTimeRequest;
import com.sericulture.marketandauction.model.entity.ExceptionalTime;
import com.sericulture.marketandauction.model.entity.FlexTime;
import com.sericulture.marketandauction.model.exceptions.ValidationException;
import com.sericulture.marketandauction.model.mapper.Mapper;
import com.sericulture.marketandauction.repository.ExceptionalTimeRepository;
import com.sericulture.marketandauction.repository.FlexTimeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;

@Service
@Slf4j
public class MISCService {

    @Autowired
    FlexTimeRepository flexTimeRepository;

    @Autowired
    MarketAuctionHelper marketAuctionHelper;

    @Autowired
    ExceptionalTimeRepository exceptionalTimeRepository;

    @Autowired
    Mapper mapper;

    public ResponseEntity<?> flipFlexTime(FLexTimeRequest fLexTimeRequest){
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);

        FlexTime flexTime = flexTimeRepository.findByActivityTypeAndMarketIdAndGodownId(fLexTimeRequest.getActivityType(), fLexTimeRequest.getMarketId(), fLexTimeRequest.getGodownId());
        if(flexTime==null){
            flexTime = mapper.flextimeObjectToEntity(fLexTimeRequest,FlexTime.class);
        }else{
            flexTime.setStart(fLexTimeRequest.isStart());
        }
        flexTimeRepository.save(flexTime);
        return ResponseEntity.ok(rw);
    }

    public ResponseEntity<?> getFlexTime(FLexTimeRequest fLexTimeRequest){
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);

        FlexTime flexTime = flexTimeRepository.findByActivityTypeAndMarketIdAndGodownId(fLexTimeRequest.getActivityType(), fLexTimeRequest.getMarketId(), fLexTimeRequest.getGodownId());
        if(flexTime==null){
            return marketAuctionHelper.retrunIfError(rw,"No Data found for the given request");
        }
        rw.setContent(flexTime);
        return ResponseEntity.ok(rw);
    }
    public ResponseEntity<?> saveOrUpdateExceptionalTime(ExceptionalTimeRequest exceptionalTimeRequest){
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        marketAuctionHelper.getAuthToken(exceptionalTimeRequest);
        ExceptionalTime exceptionalTime = exceptionalTimeRepository.findByMarketIdAndAuctionDate(exceptionalTimeRequest.getMarketId(), Util.getISTLocalDate());
        if(exceptionalTime==null){
            exceptionalTime = new ExceptionalTime();
            validateTimings(exceptionalTimeRequest);
            exceptionalTime.setMarketId(exceptionalTimeRequest.getMarketId());
            exceptionalTime.setAuctionDate(Util.getISTLocalDate());
        }

        exceptionalTime.setAuction1AcceptEndTime(LocalTime.parse(exceptionalTimeRequest.getAuction1AcceptEndTime()));
        exceptionalTime.setAuction1AcceptStartTime(LocalTime.parse(exceptionalTimeRequest.getAuction1AcceptStartTime()));

        exceptionalTime.setAuction2AcceptEndTime(LocalTime.parse(exceptionalTimeRequest.getAuction2AcceptEndTime()));
        exceptionalTime.setAuction2AcceptStartTime(LocalTime.parse(exceptionalTimeRequest.getAuction2AcceptStartTime()));

        exceptionalTime.setAuction3AcceptEndTime(LocalTime.parse(exceptionalTimeRequest.getAuction3AcceptEndTime()));
        exceptionalTime.setAuction3AcceptStartTime(LocalTime.parse(exceptionalTimeRequest.getAuction3AcceptStartTime()));

        exceptionalTime.setAuction1EndTime(LocalTime.parse(exceptionalTimeRequest.getAuction1EndTime()));
        exceptionalTime.setAuction1StartTime(LocalTime.parse(exceptionalTimeRequest.getAuction1StartTime()));

        exceptionalTime.setAuction2EndTime(LocalTime.parse(exceptionalTimeRequest.getAuction2EndTime()));
        exceptionalTime.setAuction2StartTime(LocalTime.parse(exceptionalTimeRequest.getAuction2StartTime()));

        exceptionalTime.setAuction3EndTime(LocalTime.parse(exceptionalTimeRequest.getAuction3EndTime()));
        exceptionalTime.setAuction3StartTime(LocalTime.parse(exceptionalTimeRequest.getAuction3StartTime()));

        exceptionalTime.setIssueBidSlipStartTime(LocalTime.parse(exceptionalTimeRequest.getIssueBidSlipStartTime()));
        exceptionalTime.setIssueBidSlipEndTime(LocalTime.parse(exceptionalTimeRequest.getIssueBidSlipEndTime()));
        exceptionalTimeRepository.save(exceptionalTime);
        return ResponseEntity.ok(rw);
    }

    public ResponseEntity<?> getExceptionalTime(RequestBody requestBody){
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        ExceptionalTime exceptionalTime = exceptionalTimeRepository.findByMarketIdAndAuctionDate(requestBody.getMarketId(),Util.getISTLocalDate());
        if(exceptionalTime==null)
            throw new ValidationException("No data found");
        rw.setContent(exceptionalTime);
        return ResponseEntity.ok(rw);
    }

    private void validateTimings(ExceptionalTimeRequest exceptionalTime) {
        //todo
    }
}
