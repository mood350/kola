package com.kola.backend.modules.admin.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Writes tables as CSV or XLSX for the accounting export — no library.
 *
 * <p>An XLSX file is a zip of a few XML parts; writing them by hand costs a hundred lines, where
 * Apache POI would add several megabytes to the jar for one download button. Cells carry real
 * types — numbers stay numbers, instants become Excel dates — so a sum in the spreadsheet works
 * without the accountant converting anything.
 *
 * <p>The CSV targets Excel as French users run it: semicolon separator, decimal comma, CRLF, and a
 * UTF-8 byte-order mark (without it, Excel reads the file as Windows-1252 and mangles every accent).
 */
final class SpreadsheetWriter {

    /** A table: a name (the XLSX tab), a header row, and rows of String / Number / Instant / null. */
    record Sheet(String name, List<String> header, List<List<Object>> rows) {
    }

    private static final LocalDateTime EXCEL_EPOCH = LocalDateTime.of(1899, 12, 30, 0, 0);

    private SpreadsheetWriter() {
    }

    // --- CSV ----------------------------------------------------------------

    static byte[] csv(Sheet sheet) {
        StringBuilder out = new StringBuilder("﻿");
        appendCsvRow(out, List.copyOf(sheet.header()));
        for (List<Object> row : sheet.rows()) {
            appendCsvRow(out, row);
        }
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void appendCsvRow(StringBuilder out, List<?> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                out.append(';');
            }
            out.append(csvCell(cells.get(i)));
        }
        out.append("\r\n");
    }

    private static String csvCell(Object value) {
        String text;
        if (value == null) {
            return "";
        } else if (value instanceof BigDecimal decimal) {
            text = decimal.stripTrailingZeros().toPlainString().replace('.', ',');
        } else if (value instanceof Instant instant) {
            text = LocalDateTime.ofInstant(instant, ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS).toString().replace('T', ' ');
        } else {
            text = String.valueOf(value);
        }
        // A leading =, +, - or @ is read by Excel as a formula: neutralise it (CSV injection) — a
        // counterparty name is customer-typed text.
        if (!text.isEmpty() && "=+-@".indexOf(text.charAt(0)) >= 0 && !(value instanceof Number)) {
            text = "'" + text;
        }
        if (text.contains(";") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            text = "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    // --- XLSX ---------------------------------------------------------------

    static byte[] xlsx(List<Sheet> sheets) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            put(zip, "[Content_Types].xml", contentTypes(sheets.size()));
            put(zip, "_rels/.rels", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">\
                    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>\
                    </Relationships>""");
            put(zip, "xl/workbook.xml", workbook(sheets));
            put(zip, "xl/_rels/workbook.xml.rels", workbookRels(sheets.size()));
            put(zip, "xl/styles.xml", STYLES);
            for (int i = 0; i < sheets.size(); i++) {
                put(zip, "xl/worksheets/sheet" + (i + 1) + ".xml", sheet(sheets.get(i)));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    /** Style 0 = default, 1 = bold header, 2 = date-time, 3 = amount with thousands separator. */
    private static final String STYLES = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">\
            <numFmts count="2"><numFmt numFmtId="164" formatCode="dd/mm/yyyy hh:mm"/><numFmt numFmtId="165" formatCode="#,##0.##"/></numFmts>\
            <fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font></fonts>\
            <fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills>\
            <borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>\
            <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>\
            <cellXfs count="4">\
            <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>\
            <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>\
            <xf numFmtId="164" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>\
            <xf numFmtId="165" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>\
            </cellXfs>            <cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>            </styleSheet>""";

    private static String contentTypes(int sheetCount) {
        StringBuilder xml = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">\
                <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>\
                <Default Extension="xml" ContentType="application/xml"/>\
                <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>\
                <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""");
        for (int i = 1; i <= sheetCount; i++) {
            xml.append("<Override PartName=\"/xl/worksheets/sheet").append(i)
                    .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
        }
        return xml.append("</Types>").toString();
    }

    private static String workbook(List<Sheet> sheets) {
        StringBuilder xml = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" \
                xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets>""");
        for (int i = 0; i < sheets.size(); i++) {
            xml.append("<sheet name=\"").append(escape(sheets.get(i).name())).append("\" sheetId=\"")
                    .append(i + 1).append("\" r:id=\"rId").append(i + 1).append("\"/>");
        }
        return xml.append("</sheets></workbook>").toString();
    }

    private static String workbookRels(int sheetCount) {
        StringBuilder xml = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""");
        for (int i = 1; i <= sheetCount; i++) {
            xml.append("<Relationship Id=\"rId").append(i)
                    .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet")
                    .append(i).append(".xml\"/>");
        }
        xml.append("<Relationship Id=\"rId").append(sheetCount + 1)
                .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>");
        return xml.append("</Relationships>").toString();
    }

    private static String sheet(Sheet sheet) {
        StringBuilder xml = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">\
                <sheetViews><sheetView workbookViewId="0"><pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/></sheetView></sheetViews>\
                <cols>""");
        for (int i = 1; i <= sheet.header().size(); i++) {
            xml.append("<col min=\"").append(i).append("\" max=\"").append(i).append("\" width=\"20\" customWidth=\"1\"/>");
        }
        xml.append("</cols><sheetData>");

        xml.append("<row r=\"1\">");
        for (String title : sheet.header()) {
            xml.append("<c t=\"inlineStr\" s=\"1\"><is><t>").append(escape(title)).append("</t></is></c>");
        }
        xml.append("</row>");

        int r = 2;
        for (List<Object> row : sheet.rows()) {
            xml.append("<row r=\"").append(r++).append("\">");
            for (Object value : row) {
                xml.append(cell(value));
            }
            xml.append("</row>");
        }
        return xml.append("</sheetData></worksheet>").toString();
    }

    private static String cell(Object value) {
        if (value == null) {
            return "<c/>";
        }
        if (value instanceof BigDecimal decimal) {
            return "<c s=\"3\"><v>" + decimal.toPlainString() + "</v></c>";
        }
        if (value instanceof Number number) {
            return "<c><v>" + number + "</v></c>";
        }
        if (value instanceof Instant instant) {
            Duration sinceEpoch = Duration.between(EXCEL_EPOCH, LocalDateTime.ofInstant(instant, ZoneOffset.UTC));
            double serial = sinceEpoch.toSeconds() / 86_400d;
            return "<c s=\"2\"><v>" + serial + "</v></c>";
        }
        return "<c t=\"inlineStr\"><is><t xml:space=\"preserve\">" + escape(String.valueOf(value)) + "</t></is></c>";
    }

    private static String escape(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (char ch : text.toCharArray()) {
            switch (ch) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                default -> {
                    // Control characters are illegal in XML 1.0 and make Excel refuse the file.
                    if (ch >= 0x20 || ch == '\t' || ch == '\n' || ch == '\r') {
                        out.append(ch);
                    }
                }
            }
        }
        return out.toString();
    }

    private static void put(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
