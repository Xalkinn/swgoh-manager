package fr.xalkinn.swgohmanager.modele;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;

@Table("joueur")
public class Joueur {

    @Id
    private Integer id;
    private String playerId;
    private String allyCode;
    private String nom;
    private String guilde;
    private LocalDateTime dateMaj;

    public Integer getId() {
        return id;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getAllyCode() {
        return allyCode;
    }

    public String getNom() {
        return nom;
    }

    public String getGuilde() {
        return guilde;
    }

    public LocalDateTime getDateMaj() {
        return dateMaj;
    }
}