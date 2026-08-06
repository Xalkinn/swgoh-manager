package fr.xalkinn.swgohmanager.service;

import org.springframework.stereotype.Service;

import lancement.UpdateMods;


@Service
public class MajModQService {

    private final ModsStatusService statusService;

    public MajModQService(ModsStatusService statusService) {
        this.statusService = statusService;
    }

    public void lancerBatch() {
        Thread thread = new Thread(() -> {
            System.out.println("🔧 Début mise à jour des mods");
            try {
                UpdateMods.lancerMiseAJour((etape, courant, total) -> {
                        // Initialisation au premier retour
                        if(statusService.getStatus().getTotal() != total) {
                            statusService.demarrer("Mise à jour Mods",total);
                        }
                        statusService.miseAJour(etape,courant);
                    }
                );
                statusService.terminer();
                System.out.println("✅ Mise à jour mods terminée");
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }
}