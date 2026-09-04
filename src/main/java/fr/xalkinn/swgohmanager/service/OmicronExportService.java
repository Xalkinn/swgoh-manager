package fr.xalkinn.swgohmanager.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import fr.xalkinn.swgohmanager.dto.OmicronGestionView.PrioriteRow;

/**
 * Génère les exports du plan Omicron de la guilde.
 *
 * Formats proposés :
 * - Excel (.xlsx) : OpenXML produit avec les API Java standard ;
 * - LibreOffice (.ods) : OpenDocument produit avec les API Java standard ;
 * - CSV (.csv) : UTF-8 avec séparateur point-virgule.
 *
 * Aucun de ces exports ne dépend d'Apache POI.
 */
@Service
public class OmicronExportService {

    private static final String CONTENT_TYPE_XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String CONTENT_TYPE_ODS =
            "application/vnd.oasis.opendocument.spreadsheet";
    private static final String CONTENT_TYPE_CSV =
            "text/csv; charset=UTF-8";

    private final JdbcTemplate jdbc;
    private final OmicronPrioriteService prioriteService;

    public OmicronExportService(JdbcTemplate jdbc, OmicronPrioriteService prioriteService) {
        this.jdbc = jdbc;
        this.prioriteService = prioriteService;
    }

    public ExportResult genererExport(String format) throws IOException {
        ExportFormat exportFormat = ExportFormat.from(format);

        List<PrioriteRow> priorites = prioriteService.getPriorites();
        if (priorites.isEmpty()) {
            throw new IllegalStateException("Aucune priorité Omicron n'est configurée.");
        }

        List<JoueurExport> joueurs = chargerJoueursActifs();
        Map<String, Set<String>> skillsParJoueur = chargerPossessions();
        Map<String, Integer> occurrencesPersonnage = compterOccurrencesPersonnage(priorites);

        String baseFilename = "omicrons-guilde-" + LocalDate.now();

        return switch (exportFormat) {
            case EXCEL -> new ExportResult(
                    baseFilename + ".xlsx",
                    CONTENT_TYPE_XLSX,
                    genererXlsx(priorites, joueurs, skillsParJoueur, occurrencesPersonnage));
            case LIBRE_OFFICE -> new ExportResult(
                    baseFilename + ".ods",
                    CONTENT_TYPE_ODS,
                    genererOds(priorites, joueurs, skillsParJoueur, occurrencesPersonnage));
            case CSV -> new ExportResult(
                    baseFilename + ".csv",
                    CONTENT_TYPE_CSV,
                    genererCsv(priorites, joueurs, skillsParJoueur, occurrencesPersonnage));
        };
    }

    /** Compatibilité avec l'ancien endpoint : Excel reste le format par défaut. */
    public ExportResult genererExport() throws IOException {
        return genererExport("excel");
    }

    // ---------------------------------------------------------------------
    // Excel / XLSX
    // ---------------------------------------------------------------------

    private byte[] genererXlsx(
            List<PrioriteRow> priorites,
            List<JoueurExport> joueurs,
            Map<String, Set<String>> skillsParJoueur,
            Map<String, Integer> occurrencesPersonnage) throws IOException {

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {

            writeEntry(zip, "[Content_Types].xml", contentTypesXml());
            writeEntry(zip, "_rels/.rels", rootRelationshipsXml());
            writeEntry(zip, "xl/workbook.xml", workbookXml());
            writeEntry(zip, "xl/_rels/workbook.xml.rels", workbookRelationshipsXml());
            writeEntry(zip, "xl/styles.xml", stylesXml());

            for (int priorite = 1; priorite <= 4; priorite++) {
                List<PrioriteRow> prioritesOnglet = filtrerPriorites(priorites, priorite);
                Map<String, Integer> occurrencesOnglet = compterOccurrencesPersonnage(prioritesOnglet);
                int lastColumn = prioritesOnglet.size() + 1;
                int lastRow = joueurs.size() + 2;
                String dimension = "A1:" + cellRef(lastColumn, lastRow);

                writeEntry(zip, "xl/worksheets/sheet" + priorite + ".xml",
                        sheetXml(prioritesOnglet, joueurs, skillsParJoueur,
                                occurrencesOnglet, dimension, lastColumn));
            }

            zip.finish();
            return out.toByteArray();
        }
    }

    private String sheetXml(
            List<PrioriteRow> priorites,
            List<JoueurExport> joueurs,
            Map<String, Set<String>> skillsParJoueur,
            Map<String, Integer> occurrencesPersonnage,
            String dimension,
            int lastColumn) {

        StringBuilder xml = new StringBuilder(32_768);
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
           .append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
           .append("<sheetPr><pageSetUpPr fitToPage=\"1\"/></sheetPr>")
           .append("<dimension ref=\"").append(dimension).append("\"/>")
           .append("<sheetViews><sheetView workbookViewId=\"0\">")
           .append("<pane xSplit=\"1\" ySplit=\"1\" topLeftCell=\"B2\" activePane=\"bottomRight\" state=\"frozen\"/>")
           .append("</sheetView></sheetViews>")
           .append("<sheetFormatPr defaultRowHeight=\"15\"/>")
           .append("<cols>")
           .append("<col min=\"1\" max=\"1\" width=\"28\" customWidth=\"1\"/>");

        if (!priorites.isEmpty()) {
            xml.append("<col min=\"2\" max=\"")
               .append(priorites.size() + 1)
               .append("\" width=\"23\" customWidth=\"1\"/>");
        }
        int totalColumnOneBased = priorites.size() + 2;
        xml.append("<col min=\"").append(totalColumnOneBased)
           .append("\" max=\"").append(totalColumnOneBased)
           .append("\" width=\"18\" customWidth=\"1\"/>")
           .append("</cols><sheetData>");

        xml.append("<row r=\"1\" ht=\"34\" customHeight=\"1\">");
        appendTextCell(xml, 0, 1, 1, "Joueur");
        for (int i = 0; i < priorites.size(); i++) {
            appendTextCell(xml, i + 1, 1, 1,
                    columnLabel(priorites.get(i), occurrencesPersonnage));
        }
        appendTextCell(xml, lastColumn, 1, 1, "Total Omicrons");
        xml.append("</row>");

        int rowNumber = 2;
        for (JoueurExport joueur : joueurs) {
            xml.append("<row r=\"").append(rowNumber)
               .append("\" ht=\"21\" customHeight=\"1\">");
            appendTextCell(xml, 0, rowNumber, 2, joueur.nom());

            Set<String> skills = skillsParJoueur.getOrDefault(joueur.playerId(), Set.of());
            int totalPossede = 0;
            for (int col = 0; col < priorites.size(); col++) {
                PrioriteRow o = priorites.get(col);
                boolean possede = o.getSkillId() != null && skills.contains(o.getSkillId());
                if (possede) totalPossede++;
                appendTextCell(xml, col + 1, rowNumber, possede ? 3 : 4, possede ? "OUI" : "NON");
            }
            appendTextCell(xml, lastColumn, rowNumber, 6,
                    totalPossede + "/" + priorites.size());
            xml.append("</row>");
            rowNumber++;
        }

        xml.append("<row r=\"").append(rowNumber)
           .append("\" ht=\"23\" customHeight=\"1\">");
        appendTextCell(xml, 0, rowNumber, 5, "TOTAL OUI");
        for (int col = 0; col < priorites.size(); col++) {
            int total = totalPossesseurs(priorites.get(col), joueurs, skillsParJoueur);
            appendNumberCell(xml, col + 1, rowNumber, 6, total);
        }
        appendTextCell(xml, lastColumn, rowNumber, 6, "—");
        xml.append("</row></sheetData>");

        String autoFilterEnd = cellRef(lastColumn, Math.max(1, joueurs.size() + 1));
        xml.append("<autoFilter ref=\"A1:").append(autoFilterEnd).append("\"/>")
           .append("<pageMargins left=\"0.3\" right=\"0.3\" top=\"0.5\" bottom=\"0.5\" header=\"0.2\" footer=\"0.2\"/>")
           .append("<pageSetup orientation=\"landscape\" fitToWidth=\"1\" fitToHeight=\"0\"/>")
           .append("</worksheet>");

        return xml.toString();
    }

    private void appendTextCell(StringBuilder xml, int zeroBasedColumn, int row, int style, String value) {
        xml.append("<c r=\"").append(cellRef(zeroBasedColumn, row))
           .append("\" s=\"").append(style).append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
           .append(escapeXml(value))
           .append("</t></is></c>");
    }

    private void appendNumberCell(StringBuilder xml, int zeroBasedColumn, int row, int style, int value) {
        xml.append("<c r=\"").append(cellRef(zeroBasedColumn, row))
           .append("\" s=\"").append(style).append("\" t=\"n\"><v>")
           .append(value)
           .append("</v></c>");
    }

    private String cellRef(int zeroBasedColumn, int row) {
        return columnName(zeroBasedColumn) + row;
    }

    private String columnName(int zeroBasedColumn) {
        int value = zeroBasedColumn + 1;
        StringBuilder name = new StringBuilder();
        while (value > 0) {
            int remainder = (value - 1) % 26;
            name.append((char) ('A' + remainder));
            value = (value - 1) / 26;
        }
        return name.reverse().toString();
    }

    private String contentTypesXml() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
           .append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
           .append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>")
           .append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
           .append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>");
        for (int i = 1; i <= 4; i++) {
            xml.append("<Override PartName=\"/xl/worksheets/sheet").append(i)
               .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
        }
        xml.append("<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>")
           .append("</Types>");
        return xml.toString();
    }

    private String rootRelationshipsXml() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                </Relationships>
                """;
    }

    private String workbookXml() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                          xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                  <bookViews><workbookView/></bookViews>
                  <sheets>
                    <sheet name="P1" sheetId="1" r:id="rId1"/>
                    <sheet name="P2" sheetId="2" r:id="rId2"/>
                    <sheet name="P3" sheetId="3" r:id="rId3"/>
                    <sheet name="P4" sheetId="4" r:id="rId4"/>
                  </sheets>
                  <calcPr fullCalcOnLoad="1" forceFullCalc="1"/>
                </workbook>
                """;
    }

    private String workbookRelationshipsXml() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
                  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/>
                  <Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet4.xml"/>
                  <Relationship Id="rId5" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
                </Relationships>
                """;
    }

    private String stylesXml() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <fonts count="6">
                    <font><sz val="11"/><name val="Calibri"/><family val="2"/></font>
                    <font><b/><color rgb="FFFFFFFF"/><sz val="11"/><name val="Calibri"/></font>
                    <font><b/><color rgb="FF1F2937"/><sz val="11"/><name val="Calibri"/></font>
                    <font><b/><color rgb="FF375623"/><sz val="11"/><name val="Calibri"/></font>
                    <font><b/><color rgb="FF9C0006"/><sz val="11"/><name val="Calibri"/></font>
                    <font><b/><color rgb="FF4C1D95"/><sz val="11"/><name val="Calibri"/></font>
                  </fonts>
                  <fills count="6">
                    <fill><patternFill patternType="none"/></fill>
                    <fill><patternFill patternType="gray125"/></fill>
                    <fill><patternFill patternType="solid"><fgColor rgb="FF6D28D9"/><bgColor indexed="64"/></patternFill></fill>
                    <fill><patternFill patternType="solid"><fgColor rgb="FFE2F0D9"/><bgColor indexed="64"/></patternFill></fill>
                    <fill><patternFill patternType="solid"><fgColor rgb="FFFCE4D6"/><bgColor indexed="64"/></patternFill></fill>
                    <fill><patternFill patternType="solid"><fgColor rgb="FFEDE9FE"/><bgColor indexed="64"/></patternFill></fill>
                  </fills>
                  <borders count="2">
                    <border><left/><right/><top/><bottom/><diagonal/></border>
                    <border>
                      <left style="thin"><color rgb="FFD1D5DB"/></left>
                      <right style="thin"><color rgb="FFD1D5DB"/></right>
                      <top style="thin"><color rgb="FFD1D5DB"/></top>
                      <bottom style="thin"><color rgb="FFD1D5DB"/></bottom>
                      <diagonal/>
                    </border>
                  </borders>
                  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
                  <cellXfs count="7">
                    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
                    <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
                    <xf numFmtId="0" fontId="2" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1" applyAlignment="1"><alignment vertical="center"/></xf>
                    <xf numFmtId="0" fontId="3" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
                    <xf numFmtId="0" fontId="4" fillId="4" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
                    <xf numFmtId="0" fontId="5" fillId="5" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment vertical="center"/></xf>
                    <xf numFmtId="0" fontId="5" fillId="5" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
                  </cellXfs>
                  <cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
                  <dxfs count="0"/>
                  <tableStyles count="0" defaultTableStyle="TableStyleMedium2" defaultPivotStyle="PivotStyleLight16"/>
                </styleSheet>
                """;
    }

    // ---------------------------------------------------------------------
    // LibreOffice / ODS
    // ---------------------------------------------------------------------

    private byte[] genererOds(
            List<PrioriteRow> priorites,
            List<JoueurExport> joueurs,
            Map<String, Set<String>> skillsParJoueur,
            Map<String, Integer> occurrencesPersonnage) throws IOException {

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {

            writeStoredEntry(zip, "mimetype", CONTENT_TYPE_ODS);
            writeEntry(zip, "content.xml",
                    odsContentXml(priorites, joueurs, skillsParJoueur));
            writeEntry(zip, "styles.xml", odsStylesXml());
            writeEntry(zip, "META-INF/manifest.xml", odsManifestXml());

            zip.finish();
            return out.toByteArray();
        }
    }

    private String odsContentXml(
            List<PrioriteRow> priorites,
            List<JoueurExport> joueurs,
            Map<String, Set<String>> skillsParJoueur) {

        StringBuilder xml = new StringBuilder(64_000);
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
           .append("<office:document-content ")
           .append("xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\" ")
           .append("xmlns:style=\"urn:oasis:names:tc:opendocument:xmlns:style:1.0\" ")
           .append("xmlns:text=\"urn:oasis:names:tc:opendocument:xmlns:text:1.0\" ")
           .append("xmlns:table=\"urn:oasis:names:tc:opendocument:xmlns:table:1.0\" ")
           .append("xmlns:fo=\"urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0\" ")
           .append("office:version=\"1.3\">")
           .append("<office:automatic-styles>")
           .append(odsAutomaticStyles())
           .append("</office:automatic-styles>")
           .append("<office:body><office:spreadsheet>");

        for (int priorite = 1; priorite <= 4; priorite++) {
            List<PrioriteRow> prioritesOnglet = filtrerPriorites(priorites, priorite);
            Map<String, Integer> occurrencesOnglet = compterOccurrencesPersonnage(prioritesOnglet);
            appendOdsTable(xml, "P" + priorite, prioritesOnglet, joueurs,
                    skillsParJoueur, occurrencesOnglet);
        }

        xml.append("</office:spreadsheet></office:body></office:document-content>");
        return xml.toString();
    }

    private void appendOdsTable(
            StringBuilder xml,
            String nomOnglet,
            List<PrioriteRow> priorites,
            List<JoueurExport> joueurs,
            Map<String, Set<String>> skillsParJoueur,
            Map<String, Integer> occurrencesPersonnage) {

        xml.append("<table:table table:name=\"").append(nomOnglet).append("\">")
           .append("<table:table-column table:style-name=\"co-player\"/>");

        for (int i = 0; i < priorites.size(); i++) {
            xml.append("<table:table-column table:style-name=\"co-omicron\"/>");
        }
        xml.append("<table:table-column table:style-name=\"co-total\"/>");

        xml.append("<table:table-row table:style-name=\"ro-header\">");
        appendOdsTextCell(xml, "ce-header", "Joueur");
        for (PrioriteRow o : priorites) {
            appendOdsTextCell(xml, "ce-header", columnLabel(o, occurrencesPersonnage));
        }
        appendOdsTextCell(xml, "ce-header", "Total Omicrons");
        xml.append("</table:table-row>");

        for (JoueurExport joueur : joueurs) {
            Set<String> skills = skillsParJoueur.getOrDefault(joueur.playerId(), Set.of());
            int totalPossede = 0;

            xml.append("<table:table-row>");
            appendOdsTextCell(xml, "ce-player", joueur.nom());
            for (PrioriteRow o : priorites) {
                boolean possede = o.getSkillId() != null && skills.contains(o.getSkillId());
                if (possede) totalPossede++;
                appendOdsTextCell(xml, possede ? "ce-yes" : "ce-no", possede ? "OUI" : "NON");
            }
            appendOdsTextCell(xml, "ce-score", totalPossede + "/" + priorites.size());
            xml.append("</table:table-row>");
        }

        xml.append("<table:table-row>");
        appendOdsTextCell(xml, "ce-total", "TOTAL OUI");
        for (PrioriteRow o : priorites) {
            appendOdsNumberCell(xml, "ce-total-number", totalPossesseurs(o, joueurs, skillsParJoueur));
        }
        appendOdsTextCell(xml, "ce-total-number", "—");
        xml.append("</table:table-row></table:table>");
    }

    private String odsAutomaticStyles() {
        return """
                <style:style style:name="co-player" style:family="table-column"><style:table-column-properties style:column-width="6.8cm"/></style:style>
                <style:style style:name="co-omicron" style:family="table-column"><style:table-column-properties style:column-width="5.2cm"/></style:style>
                <style:style style:name="co-total" style:family="table-column"><style:table-column-properties style:column-width="3.8cm"/></style:style>
                <style:style style:name="ro-header" style:family="table-row"><style:table-row-properties style:row-height="0.9cm" style:use-optimal-row-height="true"/></style:style>
                <style:style style:name="ce-header" style:family="table-cell">
                  <style:table-cell-properties fo:background-color="#6D28D9" fo:border="0.02cm solid #D1D5DB" fo:padding="0.08cm"/>
                  <style:text-properties fo:color="#FFFFFF" fo:font-weight="bold"/>
                  <style:paragraph-properties fo:text-align="center"/>
                </style:style>
                <style:style style:name="ce-player" style:family="table-cell">
                  <style:table-cell-properties fo:border="0.02cm solid #D1D5DB" fo:padding="0.08cm"/>
                  <style:text-properties fo:color="#1F2937" fo:font-weight="bold"/>
                </style:style>
                <style:style style:name="ce-yes" style:family="table-cell">
                  <style:table-cell-properties fo:background-color="#E2F0D9" fo:border="0.02cm solid #D1D5DB" fo:padding="0.08cm"/>
                  <style:text-properties fo:color="#375623" fo:font-weight="bold"/>
                  <style:paragraph-properties fo:text-align="center"/>
                </style:style>
                <style:style style:name="ce-no" style:family="table-cell">
                  <style:table-cell-properties fo:background-color="#FCE4D6" fo:border="0.02cm solid #D1D5DB" fo:padding="0.08cm"/>
                  <style:text-properties fo:color="#9C0006" fo:font-weight="bold"/>
                  <style:paragraph-properties fo:text-align="center"/>
                </style:style>
                <style:style style:name="ce-score" style:family="table-cell">
                  <style:table-cell-properties fo:background-color="#EDE9FE" fo:border="0.02cm solid #D1D5DB" fo:padding="0.08cm"/>
                  <style:text-properties fo:color="#4C1D95" fo:font-weight="bold"/>
                  <style:paragraph-properties fo:text-align="center"/>
                </style:style>
                <style:style style:name="ce-total" style:family="table-cell">
                  <style:table-cell-properties fo:background-color="#EDE9FE" fo:border="0.02cm solid #D1D5DB" fo:padding="0.08cm"/>
                  <style:text-properties fo:color="#4C1D95" fo:font-weight="bold"/>
                </style:style>
                <style:style style:name="ce-total-number" style:family="table-cell">
                  <style:table-cell-properties fo:background-color="#EDE9FE" fo:border="0.02cm solid #D1D5DB" fo:padding="0.08cm"/>
                  <style:text-properties fo:color="#4C1D95" fo:font-weight="bold"/>
                  <style:paragraph-properties fo:text-align="center"/>
                </style:style>
                """;
    }

    private String odsStylesXml() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <office:document-styles
                  xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
                  xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0"
                  office:version="1.3">
                  <office:styles/>
                </office:document-styles>
                """;
    }

    private String odsManifestXml() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <manifest:manifest
                  xmlns:manifest="urn:oasis:names:tc:opendocument:xmlns:manifest:1.0"
                  manifest:version="1.3">
                  <manifest:file-entry manifest:full-path="/" manifest:media-type="application/vnd.oasis.opendocument.spreadsheet"/>
                  <manifest:file-entry manifest:full-path="content.xml" manifest:media-type="text/xml"/>
                  <manifest:file-entry manifest:full-path="styles.xml" manifest:media-type="text/xml"/>
                </manifest:manifest>
                """;
    }

    private void appendOdsTextCell(StringBuilder xml, String style, String value) {
        xml.append("<table:table-cell table:style-name=\"").append(style)
           .append("\" office:value-type=\"string\">");
        String safeValue = value == null ? "" : value;
        String[] lines = safeValue.split("\\R", -1);
        for (String line : lines) {
            xml.append("<text:p>").append(escapeXml(line)).append("</text:p>");
        }
        xml.append("</table:table-cell>");
    }

    private void appendOdsNumberCell(StringBuilder xml, String style, int value) {
        xml.append("<table:table-cell table:style-name=\"").append(style)
           .append("\" office:value-type=\"float\" office:value=\"").append(value).append("\">")
           .append("<text:p>").append(value).append("</text:p></table:table-cell>");
    }

    // ---------------------------------------------------------------------
    // CSV
    // ---------------------------------------------------------------------

    private byte[] genererCsv(
            List<PrioriteRow> priorites,
            List<JoueurExport> joueurs,
            Map<String, Set<String>> skillsParJoueur,
            Map<String, Integer> occurrencesPersonnage) {

        StringBuilder csv = new StringBuilder(16_384);
        csv.append(csvCell("Joueur"));
        for (PrioriteRow o : priorites) {
            csv.append(';').append(csvCell(columnLabel(o, occurrencesPersonnage).replace('\n', ' ')));
        }
        csv.append(';').append(csvCell("Total Omicrons")).append("\r\n");

        for (JoueurExport joueur : joueurs) {
            Set<String> skills = skillsParJoueur.getOrDefault(joueur.playerId(), Set.of());
            int totalPossede = 0;

            csv.append(csvCell(joueur.nom()));
            for (PrioriteRow o : priorites) {
                boolean possede = o.getSkillId() != null && skills.contains(o.getSkillId());
                if (possede) totalPossede++;
                csv.append(';').append(possede ? "OUI" : "NON");
            }
            csv.append(';').append(csvCell(totalPossede + "/" + priorites.size())).append("\r\n");
        }

        csv.append(csvCell("TOTAL OUI"));
        for (PrioriteRow o : priorites) {
            csv.append(';').append(totalPossesseurs(o, joueurs, skillsParJoueur));
        }
        csv.append(';').append(csvCell("—")).append("\r\n");

        // BOM UTF-8 : meilleure ouverture directe sous Excel/LibreOffice en français.
        byte[] body = csv.toString().getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[body.length + 3];
        result[0] = (byte) 0xEF;
        result[1] = (byte) 0xBB;
        result[2] = (byte) 0xBF;
        System.arraycopy(body, 0, result, 3, body.length);
        return result;
    }

    private String csvCell(String value) {
        String safe = value == null ? "" : value;
        if (safe.indexOf(';') >= 0 || safe.indexOf('"') >= 0 || safe.indexOf('\n') >= 0 || safe.indexOf('\r') >= 0) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }

    // ---------------------------------------------------------------------
    // Données et utilitaires communs
    // ---------------------------------------------------------------------

    private List<PrioriteRow> filtrerPriorites(List<PrioriteRow> priorites, int priorite) {
        return priorites.stream()
                .filter(o -> o.getPriorite() == priorite)
                .toList();
    }

    private String columnLabel(PrioriteRow o, Map<String, Integer> occurrencesPersonnage) {
        String label = o.getNomPersonnage();
        if (occurrencesPersonnage.getOrDefault(normaliserNom(o.getNomPersonnage()), 0) > 1) {
            label += "\n" + o.getNomCapacite();
        }
        return label;
    }

    private int totalPossesseurs(
            PrioriteRow o,
            List<JoueurExport> joueurs,
            Map<String, Set<String>> skillsParJoueur) {
        if (o.getSkillId() == null) return 0;

        int total = 0;
        for (JoueurExport joueur : joueurs) {
            if (skillsParJoueur.getOrDefault(joueur.playerId(), Set.of()).contains(o.getSkillId())) {
                total++;
            }
        }
        return total;
    }

    private void writeEntry(ZipOutputStream zip, String path, String content) throws IOException {
        ZipEntry entry = new ZipEntry(path);
        zip.putNextEntry(entry);
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    /** Le fichier mimetype ODS doit être le premier du ZIP et non compressé. */
    private void writeStoredEntry(ZipOutputStream zip, String path, String content) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        CRC32 crc = new CRC32();
        crc.update(bytes);

        ZipEntry entry = new ZipEntry(path);
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(bytes.length);
        entry.setCompressedSize(bytes.length);
        entry.setCrc(crc.getValue());
        zip.putNextEntry(entry);
        zip.write(bytes);
        zip.closeEntry();
    }

    private String escapeXml(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private List<JoueurExport> chargerJoueursActifs() {
        return jdbc.query("""
                SELECT player_id, COALESCE(NULLIF(nom, ''), player_id) AS nom
                FROM joueur
                WHERE dans_guilde = 1
                  AND player_id IS NOT NULL
                ORDER BY COALESCE(NULLIF(nom, ''), player_id)
                """, (rs, rowNum) -> new JoueurExport(
                        rs.getString("player_id"),
                        rs.getString("nom")));
    }

    private Map<String, Set<String>> chargerPossessions() {
        Map<String, Set<String>> result = new LinkedHashMap<>();
        jdbc.query("""
                SELECT o.joueur_id, o.skill_id
                FROM omicron o
                JOIN joueur j ON j.player_id = o.joueur_id
                WHERE j.dans_guilde = 1
                  AND o.actif = 1
                  AND o.skill_id IS NOT NULL
                """, rs -> {
                    result.computeIfAbsent(
                            rs.getString("joueur_id"),
                            ignored -> new HashSet<>())
                            .add(rs.getString("skill_id"));
                });
        return result;
    }

    private Map<String, Integer> compterOccurrencesPersonnage(List<PrioriteRow> priorites) {
        Map<String, Integer> result = new HashMap<>();
        for (PrioriteRow o : priorites) {
            result.merge(normaliserNom(o.getNomPersonnage()), 1, Integer::sum);
        }
        return result;
    }

    private String normaliserNom(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private enum ExportFormat {
        EXCEL,
        LIBRE_OFFICE,
        CSV;

        static ExportFormat from(String value) {
            if (value == null || value.isBlank()) return EXCEL;
            return switch (value.trim().toLowerCase(Locale.ROOT)) {
                case "excel", "xlsx" -> EXCEL;
                case "libreoffice", "libre-office", "ods" -> LIBRE_OFFICE;
                case "csv" -> CSV;
                default -> throw new IllegalArgumentException("Format d'export inconnu : " + value);
            };
        }
    }

    private record JoueurExport(String playerId, String nom) {}

    public record ExportResult(String filename, String contentType, byte[] content) {}
}
