package com.sericulture.marketandauction.service.legacy;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** XC-004: validation and normalisation of one legacy export row. */
class LegacyRowParserTest {

    static final LocalDate TODAY = LocalDate.of(2026, 11, 23);

    static Map<String, String> row() {
        Map<String, String> m = new HashMap<>();
        m.put("legacy_txn_id", "OLD-1");
        m.put("market_name", "Ramanagara");
        m.put("auction_date", "15-05-2023");
        m.put("lot_number", "57");
        m.put("farmer_fruits_id", "fid2901000023662");
        m.put("farmer_mobile", "+91 81059-37769");
        m.put("reeler_licence_no", "rl/rmn/0012");
        m.put("race_name", "Cross Breed");
        m.put("weight_kg", "45.5");
        m.put("rate_per_kg", "520");
        return m;
    }

    @Test
    void validRow_isNormalised_andAmountComputed() {
        LegacyRowParser.Outcome o = LegacyRowParser.parse(row(), TODAY);

        assertTrue(o.ok(), o.errors().toString());
        assertEquals(LocalDate.of(2023, 5, 15), o.row().auctionDate());
        assertEquals("FID2901000023662", o.row().farmerFruitsId());
        assertEquals("8105937769", o.row().farmerMobile());
        assertEquals("RL/RMN/0012", o.row().reelerLicenceNo());
        assertEquals(new BigDecimal("23660.00"), o.row().amount());
    }

    @Test
    void dates_inEveryExportFormat_areRead() {
        for (String d : List.of("2023-05-15", "15-05-2023", "15/05/2023", "15.05.2023", "2023-05-15 00:00:00")) {
            Map<String, String> r = row();
            r.put("auction_date", d);
            assertEquals(LocalDate.of(2023, 5, 15), LegacyRowParser.parse(r, TODAY).row().auctionDate(), d);
        }
    }

    @Test
    void missingRequiredFields_areAllReported() {
        Map<String, String> r = new HashMap<>();
        r.put("farmer_name", "X");

        LegacyRowParser.Outcome o = LegacyRowParser.parse(r, TODAY);

        assertFalse(o.ok());
        String all = String.join(" ", o.errors());
        for (String f : List.of("legacy_txn_id", "market_name", "auction_date", "lot_number", "weight_kg", "rate_per_kg", "farmer_fruits_id or farmer_mobile")) {
            assertTrue(all.contains(f), f + " in " + all);
        }
    }

    @Test
    void badValues_areRejectedWithTheReason() {
        Map<String, String> r = row();
        r.put("auction_date", "31-02-2023");
        r.put("weight_kg", "-3");
        r.put("rate_per_kg", "abc");
        r.put("farmer_mobile", "12345");

        String all = String.join(" ", LegacyRowParser.parse(r, TODAY).errors());

        assertTrue(all.contains("not a valid date"), all);
        assertTrue(all.contains("weight_kg must be greater than 0"), all);
        assertTrue(all.contains("rate_per_kg 'abc' is not a number"), all);
        assertTrue(all.contains("not a valid 10-digit mobile"), all);
    }

    @Test
    void futureDate_andAmountNotMatchingWeightTimesRate_areRejected() {
        Map<String, String> r = row();
        r.put("auction_date", "01-01-2027");
        r.put("amount", "30000");

        String all = String.join(" ", LegacyRowParser.parse(r, TODAY).errors());

        assertTrue(all.contains("is in the future"), all);
        assertTrue(all.contains("does not match weight x rate (23660.00)"), all);
    }

    @Test
    void amountWithinOnePercent_isAccepted() {
        Map<String, String> r = row();
        r.put("amount", "23,700");
        assertTrue(LegacyRowParser.parse(r, TODAY).ok());
    }

    @Test
    void hash_changesOnlyWhenBusinessDataChanges() {
        LegacyRowParser.ParsedRow a = LegacyRowParser.parse(row(), TODAY).row();
        Map<String, String> sameValues = row();
        sameValues.put("weight_kg", "45.500");
        LegacyRowParser.ParsedRow b = LegacyRowParser.parse(sameValues, TODAY).row();
        Map<String, String> changed = row();
        changed.put("rate_per_kg", "530");
        LegacyRowParser.ParsedRow c = LegacyRowParser.parse(changed, TODAY).row();

        assertEquals(LegacyRowParser.hash(a, 1), LegacyRowParser.hash(b, 1));
        assertNotEquals(LegacyRowParser.hash(a, 1), LegacyRowParser.hash(c, 1));
        assertNotEquals(LegacyRowParser.hash(a, 1), LegacyRowParser.hash(a, 2), "market is part of the content");
    }

    @Test
    void headers_andNames_areMatchedLoosely() {
        assertEquals("legacy_txn_id", LegacyRowParser.headerKey(" Legacy Txn-ID "));
        assertEquals("weight_kg", LegacyRowParser.headerKey("WEIGHT (KG)"));
        assertEquals(List.of("rate_per_kg"), LegacyRowParser.missingColumns(List.of("legacy_txn_id", "market_name", "auction_date", "lot_number", "weight_kg")));
        assertEquals(LegacyRowParser.nameKey("Ramanagara"), LegacyRowParser.nameKey(" RAMANAGARA. "));
    }
}
