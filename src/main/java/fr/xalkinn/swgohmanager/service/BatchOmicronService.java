package fr.xalkinn.swgohmanager.service;

import java.util.List;

import org.springframework.stereotype.Service;

import fr.xalkinn.swgohmanager.modele.ResultatBatch;
import personnageOmicron.*;
import utils.BatchStep;
import utils.ConsoleUtils;
import utils.DatabaseInitializer;
import org.springframework.scheduling.annotation.Async;

@Service
public class BatchOmicronService {

    private final BatchStatusService batchStatusService;

    public BatchOmicronService(BatchStatusService batchStatusService) {
        this.batchStatusService = batchStatusService;
    }
    @Async
    public ResultatBatch lancerBatch() {

        System.out.println("================================");
        System.out.println("        Batch Omicron");
        System.out.println("================================");

        DatabaseInitializer.initialiser();

        List<BatchStep> omicrons = chargerOmicrons();

        int total = omicrons.size();

        batchStatusService.demarrer("Batch Omicron", total);

        int compteur = 1;

        for (BatchStep step : omicrons) {

            batchStatusService.miseAJour(
                    step.getNom(),
                    compteur
            );

            ConsoleUtils.executerBatchStep(
                    step,
                    compteur,
                    total
            );

            compteur++;
        }

        batchStatusService.terminer();

        System.out.println("Batch terminé !");
		return null;
    }

    private List<BatchStep> chargerOmicrons() {

        return List.of(

                new BatchStep(
                        "Boss Nass",
                        () -> AbilitiesBossNass.main(null)
                ),

                new BatchStep(
                        "Sabine",
                        () -> AbilitiesSabine.main(null)
                ),

                new BatchStep(
                        "Trench",
                        () -> AbilitiesTrench.main(null)
                )

        );
    }

}