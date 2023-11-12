package com.sericulture.marketandauction.repository;

import com.sericulture.marketandauction.model.entity.Bin;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.math.BigInteger;
import java.util.List;

public interface BinRepository extends PagingAndSortingRepository<Bin, BigInteger> {

    public Bin save(Bin bin);

    public List<Bin> findByMarketAuctionId(BigInteger marketAuctionId);

    public Iterable<Bin> saveAll(Iterable<Bin> binList);



}
