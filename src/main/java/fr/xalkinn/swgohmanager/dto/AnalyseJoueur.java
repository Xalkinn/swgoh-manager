package fr.xalkinn.swgohmanager.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Résultat d'une analyse ponctuelle d'un joueur externe.
 *
 * Ce DTO est construit en mémoire depuis /player : il n'est pas persisté dans
 * les tables joueur/personnage/mods de SWGOH Manager.
 */
public class AnalyseJoueur {

    private String nom;
    private String allyCode;
    private String playerId;
    private String guildId;
    private String guildName;
    private int niveau;

    private long pgTotal;
    private long pgPerso;
    private long pgVaisseaux;

    private String ligueGac;
    private int skillRatingGac;
    private int rangArenePersonnages;
    private int rangAreneFlotte;

    private int totalPersonnages;
    private int totalVaisseaux;
    private int personnages7Etoiles;
    private int personnagesG13;
    private int personnagesRelique;
    private int personnagesR5Plus;
    private int personnagesR7Plus;
    private int personnagesR8Plus;
    private int personnagesR9Plus;
    private int personnagesR10;

    private ModStats modStats = new ModStats();
    private List<RosterCharacter> roster = new ArrayList<>();
    private ComparaisonGuilde comparaisonGuilde = new ComparaisonGuilde();

    private List<OmicronPrioritySummary> omicronPriorities = new ArrayList<>();
    private int omicronPlanAtteint;
    private int omicronPlanTotal;
    private double omicronPlanPourcentage;

    private List<TbPerformance> tbPerformances = new ArrayList<>();
    private String tbMessage;

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getAllyCode() { return allyCode; }
    public void setAllyCode(String allyCode) { this.allyCode = allyCode; }
    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
    public String getGuildId() { return guildId; }
    public void setGuildId(String guildId) { this.guildId = guildId; }
    public String getGuildName() { return guildName; }
    public void setGuildName(String guildName) { this.guildName = guildName; }
    public int getNiveau() { return niveau; }
    public void setNiveau(int niveau) { this.niveau = niveau; }
    public long getPgTotal() { return pgTotal; }
    public void setPgTotal(long pgTotal) { this.pgTotal = pgTotal; }
    public long getPgPerso() { return pgPerso; }
    public void setPgPerso(long pgPerso) { this.pgPerso = pgPerso; }
    public long getPgVaisseaux() { return pgVaisseaux; }
    public void setPgVaisseaux(long pgVaisseaux) { this.pgVaisseaux = pgVaisseaux; }
    public String getLigueGac() { return ligueGac; }
    public void setLigueGac(String ligueGac) { this.ligueGac = ligueGac; }
    public int getSkillRatingGac() { return skillRatingGac; }
    public void setSkillRatingGac(int skillRatingGac) { this.skillRatingGac = skillRatingGac; }
    public int getRangArenePersonnages() { return rangArenePersonnages; }
    public void setRangArenePersonnages(int rangArenePersonnages) { this.rangArenePersonnages = rangArenePersonnages; }
    public int getRangAreneFlotte() { return rangAreneFlotte; }
    public void setRangAreneFlotte(int rangAreneFlotte) { this.rangAreneFlotte = rangAreneFlotte; }
    public int getTotalPersonnages() { return totalPersonnages; }
    public void setTotalPersonnages(int totalPersonnages) { this.totalPersonnages = totalPersonnages; }
    public int getTotalVaisseaux() { return totalVaisseaux; }
    public void setTotalVaisseaux(int totalVaisseaux) { this.totalVaisseaux = totalVaisseaux; }
    public int getPersonnages7Etoiles() { return personnages7Etoiles; }
    public void setPersonnages7Etoiles(int personnages7Etoiles) { this.personnages7Etoiles = personnages7Etoiles; }
    public int getPersonnagesG13() { return personnagesG13; }
    public void setPersonnagesG13(int personnagesG13) { this.personnagesG13 = personnagesG13; }
    public int getPersonnagesRelique() { return personnagesRelique; }
    public void setPersonnagesRelique(int personnagesRelique) { this.personnagesRelique = personnagesRelique; }
    public int getPersonnagesR5Plus() { return personnagesR5Plus; }
    public void setPersonnagesR5Plus(int personnagesR5Plus) { this.personnagesR5Plus = personnagesR5Plus; }
    public int getPersonnagesR7Plus() { return personnagesR7Plus; }
    public void setPersonnagesR7Plus(int personnagesR7Plus) { this.personnagesR7Plus = personnagesR7Plus; }
    public int getPersonnagesR8Plus() { return personnagesR8Plus; }
    public void setPersonnagesR8Plus(int personnagesR8Plus) { this.personnagesR8Plus = personnagesR8Plus; }
    public int getPersonnagesR9Plus() { return personnagesR9Plus; }
    public void setPersonnagesR9Plus(int personnagesR9Plus) { this.personnagesR9Plus = personnagesR9Plus; }
    public int getPersonnagesR10() { return personnagesR10; }
    public void setPersonnagesR10(int personnagesR10) { this.personnagesR10 = personnagesR10; }
    public ModStats getModStats() { return modStats; }
    public void setModStats(ModStats modStats) { this.modStats = modStats; }
    public List<RosterCharacter> getRoster() { return roster; }
    public void setRoster(List<RosterCharacter> roster) { this.roster = roster; }
    public ComparaisonGuilde getComparaisonGuilde() { return comparaisonGuilde; }
    public void setComparaisonGuilde(ComparaisonGuilde comparaisonGuilde) { this.comparaisonGuilde = comparaisonGuilde; }
    public List<OmicronPrioritySummary> getOmicronPriorities() { return omicronPriorities; }
    public void setOmicronPriorities(List<OmicronPrioritySummary> omicronPriorities) { this.omicronPriorities = omicronPriorities; }
    public int getOmicronPlanAtteint() { return omicronPlanAtteint; }
    public void setOmicronPlanAtteint(int omicronPlanAtteint) { this.omicronPlanAtteint = omicronPlanAtteint; }
    public int getOmicronPlanTotal() { return omicronPlanTotal; }
    public void setOmicronPlanTotal(int omicronPlanTotal) { this.omicronPlanTotal = omicronPlanTotal; }
    public double getOmicronPlanPourcentage() { return omicronPlanPourcentage; }
    public void setOmicronPlanPourcentage(double omicronPlanPourcentage) { this.omicronPlanPourcentage = omicronPlanPourcentage; }
    public List<TbPerformance> getTbPerformances() { return tbPerformances; }
    public void setTbPerformances(List<TbPerformance> tbPerformances) { this.tbPerformances = tbPerformances; }
    public String getTbMessage() { return tbMessage; }
    public void setTbMessage(String tbMessage) { this.tbMessage = tbMessage; }

    public static class ModStats {
        private int total;
        private int sansVitesse;
        private int vitesse1a14;
        private int vitesse15a19;
        private int vitesse20a24;
        private int vitesse25Plus;
        private int modsNormaux;
        private int modsOr;
        private double vitesseMoyenne;
        private double modQ;

        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
        public int getSansVitesse() { return sansVitesse; }
        public void setSansVitesse(int sansVitesse) { this.sansVitesse = sansVitesse; }
        public int getVitesse1a14() { return vitesse1a14; }
        public void setVitesse1a14(int vitesse1a14) { this.vitesse1a14 = vitesse1a14; }
        public int getVitesse15a19() { return vitesse15a19; }
        public void setVitesse15a19(int vitesse15a19) { this.vitesse15a19 = vitesse15a19; }
        public int getVitesse20a24() { return vitesse20a24; }
        public void setVitesse20a24(int vitesse20a24) { this.vitesse20a24 = vitesse20a24; }
        public int getVitesse25Plus() { return vitesse25Plus; }
        public void setVitesse25Plus(int vitesse25Plus) { this.vitesse25Plus = vitesse25Plus; }
        public int getModsNormaux() { return modsNormaux; }
        public void setModsNormaux(int modsNormaux) { this.modsNormaux = modsNormaux; }
        public int getModsOr() { return modsOr; }
        public void setModsOr(int modsOr) { this.modsOr = modsOr; }
        public double getVitesseMoyenne() { return vitesseMoyenne; }
        public void setVitesseMoyenne(double vitesseMoyenne) { this.vitesseMoyenne = vitesseMoyenne; }
        public double getModQ() { return modQ; }
        public void setModQ(double modQ) { this.modQ = modQ; }

        public int getVitesse20Plus() {
            return vitesse20a24 + vitesse25Plus;
        }

        public int getVitesse15Plus() {
            return vitesse15a19 + vitesse20a24 + vitesse25Plus;
        }
    }

    public static class RosterCharacter {
        private final String nom;
        private final String nomFrancais;
        private final String baseId;
        private final int etoiles;
        private final int niveau;
        private final int gear;
        private final int relic;

        public RosterCharacter(
                String nom,
                String nomFrancais,
                String baseId,
                int etoiles,
                int niveau,
                int gear,
                int relic) {
            this.nom = nom;
            this.nomFrancais = nomFrancais;
            this.baseId = baseId;
            this.etoiles = etoiles;
            this.niveau = niveau;
            this.gear = gear;
            this.relic = relic;
        }

        public String getNom() { return nom; }
        public String getNomFrancais() { return nomFrancais; }
        public String getBaseId() { return baseId; }
        public int getEtoiles() { return etoiles; }
        public int getNiveau() { return niveau; }
        public int getGear() { return gear; }
        public int getRelic() { return relic; }
    }


    public static class OmicronPrioritySummary {
        private int priorite;
        private int atteint;
        private int total;
        private int manquants;
        private double pourcentage;
        private List<OmicronPriorityDetail> manquantsDetails = new ArrayList<>();

        public int getPriorite() { return priorite; }
        public void setPriorite(int priorite) { this.priorite = priorite; }
        public int getAtteint() { return atteint; }
        public void setAtteint(int atteint) { this.atteint = atteint; }
        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
        public int getManquants() { return manquants; }
        public void setManquants(int manquants) { this.manquants = manquants; }
        public double getPourcentage() { return pourcentage; }
        public void setPourcentage(double pourcentage) { this.pourcentage = pourcentage; }
        public List<OmicronPriorityDetail> getManquantsDetails() { return manquantsDetails; }
        public void setManquantsDetails(List<OmicronPriorityDetail> manquantsDetails) { this.manquantsDetails = manquantsDetails; }
    }

    public static class OmicronPriorityDetail {
        private final String nomPersonnage;
        private final String typeCapacite;
        private final String categorie;
        private final String categorieLabel;

        public OmicronPriorityDetail(String nomPersonnage, String typeCapacite, String categorie, String categorieLabel) {
            this.nomPersonnage = nomPersonnage;
            this.typeCapacite = typeCapacite;
            this.categorie = categorie;
            this.categorieLabel = categorieLabel;
        }

        public String getNomPersonnage() { return nomPersonnage; }
        public String getTypeCapacite() { return typeCapacite; }
        public String getCategorie() { return categorie; }
        public String getCategorieLabel() { return categorieLabel; }
    }

    public static class TbPerformance {
        private String instanceId;
        private String definitionId;
        private String dateFin;
        private long endTimeEpoch;
        private int totalStars;
        private long combatsTentes;
        private long vagues;
        private double vaguesParCombat;
        private long msTentees;
        private long msReussies;
        private double tauxMs;
        private long puissanceDeployee;
        private long unitesDonnees;
        private List<TbPhasePerformance> phases = new ArrayList<>();
        private List<TbMissionPerformance> missionsSpeciales = new ArrayList<>();

        public String getInstanceId() { return instanceId; }
        public void setInstanceId(String instanceId) { this.instanceId = instanceId; }
        public String getDefinitionId() { return definitionId; }
        public void setDefinitionId(String definitionId) { this.definitionId = definitionId; }
        public String getDateFin() { return dateFin; }
        public void setDateFin(String dateFin) { this.dateFin = dateFin; }
        public long getEndTimeEpoch() { return endTimeEpoch; }
        public void setEndTimeEpoch(long endTimeEpoch) { this.endTimeEpoch = endTimeEpoch; }
        public int getTotalStars() { return totalStars; }
        public void setTotalStars(int totalStars) { this.totalStars = totalStars; }
        public long getCombatsTentes() { return combatsTentes; }
        public void setCombatsTentes(long combatsTentes) { this.combatsTentes = combatsTentes; }
        public long getVagues() { return vagues; }
        public void setVagues(long vagues) { this.vagues = vagues; }
        public double getVaguesParCombat() { return vaguesParCombat; }
        public void setVaguesParCombat(double vaguesParCombat) { this.vaguesParCombat = vaguesParCombat; }
        public long getMsTentees() { return msTentees; }
        public void setMsTentees(long msTentees) { this.msTentees = msTentees; }
        public long getMsReussies() { return msReussies; }
        public void setMsReussies(long msReussies) { this.msReussies = msReussies; }
        public double getTauxMs() { return tauxMs; }
        public void setTauxMs(double tauxMs) { this.tauxMs = tauxMs; }
        public long getPuissanceDeployee() { return puissanceDeployee; }
        public void setPuissanceDeployee(long puissanceDeployee) { this.puissanceDeployee = puissanceDeployee; }
        public long getUnitesDonnees() { return unitesDonnees; }
        public void setUnitesDonnees(long unitesDonnees) { this.unitesDonnees = unitesDonnees; }
        public List<TbPhasePerformance> getPhases() { return phases; }
        public void setPhases(List<TbPhasePerformance> phases) { this.phases = phases; }
        public List<TbMissionPerformance> getMissionsSpeciales() { return missionsSpeciales; }
        public void setMissionsSpeciales(List<TbMissionPerformance> missionsSpeciales) { this.missionsSpeciales = missionsSpeciales; }

        public long getMsEchecs() {
            return Math.max(0L, msTentees - msReussies);
        }
    }

    public static class TbMissionPerformance {
        private int phase;
        private int conflict;
        private boolean bonus;
        private int covertNum;
        private String label;
        private boolean tentee;
        private boolean reussie;

        public int getPhase() { return phase; }
        public void setPhase(int phase) { this.phase = phase; }
        public int getConflict() { return conflict; }
        public void setConflict(int conflict) { this.conflict = conflict; }
        public boolean isBonus() { return bonus; }
        public void setBonus(boolean bonus) { this.bonus = bonus; }
        public int getCovertNum() { return covertNum; }
        public void setCovertNum(int covertNum) { this.covertNum = covertNum; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public boolean isTentee() { return tentee; }
        public void setTentee(boolean tentee) { this.tentee = tentee; }
        public boolean isReussie() { return reussie; }
        public void setReussie(boolean reussie) { this.reussie = reussie; }

        public boolean isEchec() {
            return tentee && !reussie;
        }

        public String getStatut() {
            if (reussie) return "Réussie";
            if (tentee) return "Échec";
            return "Non tentée";
        }
    }

    public static class TbPhasePerformance {
        private int phase;
        private long combatsTentes;
        private long vagues;
        private double vaguesParCombat;
        private long msTentees;
        private long msReussies;

        public int getPhase() { return phase; }
        public void setPhase(int phase) { this.phase = phase; }
        public long getCombatsTentes() { return combatsTentes; }
        public void setCombatsTentes(long combatsTentes) { this.combatsTentes = combatsTentes; }
        public long getVagues() { return vagues; }
        public void setVagues(long vagues) { this.vagues = vagues; }
        public double getVaguesParCombat() { return vaguesParCombat; }
        public void setVaguesParCombat(double vaguesParCombat) { this.vaguesParCombat = vaguesParCombat; }
        public long getMsTentees() { return msTentees; }
        public void setMsTentees(long msTentees) { this.msTentees = msTentees; }
        public long getMsReussies() { return msReussies; }
        public void setMsReussies(long msReussies) { this.msReussies = msReussies; }
    }

    public static class ComparaisonGuilde {
        private int nombreJoueurs;
        private List<MetricComparison> metriques = new ArrayList<>();

        public int getNombreJoueurs() { return nombreJoueurs; }
        public void setNombreJoueurs(int nombreJoueurs) { this.nombreJoueurs = nombreJoueurs; }
        public List<MetricComparison> getMetriques() { return metriques; }
        public void setMetriques(List<MetricComparison> metriques) { this.metriques = metriques; }
    }

    public static class MetricComparison {
        private final String code;
        private final String label;
        private final String candidat;
        private final String moyenne;
        private final String mediane;
        private final String ecart;
        private final String tendance;
        private final String tendanceLibelle;

        public MetricComparison(
                String code,
                String label,
                String candidat,
                String moyenne,
                String mediane,
                String ecart,
                String tendance,
                String tendanceLibelle) {
            this.code = code;
            this.label = label;
            this.candidat = candidat;
            this.moyenne = moyenne;
            this.mediane = mediane;
            this.ecart = ecart;
            this.tendance = tendance;
            this.tendanceLibelle = tendanceLibelle;
        }

        public String getCode() { return code; }
        public String getLabel() { return label; }
        public String getCandidat() { return candidat; }
        public String getMoyenne() { return moyenne; }
        public String getMediane() { return mediane; }
        public String getEcart() { return ecart; }
        public String getTendance() { return tendance; }
        public String getTendanceLibelle() { return tendanceLibelle; }
    }
}
