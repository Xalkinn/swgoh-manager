package fr.xalkinn.swgohmanager.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import fr.xalkinn.swgohmanager.modele.BatchStatus;

/**
 * Statut totalement indépendant du Batch Roster et du Batch Omicron.
 */
@Service
public class BatchDatacronStatusService {

    private final BatchStatus status = new BatchStatus();

    public BatchStatus getStatus() {
        return status;
    }

    public synchronized boolean estEnCours() {
        return status.isEnCours();
    }

    public synchronized void demarrer(int total) {
        status.setEnCours(true);
        status.setNomBatch("Batch DTC");
        status.setEtapeActuelle("Initialisation...");
        status.setCourant(0);
        status.setTotal(Math.max(1, total));
        status.setPourcentage(0);
        status.setDebut(LocalDateTime.now());
        status.setFin(null);
    }

    public synchronized void miseAJour(String etape, int courant) {
        status.setEtapeActuelle(etape);
        status.setCourant(courant);
        int total = Math.max(1, status.getTotal());
        status.setPourcentage(Math.min(100, (int) ((courant * 100.0) / total)));
    }

    public synchronized void terminer() {
        status.setEnCours(false);
        status.setEtapeActuelle("Terminé");
        status.setCourant(status.getTotal());
        status.setPourcentage(100);
        status.setFin(LocalDateTime.now());
    }

    public synchronized void terminerAvecErreurs(int erreurs) {
        status.setEnCours(false);
        status.setEtapeActuelle(erreurs <= 0 ? "Terminé" : "Terminé avec " + erreurs + " erreur(s)");
        status.setCourant(status.getTotal());
        status.setPourcentage(100);
        status.setFin(LocalDateTime.now());
    }

    public synchronized void echouer(String message) {
        status.setEnCours(false);
        status.setEtapeActuelle(message == null || message.isBlank() ? "Échec du Batch DTC" : message);
        status.setFin(LocalDateTime.now());
    }
}
