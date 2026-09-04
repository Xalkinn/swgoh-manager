package fr.xalkinn.swgohmanager.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TerritoryWarView {

    public static class MatchRow {
        private long id;
        private String territoryWarId;
        private int score;
        private long power;
        private int opponentScore;
        private String opponentGuildId;
        private String opponentGuildName;
        private long opponentGuildGp;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private int ecart;
        private String resultat;
        private String resultatLabel;
        private double dureeHeures;
        private boolean plusSerree;
        private boolean plusGrosseVictoire;
        private boolean plusGrosseDefaite;
        private boolean adversairePlusPuissant;

        public long getId() { return id; }
        public void setId(long id) { this.id = id; }
        public String getTerritoryWarId() { return territoryWarId; }
        public void setTerritoryWarId(String territoryWarId) { this.territoryWarId = territoryWarId; }
        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }
        public long getPower() { return power; }
        public void setPower(long power) { this.power = power; }
        public int getOpponentScore() { return opponentScore; }
        public void setOpponentScore(int opponentScore) { this.opponentScore = opponentScore; }
        public String getOpponentGuildId() { return opponentGuildId; }
        public void setOpponentGuildId(String opponentGuildId) { this.opponentGuildId = opponentGuildId; }
        public String getOpponentGuildName() { return opponentGuildName; }
        public void setOpponentGuildName(String opponentGuildName) { this.opponentGuildName = opponentGuildName; }
        public long getOpponentGuildGp() { return opponentGuildGp; }
        public void setOpponentGuildGp(long opponentGuildGp) { this.opponentGuildGp = opponentGuildGp; }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        public int getEcart() { return ecart; }
        public void setEcart(int ecart) { this.ecart = ecart; }
        public String getResultat() { return resultat; }
        public void setResultat(String resultat) { this.resultat = resultat; }
        public String getResultatLabel() { return resultatLabel; }
        public void setResultatLabel(String resultatLabel) { this.resultatLabel = resultatLabel; }
        public double getDureeHeures() { return dureeHeures; }
        public void setDureeHeures(double dureeHeures) { this.dureeHeures = dureeHeures; }
        public boolean isPlusSerree() { return plusSerree; }
        public void setPlusSerree(boolean plusSerree) { this.plusSerree = plusSerree; }
        public boolean isPlusGrosseVictoire() { return plusGrosseVictoire; }
        public void setPlusGrosseVictoire(boolean plusGrosseVictoire) { this.plusGrosseVictoire = plusGrosseVictoire; }
        public boolean isPlusGrosseDefaite() { return plusGrosseDefaite; }
        public void setPlusGrosseDefaite(boolean plusGrosseDefaite) { this.plusGrosseDefaite = plusGrosseDefaite; }
        public boolean isAdversairePlusPuissant() { return adversairePlusPuissant; }
        public void setAdversairePlusPuissant(boolean adversairePlusPuissant) { this.adversairePlusPuissant = adversairePlusPuissant; }
    }

    public static class Summary {
        private int total;
        private int victoires;
        private int defaites;
        private int nuls;
        private double winRate;
        private double scoreMoyen;
        private double scoreAdverseMoyen;
        private double ecartMoyen;
        private int serieActuelle;
        private String serieType;
        private String serieLabel;
        private MatchRow matchLePlusSerre;
        private MatchRow plusGrosseVictoire;
        private MatchRow plusGrosseDefaite;
        private MatchRow adversairePlusPuissant;

        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
        public int getVictoires() { return victoires; }
        public void setVictoires(int victoires) { this.victoires = victoires; }
        public int getDefaites() { return defaites; }
        public void setDefaites(int defaites) { this.defaites = defaites; }
        public int getNuls() { return nuls; }
        public void setNuls(int nuls) { this.nuls = nuls; }
        public double getWinRate() { return winRate; }
        public void setWinRate(double winRate) { this.winRate = winRate; }
        public double getScoreMoyen() { return scoreMoyen; }
        public void setScoreMoyen(double scoreMoyen) { this.scoreMoyen = scoreMoyen; }
        public double getScoreAdverseMoyen() { return scoreAdverseMoyen; }
        public void setScoreAdverseMoyen(double scoreAdverseMoyen) { this.scoreAdverseMoyen = scoreAdverseMoyen; }
        public double getEcartMoyen() { return ecartMoyen; }
        public void setEcartMoyen(double ecartMoyen) { this.ecartMoyen = ecartMoyen; }
        public int getSerieActuelle() { return serieActuelle; }
        public void setSerieActuelle(int serieActuelle) { this.serieActuelle = serieActuelle; }
        public String getSerieType() { return serieType; }
        public void setSerieType(String serieType) { this.serieType = serieType; }
        public String getSerieLabel() { return serieLabel; }
        public void setSerieLabel(String serieLabel) { this.serieLabel = serieLabel; }
        public MatchRow getMatchLePlusSerre() { return matchLePlusSerre; }
        public void setMatchLePlusSerre(MatchRow matchLePlusSerre) { this.matchLePlusSerre = matchLePlusSerre; }
        public MatchRow getPlusGrosseVictoire() { return plusGrosseVictoire; }
        public void setPlusGrosseVictoire(MatchRow plusGrosseVictoire) { this.plusGrosseVictoire = plusGrosseVictoire; }
        public MatchRow getPlusGrosseDefaite() { return plusGrosseDefaite; }
        public void setPlusGrosseDefaite(MatchRow plusGrosseDefaite) { this.plusGrosseDefaite = plusGrosseDefaite; }
        public MatchRow getAdversairePlusPuissant() { return adversairePlusPuissant; }
        public void setAdversairePlusPuissant(MatchRow adversairePlusPuissant) { this.adversairePlusPuissant = adversairePlusPuissant; }
    }

    public static class PreparationSummary {
        private int totalJoueurs;
        private int p1Total;
        private int p2Total;
        private int p3Total;
        private int p4Total;
        private int p1Complets;
        private int p2Complets;
        private int p3Complets;
        private int p4Complets;
        private int planAtteint;
        private int planTotal;
        private double planPourcentage;
        private int joueursAvecP1Manquante;
        private int joueursAvecP2Manquante;

        public int getTotalJoueurs() { return totalJoueurs; }
        public void setTotalJoueurs(int totalJoueurs) { this.totalJoueurs = totalJoueurs; }
        public int getP1Total() { return p1Total; }
        public void setP1Total(int p1Total) { this.p1Total = p1Total; }
        public int getP2Total() { return p2Total; }
        public void setP2Total(int p2Total) { this.p2Total = p2Total; }
        public int getP3Total() { return p3Total; }
        public void setP3Total(int p3Total) { this.p3Total = p3Total; }
        public int getP4Total() { return p4Total; }
        public void setP4Total(int p4Total) { this.p4Total = p4Total; }
        public int getP1Complets() { return p1Complets; }
        public void setP1Complets(int p1Complets) { this.p1Complets = p1Complets; }
        public int getP2Complets() { return p2Complets; }
        public void setP2Complets(int p2Complets) { this.p2Complets = p2Complets; }
        public int getP3Complets() { return p3Complets; }
        public void setP3Complets(int p3Complets) { this.p3Complets = p3Complets; }
        public int getP4Complets() { return p4Complets; }
        public void setP4Complets(int p4Complets) { this.p4Complets = p4Complets; }
        public int getPlanAtteint() { return planAtteint; }
        public void setPlanAtteint(int planAtteint) { this.planAtteint = planAtteint; }
        public int getPlanTotal() { return planTotal; }
        public void setPlanTotal(int planTotal) { this.planTotal = planTotal; }
        public double getPlanPourcentage() { return planPourcentage; }
        public void setPlanPourcentage(double planPourcentage) { this.planPourcentage = planPourcentage; }
        public int getJoueursAvecP1Manquante() { return joueursAvecP1Manquante; }
        public void setJoueursAvecP1Manquante(int joueursAvecP1Manquante) { this.joueursAvecP1Manquante = joueursAvecP1Manquante; }
        public int getJoueursAvecP2Manquante() { return joueursAvecP2Manquante; }
        public void setJoueursAvecP2Manquante(int joueursAvecP2Manquante) { this.joueursAvecP2Manquante = joueursAvecP2Manquante; }
    }

    public static class PreparationPlayerRow {
        private int joueurId;
        private String playerId;
        private String nom;
        private long pgPerso;
        private int p1Atteint;
        private int p1Total;
        private int p2Atteint;
        private int p2Total;
        private int p3Atteint;
        private int p3Total;
        private int p4Atteint;
        private int p4Total;
        private int r8Plus;
        private int r9Plus;
        private int r10;
        private int mods25Plus;
        private double modQ;

        public int getJoueurId() { return joueurId; }
        public void setJoueurId(int joueurId) { this.joueurId = joueurId; }
        public String getPlayerId() { return playerId; }
        public void setPlayerId(String playerId) { this.playerId = playerId; }
        public String getNom() { return nom; }
        public void setNom(String nom) { this.nom = nom; }
        public long getPgPerso() { return pgPerso; }
        public void setPgPerso(long pgPerso) { this.pgPerso = pgPerso; }
        public int getP1Atteint() { return p1Atteint; }
        public void setP1Atteint(int p1Atteint) { this.p1Atteint = p1Atteint; }
        public int getP1Total() { return p1Total; }
        public void setP1Total(int p1Total) { this.p1Total = p1Total; }
        public int getP2Atteint() { return p2Atteint; }
        public void setP2Atteint(int p2Atteint) { this.p2Atteint = p2Atteint; }
        public int getP2Total() { return p2Total; }
        public void setP2Total(int p2Total) { this.p2Total = p2Total; }
        public int getP3Atteint() { return p3Atteint; }
        public void setP3Atteint(int p3Atteint) { this.p3Atteint = p3Atteint; }
        public int getP3Total() { return p3Total; }
        public void setP3Total(int p3Total) { this.p3Total = p3Total; }
        public int getP4Atteint() { return p4Atteint; }
        public void setP4Atteint(int p4Atteint) { this.p4Atteint = p4Atteint; }
        public int getP4Total() { return p4Total; }
        public void setP4Total(int p4Total) { this.p4Total = p4Total; }
        public int getR8Plus() { return r8Plus; }
        public void setR8Plus(int r8Plus) { this.r8Plus = r8Plus; }
        public int getR9Plus() { return r9Plus; }
        public void setR9Plus(int r9Plus) { this.r9Plus = r9Plus; }
        public int getR10() { return r10; }
        public void setR10(int r10) { this.r10 = r10; }
        public int getMods25Plus() { return mods25Plus; }
        public void setMods25Plus(int mods25Plus) { this.mods25Plus = mods25Plus; }
        public double getModQ() { return modQ; }
        public void setModQ(double modQ) { this.modQ = modQ; }

        public int getP1Manquants() { return Math.max(0, p1Total - p1Atteint); }
        public int getP2Manquants() { return Math.max(0, p2Total - p2Atteint); }
        public int getP3Manquants() { return Math.max(0, p3Total - p3Atteint); }
        public int getP4Manquants() { return Math.max(0, p4Total - p4Atteint); }
        public boolean isP1Complet() { return p1Total == 0 || p1Atteint >= p1Total; }
        public boolean isP2Complet() { return p2Total == 0 || p2Atteint >= p2Total; }
        public boolean isP3Complet() { return p3Total == 0 || p3Atteint >= p3Total; }
        public boolean isP4Complet() { return p4Total == 0 || p4Atteint >= p4Total; }
    }


    public static class OmicronMissingRow {
        private int priorite;
        private int joueurId;
        private String playerId;
        private String joueurNom;
        private String baseId;
        private String personnageNom;
        private String skillId;
        private String capaciteLabel;

        public int getPriorite() { return priorite; }
        public void setPriorite(int priorite) { this.priorite = priorite; }
        public int getJoueurId() { return joueurId; }
        public void setJoueurId(int joueurId) { this.joueurId = joueurId; }
        public String getPlayerId() { return playerId; }
        public void setPlayerId(String playerId) { this.playerId = playerId; }
        public String getJoueurNom() { return joueurNom; }
        public void setJoueurNom(String joueurNom) { this.joueurNom = joueurNom; }
        public String getBaseId() { return baseId; }
        public void setBaseId(String baseId) { this.baseId = baseId; }
        public String getPersonnageNom() { return personnageNom; }
        public void setPersonnageNom(String personnageNom) { this.personnageNom = personnageNom; }
        public String getSkillId() { return skillId; }
        public void setSkillId(String skillId) { this.skillId = skillId; }
        public String getCapaciteLabel() { return capaciteLabel; }
        public void setCapaciteLabel(String capaciteLabel) { this.capaciteLabel = capaciteLabel; }
    }

    public static class Page {
        private List<MatchRow> historique = new ArrayList<>();
        private Summary summary = new Summary();
        private PreparationSummary preparation = new PreparationSummary();
        private List<PreparationPlayerRow> joueurs = new ArrayList<>();
        private List<OmicronMissingRow> omicronsManquants = new ArrayList<>();

        public List<MatchRow> getHistorique() { return historique; }
        public void setHistorique(List<MatchRow> historique) { this.historique = historique; }
        public Summary getSummary() { return summary; }
        public void setSummary(Summary summary) { this.summary = summary; }
        public PreparationSummary getPreparation() { return preparation; }
        public void setPreparation(PreparationSummary preparation) { this.preparation = preparation; }
        public List<PreparationPlayerRow> getJoueurs() { return joueurs; }
        public void setJoueurs(List<PreparationPlayerRow> joueurs) { this.joueurs = joueurs; }
        public List<OmicronMissingRow> getOmicronsManquants() { return omicronsManquants; }
        public void setOmicronsManquants(List<OmicronMissingRow> omicronsManquants) { this.omicronsManquants = omicronsManquants; }
    }
}
