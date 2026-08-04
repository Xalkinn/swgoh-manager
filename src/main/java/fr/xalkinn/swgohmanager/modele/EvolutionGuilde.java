package fr.xalkinn.swgohmanager.modele;

public class EvolutionGuilde {

    private String joueur;
    private String personnage;

    private Integer ancienGear;
    private Integer nouveauGear;

    private Integer ancienneRelique;
    private Integer nouvelleRelique;


    public String getJoueur() {
        return joueur;
    }

    public void setJoueur(String joueur) {
        this.joueur = joueur;
    }


    public String getPersonnage() {
        return personnage;
    }

    public void setPersonnage(String personnage) {
        this.personnage = personnage;
    }


    public Integer getAncienGear() {
        return ancienGear;
    }

    public void setAncienGear(Integer ancienGear) {
        this.ancienGear = ancienGear;
    }


    public Integer getNouveauGear() {
        return nouveauGear;
    }

    public void setNouveauGear(Integer nouveauGear) {
        this.nouveauGear = nouveauGear;
    }


    public Integer getAncienneRelique() {
        return ancienneRelique;
    }

    public void setAncienneRelique(Integer ancienneRelique) {
        this.ancienneRelique = ancienneRelique;
    }


    public Integer getNouvelleRelique() {
        return nouvelleRelique;
    }

    public void setNouvelleRelique(Integer nouvelleRelique) {
        this.nouvelleRelique = nouvelleRelique;
    }
}