package fr.xalkinn.swgohmanager.modele;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("personnage")
public class Personnage {

    @Id
    private Integer id;
    private Integer extractionId;
    private Integer joueurId;
    private String baseId;
    private Integer etoiles;
    private Integer niveau;
    private Integer gear;
    private String nom;
    private Integer relic;
    private LocalDateTime dateCapture;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getExtractionId() {
        return extractionId;
    }

    public void setExtractionId(Integer extractionId) {
        this.extractionId = extractionId;
    }

    public Integer getJoueurId() {
        return joueurId;
    }

    public void setJoueurId(Integer joueurId) {
        this.joueurId = joueurId;
    }

    public String getBaseId() {
        return baseId;
    }

    public void setBaseId(String baseId) {
        this.baseId = baseId;
    }

    public Integer getEtoiles() {
        return etoiles;
    }

    public void setEtoiles(Integer etoiles) {
        this.etoiles = etoiles;
    }

    public Integer getNiveau() {
        return niveau;
    }

    public void setNiveau(Integer niveau) {
        this.niveau = niveau;
    }

    public Integer getGear() {
        return gear;
    }

    public void setGear(Integer gear) {
        this.gear = gear;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public Integer getRelic() {
        return relic;
    }

    public void setRelic(Integer relic) {
        this.relic = relic;
    }

    public LocalDateTime getDateCapture() {
        return dateCapture;
    }

    public void setDateCapture(LocalDateTime dateCapture) {
        this.dateCapture = dateCapture;
    }
}