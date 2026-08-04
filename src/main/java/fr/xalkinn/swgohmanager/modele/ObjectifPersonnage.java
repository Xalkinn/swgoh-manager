package fr.xalkinn.swgohmanager.modele;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("objectif_personnage")
public class ObjectifPersonnage {

    @Id
    private Integer id;

    private String baseId;

    private String nom;

    private Integer relicCible;

    private String commentaire;

    private Boolean actif;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }


    public String getBaseId() {
        return baseId;
    }

    public void setBaseId(String baseId) {
        this.baseId = baseId;
    }


    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }


    public Integer getRelicCible() {
        return relicCible;
    }

    public void setRelicCible(Integer relicCible) {
        this.relicCible = relicCible;
    }


    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }


    public Boolean getActif() {
        return actif;
    }

    public void setActif(Boolean actif) {
        this.actif = actif;
    }
}