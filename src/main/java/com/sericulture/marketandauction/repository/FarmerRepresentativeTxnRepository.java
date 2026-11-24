package com.sericulture.marketandauction.repository;

import com.sericulture.marketandauction.model.entity.FarmerRepresentativeTxn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** SR-005: e-Inward transactions brought by a farmer's authorised agent, and the trace queries. */
@Repository
public interface FarmerRepresentativeTxnRepository extends JpaRepository<FarmerRepresentativeTxn, Long> {

    Optional<FarmerRepresentativeTxn> findByMarketAuctionIdAndActive(Long marketAuctionId, Boolean active);

    /** 0 market_auction_id, 1 farmer_id, 2 market_id, 3 market_auction_date, 4 status. Active e-Inward only. */
    @Query(nativeQuery = true, value = """
            select ma.market_auction_id, ma.farmer_id, ma.market_id, ma.market_auction_date, ma.status
            from market_auction ma where ma.market_auction_id = :marketAuctionId and ma.active = 1""")
    List<Object[]> auction(@Param("marketAuctionId") Long marketAuctionId);

    /** 0 farmer_representative_id, 1 farmer_id, 2 ACTIVE, 3 authorised_from, 4 authorised_to. */
    @Query(nativeQuery = true, value = """
            select r.farmer_representative_id, r.farmer_id, r.ACTIVE, r.authorised_from, r.authorised_to
            from farmer_representative r where r.farmer_representative_id = :id""")
    List<Object[]> agent(@Param("id") Long farmerRepresentativeId);

    /**
     * Agent with the farmer it belongs to (also when the agent was later removed, so old transactions stay traceable).
     * 0 farmer_representative_id, 1 name, 2 mobile_number, 3 ACTIVE, 4 authorised_from, 5 authorised_to, 6 FARMER_ID,
     * 7 farmer name, 8 fruits_id, 9 farmer_number, 10 farmer mobile, 11 relationship name
     */
    @Query(nativeQuery = true, value = """
            select r.farmer_representative_id, r.name, r.mobile_number, r.ACTIVE, r.authorised_from, r.authorised_to,
                   f.FARMER_ID, ltrim(rtrim(concat(f.first_name, ' ', f.middle_name, ' ', f.last_name))), f.fruits_id,
                   f.farmer_number, f.mobile_number, rel.RELATIONSHIP_NAME
            from farmer_representative r
            join farmer f on f.FARMER_ID = r.farmer_id
            left join relationship rel on rel.RELATIONSHIP_ID = r.relationship_id
            where r.farmer_representative_id = :id""")
    List<Object[]> agentWithFarmer(@Param("id") Long farmerRepresentativeId);

    /**
     * Lots of the e-Inward transactions brought by the agent. 0 market_auction_id, 1 market_auction_date, 2 market name,
     * 3 allotted_lot_id, 4 lot status, 5 race, 6 weight after weighment, 7 accepted rate, 8 sold amount, 9 reeler name,
     * 10 reeling licence, 11 e-Inward status, 12 farmer_id
     */
    @Query(nativeQuery = true, value = """
            select top 1000 ma.market_auction_id, ma.market_auction_date, m.market_name, l.allotted_lot_id, l.status,
                   rm.race_name, l.LOT_WEIGHT_AFTER_WEIGHMENT, raa.AMOUNT, l.LOT_SOLD_OUT_AMOUNT, r.name,
                   r.reeling_license_number, ma.status, t.farmer_id
            from farmer_representative_txn t
            join market_auction ma on ma.market_auction_id = t.market_auction_id and ma.active = 1
            left join lot l on l.market_auction_id = ma.market_auction_id and l.active = 1
            left join market_master m on m.market_master_id = ma.market_id
            left join race_master rm on rm.race_id = ma.RACE_MASTER_ID
            left join reeler_auction_accepted raa on raa.REELER_AUCTION_ACCEPTED_ID = l.REELER_AUCTION_ACCEPTED_ID
            left join reeler r on r.reeler_id = raa.REELER_ID
            where t.ACTIVE = 1 and t.farmer_representative_id = :id
              and ma.market_auction_date between :fromDate and :toDate
            order by ma.market_auction_date desc, ma.market_auction_id desc, l.allotted_lot_id""")
    List<Object[]> lotsByAgent(@Param("id") Long farmerRepresentativeId,
                               @Param("fromDate") LocalDate fromDate,
                               @Param("toDate") LocalDate toDate);
}
