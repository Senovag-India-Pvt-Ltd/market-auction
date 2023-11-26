package com.sericulture.marketandauction.repository;

import com.sericulture.marketandauction.model.entity.BinCounterMaster;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface BinCounterMasterRepository extends PagingAndSortingRepository<BinCounterMaster, Integer> {

    public BinCounterMaster save(BinCounterMaster binCounter);

    public BinCounterMaster findByMarketIdAndGodownId(int marketId, int godownId);

    @Query(value = "select * from bin_counter_master with(ROWLOCK, XLOCK) where bin_counter_master_id=:id",  nativeQuery = true)
    public BinCounterMaster getByMarketIdAndAuction(int id);
}
