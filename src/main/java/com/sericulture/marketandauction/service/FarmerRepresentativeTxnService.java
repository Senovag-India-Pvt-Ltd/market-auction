package com.sericulture.marketandauction.service;

import com.sericulture.marketandauction.model.entity.FarmerRepresentativeTxn;
import com.sericulture.marketandauction.repository.FarmerRepresentativeTxnRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * SR-005: links an e-Inward transaction to the farmer's authorised agent who brought the cocoons, and traces the
 * transactions done under an agent. The link is only accepted when the agent belongs to the same farmer as the
 * transaction and was authorised on the auction date. e-Inward itself is not changed.
 */
@Service
@Slf4j
public class FarmerRepresentativeTxnService {

    private final FarmerRepresentativeTxnRepository repository;

    public FarmerRepresentativeTxnService(FarmerRepresentativeTxnRepository repository) {
        this.repository = repository;
    }

    public static class LinkException extends RuntimeException {
        public LinkException(String message) {
            super(message);
        }
    }

    @Transactional
    public Map<String, Object> link(Long marketAuctionId, Long farmerRepresentativeId) {
        if (marketAuctionId == null) throw new LinkException("transactionId is required.");
        if (farmerRepresentativeId == null) throw new LinkException("farmerRepresentativeId is required.");

        List<Object[]> auctions = repository.auction(marketAuctionId);
        if (auctions.isEmpty()) throw new LinkException("e-Inward transaction " + marketAuctionId + " not found.");
        Object[] a = auctions.get(0);
        Long auctionFarmerId = num(a[1]);
        Long marketId = num(a[2]);
        LocalDate auctionDate = date(a[3]);

        List<Object[]> agents = repository.agent(farmerRepresentativeId);
        if (agents.isEmpty() || !isTrue(agents.get(0)[2])) throw new LinkException("Agent not found or removed.");
        Object[] g = agents.get(0);
        if (!Objects.equals(num(g[1]), auctionFarmerId)) {
            throw new LinkException("This agent is not authorised for the farmer of this transaction.");
        }
        if (!isAuthorisedOn(date(g[3]), date(g[4]), auctionDate)) {
            throw new LinkException("The agent's authorisation is not valid on " + auctionDate + ".");
        }

        FarmerRepresentativeTxn existing = repository.findByMarketAuctionIdAndActive(marketAuctionId, true).orElse(null);
        if (existing != null) {
            if (Objects.equals(existing.getFarmerRepresentativeId(), farmerRepresentativeId)) {
                return view(existing); // same link sent again: nothing to do
            }
            throw new LinkException("This transaction is already linked to another agent.");
        }

        FarmerRepresentativeTxn t = new FarmerRepresentativeTxn();
        t.setMarketAuctionId(marketAuctionId);
        t.setFarmerId(auctionFarmerId);
        t.setFarmerRepresentativeId(farmerRepresentativeId);
        t.setMarketId(marketId);
        t.setAuctionDate(auctionDate);
        t.setActive(true);
        FarmerRepresentativeTxn saved = repository.save(t);
        log.info("SR-005: e-Inward {} linked to farmer agent {} (farmer {})", marketAuctionId, farmerRepresentativeId, auctionFarmerId);
        return view(saved);
    }

    public Map<String, Object> trace(Long farmerRepresentativeId, LocalDate fromDate, LocalDate toDate) {
        if (farmerRepresentativeId == null) throw new LinkException("farmerRepresentativeId is required.");
        LocalDate from = fromDate == null ? LocalDate.of(2000, 1, 1) : fromDate;
        LocalDate to = toDate == null ? LocalDate.now().plusDays(1) : toDate;
        if (from.isAfter(to)) throw new LinkException("From date is after To date.");

        List<Object[]> agents = repository.agentWithFarmer(farmerRepresentativeId);
        if (agents.isEmpty()) throw new LinkException("Agent not found.");
        Object[] g = agents.get(0);
        Map<String, Object> agent = new LinkedHashMap<>();
        agent.put("farmerRepresentativeId", num(g[0]));
        agent.put("name", str(g[1]));
        agent.put("mobileNumber", str(g[2]));
        agent.put("active", isTrue(g[3]));
        agent.put("authorisedFrom", date(g[4]));
        agent.put("authorisedTo", date(g[5]));
        agent.put("relationshipName", str(g[11]));
        Map<String, Object> farmer = new LinkedHashMap<>();
        farmer.put("farmerId", num(g[6]));
        farmer.put("farmerName", str(g[7]));
        farmer.put("fruitsId", str(g[8]));
        farmer.put("farmerNumber", str(g[9]));
        farmer.put("mobileNumber", str(g[10]));

        List<Map<String, Object>> rows = new ArrayList<>();
        Set<Long> transactions = new HashSet<>();
        BigDecimal kg = BigDecimal.ZERO;
        BigDecimal amount = BigDecimal.ZERO;
        for (Object[] o : repository.lotsByAgent(farmerRepresentativeId, from, to)) {
            Map<String, Object> m = new LinkedHashMap<>();
            Long txnId = num(o[0]);
            transactions.add(txnId);
            m.put("transactionId", txnId);
            m.put("auctionDate", date(o[1]));
            m.put("marketName", str(o[2]));
            m.put("lotNumber", o[3] == null ? null : str(o[3]));
            m.put("lotStatus", str(o[4]));
            m.put("race", str(o[5]));
            BigDecimal w = dec(o[6]);
            BigDecimal amt = dec(o[8]);
            m.put("weightKg", w);
            m.put("ratePerKg", dec(o[7]));
            m.put("amount", amt);
            m.put("reelerName", str(o[9]));
            m.put("reelerLicenceNo", str(o[10]));
            m.put("inwardStatus", str(o[11]));
            m.put("farmerId", num(o[12]));
            if (w != null) kg = kg.add(w);
            if (amt != null) amount = amount.add(amt);
            rows.add(m);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("agent", agent);
        out.put("farmer", farmer);
        out.put("transactionCount", transactions.size());
        out.put("lotCount", rows.stream().filter(r -> r.get("lotNumber") != null).count());
        out.put("totalWeightKg", kg);
        out.put("totalAmount", amount);
        out.put("rows", rows);
        return out;
    }

    // ------------------------------------------------------------------ helpers

    static boolean isAuthorisedOn(LocalDate from, LocalDate to, LocalDate day) {
        if (day == null) return false;
        if (from != null && day.isBefore(from)) return false;
        return to == null || !day.isAfter(to);
    }

    private static Map<String, Object> view(FarmerRepresentativeTxn t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("farmerRepresentativeTxnId", t.getFarmerRepresentativeTxnId());
        m.put("transactionId", t.getMarketAuctionId());
        m.put("farmerId", t.getFarmerId());
        m.put("farmerRepresentativeId", t.getFarmerRepresentativeId());
        m.put("auctionDate", t.getAuctionDate());
        return m;
    }

    private static boolean isTrue(Object o) {
        if (o instanceof Boolean b) return b;
        if (o instanceof Number n) return n.intValue() == 1;
        return o != null && "1".equals(o.toString().trim());
    }

    static LocalDate date(Object o) {
        if (o == null) return null;
        if (o instanceof LocalDate d) return d;
        if (o instanceof java.sql.Date d) return d.toLocalDate();
        if (o instanceof java.sql.Timestamp t) return t.toLocalDateTime().toLocalDate();
        if (o instanceof java.util.Date d) return new java.sql.Date(d.getTime()).toLocalDate();
        return LocalDate.parse(o.toString().substring(0, 10));
    }

    private static Long num(Object o) {
        if (o instanceof Number n) return n.longValue();
        if (o == null) return null;
        try {
            return Long.valueOf(o.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BigDecimal dec(Object o) {
        if (o == null) return null;
        if (o instanceof BigDecimal b) return b;
        if (o instanceof Number n) return new BigDecimal(n.toString());
        try {
            return new BigDecimal(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String str(Object o) {
        return o == null ? null : o.toString().trim();
    }
}
