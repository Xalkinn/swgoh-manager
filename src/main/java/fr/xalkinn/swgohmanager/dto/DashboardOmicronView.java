package fr.xalkinn.swgohmanager.dto;

import java.util.ArrayList;
import java.util.List;

/** DTOs du détail Omicron affiché depuis le dashboard principal. */
public final class DashboardOmicronView {

    private DashboardOmicronView() {}

    public static class ModeDetail {
        private String code;
        private String label;
        private String modalId;
        private int totalPoses;
        private List<OmicronDetail> omicrons = new ArrayList<>();

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getModalId() { return modalId; }
        public void setModalId(String modalId) { this.modalId = modalId; }
        public int getTotalPoses() { return totalPoses; }
        public void setTotalPoses(int totalPoses) { this.totalPoses = totalPoses; }
        public List<OmicronDetail> getOmicrons() { return omicrons; }
        public void setOmicrons(List<OmicronDetail> omicrons) { this.omicrons = omicrons; }
        public int getNombreOmicronsDifferents() { return omicrons == null ? 0 : omicrons.size(); }
    }

    public static class OmicronDetail {
        private String baseId;
        private String skillId;
        private String nomPersonnage;
        private String typeCapacite;
        private String nomCapaciteDetail;
        private List<PlayerRow> joueurs = new ArrayList<>();

        public String getBaseId() { return baseId; }
        public void setBaseId(String baseId) { this.baseId = baseId; }
        public String getSkillId() { return skillId; }
        public void setSkillId(String skillId) { this.skillId = skillId; }
        public String getNomPersonnage() { return nomPersonnage; }
        public void setNomPersonnage(String nomPersonnage) { this.nomPersonnage = nomPersonnage; }
        public String getTypeCapacite() { return typeCapacite; }
        public void setTypeCapacite(String typeCapacite) { this.typeCapacite = typeCapacite; }
        public String getNomCapaciteDetail() { return nomCapaciteDetail; }
        public void setNomCapaciteDetail(String nomCapaciteDetail) { this.nomCapaciteDetail = nomCapaciteDetail; }
        public List<PlayerRow> getJoueurs() { return joueurs; }
        public void setJoueurs(List<PlayerRow> joueurs) { this.joueurs = joueurs; }
        public int getNombreJoueurs() { return joueurs == null ? 0 : joueurs.size(); }
    }

    public static class PlayerRow {
        private String playerId;
        private String playerName;

        public PlayerRow() {}

        public PlayerRow(String playerId, String playerName) {
            this.playerId = playerId;
            this.playerName = playerName;
        }

        public String getPlayerId() { return playerId; }
        public void setPlayerId(String playerId) { this.playerId = playerId; }
        public String getPlayerName() { return playerName; }
        public void setPlayerName(String playerName) { this.playerName = playerName; }
    }
}
