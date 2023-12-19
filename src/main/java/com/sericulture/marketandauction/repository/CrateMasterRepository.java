package com.sericulture.marketandauction.repository;

import com.sericulture.marketandauction.model.entity.CrateMaster;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface CrateMasterRepository extends PagingAndSortingRepository<CrateMaster, Integer> {

    public CrateMaster findByMarketIdAndGodownIdAndRaceMasterId(int marketId,int godownId,int raceMasterId);

    public CrateMaster save(CrateMaster crateMaster);

    public Iterable<CrateMaster> saveAll(Iterable<CrateMaster> crateMasterList);
}
