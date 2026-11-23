package com.sericulture.marketandauction.service.legacy;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * XC-004: reads the legacy export (.csv or .xlsx, first sheet) into rows keyed by normalised header
 * ({@link LegacyRowParser#headerKey}). Excel dates become yyyy-MM-dd; numbers keep their plain value.
 */
public final class LegacyFileReader {

    public static class FileFormatException extends RuntimeException {
        public FileFormatException(String message) {
            super(message);
        }
    }

    /** One data row with its 1-based line / row number in the file (header = row 1). */
    public record Row(int number, Map<String, String> values) {
    }

    public record Sheet(List<String> headerKeys, List<Row> rows) {
    }

    private LegacyFileReader() {
    }

    public static Sheet read(String fileName, byte[] bytes, int maxRows) {
        if (bytes == null || bytes.length == 0) throw new FileFormatException("The file is empty.");
        String name = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        boolean xlsx = name.endsWith(".xlsx") || name.endsWith(".xls")
                || (bytes.length > 3 && bytes[0] == 'P' && bytes[1] == 'K'); // zip = xlsx
        return xlsx ? readExcel(bytes, maxRows) : readCsv(bytes, maxRows);
    }

    static Sheet readCsv(byte[] bytes, int maxRows) {
        String text = new String(bytes, StandardCharsets.UTF_8);
        if (text.startsWith("﻿")) text = text.substring(1); // Excel "CSV UTF-8" byte order mark
        try (Reader reader = new java.io.StringReader(text);
             CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreEmptyLines().withTrim().parse(reader)) {
            List<String> rawHeaders = parser.getHeaderNames();
            List<String> keys = rawHeaders.stream().map(LegacyRowParser::headerKey).toList();
            List<Row> rows = new ArrayList<>();
            for (CSVRecord rec : parser) {
                if (rows.size() >= maxRows) throw new FileFormatException("The file has more than " + maxRows + " rows. Split it into smaller files.");
                Map<String, String> values = new LinkedHashMap<>();
                for (int i = 0; i < rawHeaders.size() && i < rec.size(); i++) values.put(keys.get(i), rec.get(i));
                if (values.values().stream().allMatch(v -> v == null || v.isBlank())) continue;
                rows.add(new Row((int) rec.getRecordNumber() + 1, values));
            }
            return new Sheet(keys, rows);
        } catch (IOException | IllegalArgumentException | IllegalStateException e) {
            throw new FileFormatException("The CSV file could not be read: " + e.getMessage());
        }
    }

    static Sheet readExcel(byte[] bytes, int maxRows) {
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            if (wb.getNumberOfSheets() == 0) throw new FileFormatException("The Excel file has no sheet.");
            org.apache.poi.ss.usermodel.Sheet sheet = wb.getSheetAt(0);
            DataFormatter fmt = new DataFormatter(Locale.ENGLISH);
            FormulaEvaluator eval = wb.getCreationHelper().createFormulaEvaluator();
            org.apache.poi.ss.usermodel.Row header = sheet.getRow(sheet.getFirstRowNum());
            if (header == null) throw new FileFormatException("The Excel sheet has no header row.");
            List<String> keys = new ArrayList<>();
            for (int c = 0; c < header.getLastCellNum(); c++) keys.add(LegacyRowParser.headerKey(cellText(header.getCell(c), fmt, eval)));
            List<Row> rows = new ArrayList<>();
            for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                org.apache.poi.ss.usermodel.Row row = sheet.getRow(r);
                if (row == null) continue;
                Map<String, String> values = new LinkedHashMap<>();
                for (int c = 0; c < keys.size(); c++) values.put(keys.get(c), cellText(row.getCell(c), fmt, eval));
                if (values.values().stream().allMatch(v -> v == null || v.isBlank())) continue;
                if (rows.size() >= maxRows) throw new FileFormatException("The file has more than " + maxRows + " rows. Split it into smaller files.");
                rows.add(new Row(r + 1, values));
            }
            return new Sheet(keys, rows);
        } catch (IOException | RuntimeException e) {
            if (e instanceof FileFormatException f) throw f;
            throw new FileFormatException("The Excel file could not be read: " + e.getMessage());
        }
    }

    private static String cellText(Cell cell, DataFormatter fmt, FormulaEvaluator eval) {
        if (cell == null) return null;
        CellType type = cell.getCellType() == CellType.FORMULA ? eval.evaluateFormulaCell(cell) : cell.getCellType();
        if (type == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                LocalDate d = cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                return d.toString();
            }
            double v = cell.getNumericCellValue();
            return new java.math.BigDecimal(Double.toString(v)).stripTrailingZeros().toPlainString();
        }
        String s = fmt.formatCellValue(cell, eval);
        return s == null ? null : s.trim();
    }

    /** The import template as CSV (header + one example row). */
    public static byte[] templateCsv() {
        String header = String.join(",", LegacyRowParser.COLUMNS);
        String example = "OLD-2023-000123,Ramanagara,15-05-2023,57,FID2901000023662,BASAMMA,8105937769,RL/RMN/0012,Sri Silk Reeling,Cross Breed,45.5,520,23660,227,227";
        return ("﻿" + header + "\r\n" + example + "\r\n").getBytes(StandardCharsets.UTF_8);
    }
}
