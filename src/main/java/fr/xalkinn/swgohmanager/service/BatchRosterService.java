package fr.xalkinn.swgohmanager.service;

import org.json.JSONObject;
import org.springframework.stereotype.Service;

import swgoh.comlink.RosterImporter;
import utils.HttpUtils;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import dao.DatabaseManager;


@Service
public class BatchRosterService {
	//private ExtractionService extractionServiceold;
	private final ExtractionService extractionService;
	private final BatchRosterStatusService rosterStatusService;


	public BatchRosterService(
	        ExtractionService extractionService,
	        BatchRosterStatusService rosterStatusService) {

	    this.extractionService = extractionService;
	    this.rosterStatusService = rosterStatusService;
	}
    public void lancerBatch() {
        Thread thread = new Thread(() -> {
            System.out.println("🚀 Début mise à jour des rosters");
            try {
                traiterRosters();
                System.out.println("✅ Mise à jour rosters terminée");
            } catch(Exception e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }
    
    private void traiterRosters() throws Exception {

        int extractionId = extractionService.debutExtraction();
        try(Connection conn = DatabaseManager.getConnection();
            Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT id, player_id FROM joueur ORDER BY id"
            );
            List<Integer> joueursId = new ArrayList<>();
            List<String> playersId = new ArrayList<>();
            while(rs.next()) {
                joueursId.add(rs.getInt("id"));
                playersId.add(rs.getString("player_id"));
            }
            int totalJoueurs = joueursId.size();
            rosterStatusService.demarrer(
                    "Batch Roster",
                    totalJoueurs
            );
            System.out.println(
            	    "📊 Batch Roster : "
            	    + totalJoueurs
            	    + " joueurs à traiter"
            	);
            int traites = 0;
            for(int i = 0; i < totalJoueurs; i++) {
                int joueurId = joueursId.get(i);
                String playerId = playersId.get(i);

                System.out.println(
                    "Roster "
                    + (i+1)
                    + "/"
                    + totalJoueurs
                    + " : "
                    + playerId
                );
                JSONObject payload = new JSONObject();
                payload.put("playerId", playerId);
                JSONObject body = new JSONObject();
                body.put("payload", payload);

                JSONObject joueur =
                    HttpUtils.callComlink(
                        "http://localhost:3000/player",
                        body
                    );
                if(joueur.has("rosterUnit")) {
                    RosterImporter.insertRoster(
                        joueur,
                        joueurId,
                        extractionId
                    );
                }
                // ---- Progression ----
                traites++;
                int progression =
                    (traites * 100) / totalJoueurs;
                extractionService.majProgression(
                    extractionId,
                    progression
                );
                rosterStatusService.miseAJour(
                        "Roster joueur "
                        + traites
                        + "/"
                        + totalJoueurs,
                        traites
                );
            }
        }
        extractionService.finExtraction(extractionId);
        rosterStatusService.terminer();
    }
	public BatchRosterStatusService getRosterStatusService() {
		return rosterStatusService;
	}
	public ExtractionService getExtractionService() {
		return extractionService;
	}
}