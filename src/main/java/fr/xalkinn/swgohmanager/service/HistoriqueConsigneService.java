package fr.xalkinn.swgohmanager.service;

import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;

@Service
public class HistoriqueConsigneService {
    private final JdbcTemplate jdbc;
    private final ConsigneService consignes;
    private volatile String derniereErreur;
    public HistoriqueConsigneService(JdbcTemplate jdbc,ConsigneService consignes) {
        this.jdbc=jdbc;this.consignes=consignes;
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS consigne_campagne (
                id BIGINT AUTO_INCREMENT PRIMARY KEY, nom VARCHAR(120) NOT NULL,
                debut DATETIME NOT NULL, fin DATETIME NULL, listes_snapshot TEXT NOT NULL,
                bases_snapshot TEXT NOT NULL, creation DATETIME DEFAULT CURRENT_TIMESTAMP)
                ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS consigne_historique (
                id BIGINT AUTO_INCREMENT PRIMARY KEY, campagne_id BIGINT NOT NULL,
                evolution VARCHAR(80) NOT NULL, joueur_id INT NOT NULL, joueur_nom VARCHAR(255) NOT NULL,
                base_id VARCHAR(128) NOT NULL, personnage VARCHAR(255) NOT NULL,
                progression TEXT NOT NULL, depuis DATETIME NOT NULL, capture DATETIME NOT NULL,
                incertain BOOLEAN NOT NULL, statut VARCHAR(20) NOT NULL DEFAULT 'A_EXAMINER',
                motif VARCHAR(1000) NOT NULL DEFAULT '', date_decision DATETIME NULL,
                UNIQUE KEY uq_consigne_evenement(campagne_id,evolution),
                KEY idx_consigne_capture(capture),KEY idx_consigne_joueur(joueur_id),
                FOREIGN KEY(campagne_id) REFERENCES consigne_campagne(id))
                ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS consigne_historique_decision (
                id BIGINT AUTO_INCREMENT PRIMARY KEY, evenement_id BIGINT NOT NULL,
                statut VARCHAR(20) NOT NULL, motif VARCHAR(1000) NOT NULL,
                date_decision DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY(evenement_id) REFERENCES consigne_historique(id))
                ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
    }
    public String erreur() { return derniereErreur; }
    public List<Map<String,Object>> campagnes() { return jdbc.queryForList("SELECT * FROM consigne_campagne ORDER BY debut DESC,id DESC"); }
    @Transactional
    public synchronized long creer(String nom,LocalDateTime debut,List<Long> selection) {
        nom=nom.trim();
        if(nom.isEmpty()||nom.length()>120)throw new IllegalArgumentException("Nom de campagne requis (120 caractères maximum).");
        if(debut.isAfter(maintenant()))throw new IllegalArgumentException("Le début ne peut pas être dans le futur.");
        if(selection.isEmpty())throw new IllegalArgumentException("Sélectionne au moins une liste de personnages autorisés.");
        for(var c:campagnes()) {
            if(c.get("fin")==null)throw new IllegalArgumentException("Clôture d’abord la campagne active avant de changer de consignes.");
            if(debut.isBefore(time(c.get("fin"))))throw new IllegalArgumentException("Les campagnes ne doivent pas se chevaucher.");
        }
        Set<Long> trouves=new HashSet<>();Set<String> bases=new TreeSet<>();List<String> noms=new ArrayList<>();
        for(var liste:consignes.listes())if(selection.contains(((Number)liste.get("id")).longValue())) {
            trouves.add(((Number)liste.get("id")).longValue());noms.add((String)liste.get("nom"));
            @SuppressWarnings("unchecked") List<String> persos=(List<String>)liste.get("personnages");bases.addAll(persos);
        }
        if(!trouves.containsAll(selection)||bases.isEmpty())throw new IllegalArgumentException("Les listes doivent exister et contenir des personnages.");
        var holder=new org.springframework.jdbc.support.GeneratedKeyHolder();final String titre=nom;
        jdbc.update(c->{var ps=c.prepareStatement("INSERT INTO consigne_campagne(nom,debut,listes_snapshot,bases_snapshot) VALUES (?,?,?,?)",new String[]{"id"});ps.setString(1,titre);ps.setTimestamp(2,java.sql.Timestamp.valueOf(debut));ps.setString(3,String.join(", ",noms));ps.setString(4,String.join("\n",bases));return ps;},holder);
        return Objects.requireNonNull(holder.getKey()).longValue();
    }
    @Transactional
    public void cloturer(long id) {
        if(jdbc.update("UPDATE consigne_campagne SET fin=? WHERE id=? AND fin IS NULL",maintenant(),id)!=1)throw new IllegalArgumentException("Campagne active introuvable.");
    }
    @Scheduled(initialDelay=15000,fixedDelay=600000)
    public void analyserAutomatiquement() {
        try { synchroniser();derniereErreur=null; }
        catch(RuntimeException e) { derniereErreur="L’actualisation automatique a échoué. Réessaie avec Actualiser l’historique.";java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.WARNING,"Historique consignes",e); }
    }
    public synchronized int synchroniser() {
        int nouveaux=0;
        for(var c:campagnes()) {
            long id=((Number)c.get("id")).longValue();LocalDateTime debut=time(c.get("debut"));LocalDateTime fin=c.get("fin")==null?maintenant():time(c.get("fin"));
            Set<String> bases=new HashSet<>(Arrays.asList(((String)c.get("bases_snapshot")).split("\n")));
            Set<String> connus=new HashSet<>(jdbc.queryForList("SELECT evolution FROM consigne_historique WHERE campagne_id=?",String.class,id));
            var joueurs=consignes.suiviArchive(debut.toLocalDate(),fin.toLocalDate(),bases);
            for(var joueur:joueurs) {
                @SuppressWarnings("unchecked") var details=(List<Map<String,Object>>)joueur.get("details");
                for(var d:details) {
                    LocalDateTime capture=time(d.get("capture"));
                    if("Autorisé".equals(d.get("statut"))||capture.isBefore(debut)||capture.isAfter(fin)||connus.contains((String)d.get("evolution")))continue;
                    try {
                        jdbc.update("""
                            INSERT INTO consigne_historique(campagne_id,evolution,joueur_id,joueur_nom,base_id,personnage,progression,depuis,capture,incertain)
                            VALUES (?,?,?,?,?,?,?,?,?,?)
                            """,id,d.get("evolution"),joueur.get("id"),joueur.get("nom"),d.get("base_id"),d.get("nom"),d.get("progression"),d.get("depuis"),capture,time(d.get("depuis")).isBefore(debut));
                        nouveaux++;
                    } catch(org.springframework.dao.DuplicateKeyException e) { /* Autre instance : l'événement existe déjà. */ }
                }
            }
        }
        derniereErreur=null;return nouveaux;
    }
    @Transactional
    public void decider(long evenement,String statut,String motif) {
        if(!Set.of("CONFIRME","EXEMPTE","A_EXAMINER").contains(statut))throw new IllegalArgumentException("Statut inconnu.");
        motif=motif==null?"":motif.trim();
        if(motif.length()>1000)throw new IllegalArgumentException("Commentaire limité à 1000 caractères.");
        if(statut.equals("EXEMPTE")&&motif.isBlank())throw new IllegalArgumentException("Précise le motif d’exemption (jeton R5/R6, autorisation…).");
        if(jdbc.update("UPDATE consigne_historique SET statut=?,motif=?,date_decision=? WHERE id=?",statut,motif,maintenant(),evenement)!=1)throw new IllegalArgumentException("Évolution introuvable.");
        jdbc.update("INSERT INTO consigne_historique_decision(evenement_id,statut,motif) VALUES (?,?,?)",evenement,statut,motif);
    }
    public List<Map<String,Object>> evenements(LocalDate debut,LocalDate fin,Long campagne,String joueur,String personnage,String statut) {
        if(fin.isBefore(debut))throw new IllegalArgumentException("Période invalide.");
        String sql="SELECT h.*,c.nom AS campagne,c.listes_snapshot FROM consigne_historique h JOIN consigne_campagne c ON c.id=h.campagne_id WHERE h.capture>=? AND h.capture<?";
        List<Object> args=new ArrayList<>(List.of(debut.atStartOfDay(),fin.plusDays(1).atStartOfDay()));
        if(campagne!=null){sql+=" AND h.campagne_id=?";args.add(campagne);}
        if(joueur!=null&&!joueur.isBlank()){sql+=" AND LOWER(h.joueur_nom) LIKE ?";args.add("%"+joueur.toLowerCase(Locale.ROOT)+"%");}
        if(personnage!=null&&!personnage.isBlank()){sql+=" AND LOWER(h.personnage) LIKE ?";args.add("%"+personnage.toLowerCase(Locale.ROOT)+"%");}
        if(statut!=null&&!statut.isBlank()){if(!Set.of("CONFIRME","EXEMPTE","A_EXAMINER").contains(statut))throw new IllegalArgumentException("Statut inconnu.");sql+=" AND h.statut=?";args.add(statut);}
        return jdbc.queryForList(sql+" ORDER BY h.capture DESC,h.id DESC",args.toArray());
    }
    public List<Map<String,Object>> resume(List<Map<String,Object>> events) {
        Map<Integer,Map<String,Object>> result=new LinkedHashMap<>();
        for(var e:events) {
            int id=((Number)e.get("joueur_id")).intValue();var r=result.computeIfAbsent(id,k->{Map<String,Object> m=new HashMap<>();m.put("id",k);m.put("nom",e.get("joueur_nom"));m.put("confirmes",0);m.put("aExaminer",0);m.put("exemptes",0);return m;});
            String field=e.get("statut").equals("CONFIRME")?"confirmes":e.get("statut").equals("EXEMPTE")?"exemptes":"aExaminer";
            r.put(field,((Number)r.get(field)).intValue()+1);
            if(e.get("statut").equals("CONFIRME")&&r.get("dernier")==null)r.put("dernier",time(e.get("capture")));
        }
        var rows=new ArrayList<>(result.values());rows.sort(Comparator.comparingInt((Map<String,Object> r)->((Number)r.get("confirmes")).intValue()).reversed().thenComparing(r->(String)r.get("nom")));return rows;
    }
    public List<Map<String,Object>> decisions(long id){return jdbc.queryForList("SELECT * FROM consigne_historique_decision WHERE evenement_id=? ORDER BY id DESC",id);}
    public static LocalDateTime time(Object v){return v instanceof java.sql.Timestamp t?t.toLocalDateTime():(LocalDateTime)v;}
    public static LocalDateTime maintenant(){return LocalDateTime.now(ZoneId.of("Europe/Paris"));}
}
