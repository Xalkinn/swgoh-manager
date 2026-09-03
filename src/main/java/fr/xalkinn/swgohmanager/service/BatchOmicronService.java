package fr.xalkinn.swgohmanager.service;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import fr.xalkinn.swgohmanager.client.SwgohPlayerClient;
import fr.xalkinn.swgohmanager.modele.ResultatBatch;
import fr.xalkinn.swgohmanager.service.OmicronCatalogueService.Definition;

/**
 * Batch Omicron dynamique.
 *
 * Il remplace entièrement les classes personnageOmicron.AbilitiesXXX du projet
 * comlink : le catalogue vient de Game Data et la possession du roster /player.
 */
@Service
public class BatchOmicronService {

    private static final Logger log = LoggerFactory.getLogger(BatchOmicronService.class);

    private final BatchStatusService batchStatusService;
    private final OmicronCatalogueService catalogueService;
    private final OmicronPossessionService possessionService;
    private final SwgohPlayerClient playerClient;
    private final JdbcTemplate jdbc;

    public BatchOmicronService(
            BatchStatusService batchStatusService,
            OmicronCatalogueService catalogueService,
            OmicronPossessionService possessionService,
            SwgohPlayerClient playerClient,
            JdbcTemplate jdbc) {
        this.batchStatusService = batchStatusService;
        this.catalogueService = catalogueService;
        this.possessionService = possessionService;
        this.playerClient = playerClient;
        this.jdbc = jdbc;
    }

    @Async
    public ResultatBatch lancerBatch() {
        long debut = System.currentTimeMillis();
        List<PlayerRef> joueurs = chargerJoueurs();
        int totalEtapes = joueurs.size() + 1;
        batchStatusService.demarrer("Synchronisation Omicrons", Math.max(1, totalEtapes));

        int erreurs = 0;
        int totalOmicrons = 0;

        try {
            batchStatusService.miseAJour("Catalogue Game Data", 1);
            try {
                String resultatCatalogue = catalogueService.synchroniserCatalogue();
                log.info(resultatCatalogue);
            } catch (Exception e) {
                if (catalogueService.compterDefinitionsActives() == 0) {
                    throw e;
                }
                // Si Comlink/Game Data est temporairement indisponible, le batch
                // peut continuer avec le dernier catalogue connu.
                log.warn("Catalogue non rafraîchi, utilisation du cache BDD : {}", e.getMessage());
            }

            List<Definition> definitions = catalogueService.trouverDefinitionsActives();
            int courant = 1;

            for (PlayerRef joueur : joueurs) {
                courant++;
                batchStatusService.miseAJour("Omicrons · " + joueur.nom(), courant);
                try {
                    JSONObject player = playerClient.rechercherParPlayerId(joueur.playerId());
                    totalOmicrons += possessionService.synchroniserJoueur(player, definitions);
                } catch (Exception e) {
                    erreurs++;
                    log.warn("Omicrons non synchronisés pour {} ({}): {}",
                            joueur.nom(), joueur.playerId(), e.getMessage());
                }
            }

            possessionService.purgerJoueursHorsGuilde();
            batchStatusService.terminer();

            long duree = System.currentTimeMillis() - debut;
            log.info("Batch Omicron dynamique terminé : {} Omicron(s) actif(s), {} joueur(s) en erreur.",
                    totalOmicrons, erreurs);
            return new ResultatBatch(erreurs == 0, duree, totalEtapes);

        } catch (Exception e) {
            log.error("Échec du batch Omicron dynamique", e);
            batchStatusService.terminer();
            return new ResultatBatch(false, System.currentTimeMillis() - debut, totalEtapes);
        }
    }

    private List<PlayerRef> chargerJoueurs() {
        return jdbc.query("""
                SELECT player_id, nom
                FROM joueur
                WHERE dans_guilde = 1
                  AND player_id IS NOT NULL
                  AND player_id <> ''
                ORDER BY nom
                """,
                (rs, rowNum) -> new PlayerRef(
                        rs.getString("player_id"),
                        rs.getString("nom")));
    }

    private record PlayerRef(String playerId, String nom) {}
}
