package com.sericulture.marketandauction.repository;

import com.sericulture.marketandauction.model.entity.MarketMaster;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface MarketMasterRepository extends PagingAndSortingRepository<MarketMaster, Integer>
{
    public Iterable<MarketMaster> saveAll(Iterable<MarketMaster> marketMasterList);

    public MarketMaster save(MarketMaster marketMaster);

    public MarketMaster findById(int id);

    @Query(nativeQuery = true,value = """
            SELECT mtm.market_type_master_name , mtm.reeler_fee,mtm.farmer_fee,mtm.trader_fee \s
            from market_master mm , market_type_master mtm
            where mm.market_type_master_id  = mtm.market_type_master_id \s
            and mm.market_master_id =:marketId""")
    public Object[][] getBrokarageInPercentageForMarket(int marketId);

    @Query(nativeQuery = true,value = """
            select market_master_id from market_master mm where active = 1 and market_type_master_id = 13 and is_test is null or is_test = 0 """)
    public List<Integer> getListOfMarketIds();
}
