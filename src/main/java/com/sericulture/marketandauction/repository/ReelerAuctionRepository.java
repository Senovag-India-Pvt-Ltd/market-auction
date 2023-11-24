package com.sericulture.marketandauction.repository;

import com.sericulture.marketandauction.model.entity.ReelerAuction;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface ReelerAuctionRepository  extends PagingAndSortingRepository<ReelerAuction, Integer> {

    public ReelerAuction save(ReelerAuction reelerAuction);
}
