package com.sericulture.marketandauction.repository;

import com.sericulture.marketandauction.model.entity.LotBasePriceFixation;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.time.LocalDate;
import java.util.List;

public interface LotBasePriceFixationRepository extends PagingAndSortingRepository<LotBasePriceFixation, Integer> {
    public LotBasePriceFixation save(LotBasePriceFixation lotBasePriceFixation);
    List<LotBasePriceFixation> findTop10ByMarketIdOrderByIdDesc(int marketId);
    public LotBasePriceFixation findByMarketIdAndFixationDate(int id, LocalDate fixationDate);
}
