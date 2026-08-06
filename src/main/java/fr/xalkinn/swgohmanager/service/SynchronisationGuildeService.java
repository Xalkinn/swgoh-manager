package fr.xalkinn.swgohmanager.service;

import org.springframework.stereotype.Service;

import swgoh.comlink.PseudosGuilde;


@Service
public class SynchronisationGuildeService {


    private final SynchronisationGuildeStatusService statusService;


    public SynchronisationGuildeService(SynchronisationGuildeStatusService statusService) {
        this.statusService = statusService;
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

        PseudosGuilde.lancerSynchronisation((etape, courant, total) -> {
                if(courant == 0) {
                    statusService.demarrer("Synchronisation Guilde",total);
                }
                statusService.miseAJour(etape,courant);
            }
        );
        statusService.terminer();
    }
}