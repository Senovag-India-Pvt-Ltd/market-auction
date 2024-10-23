package com.sericulture.marketandauction.repository;

import com.sericulture.marketandauction.model.entity.PupaTestAndCocoonAssessment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PupaTestAndCocoonAssessmentRepository extends PagingAndSortingRepository<PupaTestAndCocoonAssessment, Integer> {

    public PupaTestAndCocoonAssessment save(PupaTestAndCocoonAssessment pupaTestAndCocoonAssessment);

    public List<PupaTestAndCocoonAssessment> findAllByPupaTestResult(String pupaTestResult);

    public List<PupaTestAndCocoonAssessment> findAllByCocoonAssessmentResult(String cocoonAssessmentResult);

    public List<PupaTestAndCocoonAssessment> findAllByCocoonAssessmentResultAndPupaTestResult(String cocoonAssessmentResult, String pupatestResult);

    public PupaTestAndCocoonAssessment findByMarketAuctionId(int marketAuctionId);

    List<PupaTestAndCocoonAssessment> findByMarketAuctionIdAndActive(int marketAuctionId,boolean isActive);

    public List<PupaTestAndCocoonAssessment> findAllByTestDate(LocalDate testDate);

    List<PupaTestAndCocoonAssessment> findByActiveOrderByIdDesc(boolean isActive);

    @Query(nativeQuery = true , value = """
            WITH PrimaryAddress AS (
            SELECT
                ROW_NUMBER() OVER (ORDER BY fa.farmer_id ASC) AS row_id,
                fa.farmer_id,
                fa.STATE_ID,
                fa.DISTRICT_ID,
                fa.TALUK_ID,
                fa.HOBLI_ID,
                fa.VILLAGE_ID,
                ROW_NUMBER() OVER (PARTITION BY fa.farmer_id ORDER BY fa.district_id DESC) AS rn
            FROM
                farmer_address fa
            WHERE
                fa.active = 1
        )
        SELECT
            f.farmer_id,
            f.first_name,
            f.middle_name,
            f.last_name,
            f.fruits_id,
            f.farmer_number,
            f.father_name,
            f.dob,
            f.mobile_number,
            d.DISTRICT_NAME,
            t.TALUK_NAME,
            h.hobli_name,
            v.village_name,
            ma.dfl_lot_number,
            ma.lot_variety,
            ma.lot_Parental_Level,
            s.state_name,
            rm.race_name,
            ma.estimated_weight,
            ma.market_auction_id,
            ma.market_auction_date,
            l.allotted_lot_id
        FROM
            farmer f
        LEFT JOIN
            PrimaryAddress pa ON pa.farmer_id = f.farmer_id AND pa.rn = 1
        LEFT JOIN
            state s ON pa.state_id = s.state_id AND s.active = 1
        LEFT JOIN
            district d ON pa.DISTRICT_ID = d.DISTRICT_ID AND d.active = 1
        LEFT JOIN
            taluk t ON pa.TALUK_ID = t.TALUK_ID AND t.active = 1
        LEFT JOIN
            hobli h ON pa.HOBLI_ID = h.HOBLI_ID AND h.active = 1
        LEFT JOIN
            village v ON pa.VILLAGE_ID = v.VILLAGE_ID AND v.active = 1
        INNER JOIN
            market_auction ma ON ma.farmer_id = f.farmer_id
        INNER JOIN
        lot l ON l.market_auction_id = ma.market_auction_id
        LEFT JOIN
            race_master rm ON rm.race_id = ma.lot_variety AND rm.active = 1
        LEFT JOIN
            PUPA_TEST_AND_COCOON_ASSESSMENT ptaca ON ptaca.MARKET_AUCTION_ID = ma.market_auction_id
        WHERE
            ma.market_id = :marketId
            AND ptaca.pupa_cocoon_status IS NULL
        ORDER BY
            ma.created_date DESC;
    """)
    public List<Object[]> getPupaAndCocoonAssessmentByMarket(int marketId);

    @Query(nativeQuery = true , value = """
    WITH PrimaryAddress AS (
        SELECT
            ROW_NUMBER() OVER (ORDER BY fa.farmer_id ASC) AS row_id,
            fa.farmer_id,
            fa.STATE_ID,
            fa.DISTRICT_ID,
            fa.TALUK_ID,
            fa.HOBLI_ID,
            fa.VILLAGE_ID,
            ROW_NUMBER() OVER (PARTITION BY fa.farmer_id ORDER BY fa.district_id DESC) AS rn
        FROM
            farmer_address fa
        WHERE
            fa.active = 1
    )
    SELECT
        f.farmer_id,
        f.first_name,
        f.middle_name,
        f.last_name,
        f.fruits_id,
        f.farmer_number,
        f.father_name,
        f.dob,
        f.mobile_number,
        d.DISTRICT_NAME,
        t.TALUK_NAME,
        h.hobli_name,
        v.village_name,
        ma.dfl_lot_number,
        ma.lot_variety,
        ma.lot_Parental_Level,
        s.state_name,
        rm.race_name,
        ma.estimated_weight,
        ma.market_auction_id,
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
        l.allotted_lot_id
    FROM
        farmer f
    LEFT JOIN
        PrimaryAddress pa ON pa.farmer_id = f.farmer_id AND pa.rn = 1
    LEFT JOIN
        state s ON pa.state_id = s.state_id AND s.active = 1
    LEFT JOIN
        district d ON pa.DISTRICT_ID = d.DISTRICT_ID AND d.active = 1
    LEFT JOIN
        taluk t ON pa.TALUK_ID = t.TALUK_ID AND t.active = 1
    LEFT JOIN
        hobli h ON pa.HOBLI_ID = h.HOBLI_ID AND h.active = 1
    LEFT JOIN
        village v ON pa.VILLAGE_ID = v.VILLAGE_ID AND v.active = 1
    INNER JOIN
        market_auction ma ON ma.farmer_id = f.farmer_id
    INNER JOIN
        lot l ON l.market_auction_id = ma.market_auction_id
    LEFT JOIN
        race_master rm ON rm.race_id = ma.lot_variety AND rm.active = 1
    INNER JOIN
        PUPA_TEST_AND_COCOON_ASSESSMENT ptaca ON ptaca.MARKET_AUCTION_ID = ma.market_auction_id AND ptaca.ACTIVE = 1
    INNER JOIN
        LOT_BASE_PRICE_FIXATION lbpf ON lbpf.MARKET_ID = ma.market_id AND lbpf.FIXATION_DATE = ma.market_auction_date AND lbpf.active = 1
    WHERE
        ma.market_id = :marketId
        AND ptaca.pupa_cocoon_status = 'assessment'
        AND l.status NOT IN ('weighmentcompleted')
    ORDER BY
        ptaca.created_date DESC;
    """)
    public List<Object[]> getFinalWeighmentList(int marketId);
}
