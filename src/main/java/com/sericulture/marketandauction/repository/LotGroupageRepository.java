package com.sericulture.marketandauction.repository;

import com.sericulture.marketandauction.model.entity.LotGroupage;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface LotGroupageRepository extends PagingAndSortingRepository<LotGroupage, Long> {

    public LotGroupage save(LotGroupage lotGroupage);

    @Query(nativeQuery = true, value = "SELECT ma.market_auction_id,l.lot_id " +
            "FROM market_auction ma " +
            "LEFT JOIN lot l ON ma.market_auction_id = l.market_auction_id " +
            "WHERE l.allotted_lot_id = :allottedLotId " +
            "AND ma.market_auction_date = :marketAuctionDate")
    public List<Object[]> getMarketAuctionIdByAllottedLotIdAndMarketAuctionDate(int allottedLotId, LocalDate marketAuctionDate);
}
