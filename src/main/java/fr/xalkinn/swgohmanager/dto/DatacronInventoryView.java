package fr.xalkinn.swgohmanager.dto;

import java.util.ArrayList;
import java.util.List;

/** Vue synthétique de l'inventaire DTC pour un Set donné. */
public class DatacronInventoryView {

    private List<Integer> sets = new ArrayList<>();
    private Integer selectedSetId;
    private int totalJoueurs;
    private int joueursAvecDatacrons;
    private int totalDatacrons;
    private int t3Plus;
    private int t6Plus;
    private int t9Plus;
    private int focused;
    private List<PlayerRow> joueurs = new ArrayList<>();

    public List<Integer> getSets() { return sets; }
    public void setSets(List<Integer> sets) { this.sets = sets; }
    public Integer getSelectedSetId() { return selectedSetId; }
    public void setSelectedSetId(Integer selectedSetId) { this.selectedSetId = selectedSetId; }
    public int getTotalJoueurs() { return totalJoueurs; }
    public void setTotalJoueurs(int totalJoueurs) { this.totalJoueurs = totalJoueurs; }
    public int getJoueursAvecDatacrons() { return joueursAvecDatacrons; }
    public void setJoueursAvecDatacrons(int joueursAvecDatacrons) { this.joueursAvecDatacrons = joueursAvecDatacrons; }
    public int getTotalDatacrons() { return totalDatacrons; }
    public void setTotalDatacrons(int totalDatacrons) { this.totalDatacrons = totalDatacrons; }
    public int getT3Plus() { return t3Plus; }
    public void setT3Plus(int t3Plus) { this.t3Plus = t3Plus; }
    public int getT6Plus() { return t6Plus; }
    public void setT6Plus(int t6Plus) { this.t6Plus = t6Plus; }
    public int getT9Plus() { return t9Plus; }
    public void setT9Plus(int t9Plus) { this.t9Plus = t9Plus; }
    public int getFocused() { return focused; }
    public void setFocused(int focused) { this.focused = focused; }
    public List<PlayerRow> getJoueurs() { return joueurs; }
    public void setJoueurs(List<PlayerRow> joueurs) { this.joueurs = joueurs; }

    public static class PlayerRow {
        private int id;
        private String nom;
        private int total;
        private int t3;
        private int t6;
        private int t9;
        private int focused;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getNom() { return nom; }
        public void setNom(String nom) { this.nom = nom; }
        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
        public int getT3() { return t3; }
        public void setT3(int t3) { this.t3 = t3; }
        public int getT6() { return t6; }
        public void setT6(int t6) { this.t6 = t6; }
        public int getT9() { return t9; }
        public void setT9(int t9) { this.t9 = t9; }
        public int getFocused() { return focused; }
        public void setFocused(int focused) { this.focused = focused; }
    }
}
