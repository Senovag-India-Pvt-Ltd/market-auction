package com.sericulture.marketandauction.model.mapper;

import com.sericulture.marketandauction.model.api.marketauction.FLexTimeRequest;
import com.sericulture.marketandauction.model.api.marketauction.MarketAuctionRequest;
import com.sericulture.marketandauction.model.api.marketauction.ReelerAuctionRequest;
import com.sericulture.marketandauction.model.entity.FlexTime;
import com.sericulture.marketandauction.model.entity.MarketAuction;
import com.sericulture.marketandauction.model.entity.ReelerAuction;
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
     * Maps marketauction Entity to marketauction Response Object
     * @param marketAuctionEntity
     * @param <T>
     */
    public <T> T marketAuctionEntityToObject(MarketAuction marketAuctionEntity, Class<T> claaz) {
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        log.info("Value of mapper is:",mapper, marketAuctionEntity);
        return (T) mapper.map(marketAuctionEntity, claaz);
    }

    /**
     * Maps marketauction Entity to marketauction Response Object
     * @param marketAuctionRequest
     * @param <T>
     */
    public <T> T marketAuctionObjectToEntity(MarketAuctionRequest marketAuctionRequest, Class<T> claaz) {
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        log.info("Value of mapper is:",mapper, marketAuctionRequest);
        return (T) mapper.map(marketAuctionRequest, claaz);
    }

    /**
     * Maps reelerAuction Entity to marketauction Response Object
     * @param reelerAuctionEntity
     * @param <T>
     */
    public <T> T reelerAuctionEntityToObject(ReelerAuction reelerAuctionEntity, Class<T> claaz) {
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        log.info("Value of mapper is:",mapper, reelerAuctionEntity);
        return (T) mapper.map(reelerAuctionEntity, claaz);
    }

    /**
     * Maps reelerAuction Entity to marketauction Response Object
     * @param reelerAuctionRequest
     * @param <T>
     */
    public <T> T reelerAuctionObjectToEntity(ReelerAuctionRequest reelerAuctionRequest, Class<T> claaz) {
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        log.info("Value of mapper is:",mapper, reelerAuctionRequest);
        return (T) mapper.map(reelerAuctionRequest, claaz);
    }

    /**
     * Maps flexTimeRequest Entity to flexTimeEntity Response Object
     * @param flexTimeEntity
     * @param <T>
     */
    public <T> T flexTimeEntityToObject(FlexTime flexTimeEntity, Class<T> claaz) {
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        log.info("Value of mapper is:",mapper, flexTimeEntity);
        return (T) mapper.map(flexTimeEntity, claaz);
    }

    /**
     * Maps flexTime Entity to flexTimeEntity Response Object
     * @param fLexTimeRequest
     * @param <T>
     */
    public <T> T flextimeObjectToEntity(FLexTimeRequest fLexTimeRequest, Class<T> claaz) {
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        log.info("Value of mapper is:",mapper, fLexTimeRequest);
        return (T) mapper.map(fLexTimeRequest, claaz);
    }


}
