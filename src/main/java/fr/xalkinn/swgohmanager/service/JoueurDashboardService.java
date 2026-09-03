package fr.xalkinn.swgohmanager.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import fr.xalkinn.swgohmanager.dto.JoueurDashboard;
import fr.xalkinn.swgohmanager.dto.JoueurDashboard.GearStat;
import fr.xalkinn.swgohmanager.dto.JoueurDashboard.OmicronInfo;
import fr.xalkinn.swgohmanager.dto.JoueurDashboard.OmicronPriorityDetail;
import fr.xalkinn.swgohmanager.dto.JoueurDashboard.OmicronPrioritySummary;
import fr.xalkinn.swgohmanager.dto.JoueurDashboard.RelicStat;
import fr.xalkinn.swgohmanager.dto.JoueurDashboard.RosterCharacter;

@Service
public class JoueurDashboardService {

    private final JdbcTemplate jdbc;
    private final PersonnageDefinitionService personnageDefinitionService;

    public JoueurDashboardService(
            JdbcTemplate jdbc,
            PersonnageDefinitionService personnageDefinitionService) {
        this.jdbc = jdbc;
        this.personnageDefinitionService = personnageDefinitionService;
    }

    public JoueurDashboard getDashboard(Integer joueurId) {
        if (joueurId == null) return null;

        List<Map<String,Object>> joueurs = jdbc.queryForList("""
            SELECT id, player_id, ally_code, nom, pg_perso
            FROM joueur
            WHERE id = ? AND dans_guilde = 1
            """, joueurId);

        if (joueurs.isEmpty()) return null;

        Map<String,Object> j = joueurs.get(0);
        JoueurDashboard d = new JoueurDashboard();
        d.setId(((Number) j.get("id")).intValue());
        d.setPlayerId((String) j.get("player_id"));
        d.setAllyCode((String) j.get("ally_code"));
        d.setNom((String) j.get("nom"));
        d.setPgPerso(j.get("pg_perso") == null ? 0L : ((Number) j.get("pg_perso")).longValue());

        String playerId = d.getPlayerId();
        String allyCode = d.getAllyCode();
        Map<String, String> nomsFrancais =
                personnageDefinitionService.chargerIndexNomsFrancais();

        List<Map<String,Object>> rosterRows = jdbc.queryForList("""
            SELECT nom, base_id, etoiles, niveau, gear, relic, date_capture
            FROM personnage
            WHERE joueur_id = ?
              AND extraction_id = (
                  SELECT MAX(p2.extraction_id)
                  FROM personnage p2
                  WHERE p2.joueur_id = ?
              )
            ORDER BY nom
            """, joueurId, joueurId);

        List<RosterCharacter> roster = new ArrayList<>();
        LocalDateTime latestCapture = null;
        int stars7 = 0, g13 = 0, relics = 0;
        Map<Integer,Integer> gearCounts = new LinkedHashMap<>();
        Map<Integer,Integer> relicCounts = new LinkedHashMap<>();

        for (Map<String,Object> r : rosterRows) {
            int stars = number(r.get("etoiles"));
            int level = number(r.get("niveau"));
            int gear = number(r.get("gear"));
            int relic = number(r.get("relic"));

            String nomBrut = (String) r.get("nom");
            String baseId = (String) r.get("base_id");
            String nomFrancais = personnageDefinitionService.nomAffiche(
                    nomsFrancais, baseId, nomBrut);

            roster.add(new RosterCharacter(
                nomBrut,
                nomFrancais,
                baseId,
                stars, level, gear, relic
            ));

            if (stars >= 7) stars7++;
            if (gear >= 13) g13++;
            if (relic > 0) relics++;
            gearCounts.merge(gear, 1, Integer::sum);
            if (relic > 0) relicCounts.merge(relic, 1, Integer::sum);

            Object capture = r.get("date_capture");
            if (capture instanceof LocalDateTime dt && (latestCapture == null || dt.isAfter(latestCapture))) {
                latestCapture = dt;
            }
        }

        roster.sort(
                Comparator.comparing(
                        RosterCharacter::getNomFrancais,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
        );

        d.setRoster(roster);
        d.setTotalPersonnages(roster.size());
        d.setPersonnages7Etoiles(stars7);
        d.setPersonnagesG13(g13);
        d.setPersonnagesRelique(relics);
        d.setDerniereCapture(latestCapture == null ? "-" :
            latestCapture.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        List<GearStat> gearStats = new ArrayList<>();
        for (int gear = 1; gear <= 13; gear++) {
            gearStats.add(new GearStat("G" + gear, gearCounts.getOrDefault(gear, 0)));
        }
        d.setGearStats(gearStats);

        List<RelicStat> relicStats = new ArrayList<>();
        for (int relic = 1; relic <= 10; relic++) {
            relicStats.add(new RelicStat("R" + relic, relicCounts.getOrDefault(relic, 0)));
        }
        d.setRelicStats(relicStats);

        // Omicrons réellement posés par le joueur. On conserve les anciens
        // identifiants de secours tant que toutes les données historiques n'ont
        // pas été repassées par le nouveau batch dynamique.
        List<Map<String,Object>> omicronRows = jdbc.queryForList("""
            SELECT personnage, type_capacite, categorie, actif
            FROM omicron
            WHERE joueur_id IN (?, ?, ?)
              AND actif = 1
            ORDER BY categorie, personnage
            """, playerId, allyCode, String.valueOf(joueurId));

        List<OmicronInfo> omicrons = new ArrayList<>();
        for (Map<String,Object> o : omicronRows) {
            String personnageBrut = (String)o.get("personnage");
            String categorie = o.get("categorie") == null ? null : String.valueOf(o.get("categorie"));

            String nomFrancais = personnageDefinitionService.nomAffiche(
                    nomsFrancais,
                    personnageBrut,
                    personnageBrut);

            String typeBrut = o.get("type_capacite") == null ? "" : String.valueOf(o.get("type_capacite"));
            String type = switch (typeBrut.toUpperCase(java.util.Locale.ROOT)) {
                case "CHEF" -> "Chef";
                case "UNIQUE" -> "Unique";
                case "SPÉCIALE", "SPECIALE" -> "Spéciale";
                case "BASIQUE" -> "Basique";
                default -> OmicronCatalogueService.libelleType(typeBrut, null);
            };

            omicrons.add(new OmicronInfo(
                personnageBrut,
                nomFrancais,
                type,
                categorie,
                OmicronCatalogueService.categorieLabel(categorie),
                true
            ));
        }

        omicrons.sort(Comparator
                .comparing(OmicronInfo::getCategorie, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(OmicronInfo::getNomFrancais, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        d.setOmicrons(omicrons);

        // Progression du joueur sur le plan Omicron P1-P4 configuré dans /omicrons.
        List<Map<String,Object>> planRows = jdbc.queryForList("""
            SELECT
                p.priorite,
                d.base_id,
                d.skill_id,
                d.type_capacite,
                d.numero_capacite,
                d.categorie,
                CASE WHEN EXISTS (
                    SELECT 1
                    FROM omicron o
                    WHERE o.joueur_id IN (?, ?, ?)
                      AND o.skill_id = d.skill_id
                      AND o.actif = 1
                ) THEN 1 ELSE 0 END AS possede
            FROM omicron_priorite p
            JOIN omicron_definition d ON d.id = p.omicron_definition_id
            WHERE p.actif = 1
              AND d.actif = 1
            ORDER BY p.priorite, d.categorie, d.base_id, d.type_capacite, d.numero_capacite
            """, playerId, allyCode, String.valueOf(joueurId));

        Map<Integer, OmicronPrioritySummary> priorites = new LinkedHashMap<>();
        for (int priorite = 1; priorite <= 4; priorite++) {
            OmicronPrioritySummary summary = new OmicronPrioritySummary();
            summary.setPriorite(priorite);
            priorites.put(priorite, summary);
        }

        int planAtteint = 0;
        int planTotal = 0;
        for (Map<String,Object> row : planRows) {
            int priorite = number(row.get("priorite"));
            OmicronPrioritySummary summary = priorites.get(priorite);
            if (summary == null) continue;

            summary.setTotal(summary.getTotal() + 1);
            planTotal++;

            boolean possede = number(row.get("possede")) == 1;
            if (possede) {
                summary.setAtteint(summary.getAtteint() + 1);
                planAtteint++;
            } else {
                String baseId = row.get("base_id") == null ? "" : String.valueOf(row.get("base_id"));
                String type = row.get("type_capacite") == null ? "AUTRE" : String.valueOf(row.get("type_capacite"));
                Integer numero = row.get("numero_capacite") == null ? null : number(row.get("numero_capacite"));
                String categorie = row.get("categorie") == null ? null : String.valueOf(row.get("categorie"));
                String nomPersonnage = personnageDefinitionService.nomAffiche(nomsFrancais, baseId, baseId);

                summary.getManquantsDetails().add(new OmicronPriorityDetail(
                        nomPersonnage,
                        OmicronCatalogueService.libelleType(type, numero),
                        categorie,
                        OmicronCatalogueService.categorieLabel(categorie)));
            }
        }

        List<OmicronPrioritySummary> prioritesListe = new ArrayList<>();
        for (OmicronPrioritySummary summary : priorites.values()) {
            summary.setManquants(Math.max(0, summary.getTotal() - summary.getAtteint()));
            summary.setPourcentage(summary.getTotal() > 0
                    ? summary.getAtteint() * 100.0 / summary.getTotal()
                    : 0.0);
            prioritesListe.add(summary);
        }
        d.setOmicronPriorities(prioritesListe);
        d.setOmicronPlanAtteint(planAtteint);
        d.setOmicronPlanTotal(planTotal);
        d.setOmicronPlanPourcentage(planTotal > 0 ? planAtteint * 100.0 / planTotal : 0.0);

        List<Map<String,Object>> modRows = jdbc.queryForList("""
            SELECT vitesse, rarete, type_mod
            FROM mods
            WHERE joueur_id = ?
            """, joueurId);

        int total = modRows.size();
        int noSpeed=0, s1=0, s15=0, s20=0, s25=0;
        int r1=0,r2=0,r3=0,r4=0,r5=0,r6=0,normaux=0,or=0;
        long speedSum=0; int speedCount=0;

        for (Map<String,Object> m : modRows) {
            int speed = number(m.get("vitesse"));
            int rarity = number(m.get("rarete"));
            String type = m.get("type_mod") == null ? "" : String.valueOf(m.get("type_mod"));

            if (speed <= 0) noSpeed++;
            else {
                speedCount++; speedSum += speed;
                if (speed <= 14) s1++;
                else if (speed <= 19) s15++;
                else if (speed <= 24) s20++;
                else s25++;
            }

            switch (rarity) {
                case 1 -> r1++; case 2 -> r2++; case 3 -> r3++;
                case 4 -> r4++; case 5 -> r5++; case 6 -> r6++;
                default -> {}
            }
            if ("OR".equalsIgnoreCase(type)) or++; else normaux++;
        }

        double modQ = d.getPgPerso() > 0
            ? ((((0.8 * s15) + s20 + (1.2 * s25)) / d.getPgPerso()) * 100000)
            : 0.0;

        var ms = d.getModStats();
        ms.setTotal(total); ms.setSansVitesse(noSpeed); ms.setVitesse1a14(s1);
        ms.setVitesse15a19(s15); ms.setVitesse20a24(s20); ms.setVitesse25Plus(s25);
        ms.setRarete1(r1); ms.setRarete2(r2); ms.setRarete3(r3);
        ms.setRarete4(r4); ms.setRarete5(r5); ms.setRarete6(r6);
        ms.setModsNormaux(normaux); ms.setModsOr(or);
        ms.setVitesseMoyenne(speedCount == 0 ? 0.0 : (double)speedSum / speedCount);
        ms.setModQ(modQ);

        return d;
    }

    private int number(Object value) {
        return value == null ? 0 : ((Number)value).intValue();
    }
}
