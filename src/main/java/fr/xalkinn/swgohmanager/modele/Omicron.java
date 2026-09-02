package fr.xalkinn.swgohmanager.modele;

import org.springframework.data.annotation.Transient;


public class Omicron {


    private int id;
    private String joueurId;
    private String nomJoueur;
    private String personnage;

    @Transient
    private String nomFrancais;

    private String typeCapacite;
    private boolean actif;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getJoueurId() {
        return joueurId;
    }

    public void setJoueurId(String joueurId) {
        this.joueurId = joueurId;
    }

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

    public String getNomFrancais() {
        return nomFrancais;
    }

    public void setNomFrancais(String nomFrancais) {
        this.nomFrancais = nomFrancais;
    }

    public String getTypeCapacite() {
        return typeCapacite;
    }

    public void setTypeCapacite(String typeCapacite) {
        this.typeCapacite = typeCapacite;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }

}