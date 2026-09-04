package fr.xalkinn.swgohmanager.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import fr.xalkinn.swgohmanager.dto.DashboardOmicronView.ModeDetail;
import fr.xalkinn.swgohmanager.dto.DashboardOmicronView.OmicronDetail;
import fr.xalkinn.swgohmanager.dto.DashboardOmicronView.PlayerRow;
import fr.xalkinn.swgohmanager.modele.DashboardStats;
import fr.xalkinn.swgohmanager.modele.EvolutionGuilde;
import fr.xalkinn.swgohmanager.repository.JoueurRepository;
import fr.xalkinn.swgohmanager.repository.OmicronRepository;
import fr.xalkinn.swgohmanager.repository.PersonnageRepository;

@Service
public class DashboardService {

    private final PersonnageRepository personnageRepository;
    private final JoueurRepository joueurRepository;
    private final OmicronRepository omicronRepository;
    private final JdbcTemplate jdbc;
    private final LocalizationService localizationService;

    public DashboardService(
            JoueurRepository joueurRepository,
            OmicronRepository omicronRepository,
            PersonnageRepository personnageRepository,
            JdbcTemplate jdbc,
            LocalizationService localizationService) {
        this.joueurRepository = joueurRepository;
        this.omicronRepository = omicronRepository;
        this.personnageRepository = personnageRepository;
        this.jdbc = jdbc;
        this.localizationService = localizationService;
    }

    public DashboardStats getDashboardStats() {

        DashboardStats stats = new DashboardStats();

        stats.setNombreJoueurs(joueurRepository.compterJoueursDansGuilde());
        stats.setNombreOmicrons(nombreOmicronsActifsGuilde());
        stats.setNombrePersonnages((int) personnageRepository.count());
        stats.setDerniereMajOmicrons(omicronRepository.findDerniereMaj());
        stats.setDerniereMajRoster(joueurRepository.findDerniereMaj());
        return stats;
    }
    


    /**
     * Synthèse des Omicrons réellement posés par les membres actuellement
     * présents dans la guilde. Les codes restent techniques dans la Map et
     * sont traduits uniquement dans la vue.
     */
    public Map<String, Integer> getOmicronsParMode() {
        Map<String, Integer> resultat = new LinkedHashMap<>();
        resultat.put("TW", 0);
        resultat.put("TB", 0);
        resultat.put("RAID", 0);
        resultat.put("GAC", 0);

        jdbc.query("""
                SELECT
                    CASE
                        WHEN o.categorie = 'TW' OR o.mode_omicron LIKE 'TERRITORY_WAR%' THEN 'TW'
                        WHEN o.categorie = 'TB' OR o.mode_omicron IN (
                            'TERRITORY_STRIKE_OMICRON',
                            'TERRITORY_COVERT_OMICRON',
                            'TERRITORY_BATTLE_BOTH_OMICRON') THEN 'TB'
                        WHEN o.categorie = 'RAID' OR o.mode_omicron LIKE '%RAID%' THEN 'RAID'
                        WHEN o.categorie = 'GAC' OR o.mode_omicron LIKE 'TERRITORY_TOURNAMENT%' THEN 'GAC'
                        ELSE NULL
                    END AS mode_resume,
                    COUNT(*) AS nb
                FROM omicron o
                JOIN joueur j ON j.player_id = o.joueur_id AND j.dans_guilde = 1
                WHERE o.actif = 1
                GROUP BY mode_resume
                """, rs -> {
                    String mode = rs.getString("mode_resume");
                    if (mode != null && resultat.containsKey(mode)) {
                        resultat.put(mode, rs.getInt("nb"));
                    }
                });

        return resultat;
    }

    /**
     * Détail nominatif utilisé par les popins du dashboard. Les lignes sont
     * regroupées par mode puis par capacité Omicron, avec les membres qui la
     * possèdent actuellement.
     */
    public List<ModeDetail> getOmicronDetailsParMode() {
        Map<String, ModeDetail> modes = new LinkedHashMap<>();
        modes.put("TW", creerMode("TW", "Guerre de territoires"));
        modes.put("TB", creerMode("TB", "Bataille de territoire"));
        modes.put("RAID", creerMode("RAID", "Raid"));
        modes.put("GAC", creerMode("GAC", "Grande Arène"));

        Map<String, OmicronDetail> details = new LinkedHashMap<>();

        jdbc.query("""
                SELECT
                    CASE
                        WHEN o.categorie = 'TW' OR o.mode_omicron LIKE 'TERRITORY_WAR%' THEN 'TW'
                        WHEN o.categorie = 'TB' OR o.mode_omicron IN (
                            'TERRITORY_STRIKE_OMICRON',
                            'TERRITORY_COVERT_OMICRON',
                            'TERRITORY_BATTLE_BOTH_OMICRON') THEN 'TB'
                        WHEN o.categorie = 'RAID' OR o.mode_omicron LIKE '%RAID%' THEN 'RAID'
                        WHEN o.categorie = 'GAC' OR o.mode_omicron LIKE 'TERRITORY_TOURNAMENT%' THEN 'GAC'
                        ELSE NULL
                    END AS mode_resume,
                    o.personnage AS base_id,
                    o.skill_id,
                    o.type_capacite AS type_capacite_posee,
                    d.personnage_name_key,
                    d.skill_name_key,
                    d.type_capacite AS type_capacite_definition,
                    d.numero_capacite,
                    j.player_id,
                    COALESCE(j.nom, o.nom_joueur, j.player_id) AS player_name
                FROM omicron o
                JOIN joueur j
                  ON j.player_id = o.joueur_id
                 AND j.dans_guilde = 1
                LEFT JOIN omicron_definition d
                  ON d.base_id = o.personnage
                 AND d.skill_id = o.skill_id
                 AND d.actif = 1
                WHERE o.actif = 1
                  AND o.skill_id IS NOT NULL
                ORDER BY mode_resume, o.personnage, o.skill_id, player_name
                """, rs -> {
                    String modeCode = rs.getString("mode_resume");
                    ModeDetail mode = modes.get(modeCode);
                    if (mode == null) return;

                    mode.setTotalPoses(mode.getTotalPoses() + 1);

                    String baseId = rs.getString("base_id");
                    String skillId = rs.getString("skill_id");
                    String key = modeCode + "|" + baseId + "|" + skillId;

                    OmicronDetail detail = details.get(key);
                    if (detail == null) {
                        detail = new OmicronDetail();
                        detail.setBaseId(baseId);
                        detail.setSkillId(skillId);
                        detail.setNomPersonnage(nomPersonnage(
                                baseId, rs.getString("personnage_name_key")));

                        String typeDefinition = rs.getString("type_capacite_definition");
                        Object numeroObj = rs.getObject("numero_capacite");
                        Integer numero = numeroObj == null ? null : ((Number) numeroObj).intValue();
                        String type = typeDefinition == null || typeDefinition.isBlank()
                                ? rs.getString("type_capacite_posee")
                                : OmicronCatalogueService.libelleType(typeDefinition, numero);
                        detail.setTypeCapacite(type == null || type.isBlank() ? "Capacité" : type);
                        detail.setNomCapaciteDetail(nomCapaciteDetail(
                                rs.getString("skill_name_key"), detail.getTypeCapacite()));

                        details.put(key, detail);
                        mode.getOmicrons().add(detail);
                    }

                    detail.getJoueurs().add(new PlayerRow(
                            rs.getString("player_id"),
                            rs.getString("player_name")));
                });

        for (ModeDetail mode : modes.values()) {
            mode.getOmicrons().sort((a, b) -> {
                int cmp = a.getNomPersonnage().compareToIgnoreCase(b.getNomPersonnage());
                if (cmp != 0) return cmp;
                return a.getTypeCapacite().compareToIgnoreCase(b.getTypeCapacite());
            });
        }

        return new ArrayList<>(modes.values());
    }

    private ModeDetail creerMode(String code, String label) {
        ModeDetail mode = new ModeDetail();
        mode.setCode(code);
        mode.setLabel(label);
        mode.setModalId("dashboardOmicronModal-" + code);
        return mode;
    }

    private String nomPersonnage(String baseId, String nameKey) {
        if (nameKey != null && !nameKey.isBlank()) {
            String traduit = localizationService.traduire(nameKey);
            if (traduit != null && !traduit.isBlank() && !traduit.equals(nameKey)) {
                return traduit;
            }
        }
        return localizationService.traduirePersonnage(baseId, baseId);
    }

    private String nomCapaciteDetail(String nameKey, String typeAffiche) {
        if (nameKey == null || nameKey.isBlank()) return null;
        String traduit = localizationService.traduire(nameKey);
        if (traduit == null || traduit.isBlank() || traduit.equals(nameKey)) return null;
        if (typeAffiche != null && traduit.equalsIgnoreCase(typeAffiche)) return null;
        return traduit;
    }

    private int nombreOmicronsActifsGuilde() {
        Integer n = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM omicron o
                JOIN joueur j ON j.player_id = o.joueur_id AND j.dans_guilde = 1
                WHERE o.actif = 1
                """, Integer.class);
        return n == null ? 0 : n;
    }

    public List<fr.xalkinn.swgohmanager.modele.Joueur> getJoueursGuilde() {
        return joueurRepository.trouverTousLesJoueursParNom();
    }

    public List<EvolutionGuilde> getDerniersUps() {

        return personnageRepository.trouverDerniersUpsGuilde();

    }

}