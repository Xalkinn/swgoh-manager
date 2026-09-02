package fr.xalkinn.swgohmanager.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import fr.xalkinn.swgohmanager.modele.GuildeConfiguration;
import jakarta.annotation.PostConstruct;

@Service
public class GuildeService {

    private final JdbcTemplate jdbcTemplate;

    public GuildeService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initialiser() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS guilde (
                id INT AUTO_INCREMENT PRIMARY KEY,
                guild_id VARCHAR(100) NOT NULL UNIQUE,
                nom VARCHAR(255) NULL,
                active BOOLEAN NOT NULL DEFAULT TRUE,
                date_ajout DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                date_derniere_synchro DATETIME NULL
            )
            """);

        getGuildeActive().ifPresent(this::appliquerConfigurationRuntime);
    }

    public boolean estConfiguree() {
        return getGuildeActive().isPresent();
    }

    public Optional<GuildeConfiguration> getGuildeActive() {
        List<GuildeConfiguration> guildes = jdbcTemplate.query("""
            SELECT id, guild_id, nom, active, date_ajout, date_derniere_synchro
            FROM guilde
            WHERE active = TRUE
            ORDER BY id DESC
            LIMIT 1
            """,
            (rs, rowNum) -> {
                GuildeConfiguration guilde = new GuildeConfiguration();
                guilde.setId(rs.getInt("id"));
                guilde.setGuildId(rs.getString("guild_id"));
                guilde.setNom(rs.getString("nom"));
                guilde.setActive(rs.getBoolean("active"));

                Timestamp dateAjout = rs.getTimestamp("date_ajout");
                if (dateAjout != null) {
                    guilde.setDateAjout(dateAjout.toLocalDateTime());
                }

                Timestamp dateSynchro = rs.getTimestamp("date_derniere_synchro");
                if (dateSynchro != null) {
                    guilde.setDateDerniereSynchro(dateSynchro.toLocalDateTime());
                }

                return guilde;
            });

        return guildes.stream().findFirst();
    }

    public GuildeConfiguration getGuildeActiveOuErreur() {
        return getGuildeActive()
                .orElseThrow(() -> new IllegalStateException(
                        "Aucune guilde n'est configurée. Ouvre /setup."));
    }

    public void enregistrerGuilde(String guildId, String nom) {
        if (guildId == null || guildId.isBlank()) {
            throw new IllegalArgumentException("Le Guild ID est obligatoire.");
        }

        jdbcTemplate.update("UPDATE guilde SET active = FALSE WHERE active = TRUE");

        jdbcTemplate.update("""
            INSERT INTO guilde (guild_id, nom, active)
            VALUES (?, ?, TRUE)
            ON DUPLICATE KEY UPDATE
                nom = VALUES(nom),
                active = TRUE
            """,
            guildId.trim(),
            nom == null || nom.isBlank() ? null : nom.trim());

        appliquerConfigurationRuntime(getGuildeActiveOuErreur());
    }

    public void marquerSynchronisation() {
        GuildeConfiguration guilde = getGuildeActiveOuErreur();

        jdbcTemplate.update("""
            UPDATE guilde
            SET date_derniere_synchro = ?
            WHERE id = ?
            """,
            LocalDateTime.now(),
            guilde.getId());
    }

    public void appliquerConfigurationRuntime() {
        appliquerConfigurationRuntime(getGuildeActiveOuErreur());
    }

    private void appliquerConfigurationRuntime(GuildeConfiguration guilde) {
        System.setProperty("SWGOH_GUILD_ID", guilde.getGuildId());
    }
}
