package com.sericulture.marketandauction.repository;

import com.sericulture.marketandauction.model.entity.Lot;
import com.sericulture.marketandauction.model.entity.LotWeightDetail;
import com.sericulture.marketandauction.model.entity.MarketAuction;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;

public interface LotWeightDetailRepository extends PagingAndSortingRepository<LotWeightDetail, BigInteger> {

    public Iterable<LotWeightDetail> saveAll(Iterable<LotWeightDetail> lotWeightDetails);

    public LotWeightDetail save(LotWeightDetail lotWeightDetail);

    @Query("select netWeight from LotWeightDetail where lotId=:lotId")
    public List<Float> findAllByLotId(BigInteger lotId);

    @Query("SELECT w FROM LotWeightDetail w " +
            "JOIN Lot l ON l.allottedLotId = w.lotId " +
            "WHERE l.allottedLotId = :allottedLotId " +
            "AND l.auctionDate = :auctionDate " +
            "AND w.crateNumber = :crateNumber")
    public LotWeightDetail findByAllottedLotIdAndAuctionDateAndCrateNumber(@Param("allottedLotId") Integer allottedLotId,
                                                                                 @Param("auctionDate") LocalDate auctionDate,
                                                                                 @Param("crateNumber") Integer crateNumber);


}
