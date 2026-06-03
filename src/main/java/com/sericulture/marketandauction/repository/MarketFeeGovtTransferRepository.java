package com.sericulture.marketandauction.repository;

import com.sericulture.marketandauction.model.entity.MarketFeeGovtTransfer;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sericulture.marketandauction.helper.MarketAuctionQueryConstants;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface MarketFeeGovtTransferRepository extends PagingAndSortingRepository<MarketFeeGovtTransfer, Long> {

    MarketFeeGovtTransfer save(MarketFeeGovtTransfer marketFeeGovtTransfer);

    List<MarketFeeGovtTransfer> findByCollectionDateAndMarketIdAndActiveTrue(LocalDate collectionDate, Integer marketId);

    boolean existsByLotGroupageIdAndActiveTrue(Long lotGroupageId);

    @Query(value = "SELECT TOP 1 govt_account_number FROM govt_account WHERE active = 1", nativeQuery = true)
    String getActiveGovtAccountNumber();

    List<MarketFeeGovtTransfer> findByFruitsIdAndActiveTrue(String fruitsId);

    @Query(nativeQuery = true, value = MarketAuctionQueryConstants.MARKET_FEE_TRANSFER_CSV_QUERY)
    List<Object[]> getTransferredLotsForCSV(
            @Param("marketId") int marketId,
            @Param("date") LocalDate date
    );

    @Modifying
    @Query("""
        UPDATE MarketFeeGovtTransfer t
        SET t.transferStatus = 'TRANSFERRED',
            t.transferredDate = :transferredDate,
            t.govtAccountNumber = :govtAccountNumber,
            t.transactionReference = :transactionReference
        WHERE t.id = :id
    """)
    void markAsTransferred(
            @Param("id") Long id,
            @Param("transferredDate") LocalDate transferredDate,
            @Param("govtAccountNumber") String govtAccountNumber,
            @Param("transactionReference") String transactionReference
    );
}