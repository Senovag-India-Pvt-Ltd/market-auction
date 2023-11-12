package com.sericulture.marketandauction.model.mapper;

import com.sericulture.marketandauction.model.api.marketauction.MarketAuctionRequest;
import com.sericulture.marketandauction.model.entity.MarketAuction;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

;


@Slf4j
@Component
public class Mapper {

    @Autowired
    ModelMapper mapper;

    /**
     * Maps Education Entity to Education Response Object
     * @param marketAuctionEntity
     * @param <T>
     */
    public <T> T marketAuctionEntityToObject(MarketAuction marketAuctionEntity, Class<T> claaz) {
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        log.info("Value of mapper is:",mapper, marketAuctionEntity);
        return (T) mapper.map(marketAuctionEntity, claaz);
    }

    /**
     * Maps Education Entity to Education Response Object
     * @param marketAuctionRequest
     * @param <T>
     */
    public <T> T marketAuctionObjectToEntity(MarketAuctionRequest marketAuctionRequest, Class<T> claaz) {
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        log.info("Value of mapper is:",mapper, marketAuctionRequest);
        return (T) mapper.map(marketAuctionRequest, claaz);
    }


}
