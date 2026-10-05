package fr.xalkinn.swgohmanager.service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.*;
import org.springframework.stereotype.Service;

@Service
public class ConsigneExportService {
    private final ConsigneService service;
    public ConsigneExportService(ConsigneService service) { this.service=service; }
    public byte[] exporter(LocalDate debut, LocalDate fin, List<Long> selection) throws IOException {
        if(fin.isBefore(debut)) throw new IllegalArgumentException("La fin doit suivre le début.");
        var joueurs=service.suivi(debut,fin,selection);
        List<String> noms=new ArrayList<>();
        for(var liste:service.listes()) if(selection.contains(((Number)liste.get("id")).longValue())) noms.add((String)liste.get("nom"));
        String contexte="Du "+debut+" au "+fin+" · Listes autorisées : "+(noms.isEmpty()?"aucune":String.join(", ",noms));
        try(var out=new ByteArrayOutputStream();var zip=new ZipOutputStream(out,StandardCharsets.UTF_8)) {
            entry(zip,"[Content_Types].xml",contentTypesXml()); entry(zip,"_rels/.rels",rootRelationshipsXml());
            entry(zip,"xl/workbook.xml",workbookXml());entry(zip,"xl/_rels/workbook.xml.rels",workbookRelationshipsXml());entry(zip,"xl/styles.xml",stylesXml());
            entry(zip,"xl/worksheets/sheet1.xml",sheet(joueurs,"Hors consignes",contexte));
            entry(zip,"xl/worksheets/sheet2.xml",sheet(joueurs,"À vérifier",contexte));
            zip.finish();return out.toByteArray();
        }
    }
    public record ExportResult(String extension,String contentType,byte[] content) {}
    public ExportResult exporter(String format,LocalDate debut,LocalDate fin,List<Long> selection) throws IOException {
        String type=format==null?"excel":format.toLowerCase(Locale.ROOT);
        if(type.equals("excel") || type.equals("xlsx")) return new ExportResult("xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",exporter(debut,fin,selection));
        if(!Set.of("csv","libreoffice","ods").contains(type)) throw new IllegalArgumentException("Format inconnu.");
        if(fin.isBefore(debut)) throw new IllegalArgumentException("Période invalide.");
        var joueurs=service.suivi(debut,fin,selection);
        List<String> noms=new ArrayList<>();
        for(var liste:service.listes()) if(selection.contains(((Number)liste.get("id")).longValue())) noms.add((String)liste.get("nom"));
        String contexte="Du "+debut+" au "+fin+" · Listes autorisées : "+(noms.isEmpty()?"aucune":String.join(", ",noms));
        if(type.equals("csv")) {
            var rows=matrix(joueurs,null);
            StringBuilder csv=new StringBuilder("\uFEFF");
            csvRow(csv,List.of("Suivi des consignes — "+contexte));
            csvRow(csv,List.of("Les cas incertains sont marqués À vérifier dans les cellules. Une cellule vide signifie aucun upgrade détecté ; historique insuffisant signalé à côté du nom."));
            for(var row:rows)csvRow(csv,row);
            return new ExportResult("csv","text/csv; charset=UTF-8",csv.toString().getBytes(StandardCharsets.UTF_8));
        }
        return new ExportResult("ods","application/vnd.oasis.opendocument.spreadsheet",ods(joueurs,contexte));
    }
    @SuppressWarnings("unchecked")
    private List<List<String>> matrix(List<Map<String,Object>> joueurs,String statut) {
        Map<String,String> noms=new HashMap<>();
        for(var joueur:joueurs)for(var d:(List<Map<String,Object>>)joueur.get("details"))
            if(!"Autorisé".equals(d.get("statut")) && (statut==null || statut.equals(d.get("statut"))))noms.put((String)d.get("base_id"),(String)d.get("nom"));
        List<String> bases=new ArrayList<>(noms.keySet());bases.sort(Comparator.comparing((String b)->noms.get(b),String.CASE_INSENSITIVE_ORDER).thenComparing(b->b));
        List<List<String>> rows=new ArrayList<>();List<String> head=new ArrayList<>();head.add("Joueur");for(String base:bases)head.add(noms.get(base));if(bases.isEmpty())head.add("Aucun personnage concerné");rows.add(head);
        DateTimeFormatter fmt=DateTimeFormatter.ofPattern("dd/MM HH:mm");
        for(var joueur:joueurs) {
            Map<String,List<String>> values=new HashMap<>();
            for(var d:(List<Map<String,Object>>)joueur.get("details"))if(!"Autorisé".equals(d.get("statut")) && (statut==null || statut.equals(d.get("statut")))) {
                String prefix=statut==null ? d.get("statut")+" : " : "";
                String value=prefix+d.get("progression")+" ("+((LocalDateTime)d.get("depuis")).format(fmt)+" → "+((LocalDateTime)d.get("capture")).format(fmt)+")";
                values.computeIfAbsent((String)d.get("base_id"),b->new ArrayList<>()).add(value);
            }
            List<String> row=new ArrayList<>();row.add(joueur.get("nom")+(joueur.get("reference")==null?"\nHistorique insuffisant":""));
            for(String base:bases)row.add(String.join("\n",values.getOrDefault(base,List.of())));if(bases.isEmpty())row.add("");rows.add(row);
        }
        return rows;
    }
    private static void csvRow(StringBuilder out,List<String> cells) {
        for(int i=0;i<cells.size();i++) {
            if(i>0)out.append(';');String value=cells.get(i);
            // Neutralise les noms commençant par une formule lors de l'import CSV.
            if(value.stripLeading().matches("(?s)^[=+@\\-].*"))value="'"+value;
            out.append('"').append(value.replace("\"","\"\"")).append('"');
        }
        out.append("\r\n");
    }
    private byte[] ods(List<Map<String,Object>> joueurs,String contexte)throws IOException {
        StringBuilder content=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><office:document-content xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\" xmlns:table=\"urn:oasis:names:tc:opendocument:xmlns:table:1.0\" xmlns:text=\"urn:oasis:names:tc:opendocument:xmlns:text:1.0\" xmlns:style=\"urn:oasis:names:tc:opendocument:xmlns:style:1.0\" xmlns:fo=\"urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0\" office:version=\"1.2\"><office:automatic-styles>");
        content.append("<style:style style:name=\"col\" style:family=\"table-column\"><style:table-column-properties style:column-width=\"8cm\"/></style:style><style:style style:name=\"body\" style:family=\"table-cell\"><style:table-cell-properties fo:wrap-option=\"wrap\" fo:padding=\"0.12cm\" fo:border=\"0.02cm solid #e2e8f0\"/><style:text-properties fo:font-size=\"10pt\"/></style:style><style:style style:name=\"head\" style:family=\"table-cell\"><style:table-cell-properties fo:background-color=\"#4338ca\" fo:padding=\"0.15cm\" fo:wrap-option=\"wrap\"/><style:text-properties fo:color=\"#ffffff\" fo:font-weight=\"bold\"/></style:style></office:automatic-styles><office:body><office:spreadsheet>");
        for(String status:List.of("Hors consignes","À vérifier")) {
            var rows=matrix(joueurs,status);int cols=rows.get(0).size();
            content.append("<table:table table:name=\"").append(escape(status)).append("\"><table:table-column table:style-name=\"col\" table:number-columns-repeated=\"").append(cols).append("\"/>");
            odsMerged(content,status+" — Suivi des consignes",cols,"head");odsMerged(content,contexte,cols,"body");
            odsMerged(content,status.equals("À vérifier")?"Intervalle chevauchant le début des consignes : date réelle inconnue.":"Écarts confirmés. Cas incertains dans l’onglet À vérifier. Cellule vide : aucune évolution détectée. Historique insuffisant signalé dans le nom.",cols,"body");
            content.append("<table:table-header-rows>");odsRow(content,rows.get(0),"head");content.append("</table:table-header-rows>");
            for(int i=1;i<rows.size();i++)odsRow(content,rows.get(i),"body");content.append("</table:table>");
        }
        content.append("</office:spreadsheet></office:body></office:document-content>");
        try(var out=new ByteArrayOutputStream();var zip=new ZipOutputStream(out,StandardCharsets.UTF_8)) {
            byte[] mime="application/vnd.oasis.opendocument.spreadsheet".getBytes(StandardCharsets.UTF_8);CRC32 crc=new CRC32();crc.update(mime);
            ZipEntry first=new ZipEntry("mimetype");first.setMethod(ZipEntry.STORED);first.setSize(mime.length);first.setCompressedSize(mime.length);first.setCrc(crc.getValue());zip.putNextEntry(first);zip.write(mime);zip.closeEntry();
            entry(zip,"META-INF/manifest.xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><manifest:manifest xmlns:manifest=\"urn:oasis:names:tc:opendocument:xmlns:manifest:1.0\" manifest:version=\"1.2\"><manifest:file-entry manifest:full-path=\"/\" manifest:media-type=\"application/vnd.oasis.opendocument.spreadsheet\"/><manifest:file-entry manifest:full-path=\"content.xml\" manifest:media-type=\"text/xml\"/></manifest:manifest>");
            entry(zip,"content.xml",content.toString());zip.finish();return out.toByteArray();
        }
    }
    private static void odsMerged(StringBuilder xml,String value,int cols,String style){xml.append("<table:table-row><table:table-cell table:style-name=\"").append(style).append("\" office:value-type=\"string\" table:number-columns-spanned=\"").append(cols).append("\">");odsParagraphs(xml,value);xml.append("</table:table-cell><table:covered-table-cell table:number-columns-repeated=\"").append(cols-1).append("\"/></table:table-row>");}
    private static void odsRow(StringBuilder xml,List<String> values,String style){xml.append("<table:table-row>");for(String value:values){xml.append("<table:table-cell table:style-name=\"").append(style).append("\" office:value-type=\"string\">");odsParagraphs(xml,value);xml.append("</table:table-cell>");}xml.append("</table:table-row>");}
    private static void odsParagraphs(StringBuilder xml,String value){for(String line:value.split("\n",-1))xml.append("<text:p>").append(escape(line)).append("</text:p>");}
    @SuppressWarnings("unchecked")
    private String sheet(List<Map<String,Object>> joueurs,String statut,String contexte) {
        Map<String,String> noms=new HashMap<>();
        for(var joueur:joueurs) for(var d:(List<Map<String,Object>>)joueur.get("details")) if(statut.equals(d.get("statut"))) noms.put((String)d.get("base_id"),(String)d.get("nom"));
        List<String> bases=new ArrayList<>(noms.keySet());bases.sort(Comparator.comparing((String b)->noms.get(b),String.CASE_INSENSITIVE_ORDER).thenComparing(b->b));
        int cols=Math.max(2,bases.size()+1),last=joueurs.size()+4;
        StringBuilder xml=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">");
        xml.append("<dimension ref=\"A1:").append(cell(cols-1,last)).append("\"/><sheetViews><sheetView workbookViewId=\"0\"><pane xSplit=\"1\" ySplit=\"4\" topLeftCell=\"B5\" activePane=\"bottomRight\" state=\"frozen\"/></sheetView></sheetViews><sheetFormatPr defaultRowHeight=\"22\"/><cols><col min=\"1\" max=\"1\" width=\"30\" customWidth=\"1\"/><col min=\"2\" max=\"").append(cols).append("\" width=\"46\" customWidth=\"1\"/></cols><sheetData>");
        xml.append("<row r=\"1\" ht=\"30\" customHeight=\"1\">").append(text(0,1,statut+" — Suivi des consignes",1)).append("</row>");
        xml.append("<row r=\"2\" ht=\"44\" customHeight=\"1\">").append(text(0,2,contexte,7)).append("</row>");
        String note=statut.equals("À vérifier")?"Intervalle chevauchant le début des consignes : date réelle de l’upgrade inconnue.":"Écarts confirmés uniquement. Les cas incertains sont dans l’onglet À vérifier.";
        note+=" Tous les membres sont inclus ; une cellule vide signifie aucun upgrade détecté pour ce personnage. La première colonne indique les historiques insuffisants.";
        xml.append("<row r=\"3\" ht=\"60\" customHeight=\"1\">").append(text(0,3,note,7)).append("</row><row r=\"4\" ht=\"38\" customHeight=\"1\">").append(text(0,4,"Joueur",6));
        if(bases.isEmpty()) xml.append(text(1,4,"Aucun personnage concerné",6));
        for(int c=0;c<bases.size();c++)xml.append(text(c+1,4,noms.get(bases.get(c)),6));xml.append("</row>");
        int row=5;DateTimeFormatter fmt=DateTimeFormatter.ofPattern("dd/MM HH:mm");
        for(var joueur:joueurs) {
            Map<String,List<String>> upgrades=new HashMap<>();
            for(var d:(List<Map<String,Object>>)joueur.get("details")) if(statut.equals(d.get("statut"))) {
                String intervalle=((LocalDateTime)d.get("depuis")).format(fmt)+" → "+((LocalDateTime)d.get("capture")).format(fmt);
                upgrades.computeIfAbsent((String)d.get("base_id"),b->new ArrayList<>()).add(d.get("progression")+" ("+intervalle+")");
            }
            int lines=1;for(var values:upgrades.values()) {int n=0;for(String v:values)n+=Math.max(1,(v.length()+42)/43);lines=Math.max(lines,n);}
            xml.append("<row r=\"").append(row).append("\" ht=\"").append(Math.min(409,Math.max(42,lines*18+12))).append("\" customHeight=\"1\">");
            String label=(String)joueur.get("nom");if(joueur.get("reference")==null)label+="\nHistorique insuffisant";
            xml.append(text(0,row,label,7));
            for(int c=0;c<bases.size();c++)xml.append(text(c+1,row,String.join("\n",upgrades.getOrDefault(bases.get(c),List.of())),7));
            xml.append("</row>");row++;
        }
        xml.append("</sheetData><autoFilter ref=\"A4:").append(cell(cols-1,Math.max(4,last))).append("\"/><mergeCells count=\"3\">");
        for(int r=1;r<=3;r++)xml.append("<mergeCell ref=\"A").append(r).append(":").append(cell(cols-1,r)).append("\"/>");
        return xml.append("</mergeCells></worksheet>").toString();
    }
    private static String cell(int c,int r){String s="";for(c++;c>0;c=(c-1)/26)s=(char)('A'+(c-1)%26)+s;return s+r;}
    private static String text(int c,int r,String value,int style){return "<c r=\""+cell(c,r)+"\" s=\""+style+"\" t=\"inlineStr\"><is><t xml:space=\"preserve\">"+escape(value)+"</t></is></c>";}
    private static String escape(String s){return s.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "").replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}
    private static void entry(ZipOutputStream zip,String path,String content)throws IOException{zip.putNextEntry(new ZipEntry(path));zip.write(content.getBytes(StandardCharsets.UTF_8));zip.closeEntry();}
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
                    <sheet name="Hors consignes" sheetId="1" r:id="rId1"/>
                    <sheet name="À vérifier" sheetId="2" r:id="rId2"/>
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
}
