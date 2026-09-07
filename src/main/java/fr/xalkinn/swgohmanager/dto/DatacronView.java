package fr.xalkinn.swgohmanager.dto;

import java.util.ArrayList;
import java.util.List;

public class DatacronView {

    private int total;
    private int totalT9Plus;
    private int totalFocused;
    private int joueursAvecDatacrons;
    private Integer joueurSelectionneId;
    private String joueurSelectionneNom;
    private List<JoueurResume> joueurs = new ArrayList<>();
    private List<DatacronDetail> datacrons = new ArrayList<>();
    private List<DatacronSetGroupe> sets = new ArrayList<>();
    private List<StatResume> stats = new ArrayList<>();

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public int getTotalT9Plus() { return totalT9Plus; }
    public void setTotalT9Plus(int totalT9Plus) { this.totalT9Plus = totalT9Plus; }
    public int getTotalFocused() { return totalFocused; }
    public void setTotalFocused(int totalFocused) { this.totalFocused = totalFocused; }
    public int getJoueursAvecDatacrons() { return joueursAvecDatacrons; }
    public void setJoueursAvecDatacrons(int joueursAvecDatacrons) { this.joueursAvecDatacrons = joueursAvecDatacrons; }
    public Integer getJoueurSelectionneId() { return joueurSelectionneId; }
    public void setJoueurSelectionneId(Integer joueurSelectionneId) { this.joueurSelectionneId = joueurSelectionneId; }
    public String getJoueurSelectionneNom() { return joueurSelectionneNom; }
    public void setJoueurSelectionneNom(String joueurSelectionneNom) { this.joueurSelectionneNom = joueurSelectionneNom; }
    public List<JoueurResume> getJoueurs() { return joueurs; }
    public void setJoueurs(List<JoueurResume> joueurs) { this.joueurs = joueurs; }
    public List<DatacronDetail> getDatacrons() { return datacrons; }
    public void setDatacrons(List<DatacronDetail> datacrons) { this.datacrons = datacrons; }
    public List<DatacronSetGroupe> getSets() { return sets; }
    public void setSets(List<DatacronSetGroupe> sets) { this.sets = sets; }
    public List<StatResume> getStats() { return stats; }
    public void setStats(List<StatResume> stats) { this.stats = stats; }

    public static class JoueurResume {
        private int id;
        private String nom;
        private int total;
        private int t3;
        private int t6;
        private int t9;
        private int t12;
        private int t15;
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
        public int getT12() { return t12; }
        public void setT12(int t12) { this.t12 = t12; }
        public int getT15() { return t15; }
        public void setT15(int t15) { this.t15 = t15; }
        public int getFocused() { return focused; }
        public void setFocused(int focused) { this.focused = focused; }
    }


    public static class DatacronSetGroupe {
        private int setId;
        private int total;
        private int t9Plus;
        private int focused;
        private List<DatacronDetail> datacrons = new ArrayList<>();

        public int getSetId() { return setId; }
        public void setSetId(int setId) { this.setId = setId; }
        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
        public int getT9Plus() { return t9Plus; }
        public void setT9Plus(int t9Plus) { this.t9Plus = t9Plus; }
        public int getFocused() { return focused; }
        public void setFocused(int focused) { this.focused = focused; }
        public List<DatacronDetail> getDatacrons() { return datacrons; }
        public void setDatacrons(List<DatacronDetail> datacrons) { this.datacrons = datacrons; }
    }

    public static class DatacronDetail {
        private long id;
        private String datacronId;
        private int setId;
        private String templateId;
        private int niveau;
        private boolean focused;
        private boolean locked;
        private int rerollCount;
        private String tags;
        private List<AffixDetail> affixes = new ArrayList<>();

        public long getId() { return id; }
        public void setId(long id) { this.id = id; }
        public String getDatacronId() { return datacronId; }
        public void setDatacronId(String datacronId) { this.datacronId = datacronId; }
        public int getSetId() { return setId; }
        public void setSetId(int setId) { this.setId = setId; }
        public String getTemplateId() { return templateId; }
        public void setTemplateId(String templateId) { this.templateId = templateId; }
        public int getNiveau() { return niveau; }
        public void setNiveau(int niveau) { this.niveau = niveau; }
        public boolean isFocused() { return focused; }
        public void setFocused(boolean focused) { this.focused = focused; }
        public boolean isLocked() { return locked; }
        public void setLocked(boolean locked) { this.locked = locked; }
        public int getRerollCount() { return rerollCount; }
        public void setRerollCount(int rerollCount) { this.rerollCount = rerollCount; }
        public String getTags() { return tags; }
        public void setTags(String tags) { this.tags = tags; }
        public List<AffixDetail> getAffixes() { return affixes; }
        public void setAffixes(List<AffixDetail> affixes) { this.affixes = affixes; }
    }

    public static class AffixDetail {
        private int position;
        private String tags;
        private String targetRule;
        private String abilityId;
        private Integer statType;
        private String statValue;
        private int requiredUnitTier;
        private int requiredRelicTier;
        private String scopeIcon;
        private String titre;
        private String description;
        private String statNom;
        private String statValeurAffichee;
        private String cibleAffichee;
        private String tagsAffiches;

        public int getPosition() { return position; }
        public void setPosition(int position) { this.position = position; }
        public String getTags() { return tags; }
        public void setTags(String tags) { this.tags = tags; }
        public String getTargetRule() { return targetRule; }
        public void setTargetRule(String targetRule) { this.targetRule = targetRule; }
        public String getAbilityId() { return abilityId; }
        public void setAbilityId(String abilityId) { this.abilityId = abilityId; }
        public Integer getStatType() { return statType; }
        public void setStatType(Integer statType) { this.statType = statType; }
        public String getStatValue() { return statValue; }
        public void setStatValue(String statValue) { this.statValue = statValue; }
        public int getRequiredUnitTier() { return requiredUnitTier; }
        public void setRequiredUnitTier(int requiredUnitTier) { this.requiredUnitTier = requiredUnitTier; }
        public int getRequiredRelicTier() { return requiredRelicTier; }
        public void setRequiredRelicTier(int requiredRelicTier) { this.requiredRelicTier = requiredRelicTier; }
        public String getScopeIcon() { return scopeIcon; }
        public void setScopeIcon(String scopeIcon) { this.scopeIcon = scopeIcon; }
        public String getTitre() { return titre; }
        public void setTitre(String titre) { this.titre = titre; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getStatNom() { return statNom; }
        public void setStatNom(String statNom) { this.statNom = statNom; }
        public String getStatValeurAffichee() { return statValeurAffichee; }
        public void setStatValeurAffichee(String statValeurAffichee) { this.statValeurAffichee = statValeurAffichee; }
        public String getCibleAffichee() { return cibleAffichee; }
        public void setCibleAffichee(String cibleAffichee) { this.cibleAffichee = cibleAffichee; }
        public String getTagsAffiches() { return tagsAffiches; }
        public void setTagsAffiches(String tagsAffiches) { this.tagsAffiches = tagsAffiches; }
        public boolean isMecanique() { return abilityId != null && !abilityId.isBlank(); }
    }

    public static class StatResume {
        private int statType;
        private int occurrences;
        private long valeurBrute;
        private String nom;
        private String valeurAffichee;

        public StatResume(int statType, int occurrences, long valeurBrute) {
            this.statType = statType;
            this.occurrences = occurrences;
            this.valeurBrute = valeurBrute;
        }
        public int getStatType() { return statType; }
        public int getOccurrences() { return occurrences; }
        public long getValeurBrute() { return valeurBrute; }
        public String getNom() { return nom; }
        public void setNom(String nom) { this.nom = nom; }
        public String getValeurAffichee() { return valeurAffichee; }
        public void setValeurAffichee(String valeurAffichee) { this.valeurAffichee = valeurAffichee; }
    }

    public static class CatalogueEtat {
        private String gameVersion;
        private String dateMaj;
        private int templates;
        private int affixSets;
        private int affixes;
        private int abilities;
        public String getGameVersion() { return gameVersion; }
        public void setGameVersion(String gameVersion) { this.gameVersion = gameVersion; }
        public String getDateMaj() { return dateMaj; }
        public void setDateMaj(String dateMaj) { this.dateMaj = dateMaj; }
        public int getTemplates() { return templates; }
        public void setTemplates(int templates) { this.templates = templates; }
        public int getAffixSets() { return affixSets; }
        public void setAffixSets(int affixSets) { this.affixSets = affixSets; }
        public int getAffixes() { return affixes; }
        public void setAffixes(int affixes) { this.affixes = affixes; }
        public int getAbilities() { return abilities; }
        public void setAbilities(int abilities) { this.abilities = abilities; }
    }

}
