package fr.xalkinn.swgohmanager.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

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

    public DashboardService(
            JoueurRepository joueurRepository,
            OmicronRepository omicronRepository,
            PersonnageRepository personnageRepository,
            JdbcTemplate jdbc) {
        this.joueurRepository = joueurRepository;
        this.omicronRepository = omicronRepository;
        this.personnageRepository = personnageRepository;
        this.jdbc = jdbc;
    }

    public DashboardStats getDashboardStats() {

        DashboardStats stats = new DashboardStats();

        stats.setNombreJoueurs((int) joueurRepository.count());
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