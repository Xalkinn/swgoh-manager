package fr.xalkinn.swgohmanager.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTOs d'affichage du module Territory Battle.
 */
public final class TerritoryBattleView {

    private TerritoryBattleView() {
    }

    public static class SummaryRow {
        private long id;
        private String instanceId;
        private String definitionId;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private int totalStars;
        private long vaguesP3;
        private long vaguesP4;
        private long vaguesP5;
        private long vaguesP6;
        private long msTentees;
        private String starsTrend;
        private String p3Trend;
        private String p4Trend;
        private String p5Trend;
        private String p6Trend;
        private String msTrend;

        public long getId() { return id; }
        public void setId(long id) { this.id = id; }
        public String getInstanceId() { return instanceId; }
        public void setInstanceId(String instanceId) { this.instanceId = instanceId; }
        public String getDefinitionId() { return definitionId; }
        public void setDefinitionId(String definitionId) { this.definitionId = definitionId; }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        public int getTotalStars() { return totalStars; }
        public void setTotalStars(int totalStars) { this.totalStars = totalStars; }
        public long getVaguesP3() { return vaguesP3; }
        public void setVaguesP3(long vaguesP3) { this.vaguesP3 = vaguesP3; }
        public long getVaguesP4() { return vaguesP4; }
        public void setVaguesP4(long vaguesP4) { this.vaguesP4 = vaguesP4; }
        public long getVaguesP5() { return vaguesP5; }
        public void setVaguesP5(long vaguesP5) { this.vaguesP5 = vaguesP5; }
        public long getVaguesP6() { return vaguesP6; }
        public void setVaguesP6(long vaguesP6) { this.vaguesP6 = vaguesP6; }
        public long getMsTentees() { return msTentees; }
        public void setMsTentees(long msTentees) { this.msTentees = msTentees; }
        public String getStarsTrend() { return starsTrend; }
        public void setStarsTrend(String starsTrend) { this.starsTrend = starsTrend; }
        public String getP3Trend() { return p3Trend; }
        public void setP3Trend(String p3Trend) { this.p3Trend = p3Trend; }
        public String getP4Trend() { return p4Trend; }
        public void setP4Trend(String p4Trend) { this.p4Trend = p4Trend; }
        public String getP5Trend() { return p5Trend; }
        public void setP5Trend(String p5Trend) { this.p5Trend = p5Trend; }
        public String getP6Trend() { return p6Trend; }
        public void setP6Trend(String p6Trend) { this.p6Trend = p6Trend; }
        public String getMsTrend() { return msTrend; }
        public void setMsTrend(String msTrend) { this.msTrend = msTrend; }
    }

    public static class RoundRow {
        private int roundNum;
        private long combatsTentes;
        private long vagues;
        private double vaguesParCombat;
        private long msTentees;
        private long puissanceDeployee;
        private long unitesDonnees;

        public int getRoundNum() { return roundNum; }
        public void setRoundNum(int roundNum) { this.roundNum = roundNum; }
        public long getCombatsTentes() { return combatsTentes; }
        public void setCombatsTentes(long combatsTentes) { this.combatsTentes = combatsTentes; }
        public long getVagues() { return vagues; }
        public void setVagues(long vagues) { this.vagues = vagues; }
        public double getVaguesParCombat() { return vaguesParCombat; }
        public void setVaguesParCombat(double vaguesParCombat) { this.vaguesParCombat = vaguesParCombat; }
        public long getMsTentees() { return msTentees; }
        public void setMsTentees(long msTentees) { this.msTentees = msTentees; }
        public long getPuissanceDeployee() { return puissanceDeployee; }
        public void setPuissanceDeployee(long puissanceDeployee) { this.puissanceDeployee = puissanceDeployee; }
        public long getUnitesDonnees() { return unitesDonnees; }
        public void setUnitesDonnees(long unitesDonnees) { this.unitesDonnees = unitesDonnees; }
    }

    public static class MissionRow {
        private int phase;
        private int conflict;
        private boolean bonus;
        private int covertNum;
        private String label;
        private long tentees;
        private long reussies;
        private double tauxReussite;
        private String modalId;
        private List<MissionFailureRow> echecs = new ArrayList<>();
        private List<MissionFailureRow> nonTentes = new ArrayList<>();

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
        public long getTentees() { return tentees; }
        public void setTentees(long tentees) { this.tentees = tentees; }
        public long getReussies() { return reussies; }
        public void setReussies(long reussies) { this.reussies = reussies; }
        public double getTauxReussite() { return tauxReussite; }
        public void setTauxReussite(double tauxReussite) { this.tauxReussite = tauxReussite; }
        public String getModalId() { return modalId; }
        public void setModalId(String modalId) { this.modalId = modalId; }
        public List<MissionFailureRow> getEchecs() { return echecs; }
        public void setEchecs(List<MissionFailureRow> echecs) { this.echecs = echecs; }
        public List<MissionFailureRow> getNonTentes() { return nonTentes; }
        public void setNonTentes(List<MissionFailureRow> nonTentes) { this.nonTentes = nonTentes; }
        public int getNombreEchecs() { return echecs != null ? echecs.size() : 0; }
        public int getNombreNonTentes() { return nonTentes != null ? nonTentes.size() : 0; }
    }

    public static class MissionFailureRow {
        private String playerId;
        private String playerName;

        public MissionFailureRow() {
        }

        public MissionFailureRow(String playerId, String playerName) {
            this.playerId = playerId;
            this.playerName = playerName;
        }

        public String getPlayerId() { return playerId; }
        public void setPlayerId(String playerId) { this.playerId = playerId; }
        public String getPlayerName() { return playerName; }
        public void setPlayerName(String playerName) { this.playerName = playerName; }
    }

    public static class PlayerRow {
        private String playerId;
        private String playerName;
        private long combatsTentes;
        private long vagues;
        private double vaguesParCombat;
        private long msTentees;
        private long msReussies;
        private double tauxMs;
        private long puissanceDeployee;
        private long unitesDonnees;

        public String getPlayerId() { return playerId; }
        public void setPlayerId(String playerId) { this.playerId = playerId; }
        public String getPlayerName() { return playerName; }
        public void setPlayerName(String playerName) { this.playerName = playerName; }
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
    }

    public static class Detail {
        private SummaryRow tb;
        private List<RoundRow> rounds = new ArrayList<>();
        private List<MissionRow> missions = new ArrayList<>();
        private List<PlayerRow> joueurs = new ArrayList<>();
        private long totalVagues;
        private long totalCombats;
        private long totalMsTentees;
        private long totalMsReussies;
        private double tauxMs;
        private long totalPuissanceDeployee;
        private long totalUnitesDonnees;

        public SummaryRow getTb() { return tb; }
        public void setTb(SummaryRow tb) { this.tb = tb; }
        public List<RoundRow> getRounds() { return rounds; }
        public void setRounds(List<RoundRow> rounds) { this.rounds = rounds; }
        public List<MissionRow> getMissions() { return missions; }
        public void setMissions(List<MissionRow> missions) { this.missions = missions; }
        public List<PlayerRow> getJoueurs() { return joueurs; }
        public void setJoueurs(List<PlayerRow> joueurs) { this.joueurs = joueurs; }
        public long getTotalVagues() { return totalVagues; }
        public void setTotalVagues(long totalVagues) { this.totalVagues = totalVagues; }
        public long getTotalCombats() { return totalCombats; }
        public void setTotalCombats(long totalCombats) { this.totalCombats = totalCombats; }
        public long getTotalMsTentees() { return totalMsTentees; }
        public void setTotalMsTentees(long totalMsTentees) { this.totalMsTentees = totalMsTentees; }
        public long getTotalMsReussies() { return totalMsReussies; }
        public void setTotalMsReussies(long totalMsReussies) { this.totalMsReussies = totalMsReussies; }
        public double getTauxMs() { return tauxMs; }
        public void setTauxMs(double tauxMs) { this.tauxMs = tauxMs; }
        public long getTotalPuissanceDeployee() { return totalPuissanceDeployee; }
        public void setTotalPuissanceDeployee(long totalPuissanceDeployee) { this.totalPuissanceDeployee = totalPuissanceDeployee; }
        public long getTotalUnitesDonnees() { return totalUnitesDonnees; }
        public void setTotalUnitesDonnees(long totalUnitesDonnees) { this.totalUnitesDonnees = totalUnitesDonnees; }
    }
}
