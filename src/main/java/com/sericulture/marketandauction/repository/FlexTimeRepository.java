package com.sericulture.marketandauction.repository;

import com.sericulture.marketandauction.model.entity.FlexTime;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface FlexTimeRepository extends PagingAndSortingRepository<FlexTime, Integer> {

    public FlexTime save(FlexTime flexTime);

    public List<FlexTime> findAll();

    public List<FlexTime> findByActivityType(String activityType);

    public FlexTime findByActivityTypeAndMarketIdAndGodownId(String activityType,int marketId,int godownId);
}