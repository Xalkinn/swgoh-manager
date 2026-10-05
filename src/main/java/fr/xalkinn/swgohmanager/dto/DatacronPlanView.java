package fr.xalkinn.swgohmanager.dto;

import java.util.ArrayList;
import java.util.List;

/** DTOs légers du plan Datacron de guilde. */
public final class DatacronPlanView {

    private DatacronPlanView() {}

    public static class AffixOption {
        private long optionId;
        private int setId;
        private int tier;
        private String abilityId;
        private String targetRule;
        private String tags;
        private String titre;
        private String description;
        private String cible;
        private String selectLabel;
        private String personnageBaseId;
        private String personnageNom;

        public long getOptionId() { return optionId; }
        public void setOptionId(long optionId) { this.optionId = optionId; }
        public int getSetId() { return setId; }
        public void setSetId(int setId) { this.setId = setId; }
        public int getTier() { return tier; }
        public void setTier(int tier) { this.tier = tier; }
        public String getAbilityId() { return abilityId; }
        public void setAbilityId(String abilityId) { this.abilityId = abilityId; }
        public String getTargetRule() { return targetRule; }
        public void setTargetRule(String targetRule) { this.targetRule = targetRule; }
        public String getTags() { return tags; }
        public void setTags(String tags) { this.tags = tags; }
        public String getTitre() { return titre; }
        public void setTitre(String titre) { this.titre = titre; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getCible() { return cible; }
        public void setCible(String cible) { this.cible = cible; }
        public String getSelectLabel() { return selectLabel; }
        public void setSelectLabel(String selectLabel) { this.selectLabel = selectLabel; }
        public String getPersonnageBaseId() { return personnageBaseId; }
        public void setPersonnageBaseId(String personnageBaseId) { this.personnageBaseId = personnageBaseId; }
        public String getPersonnageNom() { return personnageNom; }
        public void setPersonnageNom(String personnageNom) { this.personnageNom = personnageNom; }
    }

    public static class PlanRow {
        private long id;
        private int setId;
        private String personnageBaseId;
        private String personnageNom;
        private AffixOption t3;
        private AffixOption t6;
        private AffixOption t9;
        private int totalJoueurs;
        private int possesseurs;
        private int manquants;
        private int manqueUnTier;
        private String modalId;
        private List<JoueurMatch> possesseursDetails = new ArrayList<>();
        private List<JoueurMatch> manquantsDetails = new ArrayList<>();

        public long getId() { return id; }
        public void setId(long id) { this.id = id; }
        public int getSetId() { return setId; }
        public void setSetId(int setId) { this.setId = setId; }
        public String getPersonnageBaseId() { return personnageBaseId; }
        public void setPersonnageBaseId(String personnageBaseId) { this.personnageBaseId = personnageBaseId; }
        public String getPersonnageNom() { return personnageNom; }
        public void setPersonnageNom(String personnageNom) { this.personnageNom = personnageNom; }
        public AffixOption getT3() { return t3; }
        public void setT3(AffixOption t3) { this.t3 = t3; }
        public AffixOption getT6() { return t6; }
        public void setT6(AffixOption t6) { this.t6 = t6; }
        public AffixOption getT9() { return t9; }
        public void setT9(AffixOption t9) { this.t9 = t9; }
        public int getTotalJoueurs() { return totalJoueurs; }
        public void setTotalJoueurs(int totalJoueurs) { this.totalJoueurs = totalJoueurs; }
        public int getPossesseurs() { return possesseurs; }
        public void setPossesseurs(int possesseurs) { this.possesseurs = possesseurs; }
        public int getManquants() { return manquants; }
        public void setManquants(int manquants) { this.manquants = manquants; }
        public int getManqueUnTier() { return manqueUnTier; }
        public void setManqueUnTier(int manqueUnTier) { this.manqueUnTier = manqueUnTier; }
        public String getModalId() { return modalId; }
        public void setModalId(String modalId) { this.modalId = modalId; }
        public List<JoueurMatch> getPossesseursDetails() { return possesseursDetails; }
        public void setPossesseursDetails(List<JoueurMatch> possesseursDetails) { this.possesseursDetails = possesseursDetails; }
        public List<JoueurMatch> getManquantsDetails() { return manquantsDetails; }
        public void setManquantsDetails(List<JoueurMatch> manquantsDetails) { this.manquantsDetails = manquantsDetails; }
    }

    public static class JoueurMatch {
        private int joueurId;
        private String playerId;
        private String playerName;
        private String datacrons;
        private String meilleurDatacronId;
        private int meilleurDatacronNiveau;
        private boolean t3Ok;
        private boolean t6Ok;
        private boolean t9Ok;
        private int tiersOk;

        public JoueurMatch() {}

        public JoueurMatch(int joueurId, String playerId, String playerName, String datacrons) {
            this.joueurId = joueurId;
            this.playerId = playerId;
            this.playerName = playerName;
            this.datacrons = datacrons;
        }

        public int getJoueurId() { return joueurId; }
        public void setJoueurId(int joueurId) { this.joueurId = joueurId; }
        public String getPlayerId() { return playerId; }
        public void setPlayerId(String playerId) { this.playerId = playerId; }
        public String getPlayerName() { return playerName; }
        public void setPlayerName(String playerName) { this.playerName = playerName; }
        public String getDatacrons() { return datacrons; }
        public void setDatacrons(String datacrons) { this.datacrons = datacrons; }
        public String getMeilleurDatacronId() { return meilleurDatacronId; }
        public void setMeilleurDatacronId(String meilleurDatacronId) { this.meilleurDatacronId = meilleurDatacronId; }
        public int getMeilleurDatacronNiveau() { return meilleurDatacronNiveau; }
        public void setMeilleurDatacronNiveau(int meilleurDatacronNiveau) { this.meilleurDatacronNiveau = meilleurDatacronNiveau; }
        public boolean isT3Ok() { return t3Ok; }
        public void setT3Ok(boolean t3Ok) { this.t3Ok = t3Ok; recalculerTiers(); }
        public boolean isT6Ok() { return t6Ok; }
        public void setT6Ok(boolean t6Ok) { this.t6Ok = t6Ok; recalculerTiers(); }
        public boolean isT9Ok() { return t9Ok; }
        public void setT9Ok(boolean t9Ok) { this.t9Ok = t9Ok; recalculerTiers(); }
        public int getTiersOk() { return tiersOk; }
        public boolean isComplet() { return tiersOk == 3; }
        public boolean isManqueUnTier() { return tiersOk == 2; }

        public String getManques() {
            List<String> manques = new ArrayList<>();
            if (!t3Ok) manques.add("T3");
            if (!t6Ok) manques.add("T6");
            if (!t9Ok) manques.add("T9");
            return manques.isEmpty() ? "Complet" : "Manque " + String.join(" + ", manques);
        }

        private void recalculerTiers() {
            tiersOk = (t3Ok ? 1 : 0) + (t6Ok ? 1 : 0) + (t9Ok ? 1 : 0);
        }
    }
}
