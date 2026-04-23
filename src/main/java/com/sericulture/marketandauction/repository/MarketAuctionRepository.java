package com.sericulture.marketandauction.repository;

import com.sericulture.marketandauction.model.entity.MarketAuction;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;

public interface MarketAuctionRepository extends PagingAndSortingRepository<MarketAuction, BigInteger> {

    public MarketAuction save(MarketAuction marketAuction);

    public List<MarketAuction> findAllByFarmerIdAndMarketAuctionDate(BigInteger farmerId,LocalDate date);

    public List<MarketAuction> findAllByReelerIdAndMarketAuctionDate(Integer reelerId,LocalDate date);

    public List<MarketAuction> findAllByMarketAuctionDate(LocalDate date);

    public List<MarketAuction> findAllByStatusAndMarketAuctionDate(String status,LocalDate date);

    public MarketAuction findById(BigInteger id);

    @Modifying
    @Query("""
            UPDATE MarketAuction ma
            SET ma.active = false
            WHERE ma.id = :marketAuctionId
            """)
    void updateActiveById(@Param("marketAuctionId") BigInteger marketAuctionId);




}
