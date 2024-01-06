package com.sericulture.marketandauction.service;

import com.sericulture.marketandauction.helper.MarketAuctionHelper;
import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.api.RequestBody;
import com.sericulture.marketandauction.model.api.marketauction.FLexTimeRequest;
import com.sericulture.marketandauction.model.entity.FlexTime;
import com.sericulture.marketandauction.model.mapper.Mapper;
import com.sericulture.marketandauction.repository.FlexTimeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class MISCService {

    @Autowired
    FlexTimeRepository flexTimeRepository;

    @Autowired
    MarketAuctionHelper marketAuctionHelper;

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
}
