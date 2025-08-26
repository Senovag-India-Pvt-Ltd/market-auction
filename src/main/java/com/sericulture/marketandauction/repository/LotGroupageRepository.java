package com.sericulture.marketandauction.repository;

import com.sericulture.marketandauction.model.entity.LotGroupage;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface LotGroupageRepository extends PagingAndSortingRepository<LotGroupage, Long> {

    public LotGroupage save(LotGroupage lotGroupage);

//    public LotGroupage findByLotGroupageIdAndActiveIn(@Param("lotGroupageId") long lotGroupageId, @Param("active") Set<Boolean> active);

    Optional<LotGroupage> findByLotGroupageIdAndActiveIn(long lotGroupageId, Set<Boolean> active);

    @Query(nativeQuery = true, value = "SELECT ma.market_auction_id,l.lot_id " +
            "FROM market_auction ma " +
            "LEFT JOIN lot l ON ma.market_auction_id = l.market_auction_id " +
            "WHERE l.allotted_lot_id = :allottedLotId " +
            "AND ma.market_auction_date = :marketAuctionDate" +
            "AND l.market_id = :marketId"
    )
    public List<Object[]> getMarketAuctionIdByAllottedLotIdAndMarketAuctionDate(int allottedLotId, LocalDate marketAuctionDate,int marketId);

    @Query(value = "SELECT next value for dbo.INVOICE_SEQ", nativeQuery = true)
    public BigDecimal getNextValInvoiceSequence();

//    @Query("SELECT lg.buyerType FROM lot_groupage lg WHERE lg.allottedLotId = :allottedLotId")
//    String findBuyerTypeByAllottedLotId(Integer allottedLotId);

    @Query(value = "SELECT DISTINCT lg.buyer_type FROM lot_groupage lg " +
            "JOIN lot l ON lg.allotted_lot_id = l.allotted_lot_id " +
            "WHERE lg.allotted_lot_id = :allottedLotId " +
            "AND lg.auction_date = :auctionDate " +
            "AND l.market_id = :marketId " +
            "AND lg.buyer_type = 'Reeling'",
            nativeQuery = true)
    String findBuyerTypeByAllottedLotIdAndAuctionDateAndMarketId(
            @Param("allottedLotId") Integer allottedLotId,
            @Param("auctionDate") LocalDate auctionDate,
            @Param("marketId") Integer marketId);

    @Query(nativeQuery = true, value = """
            WITH PrimaryAddress AS (
            SELECT
                fa.farmer_id,
                fa.STATE_ID,
                fa.DISTRICT_ID,
                fa.TALUK_ID,
                fa.HOBLI_ID,
                fa.VILLAGE_ID,
                fa.address_text,
                ROW_NUMBER() OVER (PARTITION BY fa.farmer_id ORDER BY fa.district_id DESC) AS rn
            FROM
                farmer_address fa
            WHERE
                fa.active = 1
        ),
        MainQuery AS (
            SELECT
                f.farmer_number,
                f.fruits_id,
                (ISNULL(f.first_name, '') + ' ' +ISNULL(f.middle_name, '') + ' ' +ISNULL(f.last_name, '') + ' - ' +ISNULL(pa.address_text, '')
                ) AS farmer_full_name,
                ma.RACE_MASTER_ID,
                v.village_name_in_kannada,
                mm.market_name_in_kannada,
                rm.race_name,
                sm.source_name,
                mm.box_weight,
                l.status,
                lg.lot_groupage_id,
                lg.buyer_id,
                lg.buyer_type,
                lg.lot_weight,
                lg.amount,
                lg.market_fee,
                lg.sold_amount,
                CASE
                    WHEN lg.lot_groupage_id IS NOT NULL THEN lg.remaining_cocoon
                    ELSE l.LOT_WEIGHT_AFTER_WEIGHMENT
                END AS weight_to_show,
                ma.dfl_lot_number,
                ma.lot_variety,
                ma.lot_Parental_Level,
                ma.estimated_weight,
                lbpf.PRICE_PER_KG,
                lbpf.FIXATION_DATE,
                ptaca.TEST_DATE,
                ptaca.NO_OF_COCOON_TAKEN_FOR_EXAMINATION,
                ptaca.NO_OF_DFL_FROM_FC,
                ptaca.DISEASE_FREE,
                ptaca.DISEASE_TYPE,
                ptaca.NO_OF_COCOON_PER_KG,
                ptaca.MELT_PERCENTAGE,
                ptaca.pupa_cocoon_status,
                ptaca.PUPA_TEST_RESULT,
                ma.market_auction_date,
                l.allotted_lot_id,
                lg.average_yield,
                ma.dfl_lot_number AS no_of_dfls,
                lg.invoice_number,
                (l.LOT_WEIGHT_AFTER_WEIGHMENT * 100) / NULLIF(ma.dfl_lot_number, 0) AS calculatedAverageYield,
                lg.remaining_cocoon,
                (
                    SELECT COALESCE(SUM(lg_inner.lot_weight), 0)
                    FROM lot_groupage lg_inner
                    INNER JOIN lot l_inner ON lg_inner.lot_id = l_inner.lot_id
                    WHERE lg_inner.allotted_lot_id = l.allotted_lot_id
                      AND l_inner.auction_date = l.auction_date
                      AND lg_inner.lot_weight > 0
                ) AS soldCocoonInKgs,
                l.LOT_WEIGHT_AFTER_WEIGHMENT,
                CASE
                    WHEN lg.buyer_type = 'RSP' THEN es.license_number
                    WHEN lg.buyer_type = 'NSSO' THEN es.address
                    WHEN lg.buyer_type = 'Govt Grainage' THEN gm.grainage_master_name
                    WHEN lg.buyer_type = 'Reeling' THEN r.name
                    ELSE NULL
                END AS buyer_name
            FROM
                FARMER f
            INNER JOIN
                market_auction ma ON ma.farmer_id = f.FARMER_ID
            INNER JOIN
                lot l ON l.market_auction_id = ma.market_auction_id
            LEFT JOIN
                PrimaryAddress pa ON pa.farmer_id = f.FARMER_ID AND pa.rn = 1
            LEFT JOIN
                Village v ON pa.VILLAGE_ID = v.village_id AND f.ACTIVE = 1
            LEFT JOIN
                market_master mm ON mm.market_master_id = ma.market_id
            LEFT JOIN
                race_master rm ON rm.race_id = ma.lot_variety
            LEFT JOIN
                source_master sm ON sm.source_id = ma.SOURCE_MASTER_ID
            LEFT JOIN
                lot_groupage lg ON l.lot_id = lg.lot_id
            LEFT JOIN
                PUPA_TEST_AND_COCOON_ASSESSMENT ptaca ON ptaca.MARKET_AUCTION_ID = ma.market_auction_id AND ptaca.ACTIVE = 1
            LEFT JOIN
                LOT_BASE_PRICE_FIXATION lbpf ON lbpf.MARKET_ID = ma.market_id
                     AND lbpf.FIXATION_DATE = CAST(GETDATE() AS DATE)
            LEFT JOIN
                reeler r ON lg.buyer_id = r.reeler_id AND lg.buyer_type = 'Reeling'
            LEFT JOIN
                external_unit_registration es ON lg.external_unit_id = es.external_unit_registration_id
                 AND lg.buyer_type IN ('RSP', 'NSSO')
            LEFT JOIN
                grainage_master gm ON lg.external_unit_id = gm.grainage_master_id AND lg.buyer_type = 'Govt Grainage'
            WHERE
                l.auction_date BETWEEN :fromDate AND :toDate
                AND l.market_id = :marketId
                AND lg.buyer_type = 'Govt Grainage'
                AND gm.grainage_master_id = :grainageMasterId
                AND f.ACTIVE = 1
                AND ma.active = 1
                AND l.status = 'weighmentcompleted'
        )
        SELECT * FROM MainQuery
    """)
List<Object[]> getLotDistributeDetailsForInvoice(
                                       @Param("fromDate") LocalDate fromDate,
                                       @Param("toDate") LocalDate toDate,
                                       @Param("marketId") Integer marketId,
                                       @Param("grainageMasterId") Long grainageMasterId);

    @Query(nativeQuery = true, value = """
            WITH PrimaryAddress AS (
            SELECT
                fa.farmer_id,
                fa.STATE_ID,
                fa.DISTRICT_ID,
                fa.TALUK_ID,
                fa.HOBLI_ID,
                fa.VILLAGE_ID,
                fa.address_text,
                ROW_NUMBER() OVER (PARTITION BY fa.farmer_id ORDER BY fa.district_id DESC) AS rn
            FROM
                farmer_address fa
            WHERE
                fa.active = 1
        ),
        MainQuery AS (
            SELECT
                f.farmer_number,
                f.fruits_id,
                (ISNULL(f.first_name, '') + ' ' +ISNULL(f.middle_name, '') + ' ' +ISNULL(f.last_name, '') + ' - ' +ISNULL(pa.address_text, '')
                ) AS farmer_full_name,
                ma.RACE_MASTER_ID,
                v.village_name_in_kannada,
                mm.market_name_in_kannada,
                rm.race_name,
                sm.source_name,
                mm.box_weight,
                l.status,
                lg.lot_groupage_id,
                lg.buyer_id,
                lg.buyer_type,
                lg.lot_weight,
                lg.amount,
                lg.market_fee,
                lg.sold_amount,
                CASE
                    WHEN lg.lot_groupage_id IS NOT NULL THEN lg.remaining_cocoon
                    ELSE l.LOT_WEIGHT_AFTER_WEIGHMENT
                END AS weight_to_show,
                ma.dfl_lot_number,
                ma.lot_variety,
                ma.lot_Parental_Level,
                ma.estimated_weight,
                lbpf.PRICE_PER_KG,
                lbpf.FIXATION_DATE,
                ptaca.TEST_DATE,
                ptaca.NO_OF_COCOON_TAKEN_FOR_EXAMINATION,
                ptaca.NO_OF_DFL_FROM_FC,
                ptaca.DISEASE_FREE,
                ptaca.DISEASE_TYPE,
                ptaca.NO_OF_COCOON_PER_KG,
                ptaca.MELT_PERCENTAGE,
                ptaca.pupa_cocoon_status,
                ptaca.PUPA_TEST_RESULT,
                ma.market_auction_date,
                l.allotted_lot_id,
                lg.average_yield,
                ma.dfl_lot_number AS no_of_dfls,
                lg.invoice_number,
                (l.LOT_WEIGHT_AFTER_WEIGHMENT * 100) / NULLIF(ma.dfl_lot_number, 0) AS calculatedAverageYield,
                lg.remaining_cocoon,
                (
                    SELECT COALESCE(SUM(lg_inner.lot_weight), 0)
                    FROM lot_groupage lg_inner
                    INNER JOIN lot l_inner ON lg_inner.lot_id = l_inner.lot_id
                    WHERE lg_inner.allotted_lot_id = l.allotted_lot_id
                      AND l_inner.auction_date = l.auction_date
                      AND lg_inner.lot_weight > 0
                ) AS soldCocoonInKgs,
                l.LOT_WEIGHT_AFTER_WEIGHMENT,
                (ISNULL(es.name, '') + ' - ' + ISNULL(es.license_number, '')) AS buyer_name,
                es.address
            FROM
                FARMER f
            INNER JOIN
                market_auction ma ON ma.farmer_id = f.FARMER_ID
            INNER JOIN
                lot l ON l.market_auction_id = ma.market_auction_id
            LEFT JOIN
                PrimaryAddress pa ON pa.farmer_id = f.FARMER_ID AND pa.rn = 1
            LEFT JOIN
                Village v ON pa.VILLAGE_ID = v.village_id AND f.ACTIVE = 1
            LEFT JOIN
                market_master mm ON mm.market_master_id = ma.market_id
            LEFT JOIN
                race_master rm ON rm.race_id = ma.lot_variety
            LEFT JOIN
                source_master sm ON sm.source_id = ma.SOURCE_MASTER_ID
            LEFT JOIN
                lot_groupage lg ON l.lot_id = lg.lot_id
            LEFT JOIN
                PUPA_TEST_AND_COCOON_ASSESSMENT ptaca ON ptaca.MARKET_AUCTION_ID = ma.market_auction_id AND ptaca.ACTIVE = 1
            LEFT JOIN
                LOT_BASE_PRICE_FIXATION lbpf ON lbpf.MARKET_ID = ma.market_id
                     AND lbpf.FIXATION_DATE = CAST(GETDATE() AS DATE)
            LEFT JOIN
                reeler r ON lg.buyer_id = r.reeler_id AND lg.buyer_type = 'Reeling'
            LEFT JOIN
                external_unit_registration es ON lg.external_unit_id = es.external_unit_registration_id
                 AND lg.buyer_type IN ('RSP', 'NSSO')
            LEFT JOIN
                grainage_master gm ON lg.external_unit_id = gm.grainage_master_id AND lg.buyer_type = 'Govt Grainage'
            WHERE
                l.auction_date BETWEEN :fromDate AND :toDate
                AND l.market_id = :marketId
                AND lg.buyer_type IN ('RSP', 'NSSO')
                AND es.external_unit_registration_id = :externalUnitRegistrationId
                AND f.ACTIVE = 1
                AND ma.active = 1
                AND l.status = 'weighmentcompleted'
        )
        SELECT * FROM MainQuery
    """)
    List<Object[]> getLotDistributeDetailsForPermitRSP(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("marketId") Integer marketId,
            @Param("externalUnitRegistrationId") Long externalUnitRegistrationId);

    @Query(nativeQuery = true, value = """
            WITH PrimaryAddress AS (
            SELECT
                fa.farmer_id,
                fa.STATE_ID,
                fa.DISTRICT_ID,
                fa.TALUK_ID,
                fa.HOBLI_ID,
                fa.VILLAGE_ID,
                fa.address_text,
                ROW_NUMBER() OVER (PARTITION BY fa.farmer_id ORDER BY fa.district_id DESC) AS rn
            FROM
                farmer_address fa
            WHERE
                fa.active = 1
        ),
        MainQuery AS (
            SELECT
                f.farmer_number,
                f.fruits_id,
                (ISNULL(f.first_name, '') + ' ' + ISNULL(f.last_name, '')) AS farmer_full_name,
                f.father_name_kan,
                v.village_name_in_kannada,
                mm.market_name_in_kannada,
                rm.race_name,
                sm.source_name,
                SUM(mm.box_weight) AS total_box_weight,
                l.status,
                SUM(lg.lot_weight) AS total_lot_weight,
                SUM(CAST(ISNULL(lg.amount, 0) AS FLOAT)) AS total_amount,
                SUM(CAST(ISNULL(lg.market_fee, 0) AS FLOAT)) AS total_market_fee,
                SUM(CAST(ISNULL(lg.sold_amount, 0) AS FLOAT)) AS total_sold_amount,
                MAX(CAST(ma.dfl_lot_number AS FLOAT)) AS dfl_lot_number,
                ma.lot_variety,
                ma.lot_Parental_Level,
                ma.estimated_weight,
                lbpf.PRICE_PER_KG,
                lbpf.FIXATION_DATE,
                ptaca.TEST_DATE,
                ptaca.NO_OF_COCOON_TAKEN_FOR_EXAMINATION,
                ptaca.NO_OF_DFL_FROM_FC,
                ptaca.NO_OF_COCOON_PER_KG,
                ptaca.pupa_cocoon_status,
                ptaca.PUPA_TEST_RESULT,
                ma.market_auction_date,
                l.allotted_lot_id,
                lg.average_yield,
                STRING_AGG(lg.invoice_number, ', ') AS invoice_numbers,
                (CAST(SUM(l.LOT_WEIGHT_AFTER_WEIGHMENT) * 100 AS FLOAT) / NULLIF(SUM(CAST(ma.dfl_lot_number AS FLOAT)), 0)) AS total_calculatedAverageYield,
                SUM(CAST(ISNULL(lg.remaining_cocoon, 0) AS FLOAT)) AS total_remaining_cocoon,
                (
                    SELECT COALESCE(SUM(lg_inner.lot_weight), 0)
                    FROM lot_groupage lg_inner
                    INNER JOIN lot l_inner ON lg_inner.lot_id = l_inner.lot_id
                    WHERE lg_inner.allotted_lot_id = :allottedLotId
                      AND l_inner.auction_date = :auctionDate
                      AND lg_inner.lot_weight > 0
                ) AS total_soldCocoonInKgs,
                l.LOT_WEIGHT_AFTER_WEIGHMENT
            FROM
                FARMER f
            INNER JOIN
                market_auction ma ON ma.farmer_id = f.FARMER_ID
            INNER JOIN
                lot l ON l.market_auction_id = ma.market_auction_id
            LEFT JOIN
                PrimaryAddress pa ON pa.farmer_id = f.FARMER_ID AND pa.rn = 1
            LEFT JOIN
                Village v ON pa.VILLAGE_ID = v.village_id AND f.ACTIVE = 1
            LEFT JOIN
                market_master mm ON mm.market_master_id = ma.market_id
            LEFT JOIN
                race_master rm ON rm.race_id = ma.lot_variety
            LEFT JOIN
                source_master sm ON sm.source_id = ma.SOURCE_MASTER_ID
            LEFT JOIN
                lot_groupage lg ON l.lot_id = lg.lot_id
            LEFT JOIN
                PUPA_TEST_AND_COCOON_ASSESSMENT ptaca ON ptaca.MARKET_AUCTION_ID = ma.market_auction_id AND ptaca.ACTIVE = 1
            LEFT JOIN
                LOT_BASE_PRICE_FIXATION lbpf ON lbpf.MARKET_ID = ma.market_id
                     AND lbpf.FIXATION_DATE = CAST(GETDATE() AS DATE)
            LEFT JOIN
                reeler r ON lg.buyer_id = r.reeler_id AND lg.buyer_type = 'Reeling'
            LEFT JOIN
                external_unit_registration es ON lg.external_unit_id = es.external_unit_registration_id
                 AND lg.buyer_type IN ('RSP', 'NSSO')
            LEFT JOIN
                grainage_master gm ON lg.external_unit_id = gm.grainage_master_id AND lg.buyer_type = 'Govt Grainage'
            WHERE
                l.auction_date = :auctionDate
                AND l.market_id = :marketId
                AND l.allotted_lot_id = :allottedLotId
                AND f.ACTIVE = 1
                AND ma.active = 1
                AND l.status = 'weighmentcompleted'
            GROUP BY
                f.farmer_number,
                f.fruits_id,
                f.first_name,
                f.middle_name,
                f.last_name,
                f.father_name_kan,
                pa.address_text,
                v.village_name_in_kannada,
                mm.market_name_in_kannada,
                rm.race_name,
                sm.source_name,
                l.status,
                ma.lot_variety,
                ma.lot_Parental_Level,
                ma.estimated_weight,
                lbpf.PRICE_PER_KG,
                lbpf.FIXATION_DATE,
                ptaca.TEST_DATE,
                ptaca.NO_OF_COCOON_TAKEN_FOR_EXAMINATION,
                ptaca.NO_OF_DFL_FROM_FC,
                ptaca.NO_OF_COCOON_PER_KG,
                ptaca.pupa_cocoon_status,
                ptaca.PUPA_TEST_RESULT,
                ma.market_auction_date,
                l.allotted_lot_id,
                lg.average_yield,
                l.LOT_WEIGHT_AFTER_WEIGHMENT
        )
        SELECT * FROM MainQuery;
    """)
    List<Object[]> getLotDistributeDetailsForMarketReceiptAndCashReceipt(
            @Param("auctionDate") LocalDate auctionDate,
            @Param("marketId") Integer marketId,
            @Param("allottedLotId") Integer allottedLotId);


    @Query(nativeQuery = true, value = """
            WITH PrimaryAddress AS (
            SELECT
            fa.farmer_id,
            fa.STATE_ID,
            fa.DISTRICT_ID,
            fa.TALUK_ID,
            fa.HOBLI_ID,
            fa.VILLAGE_ID,
            fa.address_text,
            ROW_NUMBER() OVER (PARTITION BY fa.farmer_id ORDER BY fa.district_id DESC) AS rn
            FROM
            farmer_address fa
            WHERE
            fa.active = 1
            ),
            MainQuery AS (
            SELECT
            f.farmer_number,
            f.fruits_id,
            (ISNULL(f.first_name, '') + ' ' +ISNULL(f.middle_name, '') + ' ' +ISNULL(f.last_name, '') + ' - ' +ISNULL(pa.address_text, '')
            ) AS farmer_full_name,
            v.village_name_in_kannada,
            mm.market_name_in_kannada,
            rm.race_name,
            mm.box_weight,
            l.status,
            lg.lot_groupage_id,
            lg.buyer_id,
            lg.buyer_type,
            lg.lot_weight,
            lg.amount,
            lg.market_fee,
            lg.sold_amount,
            ma.dfl_lot_number,
            ma.lot_variety,
            ma.lot_Parental_Level,
            ma.estimated_weight,
            ptaca.TEST_DATE,
            ptaca.NO_OF_COCOON_TAKEN_FOR_EXAMINATION,
            ptaca.NO_OF_DFL_FROM_FC,
            ptaca.NO_OF_COCOON_PER_KG,
            ma.market_auction_date,
            l.allotted_lot_id,
            lg.average_yield,
            lg.invoice_number,
            l.LOT_WEIGHT_AFTER_WEIGHMENT,
            CASE
            WHEN lg.buyer_type = 'RSP' THEN es.license_number
            WHEN lg.buyer_type = 'NSSO' THEN es.address
            WHEN lg.buyer_type = 'Govt Grainage' THEN gm.grainage_master_name
            WHEN lg.buyer_type = 'Reeling' THEN r.name
            ELSE NULL
            END AS buyer_name,
            SUM(CASE WHEN lg.buyer_type IN ('RSP','NSSO','Govt Grainage') THEN lg.lot_weight ELSE 0 END)\s
                  OVER () AS sum_lot_weight_rsp_nss_govt,
            SUM(CASE WHEN lg.buyer_type IN ('RSP','NSSO','Govt Grainage') THEN lg.sold_amount ELSE 0 END)\s
                  OVER () AS sum_sold_amount_rsp_nss_govt,
            SUM(CASE WHEN lg.buyer_type = 'Reeling' THEN lg.lot_weight ELSE 0 END)\s
                  OVER () AS sum_lot_weight_reeling,
            SUM(CASE WHEN lg.buyer_type = 'Reeling' THEN lg.sold_amount ELSE 0 END)\s
                  OVER () AS sum_sold_amount_reeling,
            SUM(lg.lot_weight) OVER () AS totalLotWeight
            FROM
            FARMER f
            INNER JOIN
            market_auction ma ON ma.farmer_id = f.FARMER_ID
            INNER JOIN
            lot l ON l.market_auction_id = ma.market_auction_id
            LEFT JOIN
            PrimaryAddress pa ON pa.farmer_id = f.FARMER_ID AND pa.rn = 1
            LEFT JOIN
            Village v ON pa.VILLAGE_ID = v.village_id AND f.ACTIVE = 1
            LEFT JOIN
            market_master mm ON mm.market_master_id = ma.market_id
            LEFT JOIN
            race_master rm ON rm.race_id = ma.lot_variety
            LEFT JOIN
            lot_groupage lg ON l.lot_id = lg.lot_id
            LEFT JOIN
            PUPA_TEST_AND_COCOON_ASSESSMENT ptaca ON ptaca.MARKET_AUCTION_ID = ma.market_auction_id AND ptaca.ACTIVE = 1
            LEFT JOIN
            reeler r ON lg.buyer_id = r.reeler_id AND lg.buyer_type = 'Reeling'
            LEFT JOIN
            external_unit_registration es ON lg.external_unit_id = es.external_unit_registration_id
            AND lg.buyer_type IN ('RSP', 'NSSO')
            LEFT JOIN
            grainage_master gm ON lg.external_unit_id = gm.grainage_master_id AND lg.buyer_type = 'Govt Grainage'
            WHERE
            lg.auction_date = :auctionDate
            AND l.market_id = :marketId
            AND lg.allotted_lot_id = :allottedLotId
            AND f.ACTIVE = 1
            AND lg.fruits_id  = :fruitsId
            AND ma.active = 1
            AND l.status = 'weighmentcompleted'
            )
            SELECT * FROM MainQuery
    """)
    List<Object[]> getLotDistributeResponseForInvoiceAndBonusScheme(
            @Param("auctionDate") LocalDate auctionDate,
            @Param("marketId") Integer marketId,
            @Param("allottedLotId") Integer allottedLotId,
            @Param("fruitsId") String fruitsId);

    @Query(nativeQuery = true, value = """
    SELECT DISTINCT lg.allotted_lot_id
    FROM lot_groupage lg
    INNER JOIN lot l ON l.lot_id = lg.lot_id
    INNER JOIN market_auction ma ON ma.market_auction_id = l.market_auction_id
    WHERE lg.auction_date = :auctionDate
      AND l.market_id = :marketId
      AND lg.fruits_id = :fruitsId
""")
    List<Integer> getAllottedLotIds(
            @Param("auctionDate") LocalDate auctionDate,
            @Param("marketId") Integer marketId,
            @Param("fruitsId") String fruitsId
    );


}
