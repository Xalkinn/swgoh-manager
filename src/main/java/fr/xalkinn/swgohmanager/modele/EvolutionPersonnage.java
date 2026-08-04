package fr.xalkinn.swgohmanager.modele;

public class EvolutionPersonnage {


    private String nom;

    private Integer ancienGear;
    private Integer nouveauGear;

    private Integer ancienneRelique;
    private Integer nouvelleRelique;

    private Integer anciennesEtoiles;
    private Integer nouvellesEtoiles;


    public EvolutionPersonnage() {
    }


    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
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


    public Integer getAnciennesEtoiles() {
        return anciennesEtoiles;
    }

    public void setAnciennesEtoiles(Integer anciennesEtoiles) {
        this.anciennesEtoiles = anciennesEtoiles;
    }


    public Integer getNouvellesEtoiles() {
        return nouvellesEtoiles;
    }

    public void setNouvellesEtoiles(Integer nouvellesEtoiles) {
        this.nouvellesEtoiles = nouvellesEtoiles;
    }

}