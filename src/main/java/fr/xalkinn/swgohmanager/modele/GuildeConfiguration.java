package fr.xalkinn.swgohmanager.modele;

import java.time.LocalDateTime;

public class GuildeConfiguration {

    private Integer id;
    private String guildId;
    private String nom;
    private Boolean active;
    private LocalDateTime dateAjout;
    private LocalDateTime dateDerniereSynchro;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getGuildId() {
        return guildId;
    }

    public void setGuildId(String guildId) {
        this.guildId = guildId;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getDateAjout() {
        return dateAjout;
    }

    public void setDateAjout(LocalDateTime dateAjout) {
        this.dateAjout = dateAjout;
    }

    public LocalDateTime getDateDerniereSynchro() {
        return dateDerniereSynchro;
    }

    public void setDateDerniereSynchro(LocalDateTime dateDerniereSynchro) {
        this.dateDerniereSynchro = dateDerniereSynchro;
    }
}
