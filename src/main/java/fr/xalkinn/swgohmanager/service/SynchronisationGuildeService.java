package fr.xalkinn.swgohmanager.service;

import org.springframework.stereotype.Service;

import swgoh.comlink.PseudosGuilde;


@Service
public class SynchronisationGuildeService {


    private final SynchronisationGuildeStatusService statusService;
    private final GuildeService guildeService;


    public SynchronisationGuildeService(
            SynchronisationGuildeStatusService statusService,
            GuildeService guildeService) {
        this.statusService = statusService;
        this.guildeService = guildeService;
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
                    statusService.demarrer("Synchronisation Guilde",total);
                }
                statusService.miseAJour(etape,courant);
            }
        );
        guildeService.marquerSynchronisation();
        statusService.terminer();
    }
}