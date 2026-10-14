package fr.xalkinn.swgohmanager.service;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import org.springframework.stereotype.Service;
@Service
public class HistoriqueConsigneExportService {
    public record Result(String extension,String contentType,byte[] content){}
    public Result exporter(String format,List<Map<String,Object>> events,List<Map<String,Object>> resume,String contexte)throws IOException {
        List<List<String>> detail=new ArrayList<>();detail.add(List.of("Joueur","Campagne","Listes autorisées figées","Personnage","Progression","Capture précédente","Capture de détection","Intervalle incertain","Statut","Motif / commentaire","Date de décision"));
        for(var e:events)detail.add(List.of(value(e.get("joueur_nom")),value(e.get("campagne")),value(e.get("listes_snapshot")),value(e.get("personnage")),value(e.get("progression")),value(e.get("depuis")),value(e.get("capture")),Boolean.TRUE.equals(e.get("incertain"))?"Oui":"Non",label((String)e.get("statut")),value(e.get("motif")),value(e.get("date_decision"))));
        List<List<String>> summary=new ArrayList<>();summary.add(List.of("Joueur","Écarts confirmés","À examiner","Exemptés","Dernier écart confirmé"));
        for(var r:resume)summary.add(List.of(value(r.get("nom")),value(r.get("confirmes")),value(r.get("aExaminer")),value(r.get("exemptes")),value(r.get("dernier"))));
        format=format.toLowerCase(Locale.ROOT);
        if(format.equals("csv")) {
            StringBuilder csv=new StringBuilder("\uFEFF");
            csvRow(csv,List.of(contexte));for(var row:detail)csvRow(csv,row);
            return new Result("csv","text/csv; charset=UTF-8",csv.toString().getBytes(StandardCharsets.UTF_8));
        }
        if(format.equals("libreoffice")||format.equals("ods"))return new Result("ods","application/vnd.oasis.opendocument.spreadsheet",ods(summary,detail,contexte));
        if(!Set.of("excel","xlsx").contains(format))throw new IllegalArgumentException("Format inconnu.");
        try(var out=new ByteArrayOutputStream();var zip=new ZipOutputStream(out,StandardCharsets.UTF_8)) {
            entry(zip,"[Content_Types].xml",contentTypesXml());entry(zip,"_rels/.rels",rootRelationshipsXml());entry(zip,"xl/workbook.xml",workbookXml());entry(zip,"xl/_rels/workbook.xml.rels",workbookRelationshipsXml());entry(zip,"xl/styles.xml",stylesXml());
            entry(zip,"xl/worksheets/sheet1.xml",sheet(summary,contexte));entry(zip,"xl/worksheets/sheet2.xml",sheet(detail,contexte));zip.finish();return new Result("xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",out.toByteArray());
        }
    }
    private static String value(Object o){return o==null?"":o.toString();}
    private static String label(String s){return "CONFIRME".equals(s)?"Écart confirmé":"EXEMPTE".equals(s)?"Exempté":"À examiner";}
    private static void csvRow(StringBuilder csv,List<String> row){for(int c=0;c<row.size();c++){if(c>0)csv.append(';');String v=row.get(c);if(v.stripLeading().matches("(?s)^[=+@\\-].*"))v="'"+v;csv.append('"').append(v.replace("\"","\"\"")).append('"');}csv.append("\r\n");}
    private String sheet(List<List<String>> rows,String contexte){
        int cols=rows.get(0).size();StringBuilder xml=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetViews><sheetView workbookViewId=\"0\"><pane xSplit=\"1\" ySplit=\"2\" topLeftCell=\"B3\" activePane=\"bottomRight\" state=\"frozen\"/></sheetView></sheetViews><sheetFormatPr defaultRowHeight=\"40\"/><cols><col min=\"1\" max=\"");
        xml.append(cols).append("\" width=\"32\" customWidth=\"1\"/></cols><sheetData><row r=\"1\" ht=\"44\" customHeight=\"1\">").append(text(0,1,contexte,7)).append("</row>");int r=2;
        for(var row:rows){int lines=2;for(String v:row)lines=Math.max(lines,(v.length()+28)/29);xml.append("<row r=\"").append(r).append("\" ht=\"").append(Math.min(409,lines*16+12)).append("\" customHeight=\"1\">");for(int c=0;c<row.size();c++)xml.append(text(c,r,row.get(c),r==2?6:7));xml.append("</row>");r++;}
        return xml.append("</sheetData><autoFilter ref=\"A2:").append(cell(cols-1,r-1)).append("\"/><mergeCells count=\"1\"><mergeCell ref=\"A1:").append(cell(cols-1,1)).append("\"/></mergeCells></worksheet>").toString();
    }
    private byte[] ods(List<List<String>> summary,List<List<String>> detail,String contexte)throws IOException {
        StringBuilder xml=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><office:document-content xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\" xmlns:table=\"urn:oasis:names:tc:opendocument:xmlns:table:1.0\" xmlns:text=\"urn:oasis:names:tc:opendocument:xmlns:text:1.0\" xmlns:style=\"urn:oasis:names:tc:opendocument:xmlns:style:1.0\" xmlns:fo=\"urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0\" office:version=\"1.2\"><office:automatic-styles><style:style style:name=\"col\" style:family=\"table-column\"><style:table-column-properties style:column-width=\"6cm\"/></style:style><style:style style:name=\"cell\" style:family=\"table-cell\"><style:table-cell-properties fo:wrap-option=\"wrap\" fo:padding=\"0.12cm\" fo:border=\"0.02cm solid #e2e8f0\"/></style:style><style:style style:name=\"head\" style:family=\"table-cell\"><style:table-cell-properties fo:background-color=\"#4338ca\" fo:wrap-option=\"wrap\"/><style:text-properties fo:color=\"#ffffff\" fo:font-weight=\"bold\"/></style:style></office:automatic-styles><office:body><office:spreadsheet>");
        int index=0;for(var rows:List.of(summary,detail)) {
            xml.append("<table:table table:name=\"").append(index++==0?"Résumé":"Historique").append("\"><table:table-column table:style-name=\"col\" table:number-columns-repeated=\"").append(rows.get(0).size()).append("\"/>");odsRow(xml,List.of(contexte),"cell");xml.append("<table:table-header-rows>");odsRow(xml,rows.get(0),"head");xml.append("</table:table-header-rows>");for(int i=1;i<rows.size();i++)odsRow(xml,rows.get(i),"cell");xml.append("</table:table>");
        }
        xml.append("</office:spreadsheet></office:body></office:document-content>");
        try(var out=new ByteArrayOutputStream();var zip=new ZipOutputStream(out,StandardCharsets.UTF_8)) {
            byte[] mime="application/vnd.oasis.opendocument.spreadsheet".getBytes(StandardCharsets.UTF_8);CRC32 crc=new CRC32();crc.update(mime);ZipEntry first=new ZipEntry("mimetype");first.setMethod(ZipEntry.STORED);first.setSize(mime.length);first.setCompressedSize(mime.length);first.setCrc(crc.getValue());zip.putNextEntry(first);zip.write(mime);zip.closeEntry();
            entry(zip,"META-INF/manifest.xml","<?xml version=\"1.0\"?><manifest:manifest xmlns:manifest=\"urn:oasis:names:tc:opendocument:xmlns:manifest:1.0\" manifest:version=\"1.2\"><manifest:file-entry manifest:full-path=\"/\" manifest:media-type=\"application/vnd.oasis.opendocument.spreadsheet\"/><manifest:file-entry manifest:full-path=\"content.xml\" manifest:media-type=\"text/xml\"/></manifest:manifest>");entry(zip,"content.xml",xml.toString());zip.finish();return out.toByteArray();
        }
    }
    private static void odsRow(StringBuilder xml,List<String> row,String style){xml.append("<table:table-row>");for(String v:row){xml.append("<table:table-cell table:style-name=\"").append(style).append("\" office:value-type=\"string\">");for(String line:v.split("\n",-1))xml.append("<text:p>").append(escape(line)).append("</text:p>");xml.append("</table:table-cell>");}xml.append("</table:table-row>");}
    private static String cell(int c,int r){String s="";for(c++;c>0;c=(c-1)/26)s=(char)('A'+(c-1)%26)+s;return s+r;}
    private static String text(int c,int r,String v,int style){return "<c r=\""+cell(c,r)+"\" s=\""+style+"\" t=\"inlineStr\"><is><t xml:space=\"preserve\">"+escape(v)+"</t></is></c>";}
    private static String escape(String v){return v.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "").replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}
    private static void entry(ZipOutputStream zip,String path,String data)throws IOException{zip.putNextEntry(new ZipEntry(path));zip.write(data.getBytes(StandardCharsets.UTF_8));zip.closeEntry();}
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
                    <sheet name="Résumé" sheetId="1" r:id="rId1"/>
                    <sheet name="Historique" sheetId="2" r:id="rId2"/>
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