package fr.xalkinn.swgohmanager.dto;

public class GuildeDetectee {

    private final String guildId;
    private final String nom;
    private final int nombreMembres;

    public GuildeDetectee(String guildId, String nom, int nombreMembres) {
        this.guildId = guildId;
        this.nom = nom;
        this.nombreMembres = nombreMembres;
    }

    public String getGuildId() {
        return guildId;
    }

    public String getNom() {
        return nom;
    }

    public int getNombreMembres() {
        return nombreMembres;
    }
}
