package fr.xalkinn.swgohmanager.dto;

/** DTOs légers de la nouvelle page de gestion Omicron. */
public final class OmicronGestionView {

    private OmicronGestionView() {}

    public static class DefinitionOption {
        private long id;
        private String baseId;
        private String nomPersonnage;
        private String skillId;
        private String nomCapacite;
        private String nomCapaciteDetail;
        private String typeCapacite;
        private Integer numeroCapacite;
        private String categorie;
        private String categorieLabel;
        private String modeTechnique;
        private Integer tierRequis;

        public long getId() { return id; }
        public void setId(long id) { this.id = id; }
        public String getBaseId() { return baseId; }
        public void setBaseId(String baseId) { this.baseId = baseId; }
        public String getNomPersonnage() { return nomPersonnage; }
        public void setNomPersonnage(String nomPersonnage) { this.nomPersonnage = nomPersonnage; }
        public String getSkillId() { return skillId; }
        public void setSkillId(String skillId) { this.skillId = skillId; }
        public String getNomCapacite() { return nomCapacite; }
        public void setNomCapacite(String nomCapacite) { this.nomCapacite = nomCapacite; }
        public String getNomCapaciteDetail() { return nomCapaciteDetail; }
        public void setNomCapaciteDetail(String nomCapaciteDetail) { this.nomCapaciteDetail = nomCapaciteDetail; }
        public String getTypeCapacite() { return typeCapacite; }
        public void setTypeCapacite(String typeCapacite) { this.typeCapacite = typeCapacite; }
        public Integer getNumeroCapacite() { return numeroCapacite; }
        public void setNumeroCapacite(Integer numeroCapacite) { this.numeroCapacite = numeroCapacite; }
        public String getCategorie() { return categorie; }
        public void setCategorie(String categorie) { this.categorie = categorie; }
        public String getCategorieLabel() { return categorieLabel; }
        public void setCategorieLabel(String categorieLabel) { this.categorieLabel = categorieLabel; }
        public String getModeTechnique() { return modeTechnique; }
        public void setModeTechnique(String modeTechnique) { this.modeTechnique = modeTechnique; }
        public Integer getTierRequis() { return tierRequis; }
        public void setTierRequis(Integer tierRequis) { this.tierRequis = tierRequis; }
    }

    public static class PrioriteRow extends DefinitionOption {
        private long prioriteId;
        private int priorite;
        private int possesseurs;
        private int totalJoueurs;
        private int manquants;
        private String modalId;
        private java.util.List<JoueurOmicronRow> possesseursDetails = new java.util.ArrayList<>();
        private java.util.List<JoueurOmicronRow> manquantsDetails = new java.util.ArrayList<>();

        public long getPrioriteId() { return prioriteId; }
        public void setPrioriteId(long prioriteId) { this.prioriteId = prioriteId; }
        public int getPriorite() { return priorite; }
        public void setPriorite(int priorite) { this.priorite = priorite; }
        public int getPossesseurs() { return possesseurs; }
        public void setPossesseurs(int possesseurs) { this.possesseurs = possesseurs; }
        public int getTotalJoueurs() { return totalJoueurs; }
        public void setTotalJoueurs(int totalJoueurs) { this.totalJoueurs = totalJoueurs; }
        public int getManquants() { return manquants; }
        public void setManquants(int manquants) { this.manquants = manquants; }
        public String getModalId() { return modalId; }
        public void setModalId(String modalId) { this.modalId = modalId; }
        public java.util.List<JoueurOmicronRow> getPossesseursDetails() { return possesseursDetails; }
        public void setPossesseursDetails(java.util.List<JoueurOmicronRow> possesseursDetails) { this.possesseursDetails = possesseursDetails; }
        public java.util.List<JoueurOmicronRow> getManquantsDetails() { return manquantsDetails; }
        public void setManquantsDetails(java.util.List<JoueurOmicronRow> manquantsDetails) { this.manquantsDetails = manquantsDetails; }
    }

    public static class JoueurOmicronRow {
        private String playerId;
        private String playerName;

        public JoueurOmicronRow() {}

        public JoueurOmicronRow(String playerId, String playerName) {
            this.playerId = playerId;
            this.playerName = playerName;
        }

        public String getPlayerId() { return playerId; }
        public void setPlayerId(String playerId) { this.playerId = playerId; }
        public String getPlayerName() { return playerName; }
        public void setPlayerName(String playerName) { this.playerName = playerName; }
    }

    public record CategorieOption(String code, String label) {}
}
