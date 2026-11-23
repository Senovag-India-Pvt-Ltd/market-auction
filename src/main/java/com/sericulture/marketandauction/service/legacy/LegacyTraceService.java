package com.sericulture.marketandauction.service.legacy;

import com.sericulture.marketandauction.repository.LegacyCocoonMarketRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * XC-004: one view of a farmer's / lot's cocoon market transactions across the pre-project (legacy) system and the
 * new system. Legacy rows that are the same lot as a new-system lot are left out, so nothing appears twice.
 */
@Service
public class LegacyTraceService {

    public static final String LEGACY = "LEGACY";
    public static final String NEW_SYSTEM = "NEW_SYSTEM";

    private final LegacyCocoonMarketRepository repository;

    public LegacyTraceService(LegacyCocoonMarketRepository repository) {
        this.repository = repository;
    }

    public record TraceQuery(String fruitsId, String mobile, String lotNumber, Long marketId, String legacyTxnId,
                             LocalDate fromDate, LocalDate toDate) {
    }

    public Map<String, Object> trace(TraceQuery q) {
        String fruits = blank(q.fruitsId()).toUpperCase(Locale.ROOT);
        String mobile = blank(q.mobile()).replaceAll("\\D", "");
        if (mobile.length() > 10) mobile = mobile.substring(mobile.length() - 10);
        String lot = blank(q.lotNumber());
        String legacyId = blank(q.legacyTxnId());
        long marketId = q.marketId() == null ? 0L : q.marketId();
        if (fruits.isEmpty() && mobile.isEmpty() && lot.isEmpty() && legacyId.isEmpty() && marketId == 0) {
            throw new LegacyCocoonMarketImportService.ImportException("Enter a FRUITS ID, mobile number, lot number, market or legacy transaction id.");
        }
        LocalDate from = q.fromDate() == null ? LocalDate.of(2000, 1, 1) : q.fromDate();
        LocalDate to = q.toDate() == null ? LocalDate.now() : q.toDate();
        if (from.isAfter(to)) throw new LegacyCocoonMarketImportService.ImportException("From date is after To date.");

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object[] r : repository.traceLegacy(fruits, mobile, lot, marketId, legacyId, from, to)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("source", LEGACY);
            m.put("sourceSystem", str(r[1]));
            m.put("reference", str(r[2]));
            m.put("marketName", str(r[3]));
            m.put("auctionDate", LegacyCocoonMarketImportService.toDate(r[4]).toString());
            m.put("lotNumber", str(r[5]));
            m.put("farmerFruitsId", str(r[6]));
            m.put("farmerName", str(r[7]));
            m.put("farmerMobile", str(r[8]));
            m.put("reelerLicenceNo", str(r[9]));
            m.put("reelerName", str(r[10]));
            m.put("race", str(r[11]));
            m.put("weightKg", num(r[12]));
            m.put("ratePerKg", num(r[13]));
            m.put("amount", num(r[14]));
            rows.add(m);
        }
        for (Object[] r : repository.traceNewSystem(fruits, mobile, lot, marketId, legacyId, from, to)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("source", NEW_SYSTEM);
            m.put("sourceSystem", "e-Reshme");
            m.put("reference", "LOT-" + str(r[0]));
            m.put("marketName", str(r[1]));
            m.put("auctionDate", r[2] == null ? null : LegacyCocoonMarketImportService.toDate(r[2]).toString());
            m.put("lotNumber", str(r[3]));
            m.put("farmerFruitsId", str(r[4]));
            m.put("farmerName", str(r[5]));
            m.put("farmerMobile", str(r[6]));
            m.put("reelerLicenceNo", str(r[7]));
            m.put("reelerName", str(r[8]));
            m.put("race", str(r[9]));
            m.put("weightKg", num(r[10]));
            m.put("ratePerKg", num(r[11]));
            m.put("amount", num(r[12]));
            rows.add(m);
        }
        rows.sort(Comparator.comparing((Map<String, Object> m) -> String.valueOf(m.get("auctionDate"))).reversed());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", rows.size());
        out.put("legacyCount", rows.stream().filter(m -> LEGACY.equals(m.get("source"))).count());
        out.put("newSystemCount", rows.stream().filter(m -> NEW_SYSTEM.equals(m.get("source"))).count());
        out.put("rows", rows);
        return out;
    }

    private static String blank(String s) {
        return s == null ? "" : s.trim();
    }

    private static String str(Object o) {
        return o == null ? null : o.toString().trim();
    }

    private static BigDecimal num(Object o) {
        if (o == null) return null;
        if (o instanceof BigDecimal b) return b;
        if (o instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try {
            return new BigDecimal(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
