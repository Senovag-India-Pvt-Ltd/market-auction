package com.sericulture.marketandauction.repository;

import com.sericulture.marketandauction.model.entity.BinCounter;
import com.sericulture.marketandauction.model.entity.ReelerVidBlockedAmount;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.math.BigInteger;
import java.time.LocalDate;

public interface ReelerVidBlockedAmountRepository extends PagingAndSortingRepository<ReelerVidBlockedAmount, BigInteger> {

    public ReelerVidBlockedAmount save(ReelerVidBlockedAmount reelerVidBlockedAmount);

    @Query(value = "SELECT sum(amount) from ReelerVidBlockedAmount where  reelerVirtualAccountNumber=:reelerVirtualAccountNumber and auctionDate=:auctionDate and marketId=:marketId and status='blocked'")
    public Object getReelerBlockedAMountPerAuctionDate(String reelerVirtualAccountNumber, LocalDate auctionDate,int marketId);

    @Query(value = "select count(rvba) from ReelerVidBlockedAmount rvba where reelerVirtualAccountNumber =:reelerVirtualAccountNumber and auctionDate=:auctionDate")
    public Long findByReelerVirtualAccountNumberAndAuctionDate(String reelerVirtualAccountNumber,LocalDate auctionDate);


}
