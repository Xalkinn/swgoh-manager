package fr.xalkinn.swgohmanager.modele;

import java.time.LocalDateTime;

public class BatchStatus {

    private boolean enCours;
    private String nomBatch;
    private String etapeActuelle;
    private int courant;
    private int total;
    private int pourcentage;
    private LocalDateTime debut;
    private LocalDateTime fin;

    public boolean isEnCours() {
        return enCours;
    }

    public void setEnCours(boolean enCours) {
        this.enCours = enCours;
    }

    public String getNomBatch() {
        return nomBatch;
    }

    public void setNomBatch(String nomBatch) {
        this.nomBatch = nomBatch;
    }

    public String getEtapeActuelle() {
        return etapeActuelle;
    }

    public void setEtapeActuelle(String etapeActuelle) {
        this.etapeActuelle = etapeActuelle;
    }

    public int getCourant() {
        return courant;
    }

    public void setCourant(int courant) {
        this.courant = courant;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getPourcentage() {
        return pourcentage;
    }

    public void setPourcentage(int pourcentage) {
        this.pourcentage = pourcentage;
    }

    public LocalDateTime getDebut() {
        return debut;
    }

    public void setDebut(LocalDateTime debut) {
        this.debut = debut;
    }

    public LocalDateTime getFin() {
        return fin;
    }

    public void setFin(LocalDateTime fin) {
        this.fin = fin;
    }
}