package fr.xalkinn.swgohmanager.service;

import org.springframework.stereotype.Service;

import swgoh.comlink.PseudosGuilde;

@Service
public class SynchronisationGuildeService {

    private final SynchronisationGuildeStatusService statusService;
    private final GuildeService guildeService;
    private final TerritoryBattleService territoryBattleService;

    public SynchronisationGuildeService(
            SynchronisationGuildeStatusService statusService,
            GuildeService guildeService,
            TerritoryBattleService territoryBattleService) {
        this.statusService = statusService;
        this.guildeService = guildeService;
        this.territoryBattleService = territoryBattleService;
    }

    public void lancerBatch() {
        Thread thread = new Thread(() -> {
            System.out.println("👥 Début synchronisation guilde");
            try {
                synchroniserGuilde();
                System.out.println("✅ Synchronisation guilde terminée");
            } catch(Exception e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }

    private void synchroniserGuilde() throws Exception {

        guildeService.appliquerConfigurationRuntime();

        PseudosGuilde.lancerSynchronisation((etape, courant, total) -> {
                if(courant == 0) {
                    statusService.demarrer("Synchronisation Guilde", total);
                }
                statusService.miseAJour(etape, courant);
            }
        );

        // Une TB terminée est disponible dans recentTerritoryBattleResult.
        // L'import est idempotent grâce à instance_id UNIQUE : une seule ligne
        // est conservée par TB, même si la synchronisation est relancée.
        try {
            String resultatTb = territoryBattleService.synchroniserDepuisComlink();
            System.out.println("🌐 " + resultatTb);
        } catch (Exception e) {
            // Une erreur TB ne doit pas invalider la synchronisation des joueurs.
            System.err.println("⚠ Historisation TB non effectuée : " + e.getMessage());
        }

        guildeService.marquerSynchronisation();
        statusService.terminer();
    }
}
