package com.sericulture.marketandauction.repository;

import com.sericulture.marketandauction.model.entity.BinCounter;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.math.BigInteger;
import java.time.LocalDate;

public interface BinCounterRepository extends PagingAndSortingRepository<BinCounter, BigInteger> {

    public BinCounter save(BinCounter binCounter);


    public BinCounter findByMarketIdAndAuctionDate(int marketId, LocalDate date);

    public BinCounter findByMarketIdAndGodownIdAndAuctionDate(int marketId,int godownId, LocalDate date);

    @Query(value="select * from bin_counter with(ROWLOCK) where bin_counter_id= :id", nativeQuery=true)
    public BinCounter getByMarketEntryForTheDayLocked(long id);
}
