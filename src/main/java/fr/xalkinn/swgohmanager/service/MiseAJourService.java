package fr.xalkinn.swgohmanager.service;

import org.springframework.stereotype.Service;

import swgoh.comlink.PseudosGuilde;

@Service
public class MiseAJourService {

    private final GuildeService guildeService;

    public MiseAJourService(GuildeService guildeService) {
        this.guildeService = guildeService;
    }

    public void miseAJourGuilde() throws Exception {

        guildeService.appliquerConfigurationRuntime();

        // Lance la récupération API guild
        PseudosGuilde.main(new String[]{});

    }


    public void miseAJourMods() throws Exception {

        // Ici on branchera ton import Mods
        // Exemple :
        // ModImporter.main(new String[]{});

        System.out.println("MAJ Mods terminée");
    }
}