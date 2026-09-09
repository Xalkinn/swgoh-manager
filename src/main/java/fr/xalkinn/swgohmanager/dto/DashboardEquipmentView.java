package fr.xalkinn.swgohmanager.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Données de synthèse « Équipements & Reliques » du dashboard guilde.
 *
 * Les compteurs sont calculés uniquement sur la dernière extraction disponible
 * de chaque membre actuellement présent dans la guilde.
 */
public class DashboardEquipmentView {

    private int g13;
    private int r5Plus;
    private int r7Plus;
    private int r9Plus;
    private int r10;

    private boolean progressionDisponible;
    private Integer extractionActuelle;
    private Integer extractionPrecedente;
    private int nouveauxG13;
    private int nouveauxR7Plus;
    private int nouveauxR9Plus;

    private List<RelicBucket> distribution = new ArrayList<>();
    private List<PlayerRow> joueurs = new ArrayList<>();
    private List<CharacterRow> personnages = new ArrayList<>();

    public int getG13() {
        return g13;
    }

    public void setG13(int g13) {
        this.g13 = g13;
    }

    public int getR5Plus() {
        return r5Plus;
    }

    public void setR5Plus(int r5Plus) {
        this.r5Plus = r5Plus;
    }

    public int getR7Plus() {
        return r7Plus;
    }

    public void setR7Plus(int r7Plus) {
        this.r7Plus = r7Plus;
    }

    public int getR9Plus() {
        return r9Plus;
    }

    public void setR9Plus(int r9Plus) {
        this.r9Plus = r9Plus;
    }

    public int getR10() {
        return r10;
    }

    public void setR10(int r10) {
        this.r10 = r10;
    }

    public boolean isProgressionDisponible() {
        return progressionDisponible;
    }

    public void setProgressionDisponible(boolean progressionDisponible) {
        this.progressionDisponible = progressionDisponible;
    }

    public Integer getExtractionActuelle() {
        return extractionActuelle;
    }

    public void setExtractionActuelle(Integer extractionActuelle) {
        this.extractionActuelle = extractionActuelle;
    }

    public Integer getExtractionPrecedente() {
        return extractionPrecedente;
    }

    public void setExtractionPrecedente(Integer extractionPrecedente) {
        this.extractionPrecedente = extractionPrecedente;
    }

    public int getNouveauxG13() {
        return nouveauxG13;
    }

    public void setNouveauxG13(int nouveauxG13) {
        this.nouveauxG13 = nouveauxG13;
    }

    public int getNouveauxR7Plus() {
        return nouveauxR7Plus;
    }

    public void setNouveauxR7Plus(int nouveauxR7Plus) {
        this.nouveauxR7Plus = nouveauxR7Plus;
    }

    public int getNouveauxR9Plus() {
        return nouveauxR9Plus;
    }

    public void setNouveauxR9Plus(int nouveauxR9Plus) {
        this.nouveauxR9Plus = nouveauxR9Plus;
    }

    public List<RelicBucket> getDistribution() {
        return distribution;
    }

    public void setDistribution(List<RelicBucket> distribution) {
        this.distribution = distribution;
    }

    public List<PlayerRow> getJoueurs() {
        return joueurs;
    }

    public void setJoueurs(List<PlayerRow> joueurs) {
        this.joueurs = joueurs;
    }

    public List<CharacterRow> getPersonnages() {
        return personnages;
    }

    public void setPersonnages(List<CharacterRow> personnages) {
        this.personnages = personnages;
    }

    public static class RelicBucket {
        private String code;
        private String label;
        private int count;
        private int percent;

        public RelicBucket() {
        }

        public RelicBucket(String code, String label, int count, int percent) {
            this.code = code;
            this.label = label;
            this.count = count;
            this.percent = percent;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public int getPercent() {
            return percent;
        }

        public void setPercent(int percent) {
            this.percent = percent;
        }
    }

    public static class PlayerRow {
        private Integer joueurId;
        private String playerId;
        private String nom;
        private int g13;
        private int r5Plus;
        private int r7Plus;
        private int r9Plus;
        private int r10;

        public Integer getJoueurId() {
            return joueurId;
        }

        public void setJoueurId(Integer joueurId) {
            this.joueurId = joueurId;
        }

        public String getPlayerId() {
            return playerId;
        }

        public void setPlayerId(String playerId) {
            this.playerId = playerId;
        }

        public String getNom() {
            return nom;
        }

        public void setNom(String nom) {
            this.nom = nom;
        }

        public int getG13() {
            return g13;
        }

        public void setG13(int g13) {
            this.g13 = g13;
        }

        public int getR5Plus() {
            return r5Plus;
        }

        public void setR5Plus(int r5Plus) {
            this.r5Plus = r5Plus;
        }

        public int getR7Plus() {
            return r7Plus;
        }

        public void setR7Plus(int r7Plus) {
            this.r7Plus = r7Plus;
        }

        public int getR9Plus() {
            return r9Plus;
        }

        public void setR9Plus(int r9Plus) {
            this.r9Plus = r9Plus;
        }

        public int getR10() {
            return r10;
        }

        public void setR10(int r10) {
            this.r10 = r10;
        }
    }

    public static class CharacterRow {
        private String baseId;
        private String nom;
        private int g13;
        private int r5Plus;
        private int r7Plus;
        private int r9Plus;
        private int r10;

        public String getBaseId() {
            return baseId;
        }

        public void setBaseId(String baseId) {
            this.baseId = baseId;
        }

        public String getNom() {
            return nom;
        }

        public void setNom(String nom) {
            this.nom = nom;
        }

        public int getG13() {
            return g13;
        }

        public void setG13(int g13) {
            this.g13 = g13;
        }

        public int getR5Plus() {
            return r5Plus;
        }

        public void setR5Plus(int r5Plus) {
            this.r5Plus = r5Plus;
        }

        public int getR7Plus() {
            return r7Plus;
        }

        public void setR7Plus(int r7Plus) {
            this.r7Plus = r7Plus;
        }

        public int getR9Plus() {
            return r9Plus;
        }

        public void setR9Plus(int r9Plus) {
            this.r9Plus = r9Plus;
        }

        public int getR10() {
            return r10;
        }

        public void setR10(int r10) {
            this.r10 = r10;
        }
    }
}
