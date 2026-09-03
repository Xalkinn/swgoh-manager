package fr.xalkinn.swgohmanager.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.xalkinn.swgohmanager.service.OmicronCatalogueService.Definition;

/** Détecte les Omicrons réellement appliqués à partir du roster /player. */
@Service
public class OmicronPossessionService {

    private final JdbcTemplate jdbc;

    public OmicronPossessionService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public int synchroniserJoueur(JSONObject player, List<Definition> definitions) {
        String playerId = player.optString("playerId", "").trim();
        if (playerId.isBlank()) {
            throw new IllegalArgumentException("playerId absent du JSON joueur.");
        }
        String nom = player.optString("name", playerId);

        Map<String, Map<String, Integer>> roster = lireRoster(player);
        List<Object[]> actifs = new ArrayList<>();

        for (Definition d : definitions) {
            Map<String, Integer> skills = roster.get(d.baseId());
            if (skills == null) continue;

            Integer tier = skills.get(d.skillId());
            if (tier == null || d.tierRequis() == null) continue;

            // Game Data repère le tier Omicron avec un index de liste 1-based,
            // alors que le /player expose le niveau de skill avec un décalage.
            int tierRosterRequis = Math.max(0, d.tierRequis() - 1);
            if (tier < tierRosterRequis) continue;

            String typeAffiche = OmicronCatalogueService.libelleType(
                    d.typeCapacite(), d.numeroCapacite());

            actifs.add(new Object[] {
                    playerId,
                    nom,
                    d.baseId(),
                    d.skillId(),
                    typeAffiche,
                    d.modeTechnique(),
                    d.categorie()
            });
        }

        // La requête /player a été parsée avec succès : on peut remplacer
        // atomiquement l'ancien état de ce joueur.
        jdbc.update("DELETE FROM omicron WHERE joueur_id = ?", playerId);

        if (!actifs.isEmpty()) {
            jdbc.batchUpdate("""
                    INSERT INTO omicron
                        (joueur_id, nom_joueur, personnage, skill_id,
                         type_capacite, mode_omicron, categorie, actif, date_maj)
                    VALUES (?, ?, ?, ?, ?, ?, ?, TRUE, CURRENT_TIMESTAMP)
                    ON DUPLICATE KEY UPDATE
                        nom_joueur = VALUES(nom_joueur),
                        personnage = VALUES(personnage),
                        type_capacite = VALUES(type_capacite),
                        mode_omicron = VALUES(mode_omicron),
                        categorie = VALUES(categorie),
                        actif = TRUE,
                        date_maj = CURRENT_TIMESTAMP
                    """, actifs);
        }

        return actifs.size();
    }

    public void purgerJoueursHorsGuilde() {
        jdbc.update("""
                DELETE o
                FROM omicron o
                LEFT JOIN joueur j ON j.player_id = o.joueur_id
                WHERE j.player_id IS NULL OR j.dans_guilde = 0
                """);
    }

    private Map<String, Map<String, Integer>> lireRoster(JSONObject player) {
        Map<String, Map<String, Integer>> result = new HashMap<>();
        JSONArray roster = player.optJSONArray("rosterUnit");
        if (roster == null) return result;

        for (int i = 0; i < roster.length(); i++) {
            JSONObject unit = roster.optJSONObject(i);
            if (unit == null) continue;

            String definitionId = unit.optString("definitionId", "").trim();
            if (definitionId.isBlank()) continue;
            String baseId = normaliserBaseId(definitionId);

            JSONArray skills = unit.optJSONArray("skill");
            if (skills == null) continue;

            Map<String, Integer> bySkill = result.computeIfAbsent(baseId, k -> new HashMap<>());
            for (int j = 0; j < skills.length(); j++) {
                JSONObject skill = skills.optJSONObject(j);
                if (skill == null) continue;
                String skillId = skill.optString("id", "").trim();
                if (skillId.isBlank()) continue;
                bySkill.put(skillId, skill.optInt("tier", -1));
            }
        }
        return result;
    }

    private String normaliserBaseId(String definitionId) {
        String baseId = definitionId;
        int idx = baseId.indexOf(':');
        if (idx > 0) baseId = baseId.substring(0, idx);
        return baseId.replace(",", "").trim();
    }
}
