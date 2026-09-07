package fr.xalkinn.swgohmanager.service;

import java.util.List;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import fr.xalkinn.swgohmanager.client.SwgohPlayerClient;

/**
 * Batch Datacron dédié.
 *
 * Important : aucun appel au BatchRosterService, à ExtractionService ou au
 * RosterImporter. Le Batch Roster reste donc strictement indépendant.
 */
@Service
public class BatchDatacronService {

    private static final Logger log = LoggerFactory.getLogger(BatchDatacronService.class);

    private final BatchDatacronStatusService statusService;
    private final DatacronService datacronService;
    private final SwgohPlayerClient playerClient;
    private final JdbcTemplate jdbc;

    public BatchDatacronService(
            BatchDatacronStatusService statusService,
            DatacronService datacronService,
            SwgohPlayerClient playerClient,
            JdbcTemplate jdbc) {
        this.statusService = statusService;
        this.datacronService = datacronService;
        this.playerClient = playerClient;
        this.jdbc = jdbc;
    }

    public synchronized boolean lancerBatch() {
        if (statusService.estEnCours()) {
            return false;
        }

        // Réserve immédiatement le batch afin d'empêcher un double-clic de lancer
        // deux traitements avant même que le thread ait chargé la liste des joueurs.
        statusService.demarrer(1);
        statusService.miseAJour("Préparation du Batch DTC", 0);

        Thread thread = new Thread(this::traiterDatacrons, "swgoh-batch-datacrons");
        thread.setDaemon(true);
        thread.start();
        return true;
    }

    private void traiterDatacrons() {
        List<PlayerRef> joueurs;
        try {
            joueurs = chargerJoueurs();
        } catch (Exception e) {
            log.error("Impossible de charger les joueurs pour le Batch DTC", e);
            statusService.demarrer(1);
            statusService.echouer("Impossible de charger les joueurs");
            return;
        }

        statusService.demarrer(joueurs.size());
        int erreurs = 0;
        int datacrons = 0;
        int courant = 0;

        log.info("Début Batch DTC : {} joueur(s)", joueurs.size());

        for (PlayerRef joueur : joueurs) {
            courant++;
            statusService.miseAJour(
                    "DTC · " + (joueur.nom() == null ? joueur.playerId() : joueur.nom()),
                    courant - 1);
            try {
                JSONObject player = playerClient.rechercherParPlayerId(joueur.playerId());
                datacrons += datacronService.synchroniserJoueur(joueur.id(), player);
            } catch (Exception e) {
                erreurs++;
                log.warn("DTC non synchronisés pour {} ({}): {}",
                        joueur.nom(), joueur.playerId(), e.getMessage());
            }
            statusService.miseAJour(
                    "DTC · " + courant + "/" + joueurs.size(),
                    courant);
        }

        try {
            datacronService.purgerJoueursHorsGuilde();
        } catch (Exception e) {
            erreurs++;
            log.warn("Nettoyage DTC hors guilde impossible : {}", e.getMessage());
        }

        statusService.terminerAvecErreurs(erreurs);
        log.info("Batch DTC terminé : {} Datacron(s), {} erreur(s)", datacrons, erreurs);
    }

    private List<PlayerRef> chargerJoueurs() {
        return jdbc.query("""
            SELECT id, player_id, nom
            FROM joueur
            WHERE dans_guilde = 1
              AND player_id IS NOT NULL
              AND player_id <> ''
            ORDER BY nom
            """, (rs, rowNum) -> new PlayerRef(
                rs.getInt("id"),
                rs.getString("player_id"),
                rs.getString("nom")));
    }

    private record PlayerRef(int id, String playerId, String nom) {}
}
