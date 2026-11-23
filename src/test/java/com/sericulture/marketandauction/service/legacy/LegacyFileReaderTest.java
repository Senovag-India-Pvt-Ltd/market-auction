package com.sericulture.marketandauction.service.legacy;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/** XC-004: reading the legacy export as CSV (incl. Excel's UTF-8 BOM) and as .xlsx (date / number cells). */
class LegacyFileReaderTest {

    @Test
    void csvWithBomAndSpacedHeaders_isRead_andBlankLinesSkipped() {
        String csv = "﻿Legacy Txn ID, Market Name ,auction_date,lot_number,weight_kg,rate_per_kg\r\n"
                + "OLD-1,Ramanagara,15-05-2023,57,45.5,520\r\n"
                + ",,,,,\r\n"
                + "OLD-2,\"Kollegal, Main\",16-05-2023,8,10,500\r\n";

        LegacyFileReader.Sheet s = LegacyFileReader.read("export.csv", csv.getBytes(StandardCharsets.UTF_8), 100);

        assertTrue(s.headerKeys().contains("legacy_txn_id"));
        assertEquals(2, s.rows().size());
        assertEquals("Ramanagara", s.rows().get(0).values().get("market_name"));
        assertEquals("Kollegal, Main", s.rows().get(1).values().get("market_name"));
        assertEquals(2, s.rows().get(0).number(), "header is row 1");
    }

    @Test
    void xlsx_datesAndNumbers_areReadAsText() throws Exception {
        byte[] bytes;
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            var sheet = wb.createSheet("Export");
            Row h = sheet.createRow(0);
            String[] cols = {"legacy_txn_id", "market_name", "auction_date", "lot_number", "weight_kg", "rate_per_kg"};
            for (int i = 0; i < cols.length; i++) h.createCell(i).setCellValue(cols[i]);
            CreationHelper ch = wb.getCreationHelper();
            CellStyle dateStyle = wb.createCellStyle();
            dateStyle.setDataFormat(ch.createDataFormat().getFormat("dd-mm-yyyy"));
            Row r = sheet.createRow(1);
            r.createCell(0).setCellValue("OLD-9");
            r.createCell(1).setCellValue("Ramanagara");
            var dc = r.createCell(2);
            dc.setCellValue(Date.from(LocalDate.of(2023, 5, 15).atStartOfDay(ZoneId.systemDefault()).toInstant()));
            dc.setCellStyle(dateStyle);
            r.createCell(3).setCellValue(57);
            r.createCell(4).setCellValue(45.5);
            r.createCell(5).setCellValue(520);
            wb.write(out);
            bytes = out.toByteArray();
        }

        LegacyFileReader.Sheet s = LegacyFileReader.read("export.xlsx", bytes, 100);

        assertEquals(1, s.rows().size());
        assertEquals("2023-05-15", s.rows().get(0).values().get("auction_date"));
        assertEquals("57", s.rows().get(0).values().get("lot_number"));
        assertEquals("45.5", s.rows().get(0).values().get("weight_kg"));
    }

    @Test
    void tooManyRows_orEmptyFile_isRefused() {
        String csv = "legacy_txn_id,market_name\nA,M\nB,M\nC,M\n";
        assertThrows(LegacyFileReader.FileFormatException.class,
                () -> LegacyFileReader.read("x.csv", csv.getBytes(StandardCharsets.UTF_8), 2));
        assertThrows(LegacyFileReader.FileFormatException.class, () -> LegacyFileReader.read("x.csv", new byte[0], 10));
    }

    @Test
    void template_hasEveryColumn() {
        String t = new String(LegacyFileReader.templateCsv(), StandardCharsets.UTF_8);
        for (String c : LegacyRowParser.COLUMNS) assertTrue(t.contains(c), c);
        LegacyFileReader.Sheet s = LegacyFileReader.read("t.csv", LegacyFileReader.templateCsv(), 10);
        assertTrue(LegacyRowParser.missingColumns(s.headerKeys()).isEmpty());
        assertTrue(LegacyRowParser.parse(s.rows().get(0).values(), LocalDate.of(2026, 1, 1)).ok(), "the example row is valid");
    }
}
