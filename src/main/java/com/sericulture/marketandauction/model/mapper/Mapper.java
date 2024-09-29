package com.sericulture.marketandauction.model.mapper;

import com.sericulture.marketandauction.model.api.cocoon.LotBasePriceFixationRequest;
import com.sericulture.marketandauction.model.api.marketauction.*;
import com.sericulture.marketandauction.model.entity.*;
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


    /**
     * Maps lotGroupage Object to lotGroupage  Response Object
     * @param lotGroupageRequestEdit
     * @param <T>
     */
    public <T> T editLotGroupageObjectToEntity(LotGroupageRequestEdit lotGroupageRequestEdit , Class<T> claaz) {
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        log.info("Value of mapper is:",mapper, lotGroupageRequestEdit);
        return (T) mapper.map(lotGroupageRequestEdit, claaz);


    }

    public void editLotGroupageObjectToEntity(LotGroupageRequestEdit lotGroupageRequestEdit, LotGroupage lotGroupage) {
        // Assuming you have a mapping logic here to map fields from LotGroupageRequestEdit to LotGroupage
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        log.info("Mapping lotGroupageRequestEdit to lotGroupage: {}", lotGroupageRequestEdit);
        mapper.map(lotGroupageRequestEdit, lotGroupage);
    }

    public <T> T lotBasePriceFixationObjectToEntity(LotBasePriceFixationRequest lotBasePriceFixationRequest , Class<T> claaz) {
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        log.info("Value of mapper is:",mapper, lotBasePriceFixationRequest);
        return (T) mapper.map(lotBasePriceFixationRequest, claaz);
    }

    public <T> T lotBasePriceFixationEntityToObject(LotBasePriceFixation lotBasePriceFixation , Class<T> claaz) {
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        log.info("Value of mapper is:",mapper, lotBasePriceFixation);
        return (T) mapper.map(lotBasePriceFixation, claaz);
    }


}
