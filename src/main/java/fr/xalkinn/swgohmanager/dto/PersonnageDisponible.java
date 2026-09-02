package fr.xalkinn.swgohmanager.dto;

/**
 * Personnage proposé dans une liste de choix.
 * nom reste la valeur brute historique ; nomFrancais est destiné à l'écran.
 */
public class PersonnageDisponible {

    private String nom;
    private String baseId;
    private String nomFrancais;

    public PersonnageDisponible() {
    }

    public PersonnageDisponible(String nom, String baseId, String nomFrancais) {
        this.nom = nom;
        this.baseId = baseId;
        this.nomFrancais = nomFrancais;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getBaseId() {
        return baseId;
    }

    public void setBaseId(String baseId) {
        this.baseId = baseId;
    }

    public String getNomFrancais() {
        return nomFrancais;
    }

    public void setNomFrancais(String nomFrancais) {
        this.nomFrancais = nomFrancais;
    }
}
