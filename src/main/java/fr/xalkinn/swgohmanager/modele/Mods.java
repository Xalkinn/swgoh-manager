package fr.xalkinn.swgohmanager.modele;

public class Mods {

    private Integer id;
    private String modId;

    private Integer joueurId;

    private String personnage;

    private String emplacement;

    private Integer rarete;

    private String typeMod;

    private Integer vitesse;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }


    public String getModId() {
        return modId;
    }

    public void setModId(String modId) {
        this.modId = modId;
    }


    public Integer getJoueurId() {
        return joueurId;
    }

    public void setJoueurId(Integer joueurId) {
        this.joueurId = joueurId;
    }


    public String getPersonnage() {
        return personnage;
    }

    public void setPersonnage(String personnage) {
        this.personnage = personnage;
    }


    public String getEmplacement() {
        return emplacement;
    }

    public void setEmplacement(String emplacement) {
        this.emplacement = emplacement;
    }


    public Integer getRarete() {
        return rarete;
    }

    public void setRarete(Integer rarete) {
        this.rarete = rarete;
    }


    public String getTypeMod() {
        return typeMod;
    }

    public void setTypeMod(String typeMod) {
        this.typeMod = typeMod;
    }


    public Integer getVitesse() {
        return vitesse;
    }

    public void setVitesse(Integer vitesse) {
        this.vitesse = vitesse;
    }
}