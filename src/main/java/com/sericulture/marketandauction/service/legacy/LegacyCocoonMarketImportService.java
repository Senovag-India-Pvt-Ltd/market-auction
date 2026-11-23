package com.sericulture.marketandauction.service.legacy;

import com.sericulture.marketandauction.model.entity.LegacyCocoonMarketImport;
import com.sericulture.marketandauction.model.entity.LegacyCocoonMarketImportError;
import com.sericulture.marketandauction.model.entity.LegacyCocoonMarketTxn;
import com.sericulture.marketandauction.repository.LegacyCocoonMarketImportErrorRepository;
import com.sericulture.marketandauction.repository.LegacyCocoonMarketImportRepository;
import com.sericulture.marketandauction.repository.LegacyCocoonMarketRepository;
import com.sericulture.marketandauction.service.legacy.LegacyRowParser.Outcome;
import com.sericulture.marketandauction.service.legacy.LegacyRowParser.ParsedRow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;

/**
 * XC-004: imports the legacy cocoon market export (fixed template, .xlsx / .csv) into legacy_cocoon_market_txn.
 * Re-importing a newer export synchronises without duplication: rows are matched on (source system, legacy
 * transaction id) - new ones are inserted, changed ones (different content hash) updated, the rest left as they are.
 * A legacy row that is the same lot as a new-system lot (market + auction date + lot number) is linked to it.
 * One transaction per file: a failure leaves nothing half-imported.
 *
 * <p>Setting: LEGACY_IMPORT_MAX_ROWS (100000).
 */
@Service
@Slf4j
public class LegacyCocoonMarketImportService {

    public static class ImportException extends RuntimeException {
        public ImportException(String message) {
            super(message);
        }
    }

    static final int CHUNK = 900; // SQL Server allows 2100 parameters per statement

    private final LegacyCocoonMarketRepository repository;
    private final LegacyCocoonMarketImportRepository importRepository;
    private final LegacyCocoonMarketImportErrorRepository errorRepository;
    private final int maxRows;

    public LegacyCocoonMarketImportService(LegacyCocoonMarketRepository repository,
                                           LegacyCocoonMarketImportRepository importRepository,
                                           LegacyCocoonMarketImportErrorRepository errorRepository,
                                           @Value("${LEGACY_IMPORT_MAX_ROWS:100000}") int maxRows) {
        this.repository = repository;
        this.importRepository = importRepository;
        this.errorRepository = errorRepository;
        this.maxRows = Math.max(1, maxRows);
    }

    @Transactional
    public Map<String, Object> importFile(String sourceSystem, String fileName, byte[] bytes) {
        String source = normaliseSource(sourceSystem);
        LegacyFileReader.Sheet sheet;
        try {
            sheet = LegacyFileReader.read(fileName, bytes, maxRows);
        } catch (LegacyFileReader.FileFormatException e) {
            throw new ImportException(e.getMessage());
        }
        List<String> missing = LegacyRowParser.missingColumns(sheet.headerKeys());
        if (!missing.isEmpty()) {
            throw new ImportException("The file does not follow the template. Missing column(s): " + String.join(", ", missing)
                    + ". Download the template and keep its header row.");
        }
        if (sheet.rows().isEmpty()) throw new ImportException("The file has no data rows.");

        LegacyCocoonMarketImport run = new LegacyCocoonMarketImport();
        run.setSourceSystem(source);
        run.setFileName(fileName == null ? null : fileName.substring(0, Math.min(255, fileName.length())));
        run.setStatus(LegacyCocoonMarketImport.COMPLETED);
        run.setActive(true);
        run = importRepository.save(run);
        Long runId = run.getLegacyCocoonMarketImportId();

        // ---- parse
        LocalDate today = LocalDate.now();
        List<LegacyCocoonMarketImportError> errors = new ArrayList<>();
        Map<String, Integer> seenIds = new HashMap<>();
        List<Object[]> good = new ArrayList<>(); // [rowNumber, ParsedRow]
        for (LegacyFileReader.Row row : sheet.rows()) {
            Outcome o = LegacyRowParser.parse(row.values(), today);
            String txnId = o.ok() ? o.row().legacyTxnId() : row.values().get("legacy_txn_id");
            if (!o.ok()) {
                errors.add(error(runId, row.number(), txnId, String.join(" ", o.errors())));
                continue;
            }
            Integer firstRow = seenIds.putIfAbsent(o.row().legacyTxnId(), row.number());
            if (firstRow != null) {
                errors.add(error(runId, row.number(), txnId, "legacy_txn_id " + txnId + " is repeated in this file (first on row " + firstRow + ")."));
                continue;
            }
            good.add(new Object[]{row.number(), o.row()});
        }

        // ---- resolve masters (in bulk)
        Map<String, Long> markets = byName(repository.markets());
        Map<String, Long> races = byName(repository.races());
        Map<String, Long> farmersByFruits = lookup(good, r -> r.farmerFruitsId(), repository::farmersByFruitsId, false);
        Map<String, Long> farmersByMobile = lookup(good, r -> r.farmerMobile(), repository::farmersByMobile, true);
        Map<String, Long> reelers = lookup(good, r -> r.reelerLicenceNo(), repository::reelersByLicence, false);

        // ---- lots already in the new system (same market + date + lot number)
        Map<String, Long> newLots = new HashMap<>();
        Set<Long> marketIds = new HashSet<>();
        LocalDate min = null, max = null;
        for (Object[] g : good) {
            ParsedRow r = (ParsedRow) g[1];
            Long m = markets.get(LegacyRowParser.nameKey(r.marketName()));
            if (m == null) continue;
            marketIds.add(m);
            min = min == null || r.auctionDate().isBefore(min) ? r.auctionDate() : min;
            max = max == null || r.auctionDate().isAfter(max) ? r.auctionDate() : max;
        }
        if (!marketIds.isEmpty()) {
            for (Object[] l : repository.newSystemLots(marketIds, min, max)) {
                if (l[0] == null || l[1] == null || l[2] == null) continue;
                newLots.put(lotKey(((Number) l[0]).longValue(), toDate(l[1]), String.valueOf(((Number) l[2]).longValue())),
                        ((Number) l[3]).longValue());
            }
        }

        // ---- existing legacy rows of this source (for upsert)
        Map<String, LegacyCocoonMarketTxn> existing = new HashMap<>();
        List<String> ids = new ArrayList<>(seenIds.keySet());
        for (int i = 0; i < ids.size(); i += CHUNK) {
            for (LegacyCocoonMarketTxn t : repository.findBySourceSystemAndLegacyTxnIdIn(source, ids.subList(i, Math.min(ids.size(), i + CHUNK)))) {
                existing.put(t.getLegacyTxnId(), t);
            }
        }

        // ---- upsert
        int inserted = 0, updated = 0, unchanged = 0, inNewSystem = 0;
        List<LegacyCocoonMarketTxn> toSave = new ArrayList<>();
        for (Object[] g : good) {
            int rowNumber = (Integer) g[0];
            ParsedRow r = (ParsedRow) g[1];
            Long marketId = markets.get(LegacyRowParser.nameKey(r.marketName()));
            if (marketId == null) {
                errors.add(error(runId, rowNumber, r.legacyTxnId(), "Market '" + r.marketName() + "' is not in the market master."));
                continue;
            }
            String hash = LegacyRowParser.hash(r, marketId);
            Long newLotId = newLots.get(lotKey(marketId, r.auctionDate(), lotDigits(r.lotNumber())));
            if (newLotId != null) inNewSystem++;
            LegacyCocoonMarketTxn t = existing.get(r.legacyTxnId());
            if (t != null && hash.equals(t.getRowHash()) && Objects.equals(t.getNewSystemLotId(), newLotId) && Boolean.TRUE.equals(t.getActive())) {
                unchanged++;
                continue;
            }
            if (t == null) {
                t = new LegacyCocoonMarketTxn();
                t.setSourceSystem(source);
                t.setLegacyTxnId(r.legacyTxnId());
                t.setFirstImportId(runId);
                inserted++;
            } else {
                updated++;
            }
            t.setMarketMasterId(marketId);
            t.setMarketNameRaw(r.marketName());
            t.setAuctionDate(r.auctionDate());
            t.setLotNumber(r.lotNumber());
            t.setFarmerFruitsId(r.farmerFruitsId());
            t.setFarmerName(r.farmerName());
            t.setFarmerMobile(r.farmerMobile());
            Long farmerId = r.farmerFruitsId() == null ? null : farmersByFruits.get(r.farmerFruitsId());
            if (farmerId == null && r.farmerMobile() != null) farmerId = farmersByMobile.get(r.farmerMobile());
            t.setFarmerId(farmerId);
            t.setReelerLicenceNo(r.reelerLicenceNo());
            t.setReelerName(r.reelerName());
            t.setReelerId(r.reelerLicenceNo() == null ? null : reelers.get(r.reelerLicenceNo()));
            t.setRaceNameRaw(r.raceName());
            t.setRaceMasterId(r.raceName() == null ? null : races.get(LegacyRowParser.nameKey(r.raceName())));
            t.setWeightKg(r.weightKg());
            t.setRatePerKg(r.ratePerKg());
            t.setAmount(r.amount());
            t.setFarmerMarketFee(r.farmerMarketFee());
            t.setReelerMarketFee(r.reelerMarketFee());
            t.setNewSystemLotId(newLotId);
            t.setRowHash(hash);
            t.setLastImportId(runId);
            t.setActive(true);
            toSave.add(t);
        }
        for (int i = 0; i < toSave.size(); i += 500) {
            repository.saveAll(toSave.subList(i, Math.min(toSave.size(), i + 500)));
        }
        errorRepository.saveAll(errors);

        run.setTotalRows(sheet.rows().size());
        run.setInsertedRows(inserted);
        run.setUpdatedRows(updated);
        run.setUnchangedRows(unchanged);
        run.setRejectedRows(errors.size());
        run.setAlreadyInNewSystemRows(inNewSystem);
        run.setMessage(inserted + " added, " + updated + " updated, " + unchanged + " unchanged, " + errors.size() + " rejected"
                + (inNewSystem > 0 ? "; " + inNewSystem + " also in the new system (shown once)" : "") + ".");
        importRepository.save(run);
        log.info("XC-004 legacy import {} ({}, {}): {}", runId, source, fileName, run.getMessage());
        return importView(run);
    }

    public List<Map<String, Object>> history() {
        return importRepository.findTop50ByActiveOrderByLegacyCocoonMarketImportIdDesc(true).stream().map(LegacyCocoonMarketImportService::importView).toList();
    }

    public List<Map<String, Object>> errors(Long importId) {
        return errorRepository.findTop1000ByLegacyCocoonMarketImportIdOrderByRowNumberInFileAsc(importId).stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("row", e.getRowNumberInFile());
            m.put("legacyTxnId", e.getLegacyTxnId());
            m.put("message", e.getMessage());
            return m;
        }).toList();
    }

    // ------------------------------------------------------------------ helpers

    static String normaliseSource(String s) {
        String v = s == null ? "" : s.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_-]", "_");
        if (v.isEmpty()) throw new ImportException("Enter the legacy system name (source system).");
        return v.substring(0, Math.min(50, v.length()));
    }

    /** "57", "057", "LOT-57" -> "57"; used to match a new-system allotted lot id. */
    static String lotDigits(String lot) {
        String d = lot == null ? "" : lot.replaceAll("\\D", "").replaceFirst("^0+(?=\\d)", "");
        return d.isEmpty() ? "-" : d;
    }

    static String lotKey(long marketId, LocalDate date, String lot) {
        return marketId + "|" + date + "|" + lot;
    }

    private static Map<String, Long> byName(List<Object[]> rows) {
        Map<String, Long> m = new HashMap<>();
        for (Object[] r : rows) {
            if (r[0] != null && r[1] != null) m.putIfAbsent(LegacyRowParser.nameKey(r[1].toString()), ((Number) r[0]).longValue());
        }
        return m;
    }

    /** Resolves a key of the parsed rows to an id in chunks; with {@code uniqueOnly} an ambiguous key resolves to nothing. */
    private static Map<String, Long> lookup(List<Object[]> good, Function<ParsedRow, String> key,
                                            Function<List<String>, List<Object[]>> query, boolean uniqueOnly) {
        List<String> keys = good.stream().map(g -> key.apply((ParsedRow) g[1])).filter(Objects::nonNull).distinct().toList();
        Map<String, Long> out = new HashMap<>();
        Set<String> ambiguous = new HashSet<>();
        for (int i = 0; i < keys.size(); i += CHUNK) {
            for (Object[] r : query.apply(keys.subList(i, Math.min(keys.size(), i + CHUNK)))) {
                if (r[0] == null || r[1] == null) continue;
                String k = r[1].toString().trim().toUpperCase(Locale.ROOT);
                Long id = ((Number) r[0]).longValue();
                Long prev = out.putIfAbsent(k, id);
                if (prev != null && !prev.equals(id)) ambiguous.add(k);
            }
        }
        if (uniqueOnly) ambiguous.forEach(out::remove);
        // keys are compared upper-cased; mobiles are digits so unaffected
        Map<String, Long> byOriginal = new HashMap<>();
        for (String k : keys) {
            Long id = out.get(k.toUpperCase(Locale.ROOT));
            if (id != null) byOriginal.put(k, id);
        }
        return byOriginal;
    }

    private static LegacyCocoonMarketImportError error(Long runId, int row, String txnId, String message) {
        return new LegacyCocoonMarketImportError(null, runId, row,
                txnId == null ? null : txnId.substring(0, Math.min(100, txnId.length())),
                message.substring(0, Math.min(1000, message.length())));
    }

    static LocalDate toDate(Object o) {
        if (o instanceof LocalDate d) return d;
        if (o instanceof java.sql.Date d) return d.toLocalDate();
        if (o instanceof java.sql.Timestamp t) return t.toLocalDateTime().toLocalDate();
        if (o instanceof java.util.Date d) return new java.sql.Date(d.getTime()).toLocalDate();
        return LocalDate.parse(o.toString().substring(0, 10));
    }

    static Map<String, Object> importView(LegacyCocoonMarketImport r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("importId", r.getLegacyCocoonMarketImportId());
        m.put("sourceSystem", r.getSourceSystem());
        m.put("fileName", r.getFileName());
        m.put("status", r.getStatus());
        m.put("totalRows", r.getTotalRows());
        m.put("inserted", r.getInsertedRows());
        m.put("updated", r.getUpdatedRows());
        m.put("unchanged", r.getUnchangedRows());
        m.put("rejected", r.getRejectedRows());
        m.put("alreadyInNewSystem", r.getAlreadyInNewSystemRows());
        m.put("message", r.getMessage());
        return m;
    }
}
