package com.sericulture.marketandauction.repository;

import com.sericulture.marketandauction.model.entity.ReelerBankTransfer;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReelerBankTransferRepository extends PagingAndSortingRepository<ReelerBankTransfer, Long> {

    ReelerBankTransfer save(ReelerBankTransfer reelerBankTransfer);

    boolean existsByReelerIdAndMarketIdAndAuctionDateAndTransferStatusAndActiveTrue(
            Integer reelerId, Integer marketId, LocalDate auctionDate, String transferStatus);

    boolean existsByExternalUnitIdAndMarketIdAndAuctionDateAndTransferStatusAndActiveTrue(Integer externalUnitId, Integer marketId, LocalDate auctionDate, String transferStatus);

    @Query(nativeQuery = true, value = """
            SELECT ROW_NUMBER() OVER (ORDER BY rbt.id) AS serial_number,
                   r.name, r.reeling_license_number, r.mobile_number,
                   rbt.bank_account_number, rbt.ifsc_code,
                   rbt.bank_name, rbt.branch_name,
                   rbt.virtual_account_number, rbt.transfer_amount
            FROM reeler_bank_transfer rbt
            INNER JOIN reeler r ON r.reeler_id = rbt.reeler_id AND r.active = 1
            WHERE rbt.market_id = :marketId
              AND CAST(rbt.auction_date AS DATE) = :auctionDate
              AND rbt.active = 1
            ORDER BY rbt.id
            """)
    List<Object[]> getTransferRecordsForCSV(@Param("marketId") int marketId,
                                            @Param("auctionDate") LocalDate auctionDate);

    @Query(nativeQuery = true, value = """
            SELECT ROW_NUMBER() OVER (ORDER BY rbt.id) AS serial_number,
                   eur.name, eur.license_number, '' AS mobile_number,
                   rbt.bank_account_number, rbt.ifsc_code,
                   rbt.bank_name, rbt.branch_name,
                   rbt.virtual_account_number, rbt.transfer_amount
            FROM reeler_bank_transfer rbt
            INNER JOIN external_unit_registration eur
                ON eur.external_unit_registration_id = rbt.external_unit_id AND eur.active = 1
            WHERE rbt.market_id = :marketId
              AND CAST(rbt.auction_date AS DATE) = :auctionDate
              AND rbt.buyer_type = 'RSP'
              AND rbt.active = 1
            ORDER BY rbt.id
            """)
    List<Object[]> getTransferRecordsForRspCSV(@Param("marketId") int marketId,
                                               @Param("auctionDate") LocalDate auctionDate);
}