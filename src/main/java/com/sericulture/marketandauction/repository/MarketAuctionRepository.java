package com.sericulture.marketandauction.repository;

import com.sericulture.marketandauction.model.entity.MarketAuction;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;

public interface MarketAuctionRepository extends PagingAndSortingRepository<MarketAuction, BigInteger> {

    public MarketAuction save(MarketAuction marketAuction);

    public List<MarketAuction> findAllByFarmerIdAndMarketAuctionDate(BigInteger farmerId,LocalDate date);


    public List<MarketAuction> findAllByMarketAuctionDate(LocalDate date);

    public List<MarketAuction> findAllByStatusAndMarketAuctionDate(String status,LocalDate date);


}
