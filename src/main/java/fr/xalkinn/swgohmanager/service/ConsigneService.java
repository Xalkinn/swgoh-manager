package fr.xalkinn.swgohmanager.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsigneService {
    private final JdbcTemplate jdbc;
    public ConsigneService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        jdbc.execute("CREATE TABLE IF NOT EXISTS consigne_liste (id BIGINT AUTO_INCREMENT PRIMARY KEY, nom VARCHAR(120) NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        jdbc.execute("CREATE TABLE IF NOT EXISTS consigne_personnage (liste_id BIGINT NOT NULL, base_id VARCHAR(128) NOT NULL, PRIMARY KEY(liste_id, base_id), FOREIGN KEY(liste_id) REFERENCES consigne_liste(id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        jdbc.execute("CREATE TABLE IF NOT EXISTS consigne_decision (contexte VARCHAR(64) NOT NULL, evolution VARCHAR(80) NOT NULL, decision VARCHAR(20) NOT NULL, date_decision TIMESTAMP DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY(contexte,evolution)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }
    private static String contexte(LocalDate debut,List<Long> selection,Set<String> bases) {
        String value=debut+"|"+new TreeSet<>(selection)+"|"+new TreeSet<>(bases);
        try { return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch(java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    @Transactional
    public void decider(LocalDate debut,LocalDate fin,List<Long> selection,int joueur,List<String> evolutions,String decision) {
        if(!Set.of("APPROUVER","REFUSER","REINITIALISER").contains(decision))throw new IllegalArgumentException("Décision invalide.");
        var rows=suivi(debut,fin,selection);Map<String,String> valid=new HashMap<>();
        for(var row:rows)if(((Number)row.get("id")).intValue()==joueur) {
            @SuppressWarnings("unchecked") var details=(List<Map<String,Object>>)row.get("details");
            for(var detail:details)if(Boolean.TRUE.equals(detail.get("verifiable")))valid.put((String)detail.get("evolution"),(String)detail.get("contexte"));
        }
        if(evolutions.isEmpty() || !valid.keySet().containsAll(evolutions))throw new IllegalArgumentException("Sélectionne une évolution à vérifier de ce joueur.");
        for(String evolution:new LinkedHashSet<>(evolutions)) {
            String context=valid.get(evolution);
            jdbc.update("DELETE FROM consigne_decision WHERE contexte=? AND evolution=?",context,evolution);
            if(!decision.equals("REINITIALISER"))jdbc.update("INSERT INTO consigne_decision(contexte,evolution,decision) VALUES (?,?,?)",context,evolution,decision);
        }
    }
    public List<Map<String,Object>> catalogue() {
        return jdbc.queryForList("SELECT base_id, COALESCE(NULLIF(nom_francais,''),nom_brut,base_id) AS nom FROM personnage_definition ORDER BY nom");
    }
    public List<Map<String,Object>> listes() {
        List<Map<String,Object>> lists = jdbc.queryForList("SELECT id, nom FROM consigne_liste ORDER BY nom, id");
        for (Map<String,Object> list : lists) list.put("personnages", jdbc.queryForList("SELECT base_id FROM consigne_personnage WHERE liste_id=?", String.class, list.get("id")));
        return lists;
    }
    @Transactional
    public void enregistrer(Long id, String nom, List<String> personnages) {
        nom = nom.trim();
        if (nom.isEmpty() || nom.length()>120) throw new IllegalArgumentException("Le nom doit contenir entre 1 et 120 caractères.");
        Set<String> valid = new HashSet<>();
        for (Map<String,Object> p : catalogue()) valid.add((String)p.get("base_id"));
        if (!valid.containsAll(personnages)) throw new IllegalArgumentException("Personnage inconnu : actualise le catalogue.");
        if (id == null) {
            var holder = new org.springframework.jdbc.support.GeneratedKeyHolder();
            final String name = nom;
            jdbc.update(c -> { var s=c.prepareStatement("INSERT INTO consigne_liste(nom) VALUES (?)", java.sql.Statement.RETURN_GENERATED_KEYS); s.setString(1,name); return s; },holder);
            id = Objects.requireNonNull(holder.getKey()).longValue();
        } else if(jdbc.update("UPDATE consigne_liste SET nom=? WHERE id=?",nom,id)!=1) throw new IllegalArgumentException("Liste introuvable.");
        jdbc.update("DELETE FROM consigne_personnage WHERE liste_id=?",id);
        for (String base : new LinkedHashSet<>(personnages)) jdbc.update("INSERT INTO consigne_personnage(liste_id,base_id) VALUES (?,?)",id,base);
    }
    @Transactional
    public void supprimer(long id) { jdbc.update("DELETE FROM consigne_liste WHERE id=?",id); }

    public List<Map<String,Object>> suivi(LocalDate debut, LocalDate fin, List<Long> selection) {
        Map<Integer,Map<String,Object>> joueurs = new LinkedHashMap<>();
        for (Map<String,Object> j : jdbc.queryForList("SELECT id,nom FROM joueur WHERE dans_guilde=1 ORDER BY nom")) {
            j.put("details",new ArrayList<Map<String,Object>>()); j.put("autorises",0); j.put("ecarts",0); j.put("incertains",0); j.put("captures",0); joueurs.put(((Number)j.get("id")).intValue(),j);
        }
        Set<String> autorises = new HashSet<>();
        for (Map<String,Object> l : listes()) if(selection.contains(((Number)l.get("id")).longValue())) {
            @SuppressWarnings("unchecked") List<String> bases = (List<String>)l.get("personnages"); autorises.addAll(bases);
        }
        String context=contexte(debut,selection,autorises);
        Map<String,String> decisions=new HashMap<>();
        for(var d:jdbc.queryForList("SELECT evolution,decision FROM consigne_decision WHERE contexte=?",context))decisions.put((String)d.get("evolution"),(String)d.get("decision"));
        // Compare les extractions réellement disponibles pour chaque membre, jamais id - 1.
        var snapshots = jdbc.queryForList("""
            SELECT p.joueur_id, p.extraction_id, MIN(p.date_capture) AS debut_capture, MAX(p.date_capture) AS capture
            FROM personnage p JOIN extraction e ON e.id=p.extraction_id
            JOIN joueur j ON j.id=p.joueur_id AND j.dans_guilde=1
            WHERE e.statut='TERMINE' AND p.date_capture < ?
            GROUP BY p.joueur_id,p.extraction_id ORDER BY p.joueur_id,capture,p.extraction_id
            """, fin.plusDays(1).atStartOfDay());
        Map<Integer,Map<String,Object>> previous = new HashMap<>();
        for (Map<String,Object> snap : snapshots) {
            int joueur = ((Number)snap.get("joueur_id")).intValue();
            var summary = joueurs.get(joueur);
            LocalDateTime capture = time(snap.get("capture"));
            if(capture.isBefore(debut.atStartOfDay())) { previous.put(joueur,snap); continue; }
            summary.put("captures",((Number)summary.get("captures")).intValue()+1);
            summary.put("derniereCapture",capture);
            var ancien = previous.put(joueur,snap);
            if(ancien==null) continue; // Premier roster connu : aucun upgrade ne peut être prouvé.
            summary.put("reference", summary.getOrDefault("reference",time(ancien.get("capture"))));
            boolean incertain=time(ancien.get("debut_capture")).isBefore(debut.atStartOfDay());
            var changes = jdbc.queryForList("""
                SELECT n.id AS nouveau_id, SUBSTRING_INDEX(n.base_id,':',1) AS base_id,
                    COALESCE(NULLIF(d.nom_francais,''),n.nom) AS nom,
                    a.id AS ancien_id,a.niveau AS ancien_niveau,n.niveau AS niveau,
                    a.etoiles AS anciennes_etoiles,n.etoiles AS etoiles,
                    a.gear AS ancien_gear,n.gear AS gear,a.relic AS ancienne_relique,n.relic AS relic
                FROM personnage n
                LEFT JOIN personnage a ON a.joueur_id=n.joueur_id AND a.extraction_id=?
                    AND SUBSTRING_INDEX(a.base_id,':',1)=SUBSTRING_INDEX(n.base_id,':',1)
                LEFT JOIN personnage_definition d ON d.base_id=SUBSTRING_INDEX(n.base_id,':',1)
                WHERE n.joueur_id=? AND n.extraction_id=?
                  AND (a.id IS NULL OR n.niveau>a.niveau OR n.etoiles>a.etoiles OR n.gear>a.gear OR n.relic>a.relic)
                ORDER BY nom
                """,ancien.get("extraction_id"),joueur,snap.get("extraction_id"));
            for(var change:changes) {
                boolean allowed=autorises.contains((String)change.get("base_id"));
                change.put("depuis",time(ancien.get("debut_capture"))); change.put("capture",capture);
                String evolution=ancien.get("extraction_id")+":"+change.get("nouveau_id");
                String decision=decisions.get(evolution);
                boolean verifiable=!allowed && incertain;
                String statut=allowed ? "Autorisé" : incertain ? "À vérifier" : "Hors consignes";
                if(verifiable && "APPROUVER".equals(decision))statut="Autorisé";
                if(verifiable && "REFUSER".equals(decision))statut="Hors consignes";
                change.put("statut",statut);change.put("decision",verifiable?decision:null);
                change.put("verifiable",verifiable);change.put("evolution",evolution);change.put("contexte",context);
                change.put("progression",progression(change));
                String compteur=statut.equals("Autorisé") ? "autorises" : statut.equals("À vérifier") ? "incertains" : "ecarts";
                summary.put(compteur,((Number)summary.get(compteur)).intValue()+1);
                @SuppressWarnings("unchecked") var details=(List<Map<String,Object>>)summary.get("details"); details.add(change);
            }
        }
        return new ArrayList<>(joueurs.values());
    }
    private static LocalDateTime time(Object v) { return v instanceof java.sql.Timestamp t ? t.toLocalDateTime() : (LocalDateTime)v; }
    public static String progression(Map<String,Object> c) {
        if(c.get("ancien_id")==null) return "Déblocage / première apparition : niveau "+c.get("niveau")+", "+c.get("etoiles")+" ★, G"+c.get("gear")+", R"+c.get("relic");
        List<String> parts=new ArrayList<>();
        add(parts,c,"ancien_niveau","niveau","Niveau "); add(parts,c,"anciennes_etoiles","etoiles","Étoiles ");
        add(parts,c,"ancien_gear","gear","Gear "); add(parts,c,"ancienne_relique","relic","Relique ");
        return String.join(" · ",parts);
    }
    private static void add(List<String> parts,Map<String,Object> c,String old,String now,String label) {
        if(c.get(old) instanceof Number a && c.get(now) instanceof Number b && b.intValue()>a.intValue()) parts.add(label+a+" → "+b);
    }
}
