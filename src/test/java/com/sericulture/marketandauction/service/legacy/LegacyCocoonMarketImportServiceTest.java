package com.sericulture.marketandauction.service.legacy;

import com.sericulture.marketandauction.model.entity.LegacyCocoonMarketImport;
import com.sericulture.marketandauction.model.entity.LegacyCocoonMarketImportError;
import com.sericulture.marketandauction.model.entity.LegacyCocoonMarketTxn;
import com.sericulture.marketandauction.repository.LegacyCocoonMarketImportErrorRepository;
import com.sericulture.marketandauction.repository.LegacyCocoonMarketImportRepository;
import com.sericulture.marketandauction.repository.LegacyCocoonMarketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** XC-004: import and re-import (sync) without duplication. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LegacyCocoonMarketImportServiceTest {

    @Mock
    LegacyCocoonMarketRepository repository;
    @Mock
    LegacyCocoonMarketImportRepository importRepository;
    @Mock
    LegacyCocoonMarketImportErrorRepository errorRepository;

    LegacyCocoonMarketImportService service;
    final Map<String, LegacyCocoonMarketTxn> stored = new HashMap<>();
    final List<LegacyCocoonMarketImportError> savedErrors = new ArrayList<>();

    static final String HEADER = "legacy_txn_id,market_name,auction_date,lot_number,farmer_fruits_id,farmer_mobile,reeler_licence_no,race_name,weight_kg,rate_per_kg\n";

    static List<Object[]> rows(Object[]... r) {
        return new ArrayList<>(Arrays.asList(r));
    }

    @BeforeEach
    void setUp() {
        service = new LegacyCocoonMarketImportService(repository, importRepository, errorRepository, 1000);
        when(importRepository.save(any())).thenAnswer(i -> {
            LegacyCocoonMarketImport r = i.getArgument(0);
            if (r.getLegacyCocoonMarketImportId() == null) r.setLegacyCocoonMarketImportId(77L);
            return r;
        });
        when(repository.markets()).thenReturn(rows(new Object[]{1, "Ramanagara"}, new Object[]{2, "Kollegal"}));
        when(repository.races()).thenReturn(rows(new Object[]{17, "Cross Breed"}));
        when(repository.farmersByFruitsId(any())).thenReturn(rows(new Object[]{501L, "fid2901000023662"}));
        when(repository.farmersByMobile(any())).thenReturn(rows());
        when(repository.reelersByLicence(any())).thenReturn(rows(new Object[]{9, "RL/RMN/0012"}));
        when(repository.newSystemLots(any(), any(), any())).thenReturn(rows());
        // the "database": rows saved by earlier imports are found again on re-import
        when(repository.findBySourceSystemAndLegacyTxnIdIn(eq("OLD_E-MARKET"), any())).thenAnswer(i -> {
            Collection<String> ids = i.getArgument(1);
            return stored.values().stream().filter(t -> ids.contains(t.getLegacyTxnId())).toList();
        });
        when(repository.saveAll(any())).thenAnswer(i -> {
            List<LegacyCocoonMarketTxn> l = new ArrayList<>();
            ((Iterable<LegacyCocoonMarketTxn>) i.getArgument(0)).forEach(t -> {
                stored.put(t.getLegacyTxnId(), t);
                l.add(t);
            });
            return l;
        });
        when(errorRepository.saveAll(any())).thenAnswer(i -> {
            ((Iterable<LegacyCocoonMarketImportError>) i.getArgument(0)).forEach(savedErrors::add);
            return List.of();
        });
    }

    Map<String, Object> importCsv(String body) {
        return service.importFile("old e-market", "export.csv", (HEADER + body).getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void firstImport_addsRows_resolvingFarmerReelerRaceAndMarket() {
        Map<String, Object> r = importCsv("OLD-1,ramanagara,15-05-2023,57,FID2901000023662,,rl/rmn/0012,cross breed,45.5,520\n");

        assertEquals(1, r.get("inserted"));
        LegacyCocoonMarketTxn t = stored.get("OLD-1");
        assertEquals("OLD_E-MARKET", t.getSourceSystem());
        assertEquals(1L, t.getMarketMasterId());
        assertEquals(501L, t.getFarmerId());
        assertEquals(9L, t.getReelerId());
        assertEquals(17L, t.getRaceMasterId());
        assertEquals(77L, t.getFirstImportId());
    }

    @Test
    void reImportingTheSameExport_addsNothing() {
        String body = "OLD-1,Ramanagara,15-05-2023,57,FID2901000023662,,,,45.5,520\nOLD-2,Kollegal,16-05-2023,8,,9845012345,,,10,500\n";
        importCsv(body);
        int before = stored.size();

        Map<String, Object> second = importCsv(body);

        assertEquals(0, second.get("inserted"));
        assertEquals(0, second.get("updated"));
        assertEquals(2, second.get("unchanged"));
        assertEquals(before, stored.size(), "no duplicates");
    }

    @Test
    void reImportWithAChangedAndANewRow_updatesOne_addsOne_keepsTheRest() {
        importCsv("OLD-1,Ramanagara,15-05-2023,57,FID2901000023662,,,,45.5,520\nOLD-2,Kollegal,16-05-2023,8,,9845012345,,,10,500\n");

        Map<String, Object> sync = importCsv("OLD-1,Ramanagara,15-05-2023,57,FID2901000023662,,,,45.5,530\n"
                + "OLD-2,Kollegal,16-05-2023,8,,9845012345,,,10,500\n"
                + "OLD-3,Kollegal,17-05-2023,9,,9845012345,,,12,500\n");

        assertEquals(1, sync.get("inserted"));
        assertEquals(1, sync.get("updated"));
        assertEquals(1, sync.get("unchanged"));
        assertEquals(3, stored.size());
        assertEquals(0, stored.get("OLD-1").getRatePerKg().compareTo(new java.math.BigDecimal("530")));
        assertEquals(77L, stored.get("OLD-1").getLastImportId());
    }

    @Test
    void duplicateIdInOneFile_unknownMarket_andInvalidRow_areRejectedNotSaved() {
        Map<String, Object> r = importCsv("OLD-1,Ramanagara,15-05-2023,57,FID2901000023662,,,,45.5,520\n"
                + "OLD-1,Ramanagara,15-05-2023,58,FID2901000023662,,,,10,520\n"
                + "OLD-4,Nowhere Market,15-05-2023,5,FID2901000023662,,,,10,520\n"
                + "OLD-5,Ramanagara,not-a-date,5,FID2901000023662,,,,10,520\n");

        assertEquals(1, r.get("inserted"));
        assertEquals(3, r.get("rejected"));
        assertEquals(1, stored.size());
        String all = savedErrors.stream().map(LegacyCocoonMarketImportError::getMessage).reduce("", (a, b) -> a + " | " + b);
        assertTrue(all.contains("repeated in this file (first on row 2)"), all);
        assertTrue(all.contains("Market 'Nowhere Market' is not in the market master"), all);
        assertTrue(all.contains("not a valid date"), all);
        assertEquals(List.of(3, 4, 5), savedErrors.stream().map(LegacyCocoonMarketImportError::getRowNumberInFile).sorted().toList());
    }

    @Test
    void lotAlreadyInTheNewSystem_isLinked_soItIsNotShownTwice() {
        when(repository.newSystemLots(any(), any(), any())).thenReturn(rows(new Object[]{1, Date.valueOf("2024-07-01"), 57, 9001L}));

        Map<String, Object> r = importCsv("OLD-7,Ramanagara,01-07-2024,057,FID2901000023662,,,,45.5,520\n");

        assertEquals(1, r.get("alreadyInNewSystem"));
        assertEquals(9001L, stored.get("OLD-7").getNewSystemLotId());
    }

    @Test
    void ambiguousMobile_isNotResolvedToAFarmer() {
        when(repository.farmersByFruitsId(any())).thenReturn(rows());
        when(repository.farmersByMobile(any())).thenReturn(rows(new Object[]{1L, "9845012345"}, new Object[]{2L, "9845012345"}));

        importCsv("OLD-8,Kollegal,16-05-2023,8,,9845012345,,,10,500\n");

        assertNull(stored.get("OLD-8").getFarmerId(), "two farmers share the mobile: leave unresolved");
    }

    @Test
    void wrongTemplate_orNoSourceSystem_isRefused() {
        LegacyCocoonMarketImportService.ImportException e = assertThrows(LegacyCocoonMarketImportService.ImportException.class,
                () -> service.importFile("OLD", "x.csv", "id,market\n1,M\n".getBytes(StandardCharsets.UTF_8)));
        assertTrue(e.getMessage().contains("Missing column(s): legacy_txn_id"));
        assertThrows(LegacyCocoonMarketImportService.ImportException.class,
                () -> service.importFile("  ", "x.csv", (HEADER + "A,M,01-01-2023,1,F,,,,1,1\n").getBytes(StandardCharsets.UTF_8)));
        verify(repository, never()).saveAll(any());
    }

    @Test
    void lotDigits_matchesNewSystemLotIds() {
        assertEquals("57", LegacyCocoonMarketImportService.lotDigits("057"));
        assertEquals("57", LegacyCocoonMarketImportService.lotDigits("LOT-57"));
        assertEquals("-", LegacyCocoonMarketImportService.lotDigits("A"));
        assertEquals("OLD_E-MARKET", LegacyCocoonMarketImportService.normaliseSource(" old e-market "));
    }
}
