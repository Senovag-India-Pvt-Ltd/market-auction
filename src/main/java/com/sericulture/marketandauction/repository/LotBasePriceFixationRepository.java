package com.sericulture.marketandauction.repository;

import com.sericulture.marketandauction.model.entity.LotBasePriceFixation;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LotBasePriceFixationRepository extends PagingAndSortingRepository<LotBasePriceFixation, Integer> {
    public LotBasePriceFixation save(LotBasePriceFixation lotBasePriceFixation);
    public LotBasePriceFixation findByIdAndActive(long id,boolean isActive);
    List<LotBasePriceFixation> findTop10ByMarketIdAndPriceTypeAndActiveOrderByIdDesc(int marketId,String priceType,boolean isActive);
    List<LotBasePriceFixation> findByMarketIdAndPriceTypeAndActiveOrderByIdDesc(int marketId,String priceType,boolean isActive);
    public LotBasePriceFixation findByMarketIdAndFixationDateAndPriceTypeAndActive(int id, LocalDate fixationDate,String priceType,boolean isActive);

    public LotBasePriceFixation findByMarketIdAndFixationDateAndAllottedLotIdAndPriceTypeAndActive(int id, LocalDate fixationDate,int allottedLotId ,String priceType,boolean isActive);

    @Query(nativeQuery = true, value = """
    SELECT DISTINCT l.allotted_lot_id
    FROM lot l
    INNER JOIN market_auction ma ON ma.market_auction_id = l.market_auction_id and ma.active = 1
    WHERE l.auction_date = :auctionDate
      AND l.market_id = :marketId
      And l.active = 1
""")
    List<Integer> getAllottedLotIdsForPrice(
            @Param("auctionDate") LocalDate auctionDate,
            @Param("marketId") Integer marketId
    );
}
