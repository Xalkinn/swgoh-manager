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
import fr.xalkinn.swgohmanager.dto.DashboardEquipmentView;
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
     * Synthèse « Équipements & Reliques » du dashboard.
     *
     * La photo courante utilise la dernière extraction Roster terminée afin de
     * ne jamais mélanger un batch en cours avec le précédent. Si aucune
     * extraction terminée n'existe encore (ancienne base), on retombe sur la
     * dernière extraction connue de chaque joueur.
     */
    public DashboardEquipmentView getEquipmentDashboard() {
        DashboardEquipmentView view = new DashboardEquipmentView();

        List<Integer> extractions = jdbc.queryForList("""
                SELECT id
                FROM extraction
                WHERE statut = 'TERMINE'
                ORDER BY id DESC
                LIMIT 2
                """, Integer.class);

        Integer extractionActuelle = extractions.isEmpty() ? null : extractions.get(0);
        Integer extractionPrecedente = extractions.size() < 2 ? null : extractions.get(1);

        view.setExtractionActuelle(extractionActuelle);
        view.setExtractionPrecedente(extractionPrecedente);

        Map<String, Object> synthese = extractionActuelle == null
                ? chargerSyntheseDerniereExtractionParJoueur()
                : chargerSyntheseExtraction(extractionActuelle);

        int g13 = nombre(synthese.get("g13"));
        int r5 = nombre(synthese.get("r5_plus"));
        int r7 = nombre(synthese.get("r7_plus"));
        int r9 = nombre(synthese.get("r9_plus"));
        int r10 = nombre(synthese.get("r10"));

        view.setG13(g13);
        view.setR5Plus(r5);
        view.setR7Plus(r7);
        view.setR9Plus(r9);
        view.setR10(r10);

        int r0a4 = nombre(synthese.get("r0_4"));
        int r5a6 = nombre(synthese.get("r5_6"));
        int r7a8 = nombre(synthese.get("r7_8"));
        int r9Plus = nombre(synthese.get("r9_bucket"));

        view.getDistribution().add(bucket("r0-4", "R0–R4", r0a4, g13));
        view.getDistribution().add(bucket("r5-6", "R5–R6", r5a6, g13));
        view.getDistribution().add(bucket("r7-8", "R7–R8", r7a8, g13));
        view.getDistribution().add(bucket("r9", "R9+", r9Plus, g13));

        if (extractionActuelle != null && extractionPrecedente != null) {
            Map<String, Object> progression = jdbc.queryForMap("""
                    SELECT
                        COALESCE(SUM(CASE
                            WHEN ancien.gear < 13 AND nouveau.gear >= 13 THEN 1 ELSE 0 END), 0) AS nouveaux_g13,
                        COALESCE(SUM(CASE
                            WHEN COALESCE(ancien.relic, 0) < 7 AND COALESCE(nouveau.relic, 0) >= 7 THEN 1 ELSE 0 END), 0) AS nouveaux_r7,
                        COALESCE(SUM(CASE
                            WHEN COALESCE(ancien.relic, 0) < 9 AND COALESCE(nouveau.relic, 0) >= 9 THEN 1 ELSE 0 END), 0) AS nouveaux_r9
                    FROM personnage ancien
                    JOIN personnage nouveau
                      ON nouveau.joueur_id = ancien.joueur_id
                     AND SUBSTRING_INDEX(nouveau.base_id, ':', 1) = SUBSTRING_INDEX(ancien.base_id, ':', 1)
                    JOIN joueur j
                      ON j.id = nouveau.joueur_id
                     AND j.dans_guilde = 1
                    WHERE ancien.extraction_id = ?
                      AND nouveau.extraction_id = ?
                    """, extractionPrecedente, extractionActuelle);

            view.setNouveauxG13(nombre(progression.get("nouveaux_g13")));
            view.setNouveauxR7Plus(nombre(progression.get("nouveaux_r7")));
            view.setNouveauxR9Plus(nombre(progression.get("nouveaux_r9")));
            view.setProgressionDisponible(true);
        }

        view.setJoueurs(extractionActuelle == null
                ? chargerDetailsJoueursDerniereExtraction()
                : chargerDetailsJoueurs(extractionActuelle));
        view.setPersonnages(extractionActuelle == null
                ? chargerDetailsPersonnagesDerniereExtraction()
                : chargerDetailsPersonnages(extractionActuelle));

        return view;
    }

    private Map<String, Object> chargerSyntheseExtraction(int extractionId) {
        return jdbc.queryForMap("""
                SELECT
                    COALESCE(SUM(CASE WHEN p.gear >= 13 THEN 1 ELSE 0 END), 0) AS g13,
                    COALESCE(SUM(CASE WHEN COALESCE(p.relic, 0) >= 5 THEN 1 ELSE 0 END), 0) AS r5_plus,
                    COALESCE(SUM(CASE WHEN COALESCE(p.relic, 0) >= 7 THEN 1 ELSE 0 END), 0) AS r7_plus,
                    COALESCE(SUM(CASE WHEN COALESCE(p.relic, 0) >= 9 THEN 1 ELSE 0 END), 0) AS r9_plus,
                    COALESCE(SUM(CASE WHEN COALESCE(p.relic, 0) >= 10 THEN 1 ELSE 0 END), 0) AS r10,
                    COALESCE(SUM(CASE WHEN p.gear >= 13 AND COALESCE(p.relic, 0) BETWEEN 0 AND 4 THEN 1 ELSE 0 END), 0) AS r0_4,
                    COALESCE(SUM(CASE WHEN COALESCE(p.relic, 0) BETWEEN 5 AND 6 THEN 1 ELSE 0 END), 0) AS r5_6,
                    COALESCE(SUM(CASE WHEN COALESCE(p.relic, 0) BETWEEN 7 AND 8 THEN 1 ELSE 0 END), 0) AS r7_8,
                    COALESCE(SUM(CASE WHEN COALESCE(p.relic, 0) >= 9 THEN 1 ELSE 0 END), 0) AS r9_bucket
                FROM personnage p
                JOIN joueur j ON j.id = p.joueur_id AND j.dans_guilde = 1
                WHERE p.extraction_id = ?
                """, extractionId);
    }

    private Map<String, Object> chargerSyntheseDerniereExtractionParJoueur() {
        return jdbc.queryForMap("""
                SELECT
                    COALESCE(SUM(CASE WHEN p.gear >= 13 THEN 1 ELSE 0 END), 0) AS g13,
                    COALESCE(SUM(CASE WHEN COALESCE(p.relic, 0) >= 5 THEN 1 ELSE 0 END), 0) AS r5_plus,
                    COALESCE(SUM(CASE WHEN COALESCE(p.relic, 0) >= 7 THEN 1 ELSE 0 END), 0) AS r7_plus,
                    COALESCE(SUM(CASE WHEN COALESCE(p.relic, 0) >= 9 THEN 1 ELSE 0 END), 0) AS r9_plus,
                    COALESCE(SUM(CASE WHEN COALESCE(p.relic, 0) >= 10 THEN 1 ELSE 0 END), 0) AS r10,
                    COALESCE(SUM(CASE WHEN p.gear >= 13 AND COALESCE(p.relic, 0) BETWEEN 0 AND 4 THEN 1 ELSE 0 END), 0) AS r0_4,
                    COALESCE(SUM(CASE WHEN COALESCE(p.relic, 0) BETWEEN 5 AND 6 THEN 1 ELSE 0 END), 0) AS r5_6,
                    COALESCE(SUM(CASE WHEN COALESCE(p.relic, 0) BETWEEN 7 AND 8 THEN 1 ELSE 0 END), 0) AS r7_8,
                    COALESCE(SUM(CASE WHEN COALESCE(p.relic, 0) >= 9 THEN 1 ELSE 0 END), 0) AS r9_bucket
                FROM personnage p
                JOIN joueur j ON j.id = p.joueur_id AND j.dans_guilde = 1
                JOIN (
                    SELECT joueur_id, MAX(extraction_id) AS extraction_id
                    FROM personnage
                    GROUP BY joueur_id
                ) latest
                  ON latest.joueur_id = p.joueur_id
                 AND latest.extraction_id = p.extraction_id
                """);
    }

    private List<DashboardEquipmentView.PlayerRow> chargerDetailsJoueurs(int extractionId) {
        return jdbc.query("""
                SELECT
                    j.id AS joueur_id,
                    j.player_id,
                    j.nom,
                    SUM(CASE WHEN p.gear >= 13 THEN 1 ELSE 0 END) AS g13,
                    SUM(CASE WHEN COALESCE(p.relic, 0) >= 5 THEN 1 ELSE 0 END) AS r5_plus,
                    SUM(CASE WHEN COALESCE(p.relic, 0) >= 7 THEN 1 ELSE 0 END) AS r7_plus,
                    SUM(CASE WHEN COALESCE(p.relic, 0) >= 9 THEN 1 ELSE 0 END) AS r9_plus,
                    SUM(CASE WHEN COALESCE(p.relic, 0) >= 10 THEN 1 ELSE 0 END) AS r10
                FROM joueur j
                JOIN personnage p ON p.joueur_id = j.id AND p.extraction_id = ?
                WHERE j.dans_guilde = 1
                GROUP BY j.id, j.player_id, j.nom
                ORDER BY r9_plus DESC, r7_plus DESC, r5_plus DESC, j.nom
                """, (rs, rowNum) -> {
            DashboardEquipmentView.PlayerRow row = new DashboardEquipmentView.PlayerRow();
            row.setJoueurId(rs.getInt("joueur_id"));
            row.setPlayerId(rs.getString("player_id"));
            row.setNom(rs.getString("nom"));
            row.setG13(rs.getInt("g13"));
            row.setR5Plus(rs.getInt("r5_plus"));
            row.setR7Plus(rs.getInt("r7_plus"));
            row.setR9Plus(rs.getInt("r9_plus"));
            row.setR10(rs.getInt("r10"));
            return row;
        }, extractionId);
    }

    private List<DashboardEquipmentView.PlayerRow> chargerDetailsJoueursDerniereExtraction() {
        return jdbc.query("""
                SELECT
                    j.id AS joueur_id,
                    j.player_id,
                    j.nom,
                    SUM(CASE WHEN p.gear >= 13 THEN 1 ELSE 0 END) AS g13,
                    SUM(CASE WHEN COALESCE(p.relic, 0) >= 5 THEN 1 ELSE 0 END) AS r5_plus,
                    SUM(CASE WHEN COALESCE(p.relic, 0) >= 7 THEN 1 ELSE 0 END) AS r7_plus,
                    SUM(CASE WHEN COALESCE(p.relic, 0) >= 9 THEN 1 ELSE 0 END) AS r9_plus,
                    SUM(CASE WHEN COALESCE(p.relic, 0) >= 10 THEN 1 ELSE 0 END) AS r10
                FROM joueur j
                JOIN (
                    SELECT joueur_id, MAX(extraction_id) AS extraction_id
                    FROM personnage
                    GROUP BY joueur_id
                ) latest ON latest.joueur_id = j.id
                JOIN personnage p
                  ON p.joueur_id = latest.joueur_id
                 AND p.extraction_id = latest.extraction_id
                WHERE j.dans_guilde = 1
                GROUP BY j.id, j.player_id, j.nom
                ORDER BY r9_plus DESC, r7_plus DESC, r5_plus DESC, j.nom
                """, (rs, rowNum) -> {
            DashboardEquipmentView.PlayerRow row = new DashboardEquipmentView.PlayerRow();
            row.setJoueurId(rs.getInt("joueur_id"));
            row.setPlayerId(rs.getString("player_id"));
            row.setNom(rs.getString("nom"));
            row.setG13(rs.getInt("g13"));
            row.setR5Plus(rs.getInt("r5_plus"));
            row.setR7Plus(rs.getInt("r7_plus"));
            row.setR9Plus(rs.getInt("r9_plus"));
            row.setR10(rs.getInt("r10"));
            return row;
        });
    }

    private List<DashboardEquipmentView.CharacterRow> chargerDetailsPersonnages(int extractionId) {
        return jdbc.query("""
                SELECT
                    SUBSTRING_INDEX(p.base_id, ':', 1) AS base_id,
                    COALESCE(MAX(pd.nom_francais), MAX(p.nom), MAX(SUBSTRING_INDEX(p.base_id, ':', 1))) AS nom,
                    COUNT(DISTINCT CASE WHEN p.gear >= 13 THEN p.joueur_id END) AS g13,
                    COUNT(DISTINCT CASE WHEN COALESCE(p.relic, 0) >= 5 THEN p.joueur_id END) AS r5_plus,
                    COUNT(DISTINCT CASE WHEN COALESCE(p.relic, 0) >= 7 THEN p.joueur_id END) AS r7_plus,
                    COUNT(DISTINCT CASE WHEN COALESCE(p.relic, 0) >= 9 THEN p.joueur_id END) AS r9_plus,
                    COUNT(DISTINCT CASE WHEN COALESCE(p.relic, 0) >= 10 THEN p.joueur_id END) AS r10
                FROM personnage p
                JOIN joueur j ON j.id = p.joueur_id AND j.dans_guilde = 1
                LEFT JOIN personnage_definition pd
                  ON pd.base_id = SUBSTRING_INDEX(p.base_id, ':', 1)
                WHERE p.extraction_id = ?
                GROUP BY SUBSTRING_INDEX(p.base_id, ':', 1)
                HAVING g13 > 0
                ORDER BY r9_plus DESC, r7_plus DESC, r5_plus DESC, nom
                """, (rs, rowNum) -> {
            DashboardEquipmentView.CharacterRow row = new DashboardEquipmentView.CharacterRow();
            row.setBaseId(rs.getString("base_id"));
            row.setNom(rs.getString("nom"));
            row.setG13(rs.getInt("g13"));
            row.setR5Plus(rs.getInt("r5_plus"));
            row.setR7Plus(rs.getInt("r7_plus"));
            row.setR9Plus(rs.getInt("r9_plus"));
            row.setR10(rs.getInt("r10"));
            return row;
        }, extractionId);
    }

    private List<DashboardEquipmentView.CharacterRow> chargerDetailsPersonnagesDerniereExtraction() {
        return jdbc.query("""
                SELECT
                    SUBSTRING_INDEX(p.base_id, ':', 1) AS base_id,
                    COALESCE(MAX(pd.nom_francais), MAX(p.nom), MAX(SUBSTRING_INDEX(p.base_id, ':', 1))) AS nom,
                    COUNT(DISTINCT CASE WHEN p.gear >= 13 THEN p.joueur_id END) AS g13,
                    COUNT(DISTINCT CASE WHEN COALESCE(p.relic, 0) >= 5 THEN p.joueur_id END) AS r5_plus,
                    COUNT(DISTINCT CASE WHEN COALESCE(p.relic, 0) >= 7 THEN p.joueur_id END) AS r7_plus,
                    COUNT(DISTINCT CASE WHEN COALESCE(p.relic, 0) >= 9 THEN p.joueur_id END) AS r9_plus,
                    COUNT(DISTINCT CASE WHEN COALESCE(p.relic, 0) >= 10 THEN p.joueur_id END) AS r10
                FROM personnage p
                JOIN joueur j ON j.id = p.joueur_id AND j.dans_guilde = 1
                JOIN (
                    SELECT joueur_id, MAX(extraction_id) AS extraction_id
                    FROM personnage
                    GROUP BY joueur_id
                ) latest
                  ON latest.joueur_id = p.joueur_id
                 AND latest.extraction_id = p.extraction_id
                LEFT JOIN personnage_definition pd
                  ON pd.base_id = SUBSTRING_INDEX(p.base_id, ':', 1)
                GROUP BY SUBSTRING_INDEX(p.base_id, ':', 1)
                HAVING g13 > 0
                ORDER BY r9_plus DESC, r7_plus DESC, r5_plus DESC, nom
                """, (rs, rowNum) -> {
            DashboardEquipmentView.CharacterRow row = new DashboardEquipmentView.CharacterRow();
            row.setBaseId(rs.getString("base_id"));
            row.setNom(rs.getString("nom"));
            row.setG13(rs.getInt("g13"));
            row.setR5Plus(rs.getInt("r5_plus"));
            row.setR7Plus(rs.getInt("r7_plus"));
            row.setR9Plus(rs.getInt("r9_plus"));
            row.setR10(rs.getInt("r10"));
            return row;
        });
    }

    private DashboardEquipmentView.RelicBucket bucket(String code, String label, int count, int total) {
        int percent = total <= 0 ? 0 : (int) Math.round((count * 100.0) / total);
        return new DashboardEquipmentView.RelicBucket(code, label, count, percent);
    }

    private int nombre(Object value) {
        return value instanceof Number n ? n.intValue() : 0;
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