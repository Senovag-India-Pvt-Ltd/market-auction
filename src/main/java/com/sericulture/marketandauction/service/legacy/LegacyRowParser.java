package com.sericulture.marketandauction.service.legacy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * XC-004: validates and normalises one row of the legacy cocoon market export (the fixed import template) and
 * computes the hash that tells whether a re-imported row changed. Pure logic, unit tested.
 */
public final class LegacyRowParser {

    /** Columns of the import template, in order. Headers are matched ignoring case, spaces and punctuation. */
    public static final List<String> COLUMNS = List.of(
            "legacy_txn_id", "market_name", "auction_date", "lot_number",
            "farmer_fruits_id", "farmer_name", "farmer_mobile",
            "reeler_licence_no", "reeler_name", "race_name",
            "weight_kg", "rate_per_kg", "amount", "farmer_market_fee", "reeler_market_fee");
    public static final List<String> REQUIRED = List.of(
            "legacy_txn_id", "market_name", "auction_date", "lot_number", "weight_kg", "rate_per_kg");

    // STRICT: an impossible date such as 31-02-2023 is rejected instead of being silently moved to 28-02-2023.
    private static final List<DateTimeFormatter> DATE_FORMATS = java.util.stream.Stream.of(
                    "uuuu-MM-dd", "dd-MM-uuuu", "dd/MM/uuuu", "d/M/uuuu", "d-M-uuuu", "dd.MM.uuuu")
            .map(p -> DateTimeFormatter.ofPattern(p).withResolverStyle(java.time.format.ResolverStyle.STRICT))
            .toList();

    private LegacyRowParser() {
    }

    public record ParsedRow(String legacyTxnId, String marketName, LocalDate auctionDate, String lotNumber,
                            String farmerFruitsId, String farmerName, String farmerMobile,
                            String reelerLicenceNo, String reelerName, String raceName,
                            BigDecimal weightKg, BigDecimal ratePerKg, BigDecimal amount,
                            BigDecimal farmerMarketFee, BigDecimal reelerMarketFee) {
    }

    /** Either a parsed row or the reasons it was rejected. */
    public record Outcome(ParsedRow row, List<String> errors) {
        public boolean ok() {
            return errors.isEmpty();
        }
    }

    /** "Legacy Txn ID", "legacy-txn-id", " LEGACY_TXN_ID " -> "legacy_txn_id". */
    public static String headerKey(String header) {
        if (header == null) return "";
        return header.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    /** Required template columns missing from the file's header row (empty when all are there). */
    public static List<String> missingColumns(Collection<String> headerKeys) {
        List<String> missing = new ArrayList<>();
        for (String c : REQUIRED) if (headerKeys == null || !headerKeys.contains(c)) missing.add(c);
        return missing;
    }

    public static Outcome parse(Map<String, String> raw, LocalDate today) {
        List<String> errors = new ArrayList<>();
        String txnId = text(raw, "legacy_txn_id", 100, errors, true);
        String market = text(raw, "market_name", 200, errors, true);
        String lot = text(raw, "lot_number", 50, errors, true);
        String fruits = text(raw, "farmer_fruits_id", 50, errors, false);
        String farmerName = text(raw, "farmer_name", 200, errors, false);
        String mobile = text(raw, "farmer_mobile", 15, errors, false);
        String licence = text(raw, "reeler_licence_no", 100, errors, false);
        String reelerName = text(raw, "reeler_name", 200, errors, false);
        String race = text(raw, "race_name", 100, errors, false);

        LocalDate date = null;
        String dateText = value(raw, "auction_date");
        if (dateText == null) {
            errors.add("auction_date is required.");
        } else {
            date = parseDate(dateText);
            if (date == null) errors.add("auction_date '" + dateText + "' is not a valid date (use dd-MM-yyyy or yyyy-MM-dd).");
            else if (today != null && date.isAfter(today)) errors.add("auction_date " + dateText + " is in the future.");
        }

        if (mobile != null) {
            mobile = mobile.replaceAll("[\\s-]", "").replaceFirst("^(\\+91|91)(?=\\d{10}$)", "");
            if (!mobile.matches("[6-9]\\d{9}")) errors.add("farmer_mobile '" + value(raw, "farmer_mobile") + "' is not a valid 10-digit mobile number.");
        }
        if (fruits == null && mobile == null) {
            errors.add("farmer_fruits_id or farmer_mobile is required to identify the farmer.");
        }
        if (fruits != null) fruits = fruits.toUpperCase(Locale.ROOT);
        if (licence != null) licence = licence.toUpperCase(Locale.ROOT);

        BigDecimal weight = number(raw, "weight_kg", errors, true, true);
        BigDecimal rate = number(raw, "rate_per_kg", errors, true, true);
        BigDecimal amount = number(raw, "amount", errors, false, false);
        BigDecimal farmerFee = number(raw, "farmer_market_fee", errors, false, false);
        BigDecimal reelerFee = number(raw, "reeler_market_fee", errors, false, false);

        if (weight != null && rate != null) {
            BigDecimal computed = weight.multiply(rate).setScale(2, RoundingMode.HALF_UP);
            if (amount == null) {
                amount = computed;
            } else {
                BigDecimal diff = amount.subtract(computed).abs();
                BigDecimal allowed = computed.multiply(new BigDecimal("0.01")).max(BigDecimal.ONE);
                if (diff.compareTo(allowed) > 0) {
                    errors.add("amount " + amount.toPlainString() + " does not match weight x rate (" + computed.toPlainString() + ").");
                }
            }
        }
        if (!errors.isEmpty()) return new Outcome(null, errors);
        return new Outcome(new ParsedRow(txnId, market, date, lot, fruits, farmerName, mobile, licence, reelerName, race,
                scale(weight, 3), scale(rate, 2), scale(amount, 2), scale(farmerFee, 2), scale(reelerFee, 2)), errors);
    }

    /** SHA-256 over every business field, so an export row that changed in the legacy system is detected. */
    public static String hash(ParsedRow r, long marketId) {
        String joined = String.join("|", Arrays.asList(
                String.valueOf(marketId), String.valueOf(r.auctionDate()), r.lotNumber(), n(r.farmerFruitsId()),
                n(r.farmerName()), n(r.farmerMobile()), n(r.reelerLicenceNo()), n(r.reelerName()), n(r.raceName()),
                plain(r.weightKg()), plain(r.ratePerKg()), plain(r.amount()), plain(r.farmerMarketFee()), plain(r.reelerMarketFee())));
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(joined.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Market / race names compared ignoring case, spaces and punctuation. */
    public static String nameKey(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    static LocalDate parseDate(String s) {
        String t = s.trim();
        if (t.length() > 10 && t.charAt(10) == ' ') t = t.substring(0, 10); // "2023-05-01 00:00:00"
        for (DateTimeFormatter f : DATE_FORMATS) {
            try {
                return LocalDate.parse(t, f);
            } catch (DateTimeParseException ignored) {
                // try the next format
            }
        }
        return null;
    }

    private static String value(Map<String, String> raw, String key) {
        String v = raw == null ? null : raw.get(key);
        return v == null || v.trim().isEmpty() ? null : v.trim();
    }

    private static String text(Map<String, String> raw, String key, int max, List<String> errors, boolean required) {
        String v = value(raw, key);
        if (v == null) {
            if (required) errors.add(key + " is required.");
            return null;
        }
        if (v.length() > max) {
            errors.add(key + " is longer than " + max + " characters.");
            return null;
        }
        return v;
    }

    private static BigDecimal number(Map<String, String> raw, String key, List<String> errors, boolean required, boolean positive) {
        String v = value(raw, key);
        if (v == null) {
            if (required) errors.add(key + " is required.");
            return null;
        }
        try {
            BigDecimal b = new BigDecimal(v.replace(",", ""));
            if (positive ? b.signum() <= 0 : b.signum() < 0) {
                errors.add(key + " must be " + (positive ? "greater than 0" : "0 or more") + ".");
                return null;
            }
            return b;
        } catch (NumberFormatException e) {
            errors.add(key + " '" + v + "' is not a number.");
            return null;
        }
    }

    private static BigDecimal scale(BigDecimal b, int s) {
        return b == null ? null : b.setScale(s, RoundingMode.HALF_UP);
    }

    private static String plain(BigDecimal b) {
        return b == null ? "" : b.stripTrailingZeros().toPlainString();
    }

    private static String n(String s) {
        return s == null ? "" : s;
    }
}
