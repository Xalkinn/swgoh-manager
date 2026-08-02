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
                        "Brutus",
						() -> AbilitiesBrutus.main(null)
                ),
                new BatchStep(
                        "Cal Kestis OG",
                        () -> AbilitiesCalKestis.main(null)
                ),
                new BatchStep(
                        "Clone Trooper deguise",
                        () -> AbilitiesDCT.main(null)
                ),
                new BatchStep(
                        "Droideka",
                        () -> AbilitiesDroideka.main(null)
                ),
                new BatchStep(
                        "DTMG",
                        () -> AbilitiesDTMG.main(null)
                ),
                new BatchStep(
                        "Embo",
                        () -> AbilitiesEmbo.main(null)
                ),
                new BatchStep(
                        "Fulcrum",
                        () -> AbilitiesFulcrum.main(null)
                ),
                new BatchStep(
                        "Grand inquisiteur",
                        () -> AbilitiesGrandInquisitor.main(null)
                ),
                new BatchStep(
                        "Grandes Matriarches",
                        () -> AbilitiesGreatMothers.main(null)
                ),
                new BatchStep(
                        "Hera Syndulla",
                        () -> AbilitiesHera.main(null)
                ),
                new BatchStep(
                        "Ithano",
                        () -> AbilitiesIthano.main(null)
                ),
                new BatchStep(
                        "Juhani",
                        () -> AbilitiesJuhani.main(null)
                ),
                new BatchStep(
                        "Kelleran Beq",
                        () -> AbilitiesKelleranBeq.main(null)
                ),
                new BatchStep(
                        "Kix",
                        () -> AbilitiesKix.main(null)
                ),
                new BatchStep(
                        "Mace Windu",
                        () -> AbilitiesMace.main(null)
                ),
                new BatchStep(
                        "Mara Jade",
                        () -> AbilitiesMaraJade.main(null)
                ),
                new BatchStep(
                        "Master Qui-Gon",
                        () -> AbilitiesMasterQuiGon.main(null)
                ),
                new BatchStep(
                        "Sabine Wren",
						() -> AbilitiesSabine.main(null)
                ),
                new BatchStep(
                        "Shin Hati",
						() -> AbilitiesShin.main(null)
                ),
                new BatchStep(
                        "SM-33",
						() -> AbilitiesSM33.main(null)
                ),
                new BatchStep(
                        "Spider Maul",
                        () -> AbilitiesSpiderMaul.main(null)
                ),
                new BatchStep(
                        "General Syndulla",
						() -> AbilitiesSyndulla.main(null)
                ),
                new BatchStep(
                        "Trench",
						() -> AbilitiesTrench.main(null)
                ),
                new BatchStep(
                        "Wampa",
                        () -> AbilitiesWampa.main(null)
                )
        );
    }

}