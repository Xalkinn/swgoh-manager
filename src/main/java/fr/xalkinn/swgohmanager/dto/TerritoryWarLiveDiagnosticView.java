package fr.xalkinn.swgohmanager.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Vue d'un snapshot de diagnostic TW Live.
 *
 * Le snapshot conserve volontairement la réponse /guild brute afin de pouvoir
 * vérifier les données réellement exposées par la version de Comlink utilisée
 * par l'application avant d'implémenter l'historisation détaillée des combats.
 */
public class TerritoryWarLiveDiagnosticView {

    private long id;
    private String guildId;
    private String source;
    private LocalDateTime capturedAt;
    private boolean statusKeyFound;
    private int statusItemCount;
    private int recentResultCount;
    private int memberCount;
    private int rawSizeBytes;
    private List<String> topLevelKeys = new ArrayList<>();
    private List<String> candidatePaths = new ArrayList<>();
    private String rawJson;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getGuildId() {
        return guildId;
    }

    public void setGuildId(String guildId) {
        this.guildId = guildId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public LocalDateTime getCapturedAt() {
        return capturedAt;
    }

    public void setCapturedAt(LocalDateTime capturedAt) {
        this.capturedAt = capturedAt;
    }

    public boolean isStatusKeyFound() {
        return statusKeyFound;
    }

    public void setStatusKeyFound(boolean statusKeyFound) {
        this.statusKeyFound = statusKeyFound;
    }

    public int getStatusItemCount() {
        return statusItemCount;
    }

    public void setStatusItemCount(int statusItemCount) {
        this.statusItemCount = statusItemCount;
    }

    public int getRecentResultCount() {
        return recentResultCount;
    }

    public void setRecentResultCount(int recentResultCount) {
        this.recentResultCount = recentResultCount;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public int getRawSizeBytes() {
        return rawSizeBytes;
    }

    public void setRawSizeBytes(int rawSizeBytes) {
        this.rawSizeBytes = rawSizeBytes;
    }

    public List<String> getTopLevelKeys() {
        return topLevelKeys;
    }

    public void setTopLevelKeys(List<String> topLevelKeys) {
        this.topLevelKeys = topLevelKeys == null ? new ArrayList<>() : topLevelKeys;
    }

    public List<String> getCandidatePaths() {
        return candidatePaths;
    }

    public void setCandidatePaths(List<String> candidatePaths) {
        this.candidatePaths = candidatePaths == null ? new ArrayList<>() : candidatePaths;
    }

    public String getRawJson() {
        return rawJson;
    }

    public void setRawJson(String rawJson) {
        this.rawJson = rawJson;
    }

    public boolean hasLiveData() {
        return statusItemCount > 0;
    }

    public String getRawSizeLabel() {
        if (rawSizeBytes < 1024) {
            return rawSizeBytes + " o";
        }
        if (rawSizeBytes < 1024 * 1024) {
            return String.format(java.util.Locale.FRANCE, "%.1f Ko", rawSizeBytes / 1024.0);
        }
        return String.format(java.util.Locale.FRANCE, "%.2f Mo", rawSizeBytes / (1024.0 * 1024.0));
    }
}
