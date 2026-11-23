package com.sericulture.marketandauction.repository;

import com.sericulture.marketandauction.model.entity.LegacyCocoonMarketTxn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/** XC-004: legacy cocoon market transactions, master lookups for the import, and the trace queries. */
@Repository
public interface LegacyCocoonMarketRepository extends JpaRepository<LegacyCocoonMarketTxn, Long> {

    List<LegacyCocoonMarketTxn> findBySourceSystemAndLegacyTxnIdIn(String sourceSystem, Collection<String> legacyTxnIds);

    /** 0 market_master_id, 1 market_name. */
    @Query(nativeQuery = true, value = "select market_master_id, market_name from market_master where active = 1")
    List<Object[]> markets();

    /** 0 race_id, 1 race_name. */
    @Query(nativeQuery = true, value = "select race_id, race_name from race_master where active = 1")
    List<Object[]> races();

    /** 0 FARMER_ID, 1 fruits_id. */
    @Query(nativeQuery = true, value = "select FARMER_ID, fruits_id from farmer where ACTIVE = 1 and fruits_id in (:fruitsIds)")
    List<Object[]> farmersByFruitsId(@Param("fruitsIds") Collection<String> fruitsIds);

    /** 0 FARMER_ID, 1 mobile_number. */
    @Query(nativeQuery = true, value = "select FARMER_ID, mobile_number from farmer where ACTIVE = 1 and mobile_number in (:mobiles)")
    List<Object[]> farmersByMobile(@Param("mobiles") Collection<String> mobiles);

    /** 0 reeler_id, 1 reeling_license_number. */
    @Query(nativeQuery = true, value = "select reeler_id, reeling_license_number from reeler where active = 1 and reeling_license_number in (:licences)")
    List<Object[]> reelersByLicence(@Param("licences") Collection<String> licences);

    /** New-system lots of the markets / dates in the file: 0 market_id, 1 auction_date, 2 allotted_lot_id, 3 lot_id. */
    @Query(nativeQuery = true, value = """
            select l.market_id, l.auction_date, l.allotted_lot_id, l.lot_id
            from lot l
            where l.active = 1 and l.market_id in (:marketIds) and l.auction_date between :fromDate and :toDate""")
    List<Object[]> newSystemLots(@Param("marketIds") Collection<Long> marketIds,
                                 @Param("fromDate") LocalDate fromDate,
                                 @Param("toDate") LocalDate toDate);

    /**
     * Trace, legacy side (rows also present in the new system are left out to avoid showing them twice).
     * 0 id, 1 source_system, 2 legacy_txn_id, 3 market name, 4 auction_date, 5 lot_number, 6 fruits id, 7 farmer name,
     * 8 mobile, 9 reeler licence, 10 reeler name, 11 race, 12 weight, 13 rate, 14 amount
     */
    @Query(nativeQuery = true, value = """
            select top 500 t.legacy_cocoon_market_txn_id, t.source_system, t.legacy_txn_id,
                   isnull(m.market_name, t.market_name_raw), t.auction_date, t.lot_number,
                   t.farmer_fruits_id, t.farmer_name, t.farmer_mobile, t.reeler_licence_no, t.reeler_name,
                   isnull(rm.race_name, t.race_name_raw), t.weight_kg, t.rate_per_kg, t.amount
            from legacy_cocoon_market_txn t
            left join market_master m on m.market_master_id = t.market_master_id
            left join race_master rm on rm.race_id = t.race_master_id
            where t.ACTIVE = 1 and t.new_system_lot_id is null
              and (:fruitsId = '' or t.farmer_fruits_id = :fruitsId)
              and (:mobile = '' or t.farmer_mobile = :mobile)
              and (:lotNumber = '' or t.lot_number = :lotNumber)
              and (:marketId = 0 or t.market_master_id = :marketId)
              and (:legacyTxnId = '' or t.legacy_txn_id = :legacyTxnId)
              and t.auction_date between :fromDate and :toDate
            order by t.auction_date desc, t.lot_number""")
    List<Object[]> traceLegacy(@Param("fruitsId") String fruitsId, @Param("mobile") String mobile,
                               @Param("lotNumber") String lotNumber, @Param("marketId") Long marketId,
                               @Param("legacyTxnId") String legacyTxnId,
                               @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    /**
     * Trace, new-system side: weighed lots. 0 lot_id, 1 market name, 2 auction_date, 3 allotted_lot_id, 4 fruits id,
     * 5 farmer name, 6 mobile, 7 reeler licence, 8 reeler name, 9 race, 10 weight, 11 rate (accepted bid), 12 amount
     */
    @Query(nativeQuery = true, value = """
            select top 500 l.lot_id, m.market_name, l.auction_date, l.allotted_lot_id,
                   f.fruits_id, f.first_name, f.mobile_number, r.reeling_license_number, r.name,
                   rm.race_name, l.LOT_WEIGHT_AFTER_WEIGHMENT, raa.AMOUNT, l.LOT_SOLD_OUT_AMOUNT
            from lot l
            join market_auction ma on ma.market_auction_id = l.market_auction_id and ma.active = 1
            left join farmer f on f.FARMER_ID = ma.farmer_id
            left join market_master m on m.market_master_id = l.market_id
            left join reeler_auction_accepted raa on raa.REELER_AUCTION_ACCEPTED_ID = l.REELER_AUCTION_ACCEPTED_ID
            left join reeler r on r.reeler_id = raa.REELER_ID
            left join race_master rm on rm.race_id = ma.RACE_MASTER_ID
            where l.active = 1
              and (:fruitsId = '' or f.fruits_id = :fruitsId)
              and (:mobile = '' or f.mobile_number = :mobile)
              and (:lotNumber = '' or cast(l.allotted_lot_id as varchar(50)) = :lotNumber)
              and (:marketId = 0 or l.market_id = :marketId)
              and :legacyTxnId = ''
              and l.auction_date between :fromDate and :toDate
            order by l.auction_date desc, l.allotted_lot_id""")
    List<Object[]> traceNewSystem(@Param("fruitsId") String fruitsId, @Param("mobile") String mobile,
                                  @Param("lotNumber") String lotNumber, @Param("marketId") Long marketId,
                                  @Param("legacyTxnId") String legacyTxnId,
                                  @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);
}
