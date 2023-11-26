package com.sericulture.marketandauction.repository;

import com.sericulture.marketandauction.model.entity.MarketMaster;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface MarketMasterRepository extends PagingAndSortingRepository<MarketMaster, Integer>
{
    public Iterable<MarketMaster> saveAll(Iterable<MarketMaster> marketMasterList);

    public MarketMaster save(MarketMaster marketMaster);

    public MarketMaster findById(int id);
}
