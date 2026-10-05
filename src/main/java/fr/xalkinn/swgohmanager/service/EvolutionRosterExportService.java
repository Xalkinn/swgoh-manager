package fr.xalkinn.swgohmanager.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.stereotype.Service;

import fr.xalkinn.swgohmanager.modele.EvolutionPersonnage;
import fr.xalkinn.swgohmanager.modele.Extraction;
import fr.xalkinn.swgohmanager.modele.Joueur;
import fr.xalkinn.swgohmanager.repository.ExtractionRepository;
import fr.xalkinn.swgohmanager.repository.JoueurRepository;
import fr.xalkinn.swgohmanager.repository.PersonnageRepository;

/**
 * Exporte la comparaison avancée du roster dans les mêmes formats que le plan Omicron :
 * Excel (.xlsx), LibreOffice (.ods) et CSV (.csv).
 *
 * Les exports XLSX/ODS sont générés uniquement avec les API Java standard afin de ne pas
 * ajouter de dépendance Apache POI au projet.
 */
@Service
public class EvolutionRosterExportService {

    private static final String CONTENT_TYPE_XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String CONTENT_TYPE_ODS =
            "application/vnd.oasis.opendocument.spreadsheet";
    private static final String CONTENT_TYPE_CSV =
            "text/csv; charset=UTF-8";

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRANCE);

    private final JoueurRepository joueurRepository;
    private final PersonnageRepository personnageRepository;
    private final ExtractionRepository extractionRepository;

    public EvolutionRosterExportService(
            JoueurRepository joueurRepository,
            PersonnageRepository personnageRepository,
            ExtractionRepository extractionRepository) {
        this.joueurRepository = joueurRepository;
        this.personnageRepository = personnageRepository;
        this.extractionRepository = extractionRepository;
    }

    public ExportResult genererExport(
            String format,
            Integer joueurId,
            Integer ancienneExtractionId,
            Integer nouvelleExtractionId) throws IOException {

        if (ancienneExtractionId == null || nouvelleExtractionId == null) {
            throw new IllegalArgumentException("Les deux extractions sont obligatoires pour exporter une comparaison.");
        }

        Extraction ancienne = extractionRepository.findById(ancienneExtractionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Extraction #" + ancienneExtractionId + " introuvable."));
        Extraction nouvelle = extractionRepository.findById(nouvelleExtractionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Extraction #" + nouvelleExtractionId + " introuvable."));

        String joueurSelectionne = null;
        List<EvolutionPersonnage> evolutions;
        if (joueurId != null) {
            Joueur joueur = joueurRepository.findById(joueurId)
                    .orElseThrow(() -> new IllegalArgumentException("Joueur introuvable : " + joueurId));
            joueurSelectionne = joueur.getNom();
            evolutions = personnageRepository.comparerExtraction(
                    joueurId, ancienneExtractionId, nouvelleExtractionId);
        } else {
            evolutions = personnageRepository.comparerTousLesJoueurs(
                    ancienneExtractionId, nouvelleExtractionId);
        }

        ExportData data = construireDonnees(
                ancienne, nouvelle, joueurSelectionne, evolutions);

        String scopeFilename = joueurSelectionne == null
                ? "guilde"
                : slug(joueurSelectionne);
        String baseFilename = "progression-roster-" + scopeFilename
                + "-e" + ancienneExtractionId
                + "-vers-e" + nouvelleExtractionId
                + "-" + LocalDate.now();

        ExportFormat exportFormat = ExportFormat.from(format);
        return switch (exportFormat) {
            case EXCEL -> new ExportResult(
                    baseFilename + ".xlsx",
                    CONTENT_TYPE_XLSX,
                    genererXlsx(data));
            case LIBRE_OFFICE -> new ExportResult(
                    baseFilename + ".ods",
                    CONTENT_TYPE_ODS,
                    genererOds(data));
            case CSV -> new ExportResult(
                    baseFilename + ".csv",
                    CONTENT_TYPE_CSV,
                    genererCsv(data));
        };
    }

    private ExportData construireDonnees(
            Extraction ancienne,
            Extraction nouvelle,
            String joueurSelectionne,
            List<EvolutionPersonnage> evolutions) {

        List<EvolutionRow> rows = new ArrayList<>();
        String fallbackJoueur = joueurSelectionne == null ? "—" : joueurSelectionne;

        for (EvolutionPersonnage evo : evolutions) {
            String joueur = texteNonVide(evo.getJoueur()) ? evo.getJoueur() : fallbackJoueur;
            int ancienGear = valeur(evo.getAncienGear());
            int nouveauGear = valeur(evo.getNouveauGear());
            int ancienneRelique = valeur(evo.getAncienneRelique());
            int nouvelleRelique = valeur(evo.getNouvelleRelique());
            int anciennesEtoiles = valeur(evo.getAnciennesEtoiles());
            int nouvellesEtoiles = valeur(evo.getNouvellesEtoiles());

            rows.add(new EvolutionRow(
                    joueur,
                    Objects.toString(evo.getNom(), "—"),
                    ancienGear,
                    nouveauGear,
                    nouveauGear - ancienGear,
                    ancienneRelique,
                    nouvelleRelique,
                    nouvelleRelique - ancienneRelique,
                    anciennesEtoiles,
                    nouvellesEtoiles,
                    nouvellesEtoiles - anciennesEtoiles,
                    construireProgression(
                            ancienGear, nouveauGear,
                            ancienneRelique, nouvelleRelique,
                            anciennesEtoiles, nouvellesEtoiles)));
        }

        rows.sort(Comparator
                .comparing(EvolutionRow::joueur, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(EvolutionRow::personnage, String.CASE_INSENSITIVE_ORDER));

        int gearUps = (int) rows.stream().filter(r -> r.deltaGear() > 0).count();
        int relicUps = (int) rows.stream().filter(r -> r.deltaRelique() > 0).count();
        int starUps = (int) rows.stream().filter(r -> r.deltaEtoiles() > 0).count();
        int joueursConcernes = (int) rows.stream()
                .map(EvolutionRow::joueur)
                .filter(n -> !"—".equals(n))
                .distinct()
                .count();

        List<JoueurSummary> joueurs = construireSyntheseJoueurs(rows);

        String perimetre = joueurSelectionne == null
                ? "Ensemble de la guilde"
                : "Joueur : " + joueurSelectionne;

        return new ExportData(
                perimetre,
                extractionLabel(ancienne),
                extractionLabel(nouvelle),
                rows,
                joueurs,
                rows.size(),
                joueursConcernes,
                gearUps,
                relicUps,
                starUps);
    }

    private List<JoueurSummary> construireSyntheseJoueurs(List<EvolutionRow> rows) {
        Map<String, MutableSummary> parJoueur = new LinkedHashMap<>();
        for (EvolutionRow row : rows) {
            MutableSummary summary = parJoueur.computeIfAbsent(
                    row.joueur(), ignored -> new MutableSummary());
            summary.evolutions++;
            if (row.deltaGear() > 0) summary.gearUps++;
            if (row.deltaRelique() > 0) summary.relicUps++;
            if (row.deltaEtoiles() > 0) summary.starUps++;
        }

        return parJoueur.entrySet().stream()
                .map(entry -> new JoueurSummary(
                        entry.getKey(),
                        entry.getValue().evolutions,
                        entry.getValue().gearUps,
                        entry.getValue().relicUps,
                        entry.getValue().starUps,
                        resumeJoueur(entry.getValue())))
                .sorted(Comparator
                        .comparingInt(JoueurSummary::evolutions).reversed()
                        .thenComparing(JoueurSummary::joueur, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private String resumeJoueur(MutableSummary summary) {
        List<String> parts = new ArrayList<>();
        if (summary.gearUps > 0) parts.add(summary.gearUps + " Gear+");
        if (summary.relicUps > 0) parts.add(summary.relicUps + " Relique+");
        if (summary.starUps > 0) parts.add(summary.starUps + " Étoile+");
        return parts.isEmpty() ? "Changement détecté" : String.join(" · ", parts);
    }

    private String construireProgression(
            int ancienGear,
            int nouveauGear,
            int ancienneRelique,
            int nouvelleRelique,
            int anciennesEtoiles,
            int nouvellesEtoiles) {

        List<String> parts = new ArrayList<>();
        if (ancienGear != nouveauGear) {
            parts.add("G" + ancienGear + " → G" + nouveauGear);
        }
        if (ancienneRelique != nouvelleRelique) {
            parts.add("R" + ancienneRelique + " → R" + nouvelleRelique);
        }
        if (anciennesEtoiles != nouvellesEtoiles) {
            parts.add(anciennesEtoiles + "★ → " + nouvellesEtoiles + "★");
        }
        return parts.isEmpty() ? "—" : String.join(" · ", parts);
    }

    private String extractionLabel(Extraction extraction) {
        LocalDateTime date = extraction.getDateFin();
        String dateTexte = date == null ? "date inconnue" : DATE_TIME_FORMAT.format(date);
        return "Extraction #" + extraction.getId() + " · " + dateTexte;
    }

    // ---------------------------------------------------------------------
    // Excel / XLSX
    // ---------------------------------------------------------------------

    private byte[] genererXlsx(ExportData data) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {

            writeEntry(zip, "[Content_Types].xml", contentTypesXml());
            writeEntry(zip, "_rels/.rels", rootRelationshipsXml());
            writeEntry(zip, "xl/workbook.xml", workbookXml());
            writeEntry(zip, "xl/_rels/workbook.xml.rels", workbookRelationshipsXml());
            writeEntry(zip, "xl/styles.xml", stylesXml());
            writeEntry(zip, "xl/worksheets/sheet1.xml", progressionSheetXml(data));
            writeEntry(zip, "xl/worksheets/sheet2.xml", joueursSheetXml(data));

            zip.finish();
            return out.toByteArray();
        }
    }

    private String progressionSheetXml(ExportData data) {
        int dataStartRow = 9;
        int lastRow = Math.max(dataStartRow, dataStartRow + data.rows().size());

        StringBuilder xml = new StringBuilder(64_000);
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
           .append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
           .append("<sheetPr><pageSetUpPr fitToPage=\"1\"/></sheetPr>")
           .append("<dimension ref=\"A1:L").append(lastRow).append("\"/>")
           .append("<sheetViews><sheetView workbookViewId=\"0\">")
           .append("<pane ySplit=\"8\" topLeftCell=\"A9\" activePane=\"bottomLeft\" state=\"frozen\"/>")
           .append("</sheetView></sheetViews>")
           .append("<sheetFormatPr defaultRowHeight=\"18\"/>")
           .append("<cols>")
           .append("<col min=\"1\" max=\"1\" width=\"23\" customWidth=\"1\"/>")
           .append("<col min=\"2\" max=\"2\" width=\"31\" customWidth=\"1\"/>")
           .append("<col min=\"3\" max=\"11\" width=\"12\" customWidth=\"1\"/>")
           .append("<col min=\"12\" max=\"12\" width=\"38\" customWidth=\"1\"/>")
           .append("</cols><sheetData>");

        xml.append("<row r=\"1\" ht=\"31\" customHeight=\"1\">");
        appendTextCell(xml, 0, 1, 1, "PROGRESSION DU ROSTER");
        xml.append("</row>");

        xml.append("<row r=\"2\" ht=\"21\" customHeight=\"1\">");
        appendTextCell(xml, 0, 2, 2, data.perimetre());
        xml.append("</row>");

        xml.append("<row r=\"3\" ht=\"20\" customHeight=\"1\">");
        appendTextCell(xml, 0, 3, 3, data.ancienneExtraction() + "  →  " + data.nouvelleExtraction());
        xml.append("</row>");

        String[] labels = {"ÉVOLUTIONS", "JOUEURS CONCERNÉS", "GEAR +", "RELIQUES +", "ÉTOILES +"};
        int[] values = {data.totalEvolutions(), data.joueursConcernes(), data.gearUps(), data.relicUps(), data.starUps()};
        int[] startCols = {0, 2, 4, 6, 8};

        xml.append("<row r=\"5\" ht=\"21\" customHeight=\"1\">");
        for (int i = 0; i < labels.length; i++) {
            appendTextCell(xml, startCols[i], 5, 4, labels[i]);
        }
        xml.append("</row>");

        xml.append("<row r=\"6\" ht=\"31\" customHeight=\"1\">");
        for (int i = 0; i < values.length; i++) {
            appendNumberCell(xml, startCols[i], 6, 5, values[i]);
        }
        xml.append("</row>");

        String[] headers = {
                "Joueur", "Personnage",
                "Gear avant", "Gear après", "Δ Gear",
                "Relique avant", "Relique après", "Δ Relique",
                "Étoiles avant", "Étoiles après", "Δ Étoiles",
                "Progression"
        };
        xml.append("<row r=\"8\" ht=\"30\" customHeight=\"1\">");
        for (int col = 0; col < headers.length; col++) {
            appendTextCell(xml, col, 8, 6, headers[col]);
        }
        xml.append("</row>");

        int rowNumber = dataStartRow;
        for (EvolutionRow row : data.rows()) {
            xml.append("<row r=\"").append(rowNumber).append("\" ht=\"22\" customHeight=\"1\">");
            appendTextCell(xml, 0, rowNumber, 12, row.joueur());
            appendTextCell(xml, 1, rowNumber, 7, row.personnage());
            appendNumberCell(xml, 2, rowNumber, 8, row.ancienGear());
            appendNumberCell(xml, 3, rowNumber, 8, row.nouveauGear());
            appendSignedNumberCell(xml, 4, rowNumber, row.deltaGear());
            appendNumberCell(xml, 5, rowNumber, 8, row.ancienneRelique());
            appendNumberCell(xml, 6, rowNumber, 8, row.nouvelleRelique());
            appendSignedNumberCell(xml, 7, rowNumber, row.deltaRelique());
            appendNumberCell(xml, 8, rowNumber, 8, row.anciennesEtoiles());
            appendNumberCell(xml, 9, rowNumber, 8, row.nouvellesEtoiles());
            appendSignedNumberCell(xml, 10, rowNumber, row.deltaEtoiles());
            appendTextCell(xml, 11, rowNumber, 11, row.progression());
            xml.append("</row>");
            rowNumber++;
        }

        if (data.rows().isEmpty()) {
            xml.append("<row r=\"9\" ht=\"24\" customHeight=\"1\">");
            appendTextCell(xml, 0, 9, 3, "Aucune évolution détectée pour cette comparaison.");
            xml.append("</row>");
        }

        xml.append("</sheetData>")
           .append("<mergeCells count=\"13\">")
           .append("<mergeCell ref=\"A1:L1\"/>")
           .append("<mergeCell ref=\"A2:L2\"/>")
           .append("<mergeCell ref=\"A3:L3\"/>")
           .append("<mergeCell ref=\"A5:B5\"/><mergeCell ref=\"A6:B6\"/>")
           .append("<mergeCell ref=\"C5:D5\"/><mergeCell ref=\"C6:D6\"/>")
           .append("<mergeCell ref=\"E5:F5\"/><mergeCell ref=\"E6:F6\"/>")
           .append("<mergeCell ref=\"G5:H5\"/><mergeCell ref=\"G6:H6\"/>")
           .append("<mergeCell ref=\"I5:J5\"/><mergeCell ref=\"I6:J6\"/>")
           .append("</mergeCells>");

        if (!data.rows().isEmpty()) {
            xml.append("<autoFilter ref=\"A8:L").append(rowNumber - 1).append("\"/>");
        }

        xml.append("<pageMargins left=\"0.3\" right=\"0.3\" top=\"0.5\" bottom=\"0.5\" header=\"0.2\" footer=\"0.2\"/>")
           .append("<pageSetup orientation=\"landscape\" fitToWidth=\"1\" fitToHeight=\"0\"/>")
           .append("</worksheet>");
        return xml.toString();
    }

    private String joueursSheetXml(ExportData data) {
        int lastRow = Math.max(5, data.joueurs().size() + 4);
        StringBuilder xml = new StringBuilder(32_000);
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
           .append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
           .append("<dimension ref=\"A1:F").append(lastRow).append("\"/>")
           .append("<sheetViews><sheetView workbookViewId=\"0\">")
           .append("<pane ySplit=\"4\" topLeftCell=\"A5\" activePane=\"bottomLeft\" state=\"frozen\"/>")
           .append("</sheetView></sheetViews>")
           .append("<sheetFormatPr defaultRowHeight=\"18\"/>")
           .append("<cols>")
           .append("<col min=\"1\" max=\"1\" width=\"27\" customWidth=\"1\"/>")
           .append("<col min=\"2\" max=\"5\" width=\"15\" customWidth=\"1\"/>")
           .append("<col min=\"6\" max=\"6\" width=\"38\" customWidth=\"1\"/>")
           .append("</cols><sheetData>");

        xml.append("<row r=\"1\" ht=\"31\" customHeight=\"1\">");
        appendTextCell(xml, 0, 1, 1, "PROGRESSION PAR JOUEUR");
        xml.append("</row>");
        xml.append("<row r=\"2\" ht=\"20\" customHeight=\"1\">");
        appendTextCell(xml, 0, 2, 3, data.ancienneExtraction() + "  →  " + data.nouvelleExtraction());
        xml.append("</row>");

        String[] headers = {"Joueur", "Évolutions", "Gear +", "Reliques +", "Étoiles +", "Résumé"};
        xml.append("<row r=\"4\" ht=\"27\" customHeight=\"1\">");
        for (int col = 0; col < headers.length; col++) {
            appendTextCell(xml, col, 4, 6, headers[col]);
        }
        xml.append("</row>");

        int rowNumber = 5;
        for (JoueurSummary row : data.joueurs()) {
            xml.append("<row r=\"").append(rowNumber).append("\" ht=\"22\" customHeight=\"1\">");
            appendTextCell(xml, 0, rowNumber, 12, row.joueur());
            appendNumberCell(xml, 1, rowNumber, 8, row.evolutions());
            appendNumberCell(xml, 2, rowNumber, row.gearUps() > 0 ? 9 : 8, row.gearUps());
            appendNumberCell(xml, 3, rowNumber, row.relicUps() > 0 ? 9 : 8, row.relicUps());
            appendNumberCell(xml, 4, rowNumber, row.starUps() > 0 ? 9 : 8, row.starUps());
            appendTextCell(xml, 5, rowNumber, 11, row.resume());
            xml.append("</row>");
            rowNumber++;
        }

        if (data.joueurs().isEmpty()) {
            xml.append("<row r=\"5\"><c r=\"A5\" s=\"3\" t=\"inlineStr\"><is><t>Aucune progression à synthétiser.</t></is></c></row>");
        }

        xml.append("</sheetData>")
           .append("<mergeCells count=\"2\"><mergeCell ref=\"A1:F1\"/><mergeCell ref=\"A2:F2\"/></mergeCells>");
        if (!data.joueurs().isEmpty()) {
            xml.append("<autoFilter ref=\"A4:F").append(rowNumber - 1).append("\"/>");
        }
        xml.append("<pageMargins left=\"0.4\" right=\"0.4\" top=\"0.5\" bottom=\"0.5\" header=\"0.2\" footer=\"0.2\"/>")
           .append("<pageSetup orientation=\"landscape\" fitToWidth=\"1\" fitToHeight=\"0\"/>")
           .append("</worksheet>");
        return xml.toString();
    }

    private void appendSignedNumberCell(StringBuilder xml, int column, int row, int value) {
        int style = value > 0 ? 9 : value < 0 ? 10 : 8;
        appendNumberCell(xml, column, row, style, value);
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
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                  <Default Extension="xml" ContentType="application/xml"/>
                  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
                </Types>
                """;
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
                  <bookViews><workbookView activeTab="0"/></bookViews>
                  <sheets>
                    <sheet name="Progression" sheetId="1" r:id="rId1"/>
                    <sheet name="Par joueur" sheetId="2" r:id="rId2"/>
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
                  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
                </Relationships>
                """;
    }

    private String stylesXml() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <fonts count="7">
                    <font><sz val="10"/><name val="Calibri"/><family val="2"/></font>
                    <font><b/><color rgb="FFFFFFFF"/><sz val="16"/><name val="Calibri"/></font>
                    <font><color rgb="FF475569"/><sz val="11"/><name val="Calibri"/></font>
                    <font><b/><color rgb="FFFFFFFF"/><sz val="10"/><name val="Calibri"/></font>
                    <font><b/><color rgb="FF334155"/><sz val="10"/><name val="Calibri"/></font>
                    <font><b/><color rgb="FF166534"/><sz val="15"/><name val="Calibri"/></font>
                    <font><b/><color rgb="FF334155"/><sz val="10"/><name val="Calibri"/></font>
                  </fonts>
                  <fills count="7">
                    <fill><patternFill patternType="none"/></fill>
                    <fill><patternFill patternType="gray125"/></fill>
                    <fill><patternFill patternType="solid"><fgColor rgb="FF4338CA"/><bgColor indexed="64"/></patternFill></fill>
                    <fill><patternFill patternType="solid"><fgColor rgb="FFEFF6FF"/><bgColor indexed="64"/></patternFill></fill>
                    <fill><patternFill patternType="solid"><fgColor rgb="FFDCFCE7"/><bgColor indexed="64"/></patternFill></fill>
                    <fill><patternFill patternType="solid"><fgColor rgb="FFFEE2E2"/><bgColor indexed="64"/></patternFill></fill>
                    <fill><patternFill patternType="solid"><fgColor rgb="FFF3E8FF"/><bgColor indexed="64"/></patternFill></fill>
                  </fills>
                  <borders count="2">
                    <border><left/><right/><top/><bottom/><diagonal/></border>
                    <border>
                      <left style="thin"><color rgb="FFE2E8F0"/></left>
                      <right style="thin"><color rgb="FFE2E8F0"/></right>
                      <top style="thin"><color rgb="FFE2E8F0"/></top>
                      <bottom style="thin"><color rgb="FFE2E8F0"/></bottom>
                      <diagonal/>
                    </border>
                  </borders>
                  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
                  <cellXfs count="13">
                    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
                    <xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1" applyAlignment="1"><alignment vertical="center"/></xf>
                    <xf numFmtId="0" fontId="4" fillId="3" borderId="0" xfId="0" applyFont="1" applyFill="1" applyAlignment="1"><alignment vertical="center"/></xf>
                    <xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1" applyAlignment="1"><alignment vertical="center"/></xf>
                    <xf numFmtId="0" fontId="4" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
                    <xf numFmtId="0" fontId="5" fillId="4" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
                    <xf numFmtId="0" fontId="3" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
                    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1"><alignment vertical="center" wrapText="1"/></xf>
                    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
                    <xf numFmtId="0" fontId="6" fillId="4" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
                    <xf numFmtId="0" fontId="6" fillId="5" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
                    <xf numFmtId="0" fontId="6" fillId="6" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment vertical="center" wrapText="1"/></xf>
                    <xf numFmtId="0" fontId="6" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1" applyAlignment="1"><alignment vertical="center"/></xf>
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

    private byte[] genererOds(ExportData data) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {

            writeStoredEntry(zip, "mimetype", CONTENT_TYPE_ODS);
            writeEntry(zip, "content.xml", odsContentXml(data));
            writeEntry(zip, "styles.xml", odsStylesXml());
            writeEntry(zip, "META-INF/manifest.xml", odsManifestXml());

            zip.finish();
            return out.toByteArray();
        }
    }

    private String odsContentXml(ExportData data) {
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

        appendOdsProgressionTable(xml, data);
        appendOdsJoueursTable(xml, data);

        xml.append("</office:spreadsheet></office:body></office:document-content>");
        return xml.toString();
    }

    private void appendOdsProgressionTable(StringBuilder xml, ExportData data) {
        xml.append("<table:table table:name=\"Progression\">");
        xml.append("<table:table-column table:style-name=\"co-player\"/>")
           .append("<table:table-column table:style-name=\"co-character\"/>")
           .append("<table:table-column table:style-name=\"co-small\" table:number-columns-repeated=\"9\"/>")
           .append("<table:table-column table:style-name=\"co-progress\"/>");

        appendOdsSpannedTextCellRow(xml, "ce-title", "PROGRESSION DU ROSTER", 12);
        appendOdsSpannedTextCellRow(xml, "ce-subtitle", data.perimetre(), 12);
        appendOdsSpannedTextCellRow(xml, "ce-meta", data.ancienneExtraction() + "  →  " + data.nouvelleExtraction(), 12);
        xml.append("<table:table-row/>");

        xml.append("<table:table-row>");
        appendOdsTextCell(xml, "ce-kpi-label", "ÉVOLUTIONS");
        appendOdsTextCell(xml, "ce-kpi-label", "JOUEURS");
        appendOdsTextCell(xml, "ce-kpi-label", "GEAR +");
        appendOdsTextCell(xml, "ce-kpi-label", "RELIQUES +");
        appendOdsTextCell(xml, "ce-kpi-label", "ÉTOILES +");
        appendOdsEmptyCells(xml, 7);
        xml.append("</table:table-row>");

        xml.append("<table:table-row>");
        appendOdsNumberCell(xml, "ce-kpi-value", data.totalEvolutions());
        appendOdsNumberCell(xml, "ce-kpi-value", data.joueursConcernes());
        appendOdsNumberCell(xml, "ce-kpi-value", data.gearUps());
        appendOdsNumberCell(xml, "ce-kpi-value", data.relicUps());
        appendOdsNumberCell(xml, "ce-kpi-value", data.starUps());
        appendOdsEmptyCells(xml, 7);
        xml.append("</table:table-row>");

        xml.append("<table:table-row/>");
        String[] headers = {
                "Joueur", "Personnage",
                "Gear avant", "Gear après", "Δ Gear",
                "Relique avant", "Relique après", "Δ Relique",
                "Étoiles avant", "Étoiles après", "Δ Étoiles",
                "Progression"
        };
        xml.append("<table:table-row table:style-name=\"ro-header\">");
        for (String header : headers) appendOdsTextCell(xml, "ce-header", header);
        xml.append("</table:table-row>");

        for (EvolutionRow row : data.rows()) {
            xml.append("<table:table-row>");
            appendOdsTextCell(xml, "ce-player", row.joueur());
            appendOdsTextCell(xml, "ce-normal", row.personnage());
            appendOdsNumberCell(xml, "ce-number", row.ancienGear());
            appendOdsNumberCell(xml, "ce-number", row.nouveauGear());
            appendOdsNumberCell(xml, odsDeltaStyle(row.deltaGear()), row.deltaGear());
            appendOdsNumberCell(xml, "ce-number", row.ancienneRelique());
            appendOdsNumberCell(xml, "ce-number", row.nouvelleRelique());
            appendOdsNumberCell(xml, odsDeltaStyle(row.deltaRelique()), row.deltaRelique());
            appendOdsNumberCell(xml, "ce-number", row.anciennesEtoiles());
            appendOdsNumberCell(xml, "ce-number", row.nouvellesEtoiles());
            appendOdsNumberCell(xml, odsDeltaStyle(row.deltaEtoiles()), row.deltaEtoiles());
            appendOdsTextCell(xml, "ce-progress", row.progression());
            xml.append("</table:table-row>");
        }

        if (data.rows().isEmpty()) {
            appendOdsSpannedTextCellRow(xml, "ce-meta", "Aucune évolution détectée pour cette comparaison.", 12);
        }
        xml.append("</table:table>");
    }

    private void appendOdsJoueursTable(StringBuilder xml, ExportData data) {
        xml.append("<table:table table:name=\"Par joueur\">")
           .append("<table:table-column table:style-name=\"co-player-summary\"/>")
           .append("<table:table-column table:style-name=\"co-summary-number\" table:number-columns-repeated=\"4\"/>")
           .append("<table:table-column table:style-name=\"co-summary-text\"/>");

        appendOdsSpannedTextCellRow(xml, "ce-title", "PROGRESSION PAR JOUEUR", 6);
        appendOdsSpannedTextCellRow(xml, "ce-meta", data.ancienneExtraction() + "  →  " + data.nouvelleExtraction(), 6);
        xml.append("<table:table-row/>");

        String[] headers = {"Joueur", "Évolutions", "Gear +", "Reliques +", "Étoiles +", "Résumé"};
        xml.append("<table:table-row table:style-name=\"ro-header\">");
        for (String header : headers) appendOdsTextCell(xml, "ce-header", header);
        xml.append("</table:table-row>");

        for (JoueurSummary row : data.joueurs()) {
            xml.append("<table:table-row>");
            appendOdsTextCell(xml, "ce-player", row.joueur());
            appendOdsNumberCell(xml, "ce-number", row.evolutions());
            appendOdsNumberCell(xml, row.gearUps() > 0 ? "ce-positive" : "ce-number", row.gearUps());
            appendOdsNumberCell(xml, row.relicUps() > 0 ? "ce-positive" : "ce-number", row.relicUps());
            appendOdsNumberCell(xml, row.starUps() > 0 ? "ce-positive" : "ce-number", row.starUps());
            appendOdsTextCell(xml, "ce-progress", row.resume());
            xml.append("</table:table-row>");
        }
        if (data.joueurs().isEmpty()) {
            appendOdsSpannedTextCellRow(xml, "ce-meta", "Aucune progression à synthétiser.", 6);
        }
        xml.append("</table:table>");
    }

    private String odsDeltaStyle(int value) {
        if (value > 0) return "ce-positive";
        if (value < 0) return "ce-negative";
        return "ce-number";
    }

    private void appendOdsSpannedTextCellRow(StringBuilder xml, String style, String value, int span) {
        xml.append("<table:table-row>")
           .append("<table:table-cell table:style-name=\"").append(style)
           .append("\" table:number-columns-spanned=\"").append(span)
           .append("\" office:value-type=\"string\"><text:p>")
           .append(escapeXml(value))
           .append("</text:p></table:table-cell>");
        for (int i = 1; i < span; i++) {
            xml.append("<table:covered-table-cell/>");
        }
        xml.append("</table:table-row>");
    }

    private void appendOdsEmptyCells(StringBuilder xml, int count) {
        if (count <= 0) return;
        xml.append("<table:table-cell table:number-columns-repeated=\"")
           .append(count).append("\"/>");
    }

    private void appendOdsTextCell(StringBuilder xml, String style, String value) {
        xml.append("<table:table-cell table:style-name=\"").append(style)
           .append("\" office:value-type=\"string\"><text:p>")
           .append(escapeXml(value))
           .append("</text:p></table:table-cell>");
    }

    private void appendOdsNumberCell(StringBuilder xml, String style, int value) {
        xml.append("<table:table-cell table:style-name=\"").append(style)
           .append("\" office:value-type=\"float\" office:value=\"").append(value)
           .append("\"><text:p>").append(value).append("</text:p></table:table-cell>");
    }

    private String odsAutomaticStyles() {
        return """
                <style:style style:name="co-player" style:family="table-column"><style:table-column-properties style:column-width="4.6cm"/></style:style>
                <style:style style:name="co-character" style:family="table-column"><style:table-column-properties style:column-width="6.2cm"/></style:style>
                <style:style style:name="co-small" style:family="table-column"><style:table-column-properties style:column-width="2.5cm"/></style:style>
                <style:style style:name="co-progress" style:family="table-column"><style:table-column-properties style:column-width="8.0cm"/></style:style>
                <style:style style:name="co-player-summary" style:family="table-column"><style:table-column-properties style:column-width="5.5cm"/></style:style>
                <style:style style:name="co-summary-number" style:family="table-column"><style:table-column-properties style:column-width="3.0cm"/></style:style>
                <style:style style:name="co-summary-text" style:family="table-column"><style:table-column-properties style:column-width="8.0cm"/></style:style>
                <style:style style:name="ro-header" style:family="table-row"><style:table-row-properties style:row-height="0.8cm" style:use-optimal-row-height="true"/></style:style>
                <style:style style:name="ce-title" style:family="table-cell">
                  <style:table-cell-properties fo:background-color="#4338CA" fo:padding="0.15cm"/>
                  <style:text-properties fo:color="#FFFFFF" fo:font-size="16pt" fo:font-weight="bold"/>
                </style:style>
                <style:style style:name="ce-subtitle" style:family="table-cell">
                  <style:table-cell-properties fo:background-color="#EFF6FF" fo:padding="0.10cm"/>
                  <style:text-properties fo:color="#334155" fo:font-weight="bold"/>
                </style:style>
                <style:style style:name="ce-meta" style:family="table-cell">
                  <style:table-cell-properties fo:padding="0.08cm"/>
                  <style:text-properties fo:color="#64748B"/>
                </style:style>
                <style:style style:name="ce-kpi-label" style:family="table-cell">
                  <style:table-cell-properties fo:background-color="#EFF6FF" fo:border="0.02cm solid #E2E8F0" fo:padding="0.08cm"/>
                  <style:text-properties fo:color="#334155" fo:font-weight="bold"/>
                  <style:paragraph-properties fo:text-align="center"/>
                </style:style>
                <style:style style:name="ce-kpi-value" style:family="table-cell">
                  <style:table-cell-properties fo:background-color="#DCFCE7" fo:border="0.02cm solid #E2E8F0" fo:padding="0.08cm"/>
                  <style:text-properties fo:color="#166534" fo:font-size="15pt" fo:font-weight="bold"/>
                  <style:paragraph-properties fo:text-align="center"/>
                </style:style>
                <style:style style:name="ce-header" style:family="table-cell">
                  <style:table-cell-properties fo:background-color="#4338CA" fo:border="0.02cm solid #E2E8F0" fo:padding="0.08cm"/>
                  <style:text-properties fo:color="#FFFFFF" fo:font-weight="bold"/>
                  <style:paragraph-properties fo:text-align="center"/>
                </style:style>
                <style:style style:name="ce-player" style:family="table-cell">
                  <style:table-cell-properties fo:border="0.02cm solid #E2E8F0" fo:padding="0.08cm"/>
                  <style:text-properties fo:color="#334155" fo:font-weight="bold"/>
                </style:style>
                <style:style style:name="ce-normal" style:family="table-cell"><style:table-cell-properties fo:border="0.02cm solid #E2E8F0" fo:padding="0.08cm"/></style:style>
                <style:style style:name="ce-number" style:family="table-cell">
                  <style:table-cell-properties fo:border="0.02cm solid #E2E8F0" fo:padding="0.08cm"/>
                  <style:paragraph-properties fo:text-align="center"/>
                </style:style>
                <style:style style:name="ce-positive" style:family="table-cell">
                  <style:table-cell-properties fo:background-color="#DCFCE7" fo:border="0.02cm solid #E2E8F0" fo:padding="0.08cm"/>
                  <style:text-properties fo:color="#166534" fo:font-weight="bold"/>
                  <style:paragraph-properties fo:text-align="center"/>
                </style:style>
                <style:style style:name="ce-negative" style:family="table-cell">
                  <style:table-cell-properties fo:background-color="#FEE2E2" fo:border="0.02cm solid #E2E8F0" fo:padding="0.08cm"/>
                  <style:text-properties fo:color="#991B1B" fo:font-weight="bold"/>
                  <style:paragraph-properties fo:text-align="center"/>
                </style:style>
                <style:style style:name="ce-progress" style:family="table-cell">
                  <style:table-cell-properties fo:background-color="#F3E8FF" fo:border="0.02cm solid #E2E8F0" fo:padding="0.08cm"/>
                  <style:text-properties fo:color="#5B21B6" fo:font-weight="bold"/>
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

    // ---------------------------------------------------------------------
    // CSV
    // ---------------------------------------------------------------------

    private byte[] genererCsv(ExportData data) {
        StringBuilder csv = new StringBuilder(32_000);
        csv.append(csvCell("PROGRESSION DU ROSTER")).append("\r\n");
        csv.append(csvCell("Périmètre")).append(';').append(csvCell(data.perimetre())).append("\r\n");
        csv.append(csvCell("Ancienne extraction")).append(';').append(csvCell(data.ancienneExtraction())).append("\r\n");
        csv.append(csvCell("Nouvelle extraction")).append(';').append(csvCell(data.nouvelleExtraction())).append("\r\n");
        csv.append(csvCell("Évolutions")).append(';').append(data.totalEvolutions()).append("\r\n");
        csv.append(csvCell("Joueurs concernés")).append(';').append(data.joueursConcernes()).append("\r\n");
        csv.append(csvCell("Gear +")).append(';').append(data.gearUps()).append("\r\n");
        csv.append(csvCell("Reliques +")).append(';').append(data.relicUps()).append("\r\n");
        csv.append(csvCell("Étoiles +")).append(';').append(data.starUps()).append("\r\n\r\n");

        String[] headers = {
                "Joueur", "Personnage",
                "Gear avant", "Gear après", "Delta Gear",
                "Relique avant", "Relique après", "Delta Relique",
                "Étoiles avant", "Étoiles après", "Delta Étoiles",
                "Progression"
        };
        for (int i = 0; i < headers.length; i++) {
            if (i > 0) csv.append(';');
            csv.append(csvCell(headers[i]));
        }
        csv.append("\r\n");

        for (EvolutionRow row : data.rows()) {
            csv.append(csvCell(row.joueur())).append(';')
               .append(csvCell(row.personnage())).append(';')
               .append(row.ancienGear()).append(';')
               .append(row.nouveauGear()).append(';')
               .append(row.deltaGear()).append(';')
               .append(row.ancienneRelique()).append(';')
               .append(row.nouvelleRelique()).append(';')
               .append(row.deltaRelique()).append(';')
               .append(row.anciennesEtoiles()).append(';')
               .append(row.nouvellesEtoiles()).append(';')
               .append(row.deltaEtoiles()).append(';')
               .append(csvCell(row.progression())).append("\r\n");
        }

        csv.append("\r\n").append(csvCell("SYNTHÈSE PAR JOUEUR")).append("\r\n")
           .append("Joueur;Évolutions;Gear +;Reliques +;Étoiles +;Résumé\r\n");
        for (JoueurSummary row : data.joueurs()) {
            csv.append(csvCell(row.joueur())).append(';')
               .append(row.evolutions()).append(';')
               .append(row.gearUps()).append(';')
               .append(row.relicUps()).append(';')
               .append(row.starUps()).append(';')
               .append(csvCell(row.resume())).append("\r\n");
        }

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
    // Utilitaires
    // ---------------------------------------------------------------------

    private int valeur(Integer value) {
        return value == null ? 0 : value;
    }

    private boolean texteNonVide(String value) {
        return value != null && !value.isBlank();
    }

    private String slug(String value) {
        String normalized = Normalizer.normalize(Objects.toString(value, "joueur"), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return normalized.isBlank() ? "joueur" : normalized;
    }

    private void writeEntry(ZipOutputStream zip, String path, String content) throws IOException {
        ZipEntry entry = new ZipEntry(path);
        zip.putNextEntry(entry);
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

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

    private static final class MutableSummary {
        private int evolutions;
        private int gearUps;
        private int relicUps;
        private int starUps;
    }

    private record ExportData(
            String perimetre,
            String ancienneExtraction,
            String nouvelleExtraction,
            List<EvolutionRow> rows,
            List<JoueurSummary> joueurs,
            int totalEvolutions,
            int joueursConcernes,
            int gearUps,
            int relicUps,
            int starUps) {}

    private record EvolutionRow(
            String joueur,
            String personnage,
            int ancienGear,
            int nouveauGear,
            int deltaGear,
            int ancienneRelique,
            int nouvelleRelique,
            int deltaRelique,
            int anciennesEtoiles,
            int nouvellesEtoiles,
            int deltaEtoiles,
            String progression) {}

    private record JoueurSummary(
            String joueur,
            int evolutions,
            int gearUps,
            int relicUps,
            int starUps,
            String resume) {}

    public record ExportResult(String filename, String contentType, byte[] content) {}
}
