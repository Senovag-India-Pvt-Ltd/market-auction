package com.sericulture.marketandauction.repository;

import com.sericulture.marketandauction.model.entity.Bin;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.math.BigInteger;
import java.util.List;

public interface BinRepository extends PagingAndSortingRepository<Bin, BigInteger> {

    public Bin save(Bin bin);

    public List<Bin> findByMarketAuctionId(BigInteger marketAuctionId);

    @Query("select b.allottedBinId from Bin b where b.marketAuctionId=:marketAuctionId and b.type=:type")
    public List<Integer> findAllByMarketAuctionIdAndType(@Param("marketAuctionId") BigInteger marketAuctionId,@Param("type") String type);

    public Iterable<Bin> saveAll(Iterable<Bin> binList);



}
