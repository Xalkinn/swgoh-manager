package fr.xalkinn.swgohmanager.service;

import java.util.List;

import org.springframework.stereotype.Service;

import personnageOmicron.*;
import utils.BatchStep;
import utils.ConsoleUtils;
import utils.DatabaseInitializer;

@Service
public class BatchOmicronService {

    public void lancerBatch() {

        System.out.println("================================");
        System.out.println("        Batch Omicron");
        System.out.println("================================");
        DatabaseInitializer.initialiser();
        List<BatchStep> omicrons = List.of(
            new BatchStep("Boss Nass",
                    () -> AbilitiesBossNass.main(null)),
            new BatchStep("Sabine",
                    () -> AbilitiesSabine.main(null)),
            new BatchStep("Trench",
                    () -> AbilitiesTrench.main(null))
            // temporaire
        );
        int total = omicrons.size();
        int compteur = 1;
        
        for(BatchStep step : omicrons) {
            ConsoleUtils.executerBatchStep(
                    step,
                    compteur,
                    total
            );
            compteur++;
        }

        System.out.println("Batch terminé !");

    }

}