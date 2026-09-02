package fr.xalkinn.swgohmanager.modele;

import org.springframework.data.annotation.PersistenceCreator;

public class ComparatifObjectif {

    private String joueur;
    private Integer relic_actuelle;
    private Integer relic_cible;

    @PersistenceCreator
    public ComparatifObjectif(
            String joueur,
            Integer relic_actuelle,
            Integer relic_cible) {

        this.joueur = joueur;
        this.relic_actuelle = relic_actuelle;
        this.relic_cible = relic_cible;
    }


    public String getJoueur() {
        return joueur;
    }

    public Integer getRelicActuelle() {
        return relic_actuelle;
    }

    public Integer getRelicCible() {
        return relic_cible;
    }
    public int getProgression() {

        if (relic_cible == null || relic_cible == 0 || relic_actuelle == null) {
            return 0;
        }

        int progression = (relic_actuelle * 100) / relic_cible;
        return Math.min(progression, 100);
    }

    public boolean isObjectifTermine() {
        return relic_actuelle != null
                && relic_cible != null
                && relic_actuelle >= relic_cible;
    }
}