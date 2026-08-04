package fr.xalkinn.swgohmanager.modele;

public class ObjectifJoueurResultat {

    private String nomJoueur;
    private String personnage;
    private int relicActuelle;
    private int relicCible;
    private boolean atteint;


    public String getNomJoueur() {
        return nomJoueur;
    }

    public void setNomJoueur(String nomJoueur) {
        this.nomJoueur = nomJoueur;
    }


    public String getPersonnage() {
        return personnage;
    }

    public void setPersonnage(String personnage) {
        this.personnage = personnage;
    }


    public int getRelicActuelle() {
        return relicActuelle;
    }

    public void setRelicActuelle(int relicActuelle) {
        this.relicActuelle = relicActuelle;
    }


    public int getRelicCible() {
        return relicCible;
    }

    public void setRelicCible(int relicCible) {
        this.relicCible = relicCible;
    }


    public boolean isAtteint() {
        return atteint;
    }

    public void setAtteint(boolean atteint) {
        this.atteint = atteint;
    }
}