package com.sericulture.marketandauction.service;

import com.sericulture.marketandauction.model.entity.FarmerRepresentativeTxn;
import com.sericulture.marketandauction.repository.FarmerRepresentativeTxnRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** SR-005: agent <-> e-Inward link rules and the trace. */
@ExtendWith(MockitoExtension.class)
class FarmerRepresentativeTxnServiceTest {

    @Mock
    FarmerRepresentativeTxnRepository repository;

    static final LocalDate AUCTION_DAY = LocalDate.of(2026, 11, 20);

    private FarmerRepresentativeTxnService service() {
        return new FarmerRepresentativeTxnService(repository);
    }

    private static List<Object[]> rows(Object[]... r) {
        List<Object[]> l = new ArrayList<>();
        for (Object[] o : r) l.add(o);
        return l;
    }

    /** e-Inward 500 of farmer 10 on AUCTION_DAY. */
    private void auctionOfFarmer10() {
        when(repository.auction(500L)).thenReturn(rows(new Object[]{500L, 10L, 3, Date.valueOf(AUCTION_DAY), "generated"}));
    }

    private void agent(long id, long farmerId, int active, LocalDate from, LocalDate to) {
        when(repository.agent(id)).thenReturn(rows(new Object[]{id, farmerId, active, from == null ? null : Date.valueOf(from), to == null ? null : Date.valueOf(to)}));
    }

    @Test
    void link_agentOfTheSameFarmer_authorisedThatDay_isSaved() {
        auctionOfFarmer10();
        agent(7, 10, 1, LocalDate.of(2026, 1, 1), null);
        when(repository.findByMarketAuctionIdAndActive(500L, true)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(i -> { FarmerRepresentativeTxn t = i.getArgument(0); t.setFarmerRepresentativeTxnId(1L); return t; });

        Map<String, Object> res = service().link(500L, 7L);

        assertEquals(500L, res.get("transactionId"));
        assertEquals(10L, res.get("farmerId"));
        assertEquals(7L, res.get("farmerRepresentativeId"));
        assertEquals(AUCTION_DAY, res.get("auctionDate"));
    }

    @Test
    void link_agentOfAnotherFarmer_isRefused() {
        auctionOfFarmer10();
        agent(8, 99, 1, LocalDate.of(2026, 1, 1), null);

        var e = assertThrows(FarmerRepresentativeTxnService.LinkException.class, () -> service().link(500L, 8L));
        assertEquals("This agent is not authorised for the farmer of this transaction.", e.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void link_authorisationExpiredBeforeTheAuction_isRefused() {
        auctionOfFarmer10();
        agent(7, 10, 1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 11, 19));

        assertThrows(FarmerRepresentativeTxnService.LinkException.class, () -> service().link(500L, 7L));
        verify(repository, never()).save(any());
    }

    @Test
    void link_removedAgent_isRefused() {
        auctionOfFarmer10();
        agent(7, 10, 0, LocalDate.of(2026, 1, 1), null);

        assertThrows(FarmerRepresentativeTxnService.LinkException.class, () -> service().link(500L, 7L));
        verify(repository, never()).save(any());
    }

    @Test
    void link_unknownTransaction_isRefused() {
        when(repository.auction(501L)).thenReturn(new ArrayList<>());

        assertThrows(FarmerRepresentativeTxnService.LinkException.class, () -> service().link(501L, 7L));
    }

    @Test
    void link_sameLinkSentAgain_isNotDuplicated() {
        auctionOfFarmer10();
        agent(7, 10, 1, LocalDate.of(2026, 1, 1), null);
        FarmerRepresentativeTxn existing = new FarmerRepresentativeTxn();
        existing.setFarmerRepresentativeTxnId(1L);
        existing.setMarketAuctionId(500L);
        existing.setFarmerRepresentativeId(7L);
        when(repository.findByMarketAuctionIdAndActive(500L, true)).thenReturn(Optional.of(existing));

        Map<String, Object> res = service().link(500L, 7L);

        assertEquals(1L, res.get("farmerRepresentativeTxnId"));
        verify(repository, never()).save(any());
    }

    @Test
    void link_transactionAlreadyLinkedToAnotherAgent_isRefused() {
        auctionOfFarmer10();
        agent(7, 10, 1, LocalDate.of(2026, 1, 1), null);
        FarmerRepresentativeTxn existing = new FarmerRepresentativeTxn();
        existing.setMarketAuctionId(500L);
        existing.setFarmerRepresentativeId(6L);
        when(repository.findByMarketAuctionIdAndActive(500L, true)).thenReturn(Optional.of(existing));

        assertThrows(FarmerRepresentativeTxnService.LinkException.class, () -> service().link(500L, 7L));
        verify(repository, never()).save(any());
    }

    @Test
    void trace_returnsAgentFarmerAndTotals() {
        when(repository.agentWithFarmer(7L)).thenReturn(rows(new Object[]{7L, "Ramesh K", "9876543210", 1,
                Date.valueOf(LocalDate.of(2026, 1, 1)), null, 10L, "GOVINDAIAH", "FID1", "F-1", "8197926209", "Son"}));
        when(repository.lotsByAgent(eq(7L), any(), any())).thenReturn(rows(
                new Object[]{500L, Date.valueOf(AUCTION_DAY), "Hebbur PM", 1, "accepted", "CSR", 10.5f, 500, new BigDecimal("5250.00"), "R", "RL1", "generated", 10L},
                new Object[]{500L, Date.valueOf(AUCTION_DAY), "Hebbur PM", 2, "accepted", "CSR", 9.5f, 500, new BigDecimal("4750.00"), "R", "RL1", "generated", 10L}));

        Map<String, Object> out = service().trace(7L, null, null);

        assertEquals(1, out.get("transactionCount"));
        assertEquals(2L, out.get("lotCount"));
        assertEquals(0, new BigDecimal("10000.00").compareTo((BigDecimal) out.get("totalAmount")));
        assertEquals(0, new BigDecimal("20.0").compareTo((BigDecimal) out.get("totalWeightKg")));
        @SuppressWarnings("unchecked") Map<String, Object> farmer = (Map<String, Object>) out.get("farmer");
        assertEquals(10L, farmer.get("farmerId"));
    }

    @Test
    void trace_fromAfterTo_isRefused() {
        assertThrows(FarmerRepresentativeTxnService.LinkException.class,
                () -> service().trace(7L, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 1, 1)));
    }
}
