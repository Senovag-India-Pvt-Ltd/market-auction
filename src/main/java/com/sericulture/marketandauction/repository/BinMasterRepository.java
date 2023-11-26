package com.sericulture.marketandauction.repository;

import com.sericulture.marketandauction.model.entity.BinMaster;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;

public interface BinMasterRepository extends PagingAndSortingRepository<BinMaster, Integer> {

    public BinMaster save(BinMaster BinMaster);

    public Iterable<BinMaster> saveAll(Iterable<BinMaster> binMasters);

    @Query("select bm.binNumber from BinMaster bm where bm.marketId=:marketId and bm.type=:type and bm.status=:status and bm.binNumber>:binNumber order by bm.binNumber")
    public List<Integer> findByMarketIdAndTypeAndStatusAndBinNumber(@Param("marketId") int marketId,@Param("type") String type,@Param("status") String status,@Param("binNumber") int binNumber);

    @Query("select bm.binNumber from BinMaster bm where bm.marketId=:marketId and bm.godownId=:godownId and bm.type=:type and bm.status=:status and bm.binNumber>=:binNumberStart  and bm.binNumber<=:binNumberEnd order by bm.binNumber limit:totalBins")
    public List<Integer> findByMarketIdAndGodownIdAndTypeAndStatusAndBinNumber(@Param("marketId") int marketId,@Param("godownId")int godownId, @Param("type") String type,@Param("status") String status,@Param("binNumberStart") int binNumberStart,@Param("binNumberEnd") int binNumberEnd,@Param("totalBins") int totalBins);

}
