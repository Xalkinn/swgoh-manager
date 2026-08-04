package fr.xalkinn.swgohmanager.dto;

public class PersonnageDisponible {

    private String nom;
    private String baseId;

    public PersonnageDisponible(String nom, String baseId) {
        this.nom = nom;
        this.baseId = baseId;
    }

    public String getNom() {
        return nom;
    }

    public String getBaseId() {
        return baseId;
    }
}