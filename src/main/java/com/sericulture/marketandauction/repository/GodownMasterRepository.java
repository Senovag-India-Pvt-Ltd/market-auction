package com.sericulture.marketandauction.repository;

import com.sericulture.marketandauction.model.entity.GodownMaster;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface GodownMasterRepository extends PagingAndSortingRepository<GodownMaster, Integer>{
    public Iterable<GodownMaster> saveAll(Iterable<GodownMaster> godownMasterList);

    public GodownMaster save(GodownMaster godownMaster);
}
