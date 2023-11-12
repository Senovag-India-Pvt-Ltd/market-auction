package com.sericulture.marketandauction.repository;

import com.sericulture.marketandauction.model.entity.BinCounterMaster;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface BinCounterMasterRepository extends PagingAndSortingRepository<BinCounterMaster, Integer> {

    public BinCounterMaster save(BinCounterMaster binCounter);

    public BinCounterMaster findByMarketIdAndGodownId(int marketId, int godownId);
}
