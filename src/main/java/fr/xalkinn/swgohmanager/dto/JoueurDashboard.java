package fr.xalkinn.swgohmanager.dto;

import java.util.ArrayList;
import java.util.List;

public class JoueurDashboard {

    private Integer id;
    private String nom;
    private String playerId;
    private String allyCode;
    private long pgPerso;
    private int totalPersonnages;
    private int personnages7Etoiles;
    private int personnagesG13;
    private int personnagesRelique;
    private String derniereCapture;

    private List<OmicronInfo> omicrons = new ArrayList<>();
    private ModStats modStats = new ModStats();
    private List<GearStat> gearStats = new ArrayList<>();
    private List<RelicStat> relicStats = new ArrayList<>();
    private List<RosterCharacter> roster = new ArrayList<>();

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
    public String getAllyCode() { return allyCode; }
    public void setAllyCode(String allyCode) { this.allyCode = allyCode; }
    public long getPgPerso() { return pgPerso; }
    public void setPgPerso(long pgPerso) { this.pgPerso = pgPerso; }
    public int getTotalPersonnages() { return totalPersonnages; }
    public void setTotalPersonnages(int totalPersonnages) { this.totalPersonnages = totalPersonnages; }
    public int getPersonnages7Etoiles() { return personnages7Etoiles; }
    public void setPersonnages7Etoiles(int personnages7Etoiles) { this.personnages7Etoiles = personnages7Etoiles; }
    public int getPersonnagesG13() { return personnagesG13; }
    public void setPersonnagesG13(int personnagesG13) { this.personnagesG13 = personnagesG13; }
    public int getPersonnagesRelique() { return personnagesRelique; }
    public void setPersonnagesRelique(int personnagesRelique) { this.personnagesRelique = personnagesRelique; }
    public String getDerniereCapture() { return derniereCapture; }
    public void setDerniereCapture(String derniereCapture) { this.derniereCapture = derniereCapture; }
    public List<OmicronInfo> getOmicrons() { return omicrons; }
    public void setOmicrons(List<OmicronInfo> omicrons) { this.omicrons = omicrons; }
    public ModStats getModStats() { return modStats; }
    public void setModStats(ModStats modStats) { this.modStats = modStats; }
    public List<GearStat> getGearStats() { return gearStats; }
    public void setGearStats(List<GearStat> gearStats) { this.gearStats = gearStats; }
    public List<RelicStat> getRelicStats() { return relicStats; }
    public void setRelicStats(List<RelicStat> relicStats) { this.relicStats = relicStats; }
    public List<RosterCharacter> getRoster() { return roster; }
    public void setRoster(List<RosterCharacter> roster) { this.roster = roster; }

    public static class OmicronInfo {
        private String personnage;
        private String nomFrancais;
        private String typeCapacite;
        private boolean actif;
        public OmicronInfo() {}
        public OmicronInfo(String personnage, String nomFrancais, String typeCapacite, boolean actif) {
            this.personnage = personnage;
            this.nomFrancais = nomFrancais;
            this.typeCapacite = typeCapacite;
            this.actif = actif;
        }
        public String getPersonnage() { return personnage; }
        public String getNomFrancais() { return nomFrancais; }
        public String getTypeCapacite() { return typeCapacite; }
        public boolean isActif() { return actif; }
    }

    public static class ModStats {
        private int total;
        private int sansVitesse;
        private int vitesse1a14;
        private int vitesse15a19;
        private int vitesse20a24;
        private int vitesse25Plus;
        private int rarete1, rarete2, rarete3, rarete4, rarete5, rarete6;
        private int modsNormaux;
        private int modsOr;
        private double vitesseMoyenne;
        private double modQ;

        public int getTotal() { return total; }
        public int getSansVitesse() { return sansVitesse; }
        public int getVitesse1a14() { return vitesse1a14; }
        public int getVitesse15a19() { return vitesse15a19; }
        public int getVitesse20a24() { return vitesse20a24; }
        public int getVitesse25Plus() { return vitesse25Plus; }
        public int getRarete1() { return rarete1; }
        public int getRarete2() { return rarete2; }
        public int getRarete3() { return rarete3; }
        public int getRarete4() { return rarete4; }
        public int getRarete5() { return rarete5; }
        public int getRarete6() { return rarete6; }
        public int getModsNormaux() { return modsNormaux; }
        public int getModsOr() { return modsOr; }
        public double getVitesseMoyenne() { return vitesseMoyenne; }
        public double getModQ() { return modQ; }
        public void setTotal(int v){total=v;} public void setSansVitesse(int v){sansVitesse=v;}
        public void setVitesse1a14(int v){vitesse1a14=v;} public void setVitesse15a19(int v){vitesse15a19=v;}
        public void setVitesse20a24(int v){vitesse20a24=v;} public void setVitesse25Plus(int v){vitesse25Plus=v;}
        public void setRarete1(int v){rarete1=v;} public void setRarete2(int v){rarete2=v;}
        public void setRarete3(int v){rarete3=v;} public void setRarete4(int v){rarete4=v;}
        public void setRarete5(int v){rarete5=v;} public void setRarete6(int v){rarete6=v;}
        public void setModsNormaux(int v){modsNormaux=v;} public void setModsOr(int v){modsOr=v;}
        public void setVitesseMoyenne(double v){vitesseMoyenne=v;} public void setModQ(double v){modQ=v;}
    }

    public static class GearStat {
        private String label; private int count;
        public GearStat(String label,int count){this.label=label;this.count=count;}
        public String getLabel(){return label;} public int getCount(){return count;}
    }

    public static class RelicStat {
        private String label; private int count;
        public RelicStat(String label,int count){this.label=label;this.count=count;}
        public String getLabel(){return label;} public int getCount(){return count;}
    }

    public static class RosterCharacter {
        private String nom, nomFrancais, baseId;
        private int etoiles, niveau, gear, relic;
        public RosterCharacter(String nom,String nomFrancais,String baseId,int etoiles,int niveau,int gear,int relic){
            this.nom=nom;this.nomFrancais=nomFrancais;this.baseId=baseId;
            this.etoiles=etoiles;this.niveau=niveau;this.gear=gear;this.relic=relic;
        }
        public String getNom(){return nom;}
        public String getNomFrancais(){return nomFrancais;}
        public String getBaseId(){return baseId;}
        public int getEtoiles(){return etoiles;} public int getNiveau(){return niveau;}
        public int getGear(){return gear;} public int getRelic(){return relic;}
    }
}
