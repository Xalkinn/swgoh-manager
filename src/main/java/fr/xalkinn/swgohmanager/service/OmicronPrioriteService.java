package fr.xalkinn.swgohmanager.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.xalkinn.swgohmanager.dto.OmicronGestionView.CategorieOption;
import fr.xalkinn.swgohmanager.dto.OmicronGestionView.DefinitionOption;
import fr.xalkinn.swgohmanager.dto.OmicronGestionView.PrioriteRow;
import fr.xalkinn.swgohmanager.dto.OmicronGestionView.JoueurOmicronRow;
import fr.xalkinn.swgohmanager.service.OmicronCatalogueService.Definition;

/** Configuration P1-P4 propre à la guilde. */
@Service
public class OmicronPrioriteService {

    private final JdbcTemplate jdbc;
    private final OmicronCatalogueService catalogueService;
    private final LocalizationService localizationService;

    public OmicronPrioriteService(
            JdbcTemplate jdbc,
            OmicronCatalogueService catalogueService,
            LocalizationService localizationService) {
        this.jdbc = jdbc;
        this.catalogueService = catalogueService;
        this.localizationService = localizationService;
    }

    public List<DefinitionOption> getOptionsGestion() {
        List<DefinitionOption> result = new ArrayList<>();
        for (Definition d : catalogueService.trouverDefinitionsActives()) {
            if (!OmicronCatalogueService.CATEGORIES_GESTION.contains(d.categorie())) continue;
            result.add(toOption(d));
        }
        result.sort(Comparator
                .comparing(DefinitionOption::getCategorie)
                .thenComparing(DefinitionOption::getNomPersonnage, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(DefinitionOption::getNomCapacite, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    public List<CategorieOption> getCategoriesDisponibles(List<DefinitionOption> options) {
        Map<String, CategorieOption> map = new LinkedHashMap<>();
        for (String code : OmicronCatalogueService.CATEGORIES_GESTION) {
            boolean present = options.stream().anyMatch(o -> code.equals(o.getCategorie()));
            if (present) {
                map.put(code, new CategorieOption(code, OmicronCatalogueService.categorieLabel(code)));
            }
        }
        return new ArrayList<>(map.values());
    }

    public List<PrioriteRow> getPriorites() {
        int totalJoueurs = nombreJoueursActifs();
        Map<String, Integer> possesseurs = chargerPossesseurs();

        List<PrioriteRow> rows = jdbc.query("""
                SELECT
                    p.id AS priorite_id,
                    p.priorite,
                    d.id AS definition_id,
                    d.base_id,
                    d.personnage_name_key,
                    d.skill_id,
                    d.skill_name_key,
                    d.type_capacite,
                    d.numero_capacite,
                    d.mode_technique,
                    d.categorie,
                    d.tier_requis
                FROM omicron_priorite p
                JOIN omicron_definition d ON d.id = p.omicron_definition_id
                WHERE p.actif = 1
                  AND d.actif = 1
                  AND d.categorie IN ('TW','TB','RAID','CONQUETE')
                ORDER BY p.priorite, d.categorie, d.base_id, d.type_capacite, d.numero_capacite
                """,
                (rs, rowNum) -> {
                    PrioriteRow row = new PrioriteRow();
                    row.setPrioriteId(rs.getLong("priorite_id"));
                    row.setPriorite(rs.getInt("priorite"));
                    row.setId(rs.getLong("definition_id"));
                    row.setBaseId(rs.getString("base_id"));
                    row.setSkillId(rs.getString("skill_id"));
                    row.setTypeCapacite(rs.getString("type_capacite"));
                    Object numero = rs.getObject("numero_capacite");
                    row.setNumeroCapacite(numero == null ? null : ((Number) numero).intValue());
                    row.setModeTechnique(rs.getString("mode_technique"));
                    row.setCategorie(rs.getString("categorie"));
                    row.setCategorieLabel(OmicronCatalogueService.categorieLabel(row.getCategorie()));
                    Object tier = rs.getObject("tier_requis");
                    row.setTierRequis(tier == null ? null : ((Number) tier).intValue());
                    row.setNomPersonnage(nomPersonnage(
                            row.getBaseId(), rs.getString("personnage_name_key")));
                    row.setNomCapacite(libelleType(row.getTypeCapacite(), row.getNumeroCapacite()));
                    row.setNomCapaciteDetail(detailCapacite(
                            rs.getString("skill_name_key"),
                            row.getTypeCapacite(),
                            row.getNumeroCapacite()));
                    return row;
                });

        for (PrioriteRow row : rows) {
            int nb = possesseurs.getOrDefault(row.getSkillId(), 0);
            row.setPossesseurs(nb);
            row.setTotalJoueurs(totalJoueurs);
            row.setManquants(Math.max(0, totalJoueurs - nb));
            row.setModalId("omicronPlayersModal-" + row.getPrioriteId());
            row.setPossesseursDetails(chargerJoueursAvecOmicron(row.getSkillId()));
            row.setManquantsDetails(chargerJoueursSansOmicron(row.getSkillId()));
        }
        return rows;
    }

    @Transactional
    public void ajouter(long definitionId, int priorite) {
        validerPriorite(priorite);
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM omicron_definition
                WHERE id = ? AND actif = 1
                  AND categorie IN ('TW','TB','RAID','CONQUETE')
                """, Integer.class, definitionId);
        if (count == null || count == 0) {
            throw new IllegalArgumentException("Omicron introuvable ou non gérable dans cette catégorie.");
        }

        jdbc.update("""
                INSERT INTO omicron_priorite
                    (omicron_definition_id, priorite, actif, date_creation, date_modification)
                VALUES (?, ?, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON DUPLICATE KEY UPDATE
                    priorite = VALUES(priorite),
                    actif = TRUE,
                    date_modification = CURRENT_TIMESTAMP
                """, definitionId, priorite);
    }

    @Transactional
    public void modifier(long prioriteId, int priorite) {
        validerPriorite(priorite);
        int updated = jdbc.update("""
                UPDATE omicron_priorite
                SET priorite = ?, date_modification = CURRENT_TIMESTAMP
                WHERE id = ?
                """, priorite, prioriteId);
        if (updated == 0) {
            throw new IllegalArgumentException("Priorité Omicron introuvable.");
        }
    }

    @Transactional
    public void supprimer(long prioriteId) {
        jdbc.update("DELETE FROM omicron_priorite WHERE id = ?", prioriteId);
    }

    public int compterPriorites() {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM omicron_priorite WHERE actif = 1",
                Integer.class);
        return n == null ? 0 : n;
    }

    private DefinitionOption toOption(Definition d) {
        DefinitionOption o = new DefinitionOption();
        o.setId(d.id());
        o.setBaseId(d.baseId());
        o.setSkillId(d.skillId());
        o.setTypeCapacite(d.typeCapacite());
        o.setNumeroCapacite(d.numeroCapacite());
        o.setCategorie(d.categorie());
        o.setCategorieLabel(OmicronCatalogueService.categorieLabel(d.categorie()));
        o.setModeTechnique(d.modeTechnique());
        o.setTierRequis(d.tierRequis());
        o.setNomPersonnage(nomPersonnage(d.baseId(), d.personnageNameKey()));
        o.setNomCapacite(libelleType(d.typeCapacite(), d.numeroCapacite()));
        o.setNomCapaciteDetail(detailCapacite(d.skillNameKey(), d.typeCapacite(), d.numeroCapacite()));
        return o;
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

    private String detailCapacite(String nameKey, String type, Integer numero) {
        if (nameKey == null || nameKey.isBlank()) {
            return null;
        }
        String traduit = localizationService.traduire(nameKey);
        if (traduit == null || traduit.isBlank() || traduit.equals(nameKey)) {
            return null;
        }
        String libelleType = libelleType(type, numero);
        if (traduit.equalsIgnoreCase(libelleType)) {
            return null;
        }
        return traduit;
    }

    private String libelleType(String type, Integer numero) {
        return OmicronCatalogueService.libelleType(type, numero);
    }

    private java.util.List<JoueurOmicronRow> chargerJoueursAvecOmicron(String skillId) {
        return jdbc.query("""
                SELECT j.player_id,
                       COALESCE(j.nom, o.nom_joueur, j.player_id) AS player_name
                FROM omicron o
                JOIN joueur j ON j.player_id = o.joueur_id
                WHERE o.actif = 1
                  AND o.skill_id = ?
                  AND j.dans_guilde = 1
                ORDER BY player_name
                """,
                (rs, rowNum) -> new JoueurOmicronRow(
                        rs.getString("player_id"),
                        rs.getString("player_name")),
                skillId);
    }

    private java.util.List<JoueurOmicronRow> chargerJoueursSansOmicron(String skillId) {
        return jdbc.query("""
                SELECT j.player_id,
                       COALESCE(j.nom, j.player_id) AS player_name
                FROM joueur j
                WHERE j.dans_guilde = 1
                  AND NOT EXISTS (
                      SELECT 1
                      FROM omicron o
                      WHERE o.joueur_id = j.player_id
                        AND o.skill_id = ?
                        AND o.actif = 1
                  )
                ORDER BY player_name
                """,
                (rs, rowNum) -> new JoueurOmicronRow(
                        rs.getString("player_id"),
                        rs.getString("player_name")),
                skillId);
    }

    private Map<String, Integer> chargerPossesseurs() {
        Map<String, Integer> map = new LinkedHashMap<>();
        jdbc.query("""
                SELECT o.skill_id, COUNT(DISTINCT o.joueur_id) AS nb
                FROM omicron o
                JOIN joueur j ON j.player_id = o.joueur_id AND j.dans_guilde = 1
                WHERE o.actif = 1 AND o.skill_id IS NOT NULL
                GROUP BY o.skill_id
                """, rs -> {
                    map.put(rs.getString("skill_id"), rs.getInt("nb"));
                });
        return map;
    }

    private int nombreJoueursActifs() {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM joueur WHERE dans_guilde = 1",
                Integer.class);
        return n == null ? 0 : n;
    }

    private void validerPriorite(int priorite) {
        if (priorite < 1 || priorite > 4) {
            throw new IllegalArgumentException("La priorité doit être comprise entre P1 et P4.");
        }
    }
}
