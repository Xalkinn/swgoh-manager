package fr.xalkinn.swgohmanager.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import fr.xalkinn.swgohmanager.modele.BatchStatus;

@Service
public class SynchronisationGuildeStatusService {

    private final BatchStatus status = new BatchStatus();

    public BatchStatus getStatus() {
        return status;
    }

    public void demarrer(String nomBatch, int total) {
        status.setEnCours(true);
        status.setNomBatch(nomBatch);
        status.setEtapeActuelle("Initialisation...");
        status.setCourant(0);
        status.setTotal(total);
        status.setPourcentage(0);
        status.setDebut(LocalDateTime.now());
        status.setFin(null);
    }

    public void miseAJour(String etape, int courant) {
        status.setEtapeActuelle(etape);
        status.setCourant(courant);
        int pourcentage = (int)((courant * 100.0) / status.getTotal());
        status.setPourcentage(pourcentage);
    }

    public void terminer() {
        status.setEnCours(false);
        status.setEtapeActuelle("Terminé");
        status.setCourant(status.getTotal());
        status.setPourcentage(100);
        status.setFin(LocalDateTime.now());
    }
}