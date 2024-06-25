package com.sericulture.marketandauction.repository;

import com.sericulture.marketandauction.model.entity.LotGroupage;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LotGroupageRepository extends PagingAndSortingRepository<LotGroupage, Long> {

    public LotGroupage save(LotGroupage lotGroupage);
}
