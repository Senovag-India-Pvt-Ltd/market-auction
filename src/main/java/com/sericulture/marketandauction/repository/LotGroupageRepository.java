package com.sericulture.marketandauction.repository;

import com.sericulture.marketandauction.helper.MarketAuctionQueryConstants;
import com.sericulture.marketandauction.model.entity.Lot;
import com.sericulture.marketandauction.model.entity.LotGroupage;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.QueryHint;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Repository
public interface LotGroupageRepository extends PagingAndSortingRepository<LotGroupage, Long> {

    public LotGroupage save(LotGroupage lotGroupage);

//    public LotGroupage findByLotGroupageIdAndActiveIn(@Param("lotGroupageId") long lotGroupageId, @Param("active") Set<Boolean> active);

    Optional<LotGroupage> findByLotGroupageIdAndActiveIn(long lotGroupageId, Set<Boolean> active);

    @QueryHints(@QueryHint(name = "org.hibernate.flushMode", value = "COMMIT"))
    @Query(nativeQuery = true, value = "SELECT ma.market_auction_id,l.lot_id " +
            "FROM market_auction ma " +
            "LEFT JOIN lot l ON ma.market_auction_id = l.market_auction_id " +
            "WHERE l.allotted_lot_id = :allottedLotId " +
            "AND ma.market_auction_date = :marketAuctionDate " +
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
                SELECT DISTINCT 
                    f.farmer_number,
                    f.fruits_id,
                    (ISNULL(f.name_kan, '') + ' ' +ISNULL(f.middle_name, '') + ' ' +ISNULL(f.last_name, '') + ' - ' +ISNULL(pa.address_text, '')
                    ) AS farmer_full_name,
                    ma.RACE_MASTER_ID,
                    v.village_name_in_kannada,
                    mm.market_name_in_kannada,
                    rm.race_name_in_kannada,
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
                    CASE
                        WHEN lg.lot_weight IS NOT NULL AND ptaca.NO_OF_COCOON_PER_KG IS NOT NULL
                        THEN lg.lot_weight * ptaca.NO_OF_COCOON_PER_KG
                        ELSE NULL
                    END AS total_number,
                    ptaca.MELT_PERCENTAGE,
                    ptaca.pupa_cocoon_status,
                    ptaca.PUPA_TEST_RESULT,
                    ma.market_auction_date,
                    l.allotted_lot_id,
                    lg.average_yield,
                    ma.dfl_lot_number AS no_of_dfls,
                    lg.invoice_number,
                    fc.expected_marker_date,
                    fc.spun_date,
                    (l.LOT_WEIGHT_AFTER_WEIGHMENT * 100) / NULLIF(ma.dfl_lot_number, 0) AS calculatedAverageYield,
                    lg.remaining_cocoon,
                    (
                        SELECT COALESCE(SUM(lg_inner.lot_weight), 0)
                        FROM lot_groupage lg_inner
                        INNER JOIN lot l_inner ON lg_inner.lot_id = l_inner.lot_id
                        WHERE lg_inner.allotted_lot_id = l.allotted_lot_id
                          AND l_inner.auction_date = l.auction_date
                          AND lg_inner.lot_weight > 0
                          AND lg_inner.active = 1
                          AND l_inner.active = 1
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
                    market_master mm ON mm.market_master_id = ma.market_id AND mm.active = 1
                LEFT JOIN
                    race_master rm ON rm.race_id = ma.lot_variety AND rm.active = 1
                LEFT JOIN
                    source_master sm ON sm.source_id = ma.SOURCE_MASTER_ID AND sm.active = 1
                LEFT JOIN
                    lot_groupage lg ON l.lot_id = lg.lot_id AND lg.active = 1
                LEFT JOIN (
                    SELECT
                        fc.fruits_id,
                        MAX(fc.expected_marker_date) AS expected_marker_date,
                        MAX(fc.spun_date) AS spun_date
                    FROM fitness_certificate fc
                    WHERE fc.active = 1
                    GROUP BY fc.fruits_id
                ) fc ON fc.fruits_id = f.fruits_id
                LEFT JOIN
                    PUPA_TEST_AND_COCOON_ASSESSMENT ptaca ON ptaca.MARKET_AUCTION_ID = ma.market_auction_id AND ptaca.ACTIVE = 1
                LEFT JOIN
                    LOT_BASE_PRICE_FIXATION lbpf ON lbpf.MARKET_ID = ma.market_id
                         AND lbpf.FIXATION_DATE = CAST(GETDATE() AS DATE) AND lbpf.allotted_lot_id = lg.allotted_lot_id and lbpf.active = 1
                LEFT JOIN
                    reeler r ON lg.buyer_id = r.reeler_id AND lg.buyer_type = 'Reeling' AND r.active = 1
                LEFT JOIN
                    external_unit_registration es ON lg.external_unit_id = es.external_unit_registration_id
                         AND lg.buyer_type IN ('RSP', 'NSSO') AND es.active = 1
                LEFT JOIN
                    grainage_master gm ON lg.external_unit_id = gm.grainage_master_id AND lg.buyer_type = 'Govt Grainage' AND gm.active = 1
                WHERE
                    l.auction_date BETWEEN :fromDate AND :toDate
                    AND l.market_id = :marketId
                    AND lg.buyer_type = 'Govt Grainage'
                    AND gm.grainage_master_id = :grainageMasterId
                    AND f.ACTIVE = 1
                    AND ma.active = 1
                    AND l.active = 1
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
            SELECT  DISTINCT
                f.farmer_number,
                f.fruits_id,
                (ISNULL(f.name_kan, '') + ' ' +ISNULL(f.last_name, '') + ' - ' +ISNULL(pa.address_text, '')
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
                SUM(CAST(ISNULL(lg.sold_amount, 0) AS FLOAT)) OVER (PARTITION BY lg.lot_groupage_id) AS total_sold_amount,
                SUM(CAST(ISNULL(lg.lot_weight, 0) AS FLOAT)) OVER (PARTITION BY lg.lot_groupage_id) AS total_lot_weight,
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
                CASE
                WHEN lg.lot_weight IS NOT NULL AND ptaca.NO_OF_COCOON_PER_KG IS NOT NULL
                THEN lg.lot_weight * ptaca.NO_OF_COCOON_PER_KG
                ELSE NULL
                END AS total_number,
                ptaca.MELT_PERCENTAGE,
                ptaca.pupa_cocoon_status,
                ptaca.PUPA_TEST_RESULT,
                ma.market_auction_date,
                l.allotted_lot_id,
                lg.average_yield,
                ma.dfl_lot_number AS no_of_dfls,
                lg.invoice_number,
                fc.expected_marker_date,
                fc.spun_date,
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
                es.address,
                es.license_number
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
            LEFT JOIN (
            SELECT
            fc.fruits_id,
            MAX(fc.expected_marker_date) AS expected_marker_date,
            MAX(fc.spun_date) AS spun_date
            FROM fitness_certificate fc
              GROUP BY fc.fruits_id
              ) fc ON fc.fruits_id = f.fruits_id
            LEFT JOIN
                PUPA_TEST_AND_COCOON_ASSESSMENT ptaca ON ptaca.MARKET_AUCTION_ID = ma.market_auction_id AND ptaca.ACTIVE = 1
            LEFT JOIN
                    LOT_BASE_PRICE_FIXATION lbpf ON lbpf.MARKET_ID = ma.market_id
                         AND lbpf.FIXATION_DATE = CAST(GETDATE() AS DATE) AND lbpf.allotted_lot_id = lg.allotted_lot_id and lbpf.active = 1
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
            SELECT  DISTINCT
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
                lg.amount,
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
                fc.expected_marker_date,
                fc.spun_date,
                MAX(
                CASE
                WHEN lg.buyer_type = 'RSP' THEN es.license_number
                WHEN lg.buyer_type = 'NSSO' THEN es.address
                WHEN lg.buyer_type = 'Govt Grainage' THEN gm.grainage_master_name
                WHEN lg.buyer_type = 'Reeling' THEN r.name
                    ELSE NULL
                        END
                ) AS buyer_name,
                lg.lot_groupage_id,
                lg.buyer_id,
                lg.buyer_type,
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
            LEFT JOIN (
            SELECT
            fc.fruits_id,
            MAX(fc.expected_marker_date) AS expected_marker_date,
            MAX(fc.spun_date) AS spun_date
            FROM fitness_certificate fc
              GROUP BY fc.fruits_id
              ) fc ON fc.fruits_id = f.fruits_id
            LEFT JOIN
                PUPA_TEST_AND_COCOON_ASSESSMENT ptaca ON ptaca.MARKET_AUCTION_ID = ma.market_auction_id AND ptaca.ACTIVE = 1
            LEFT JOIN
                    LOT_BASE_PRICE_FIXATION lbpf ON lbpf.MARKET_ID = ma.market_id
                         AND lbpf.FIXATION_DATE = CAST(GETDATE() AS DATE) AND lbpf.allotted_lot_id = lg.allotted_lot_id and lbpf.active = 1
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
                l.LOT_WEIGHT_AFTER_WEIGHMENT,
                lg.amount,
                lg.market_fee,
                fc.expected_marker_date,
                fc.spun_date,
                lg.buyer_type,          
                lg.buyer_id,            
                lg.lot_groupage_id
        )
        SELECT * FROM MainQuery;
    """)
    List<Object[]> getLotDistributeDetailsForMarketReceiptAndCashReceipt(
            @Param("auctionDate") LocalDate auctionDate,
            @Param("marketId") Integer marketId,
            @Param("allottedLotId") Integer allottedLotId);


    @Query(nativeQuery = true, value = """
            WITH PrimaryAddress AS (
            SELECT DISTINCT
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

    @Query(
            nativeQuery = true,
            value = """
    SELECT
    lg.lot_groupage_id,
    lg.buyer_type,
    lg.buyer_id,
    lg.lot_weight,
    lg.amount,
    lg.market_fee,
    lg.sold_amount,
    lg.allotted_lot_id,
    lg.market_auction_id,
    lg.lot_id,
    lg.auction_date,
    lg.average_yield,
    lg.no_of_dfls,
    lg.invoice_number,
    lg.lot_parental_level,
    lg.remaining_cocoon,
    lg.user_master_id,
    lg.external_unit_id,
    lg.is_accepted,
    CASE
    WHEN lg.buyer_type = 'Reeling' THEN r.name
    ELSE NULL
    END AS buyer_name,
    f.first_name,
    mm.market_name,
    lg.fruits_id,
    f.mobile_number,
    lg.ALLOTTED_LOT_ID
    FROM lot_groupage lg
    LEFT JOIN reeler r
    ON lg.buyer_id = r.reeler_id
    AND lg.buyer_type IN ('Reeling')
    AND r.active = 1
    LEFT JOIN farmer f
    ON lg.fruits_id = f.fruits_id
    LEFT JOIN market_auction ma
    ON lg.market_auction_id = ma.market_auction_id
    LEFT JOIN market_master mm
    ON ma.market_id = mm.market_master_id
    Where lg.buyer_type = 'Reeling'
    And ma.market_id = :marketId
    """
    )
    List<Object[]> getReelingLotNumberDetails(@Param("marketId") Integer marketId);


    @Query(nativeQuery = true, value = """
    WITH PrimaryAddress AS (
    SELECT
        fa.farmer_id,
        fa.VILLAGE_ID,
        ROW_NUMBER() OVER (PARTITION BY fa.farmer_id ORDER BY fa.farmer_address_id DESC) AS rn
    FROM farmer_address fa
    WHERE fa.active = 1),
            
LatestCropInspection AS (
    SELECT * FROM (SELECT ci.*,
            ROW_NUMBER() OVER (
                PARTITION BY ci.farmer_id, ci.fruits_id
                ORDER BY ci.crop_inspection_id DESC) AS rn
        FROM crop_inspection ci WHERE ci.active = 1) x
    WHERE x.rn = 1
)
            
SELECT
    a.lot_number,
    a.number_of_dfls_disposed,
    b.spun_date,
    b.no_of_chandies,
    b.expected_cocoon,
    c.name_kan,
    c.father_name_kan,
    e.village_name_in_kannada,
    b.fitness_certificate_id,
    t.name AS tsc_name,
    b.expected_marker_date,
    b.fruits_id,
    b.transaction_date,
            
    ci.crop_status_id,
    cs.name
            
FROM sale_and_disposal_of_dfls a
            
INNER JOIN fitness_certificate b
    ON a.id = b.sale_and_disposal_id
    AND a.fruits_id = b.fruits_id
    AND b.active = 1
            
INNER JOIN FARMER c
    ON b.farmer_id = c.FARMER_ID
    AND a.fruits_id = c.fruits_id
    AND c.active = 1
            
INNER JOIN PrimaryAddress d
    ON c.FARMER_ID = d.farmer_id
    AND d.rn = 1
            
INNER JOIN VILLAGE e
    ON d.VILLAGE_ID = e.VILLAGE_ID
    AND e.active = 1
            
LEFT JOIN user_master u
    ON u.username = b.created_by
    AND u.active = 1
            
LEFT JOIN tsc_master t
    ON t.tsc_master_id = u.tsc_master_id
    AND t.active = 1
            
LEFT JOIN LatestCropInspection ci
    ON ci.farmer_id = c.farmer_id
    AND ci.fruits_id = b.fruits_id
            
LEFT JOIN crop_status cs
    ON cs.crop_status_id = ci.crop_status_id
            
WHERE
    a.active = 1
    AND b.fruits_id = :fruitsId
    AND b.fitness_certificate_id = :fitnessCertificateId
""")
    List<Object[]> getLotDisposalDetails(@Param("fruitsId") String fruitsId,@Param("fitnessCertificateId") Long fitnessCertificateId);


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
        FROM farmer_address fa
        WHERE fa.active = 1
    ),
            
    MainQuery AS (
        SELECT
            f.farmer_number,
            f.fruits_id,
            (ISNULL(f.first_name, '') + ' ' + ISNULL(f.last_name, '')) AS farmer_full_name,
            f.father_name_kan,
            d.district_name_in_kannada,
            t.taluk_name_in_kannada,
            v.village_name_in_kannada,
            mm.market_name_in_kannada,
            rm.race_name,
            sm.source_name,
            ma.lot_Parental_Level,
            lg.allotted_lot_id,
            lg.auction_date,
            
            -- Buyer Name
            MAX(
                CASE
                    WHEN lg.buyer_type = 'RSP' THEN es.license_number
                    WHEN lg.buyer_type = 'NSSO' THEN es.address
                    WHEN lg.buyer_type = 'Govt Grainage' THEN gm.grainage_master_name
                    WHEN lg.buyer_type = 'Reeling' THEN r.name
                END
            ) AS buyer_name,
            
            (CAST(SUM(l.LOT_WEIGHT_AFTER_WEIGHMENT) * 100 AS FLOAT)
                / NULLIF(SUM(CAST(ma.dfl_lot_number AS FLOAT)), 0)) AS total_calculatedAverageYield,
            
            
            SUM(CASE WHEN lg.buyer_type IN ('RSP','NSSO','Govt Grainage')
                     THEN ISNULL(lg.lot_weight,0) ELSE 0 END) AS total_rsp_nsso_grainage_lot_weight,
            
            MAX(CASE WHEN lg.buyer_type IN ('RSP','NSSO','Govt Grainage')
            THEN ISNULL(lg.amount,0) ELSE 0 END) AS total_rsp_nsso_grainage_amount,
            
            SUM(CASE WHEN lg.buyer_type IN ('RSP','NSSO','Govt Grainage')
                     THEN ISNULL(lg.sold_amount,0) ELSE 0 END) AS total_rsp_nsso_grainage_sold_amount,
            
            SUM(CASE WHEN lg.buyer_type IN ('RSP','NSSO','Govt Grainage')
                     THEN ISNULL(lg.market_fee,0) ELSE 0 END) AS total_rsp_nsso_grainage_market_fee,
            
           
            SUM(CASE WHEN lg.buyer_type = 'Reeling'
                     THEN ISNULL(lg.lot_weight,0) ELSE 0 END) AS total_reeling_lot_weight,
            
            MAX(CASE WHEN lg.buyer_type = 'Reeling'
                     THEN ISNULL(lg.amount,0) ELSE 0 END) AS total_reeling_amount,
            
            SUM(CASE WHEN lg.buyer_type = 'Reeling'
                     THEN ISNULL(lg.sold_amount,0) ELSE 0 END) AS total_reeling_sold_amount,
            
            SUM(CASE WHEN lg.buyer_type = 'Reeling'
                     THEN ISNULL(lg.market_fee,0) ELSE 0 END) AS total_reeling_market_fee,
            
            
            SUM(ISNULL(lg.lot_weight,0))  AS total_lot_weight,
            SUM(ISNULL(lg.amount,0))      AS total_amount,
            SUM(ISNULL(lg.sold_amount,0)) AS total_sold_amount,
            SUM(ISNULL(lg.market_fee,0))  AS total_market_fee,
            MAX(CASE WHEN lg.buyer_type = 'Reeling' THEN r.name END) AS reeler_name,
                    MAX(CASE WHEN lg.buyer_type = 'Reeling' THEN r.father_name END) AS reeler_father_name,
                    MAX(CASE WHEN lg.buyer_type = 'Reeling' THEN rd.district_name_in_kannada END) AS reeler_district,
                    MAX(CASE WHEN lg.buyer_type = 'Reeling' THEN rt.taluk_name_in_kannada END) AS reeler_taluk,
                    MAX(CASE WHEN lg.buyer_type = 'Reeling' THEN rh.hobli_name_in_kannada END) AS reeler_hobli,
                    MAX(CASE WHEN lg.buyer_type = 'Reeling' THEN rv.village_name_in_kannada END) AS reeler_village
            
        FROM FARMER f
        INNER JOIN market_auction ma ON ma.farmer_id = f.FARMER_ID AND ma.active = 1
        INNER JOIN lot l ON l.market_auction_id = ma.market_auction_id AND l.active = 1
        LEFT JOIN PrimaryAddress pa ON pa.farmer_id = f.FARMER_ID AND pa.rn = 1
        LEFT JOIN Village v ON pa.VILLAGE_ID = v.village_id AND v.ACTIVE = 1
        LEFT JOIN TALUK t ON pa.TALUK_ID = t.TALUK_ID AND t.ACTIVE = 1
        LEFT JOIN DISTRICT d ON pa.DISTRICT_ID = d.DISTRICT_ID AND d.ACTIVE = 1
        LEFT JOIN market_master mm ON mm.market_master_id = ma.market_id AND mm.active = 1
        LEFT JOIN race_master rm ON rm.race_id = ma.lot_variety AND rm.active = 1
        LEFT JOIN source_master sm ON sm.source_id = ma.SOURCE_MASTER_ID AND sm.active = 1
        LEFT JOIN lot_groupage lg ON l.lot_id = lg.lot_id AND lg.active = 1
        LEFT JOIN reeler r ON lg.buyer_id = r.reeler_id AND lg.buyer_type = 'Reeling'
        LEFT JOIN DISTRICT rd ON r.district_id = rd.district_id AND rd.active = 1
                LEFT JOIN TALUK rt ON r.taluk_id = rt.taluk_id AND rt.active = 1
                LEFT JOIN HOBLI rh ON r.hobli_id = rh.hobli_id AND rh.active = 1
                LEFT JOIN VILLAGE rv ON r.village_id = rv.village_id AND rv.active = 1
        LEFT JOIN external_unit_registration es ON lg.external_unit_id = es.external_unit_registration_id
               AND lg.buyer_type IN ('RSP','NSSO')
        LEFT JOIN grainage_master gm ON lg.external_unit_id = gm.grainage_master_id
               AND lg.buyer_type = 'Govt Grainage'
            
        WHERE
            l.auction_date = :auctionDate
            AND l.market_id = :marketId
            AND l.allotted_lot_id = :allottedLotId
            AND l.status = 'weighmentcompleted'            
        GROUP BY
            f.farmer_number,
            f.fruits_id,
            f.first_name,
            f.last_name,
            f.father_name_kan,
            d.district_name_in_kannada,
            t.taluk_name_in_kannada,
            v.village_name_in_kannada,
            mm.market_name_in_kannada,
            rm.race_name,
            sm.source_name,
            ma.lot_Parental_Level,
            lg.allotted_lot_id,
            lg.auction_date
    )            
    SELECT * FROM MainQuery;
    """)
    List<Object[]> getDetailsForMarketReceipt(
            @Param("auctionDate") LocalDate auctionDate,
            @Param("marketId") Integer marketId,
            @Param("allottedLotId") Integer allottedLotId);

    @Query(value = """
    SELECT 
        l.LOT_ID AS lotId,
        l.ALLOTTED_LOT_ID AS lotNo,
        l.AUCTION_DATE AS transactionDate,
        f.FIRST_NAME AS farmerName,
        lg.LOT_WEIGHT AS totalWeight,
        lg.SOLD_AMOUNT AS transactionAmount
    FROM LOT l
        LEFT JOIN MARKET_AUCTION ma ON l.MARKET_AUCTION_ID = ma.market_auction_id 
        LEFT JOIN FARMER f ON ma.FARMER_ID = f.FARMER_ID
    LEFT JOIN LOT_GROUPAGE lg ON l.LOT_ID = lg.LOT_ID AND lg.ACTIVE = 1
    WHERE l.AUCTION_DATE = :date
      AND l.ALLOTTED_LOT_ID = :lotNo
    AND l.ACTIVE = 1
""", nativeQuery = true)
    List<Map<String, Object>> getLotDetails(
            @Param("date") LocalDate date,
            @Param("lotNo") int lotNo
    );

    @Query("SELECT COUNT(lg) FROM LotGroupage lg WHERE lg.id= :lotId AND lg.status IN (:statuses)")
    int countByLotIdAndStatusIn(@Param("lotId") BigInteger lotId,
                                @Param("statuses") List<String> statuses);

    @Modifying
    @Query("UPDATE LotGroupage lg SET lg.customerReferenceNumber = :crn WHERE lg.lotGroupageId = :id")
    void updateCustomerReferenceNumber(@Param("id") Long id, @Param("crn") String crn);

    @Modifying
    @Query("""
UPDATE LotGroupage lg
SET lg.active = false
WHERE lg.id = :lotId
""")
    void updateActiveByLotId(@Param("lotId") BigInteger lotId);

    @Query("""
SELECT lg.marketAuctionId
FROM LotGroupage lg
WHERE lg.id = :lotId
AND lg.active = true
""")
    List<BigInteger> findMarketAuctionIdsByLotId(@Param("lotId") BigInteger lotId);
//    @Query(nativeQuery = true, value = """
//            WITH PrimaryAddress AS (
//                            SELECT
//                                fa.farmer_id,
//                                fa.address_text,
//                                fa.VILLAGE_ID,
//                                ROW_NUMBER() OVER (PARTITION BY fa.farmer_id ORDER BY fa.district_id DESC) AS rn
//                            FROM farmer_address fa
//                            WHERE fa.active = 1
//                        )
//
//                        SELECT
//                            l.ALLOTTED_LOT_ID,
//                            f.name_kan,
//                            f.father_name_kan,
//                            f.fruits_id,
//                            v.VILLAGE_NAME_IN_KANNADA AS villageName,
//                            lg.lot_Parental_Level,
//                            lg.no_of_dfls,
//                            ptaca.NO_OF_DFL_FROM_FC,
//                            l.LOT_WEIGHT_AFTER_WEIGHMENT,
//                            ma.estimated_weight,
//                            mm.market_name,
//                            ptaca.NO_OF_COCOON_PER_KG,
//                            ptaca.MELT_PERCENTAGE,
//
//                            (l.LOT_WEIGHT_AFTER_WEIGHMENT * ptaca.NO_OF_COCOON_PER_KG) AS totalQuantity,
//
//                            SUM(CASE WHEN lg.buyer_type = 'RSP' THEN lg.lot_weight ELSE 0 END) AS rspQty,
//                            SUM(CASE WHEN lg.buyer_type = 'NSSO' THEN lg.lot_weight ELSE 0 END) AS nssoQty,
//                            SUM(CASE WHEN lg.buyer_type = 'Govt Grainage' THEN lg.lot_weight ELSE 0 END) AS govtGrainageQty,
//                            SUM(CASE WHEN lg.buyer_type = 'Reeling' THEN lg.lot_weight ELSE 0 END) AS reelingQty,
//
//                            MAX(CASE WHEN lg.buyer_type = 'RSP' THEN es.license_number + ' - ' + es.name END) AS rspName,
//                            MAX(CASE WHEN lg.buyer_type = 'NSSO' THEN es.address + ' - ' + es.name END) AS nssoName,
//                            MAX(CASE WHEN lg.buyer_type = 'Govt Grainage' THEN gr.grainage_master_name END) AS govtGrainageName,
//                            MAX(CASE WHEN lg.buyer_type = 'Reeling' THEN r.name END) AS reelingName,
//
//                            l.auction_date,
//
//                            SUM(lg.remaining_cocoon) AS remaining_cocoon
//
//                        FROM farmer f
//
//                        INNER JOIN market_auction ma
//                            ON ma.farmer_id = f.farmer_id
//                            AND ma.active = 1
//
//                        INNER JOIN lot l
//                            ON l.market_auction_id = ma.market_auction_id
//                            AND l.active = 1
//
//                        LEFT JOIN PrimaryAddress pa
//                            ON pa.farmer_id = f.farmer_id
//                            AND pa.rn = 1
//
//                        LEFT JOIN village v
//                            ON v.village_id = pa.VILLAGE_ID
//                            AND v.active = 1
//
//                        LEFT JOIN lot_groupage lg
//                            ON lg.lot_id = l.lot_id
//                            AND lg.active = 1
//
//                        LEFT JOIN market_master mm
//                            ON mm.market_master_id = ma.market_id
//                            AND mm.active = 1
//
//                        LEFT JOIN PUPA_TEST_AND_COCOON_ASSESSMENT ptaca
//                            ON ptaca.market_auction_id = ma.market_auction_id
//                            AND ptaca.active = 1
//
//                        LEFT JOIN reeler r
//                            ON lg.buyer_id = r.reeler_id
//                            AND lg.buyer_type = 'Reeling'
//                            AND r.active = 1
//
//                        LEFT JOIN external_unit_registration es
//                            ON lg.external_unit_id = es.external_unit_registration_id
//                            AND lg.buyer_type IN ('RSP','NSSO')
//                            AND es.active = 1
//
//                        LEFT JOIN grainage_master gr
//                            ON lg.external_unit_id = gr.grainage_master_id
//                            AND lg.buyer_type = 'Govt Grainage'
//                            AND gr.active = 1
//
//                        WHERE
//                            f.active = 1
//                            AND (:allottedLotId IS NULL OR l.allotted_lot_id = :allottedLotId)
//                            AND (
//                                (:fromDate IS NULL OR :toDate IS NULL)
//                                OR l.auction_date BETWEEN :fromDate AND :toDate
//                            )
//                            AND ma.market_id = :marketId
//
//                        GROUP BY
//                            l.ALLOTTED_LOT_ID,
//                            f.name_kan,
//                            f.father_name_kan,
//                            f.fruits_id,
//                            v.VILLAGE_NAME_IN_KANNADA,
//                            lg.lot_Parental_Level,
//                            lg.no_of_dfls,
//                            ptaca.NO_OF_DFL_FROM_FC,
//                            l.LOT_WEIGHT_AFTER_WEIGHMENT,
//                            ma.estimated_weight,
//                            mm.market_name,
//                            ptaca.NO_OF_COCOON_PER_KG,
//                            ptaca.MELT_PERCENTAGE,
//                            l.auction_date
//""")
//    List<Object[]> getDetailsForSeedCocoonSeedMarketReport(
//            @Param("fromDate") LocalDate fromDate,
//            @Param("toDate") LocalDate toDate,
//            @Param("allottedLotId") Integer allottedLotId,
//            @Param("marketId") Integer marketId
//    );


    @Query(nativeQuery = true, value = """
            WITH PrimaryAddress AS (
            SELECT
                fa.farmer_id,
                fa.address_text,
                fa.VILLAGE_ID,
                ROW_NUMBER() OVER (PARTITION BY fa.farmer_id ORDER BY fa.district_id DESC) AS rn
            FROM farmer_address fa
            WHERE fa.active = 1
        ),
        LotAgg AS (
            SELECT
                lg.lot_id,
                MAX(lg.lot_Parental_Level) AS lot_Parental_Level,
                MAX(lg.no_of_dfls) AS no_of_dfls,
                SUM(CASE WHEN lg.buyer_type = 'RSP' THEN lg.lot_weight ELSE 0 END) AS rspQty,
                SUM(CASE WHEN lg.buyer_type = 'NSSO' THEN lg.lot_weight ELSE 0 END) AS nssoQty,
                SUM(CASE WHEN lg.buyer_type = 'Govt Grainage' THEN lg.lot_weight ELSE 0 END) AS govtGrainageQty,
                SUM(CASE WHEN lg.buyer_type = 'Reeling' THEN lg.lot_weight ELSE 0 END) AS reelingQty,
                STRING_AGG(CASE WHEN lg.buyer_type = 'RSP' THEN es.license_number + ' - ' + es.name END, ', ') AS rspName,
                STRING_AGG(CASE WHEN lg.buyer_type = 'NSSO' THEN es.address + ' - ' + es.name END, ', ') AS nssoName,
                STRING_AGG(CASE WHEN lg.buyer_type = 'Govt Grainage' THEN gr.grainage_master_name END, ', ') AS govtGrainageName,
                STRING_AGG(CASE WHEN lg.buyer_type = 'Reeling' THEN r.name END, ', ') AS reelingName,
                MAX(lg.remaining_cocoon) AS remaining_cocoon,
                SUM(lg.market_fee) AS market_fee,
                SUM(lg.amount) AS amount,
                SUM(lg.sold_amount) AS sold_amount,
                MAX(lg.auction_date) AS lg_auction_date
            FROM lot_groupage lg
            LEFT JOIN reeler r
                ON lg.buyer_id = r.reeler_id AND lg.buyer_type = 'Reeling' AND r.active = 1
            LEFT JOIN external_unit_registration es
                ON lg.external_unit_id = es.external_unit_registration_id
                AND lg.buyer_type IN ('RSP','NSSO')
                AND es.active = 1
            LEFT JOIN grainage_master gr
                ON lg.external_unit_id = gr.grainage_master_id
                AND lg.buyer_type = 'Govt Grainage'
                AND gr.active = 1
            WHERE lg.active = 1
            GROUP BY lg.lot_id
        )
        SELECT
            l.ALLOTTED_LOT_ID,
            f.name_kan,
            f.father_name_kan,
            f.fruits_id,
            v.VILLAGE_NAME_IN_KANNADA,
            la.lot_Parental_Level,
            la.no_of_dfls,
            ptaca.NO_OF_DFL_FROM_FC,
            l.LOT_WEIGHT_AFTER_WEIGHMENT,
            ma.estimated_weight,
            mm.market_name,
            ptaca.NO_OF_COCOON_PER_KG,
            ptaca.MELT_PERCENTAGE,
            (l.LOT_WEIGHT_AFTER_WEIGHMENT * ptaca.NO_OF_COCOON_PER_KG) AS totalQuantity,
            la.rspQty,
            la.nssoQty,
            la.govtGrainageQty,
            la.reelingQty,
            la.rspName,
            la.nssoName,
            la.govtGrainageName,
            la.reelingName,
            l.auction_date,
            la.remaining_cocoon,
            MAX(fc.spun_date) AS spun_date,
            MAX(fc.expected_marker_date) AS expected_marker_date,
            la.market_fee,
            la.amount,
            la.sold_amount
        FROM farmer f
        INNER JOIN market_auction ma
            ON ma.farmer_id = f.farmer_id AND ma.active = 1
        INNER JOIN lot l
            ON l.market_auction_id = ma.market_auction_id AND l.active = 1
        LEFT JOIN LotAgg la
            ON la.lot_id = l.lot_id
        LEFT JOIN PrimaryAddress pa
            ON pa.farmer_id = f.farmer_id AND pa.rn = 1
        LEFT JOIN village v
            ON v.village_id = pa.VILLAGE_ID AND v.active = 1
        LEFT JOIN market_master mm
            ON mm.market_master_id = ma.market_id AND mm.active = 1
        LEFT JOIN PUPA_TEST_AND_COCOON_ASSESSMENT ptaca
            ON ptaca.market_auction_id = ma.market_auction_id AND ptaca.active = 1
        LEFT JOIN fitness_certificate fc
            ON fc.farmer_id = f.farmer_id
            AND fc.fruits_id = f.fruits_id
            AND fc.active = 1
        WHERE
        f.active = 1  
        AND (:allottedLotId IS NULL OR l.allotted_lot_id = :allottedLotId)
        AND (:auctionDate IS NULL OR CONVERT(DATE, l.auction_date) = CONVERT(DATE, :auctionDate))           
        AND ma.market_id = :marketId
        GROUP BY
            l.ALLOTTED_LOT_ID,
            f.name_kan,
            f.father_name_kan,
            f.fruits_id,
            v.VILLAGE_NAME_IN_KANNADA,
            la.lot_Parental_Level,
            la.no_of_dfls,
            ptaca.NO_OF_DFL_FROM_FC,
            l.LOT_WEIGHT_AFTER_WEIGHMENT,
            ma.estimated_weight,
            mm.market_name,
            ptaca.NO_OF_COCOON_PER_KG,
            ptaca.MELT_PERCENTAGE,
            l.auction_date,
            la.rspQty,
            la.nssoQty,
            la.govtGrainageQty,
            la.reelingQty,
            la.rspName,
            la.nssoName,
            la.govtGrainageName,
            la.reelingName,
            la.remaining_cocoon,
            la.market_fee,
            la.amount,
            la.sold_amount

""")
    List<Object[]> getDetailsForSeedCocoonSeedMarketReport(
            @Param("auctionDate") LocalDate auctionDate,
            @Param("allottedLotId") Integer allottedLotId,
            @Param("marketId") Integer marketId
    );

    @Query(nativeQuery = true, value = """
            SELECT evba.virtual_account_number, rvcb.CURRENT_BALANCE
            FROM dbo.external_unit_registration eur
            INNER JOIN dbo.external_unit_type_master et ON et.external_unit_type_id = eur.external_unit_type_id
            LEFT JOIN dbo.eu_virtual_bank_account evba ON evba.eu_id = eur.external_unit_registration_id AND evba.market_master_id = :marketId
            LEFT JOIN dbo.REELER_VID_CURRENT_BALANCE rvcb ON rvcb.reeler_virtual_account_number = evba.virtual_account_number
            WHERE eur.external_unit_registration_id = :externalUnitId AND et.payment_via_bank = 1
            """)
    Object[][] getExternalUnitVirtualAccountBalance(@Param("externalUnitId") Long externalUnitId, @Param("marketId") int marketId);

    @Query(nativeQuery = true, value = """
        SELECT evba.virtual_account_number
        FROM dbo.external_unit_registration eur
        INNER JOIN dbo.external_unit_type_master et 
            ON et.external_unit_type_id = eur.external_unit_type_id
        LEFT JOIN dbo.eu_virtual_bank_account evba 
            ON evba.eu_id = eur.external_unit_registration_id 
            AND evba.market_master_id = :marketId
        LEFT JOIN dbo.REELER_VID_CURRENT_BALANCE rvcb 
            ON rvcb.reeler_virtual_account_number = evba.virtual_account_number
        WHERE eur.external_unit_registration_id = :externalUnitId 
            AND et.payment_via_bank = 1
        """)
    String getExternalUnitVirtualAccountAndBalanceSave(
            @Param("externalUnitId") Long externalUnitId,
            @Param("marketId") int marketId);

    @Query(value = """
        SELECT
             eur.name,
             eur.license_number,
             evba.virtual_account_number,
             ISNULL(rvcb.CURRENT_BALANCE, 0) AS CURRENT_BALANCE,
             FORMAT(ISNULL(rvcb.MODIFIED_DATE, rvcb.CREATED_DATE), 'dd-MM-yyyy HH:mm:ss') AS modified_date
             FROM dbo.external_unit_registration eur
             INNER JOIN dbo.external_unit_type_master et\s
             ON et.external_unit_type_id = eur.external_unit_type_id
             INNER JOIN dbo.eu_virtual_bank_account evba\s
             ON evba.eu_id = eur.external_unit_registration_id\s
             AND evba.market_master_id = :marketId
             LEFT JOIN dbo.REELER_VID_CURRENT_BALANCE rvcb\s
             ON rvcb.reeler_virtual_account_number = evba.virtual_account_number
""", nativeQuery = true)
    List<Object[]>  getExternalUnitBalanceByMarket(
            @Param("marketId") Long marketId);

    @Query(value = """
            SELECT\s
                r.name,
                rvba.virtual_account_number,
                ISNULL(rvcb.CURRENT_BALANCE, 0) AS CURRENT_BALANCE,
                mm.releer_minimum_balance,
                FORMAT(ISNULL(rvcb.MODIFIED_DATE, rvcb.CREATED_DATE), 'dd-MM-yyyy HH:mm:ss') AS updated_date
            FROM reeler r
            
            INNER JOIN reeler_virtual_bank_account rvba
                ON rvba.reeler_Id = r.reeler_Id
                AND rvba.market_master_id = :marketId
                AND rvba.active = 1
            
            LEFT JOIN REELER_VID_CURRENT_BALANCE rvcb
                ON rvcb.reeler_virtual_account_number = rvba.virtual_account_number
            
            LEFT JOIN market_master mm
                ON mm.market_master_id = rvba.market_master_id
""", nativeQuery = true)
    List<Object[]> getReelerBalance(
            @Param("marketId") Long marketId);

    @Query(nativeQuery = true, value = MarketAuctionQueryConstants.SEED_MF_REPORT_WITHOUT_LICENSE)
    List<Object[]> getSeedMFReportWithoutLicense(LocalDate fromDate, LocalDate toDate, int marketId);

    @Query(nativeQuery = true, value = MarketAuctionQueryConstants.SEED_MF_REPORT_WITH_LICENSE)
    List<Object[]> getSeedMFReportWithLicense(LocalDate fromDate, LocalDate toDate, int marketId, String licenseNumber);

    @Query(nativeQuery = true,
            value = MarketAuctionQueryConstants.SEED_MARKET_BIDDING_REPORT_QUERY_WITH_LICENSE)
    List<Object[]> getSeedMarketBiddingReport(
            int marketId,
            LocalDate auctionDate,
            String licenseNumber
    );
    @Query(nativeQuery = true,
            value = MarketAuctionQueryConstants.SEED_MARKET_BIDDING_REPORT_QUERY_WITHOUT_LICENSE)
    List<Object[]> getSeedMarketBiddingReportWithoutLicense(
            int marketId,
            LocalDate auctionDate
    );
    @Query(nativeQuery = true,
            value = MarketAuctionQueryConstants.BUYER_QUERY)
    List<Object[]> getBuyerDetails(
            int marketId,
            String licenseNumber
    );

    @Query(nativeQuery = true,
            value = MarketAuctionQueryConstants.SEED_MARKET_CURRENT_BALANCE_QUERY)
    List<Object[]> getSeedMarketCurrentBalance(
            String virtualAccount
    );


    @Query(nativeQuery = true,
            value = MarketAuctionQueryConstants.SEED_MARKET_TRANSACTION_PASS_BOOK)
    List<Object[]> getSeedMarketTransactionPassBook(
            LocalDate fromDate,
            LocalDate toDate,
            String vAccount,
            int marketId
    );

    @Query(nativeQuery = true,
            value = MarketAuctionQueryConstants.CASH_BALANCE)
    List<Object[]> getCashBalance(
            LocalDate fromDate,
            LocalDate toDate,
            int marketId
    );


    @Query(nativeQuery = true,
            value = MarketAuctionQueryConstants.CASH_BALANCE_WITH_LICENSE)
    List<Object[]> getCashBalanceWithLicense(
            LocalDate fromDate,
            LocalDate toDate,
            int marketId,
            String licenseNumber
    );

    @Query(nativeQuery = true, value = MarketAuctionQueryConstants.SEED_MARKET_DASHBOARD_QUERY)
    List<Object[]> getSeedMarketDashboard(
            @Param("auctionDate") LocalDate auctionDate
    );

    @Query(nativeQuery = true, value = MarketAuctionQueryConstants.SEED_MARKET_DASHBOARD_ALL_QUERY)
    List<Object[]> getSeedMarketDashboardAll();
}
