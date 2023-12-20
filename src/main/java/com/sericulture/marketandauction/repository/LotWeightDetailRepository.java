package com.sericulture.marketandauction.repository;

import com.sericulture.marketandauction.model.entity.LotWeightDetail;
import com.sericulture.marketandauction.model.entity.MarketAuction;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.math.BigInteger;

public interface LotWeightDetailRepository extends PagingAndSortingRepository<LotWeightDetail, BigInteger> {

    public Iterable<LotWeightDetail> saveAll(Iterable<LotWeightDetail> lotWeightDetails);


}
