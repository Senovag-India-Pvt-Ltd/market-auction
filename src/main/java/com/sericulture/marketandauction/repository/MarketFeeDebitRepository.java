package com.sericulture.marketandauction.repository;

import com.sericulture.marketandauction.model.entity.MarketFeeDebit;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MarketFeeDebitRepository extends PagingAndSortingRepository<MarketFeeDebit, Long> {

    MarketFeeDebit save(MarketFeeDebit marketFeeDebit);
}