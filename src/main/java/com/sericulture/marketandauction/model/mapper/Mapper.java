package com.sericulture.marketandauction.model.mapper;

import com.sericulture.marketandauction.model.api.marketauction.FLexTimeRequest;
import com.sericulture.marketandauction.model.api.marketauction.LotGroupageRequest;
import com.sericulture.marketandauction.model.api.marketauction.MarketAuctionRequest;
import com.sericulture.marketandauction.model.api.marketauction.ReelerBidRequest;
import com.sericulture.marketandauction.model.entity.FlexTime;
import com.sericulture.marketandauction.model.entity.LotGroupage;
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
     * @param reelerBidRequest
     * @param <T>
     */
    public <T> T reelerAuctionObjectToEntity(ReelerBidRequest reelerBidRequest, Class<T> claaz) {
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        log.info("Value of mapper is:",mapper, reelerBidRequest);
        return (T) mapper.map(reelerBidRequest, claaz);
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
    /**
     * Maps lotGroupage Entity to lotGroupage Response Object
     * @param lotGroupageEntity
     * @param <T>
     */
    public <T> T lotGroupageEntityToObject(LotGroupage lotGroupageEntity, Class<T> claaz) {
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        log.info("Value of mapper is:",mapper,lotGroupageEntity);
        return (T) mapper.map(lotGroupageEntity, claaz);
    }


    /**
     * Maps lotGroupage Object to lotGroupage  Response Object
     * @param lotGroupageRequest
     * @param <T>
     */
    public <T> T lotGroupageObjectToEntity(LotGroupageRequest lotGroupageRequest , Class<T> claaz) {
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        log.info("Value of mapper is:",mapper, lotGroupageRequest);
        return (T) mapper.map(lotGroupageRequest, claaz);
    }



}
