package com.sericulture.marketandauction.repository;

import com.sericulture.marketandauction.model.entity.CrateMaster;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface CrateMasterRepository extends PagingAndSortingRepository<CrateMaster, Integer> {

    public CrateMaster findByMarketIdAndGodownIdAndRace(int marketId,int godownId,String race);

    public CrateMaster save(CrateMaster crateMaster);

    public Iterable<CrateMaster> saveAll(Iterable<CrateMaster> crateMasterList);
}
