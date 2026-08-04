package fr.xalkinn.swgohmanager.modele;

public class ResultatBatch {

    private boolean succes;
    private long dureeMs;
    private int nombreEtapes;

    public ResultatBatch() {
    }

    public ResultatBatch(boolean succes, long dureeMs, int nombreEtapes) {
        this.succes = succes;
        this.dureeMs = dureeMs;
        this.nombreEtapes = nombreEtapes;
    }

    public boolean isSucces() {
        return succes;
    }

    public void setSucces(boolean succes) {
        this.succes = succes;
    }

    public long getDureeMs() {
        return dureeMs;
    }

    public void setDureeMs(long dureeMs) {
        this.dureeMs = dureeMs;
    }

    public int getNombreEtapes() {
        return nombreEtapes;
    }
    public void setNombreEtapes(int nombreEtapes) {
        this.nombreEtapes = nombreEtapes;
    }
}