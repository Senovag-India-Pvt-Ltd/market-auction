package com.sericulture.marketandauction.repository;

import com.sericulture.marketandauction.model.entity.TransactionFileGenQueue;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;
import java.util.Set;

public interface TransactionFileGenQueueRepository extends CrudRepository<TransactionFileGenQueue, Integer> {

    public TransactionFileGenQueue save(TransactionFileGenQueue at) ;

    @Query("select tq from TransactionFileGenQueue tq where (tq.status = 'requested' or (tq.retryCount < 2 and tq.status = 'failed')) order by tq.createdDate limit 1")
    public TransactionFileGenQueue getQueueEntry();

    public boolean existsTransactionFileGenQueueByMarketIdAndAuctionDateAndStatusIn(int marketId, LocalDate auctionDate, Set<String> status);

    public boolean existsTransactionFileGenQueueByMarketIdAndFileName(int marketId,String fileName);


}