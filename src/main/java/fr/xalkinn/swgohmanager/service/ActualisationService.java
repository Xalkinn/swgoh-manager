package fr.xalkinn.swgohmanager.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import fr.xalkinn.swgohmanager.modele.ActualisationDonnees;

@Service
public class ActualisationService {

    private final JdbcTemplate jdbcTemplate;

    public ActualisationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ActualisationDonnees getActualisation() {
        ActualisationDonnees actualisation = new ActualisationDonnees();
        LocalDateTime dateOmicrons = getDerniereMajOmicrons();
        LocalDateTime dateRoster = getDerniereMajRoster();
        LocalDateTime dateJoueurs = getDerniereMajJoueurs();

        actualisation.setDateOmicrons(formaterDate(dateOmicrons));
        actualisation.setDateRosters(formaterDate(dateRoster));
        actualisation.setDateJoueurs(formaterDate(dateJoueurs));
        actualisation.setFraicheurOmicrons(calculerFraicheur(dateOmicrons));
        actualisation.setFraicheurRosters(calculerFraicheur(dateRoster));
        actualisation.setFraicheurJoueurs(calculerFraicheur(dateJoueurs));
        actualisation.setCouleurOmicrons(calculerCouleur(dateOmicrons));
        actualisation.setCouleurRosters(calculerCouleur(dateRoster));
        actualisation.setCouleurJoueurs(calculerCouleur(dateJoueurs));

        return actualisation;
    }

    private LocalDateTime getDerniereMajOmicrons() {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT MAX(date_maj) FROM omicron",
                    LocalDateTime.class
            );
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime getDerniereMajRoster() {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT MAX(date_capture) FROM personnage",
                    LocalDateTime.class
            );
        } catch (Exception e) {
            return null;
        }
    }

    private String formaterDate(LocalDateTime date) {
        if (date == null) {
            return "Jamais";
        }
        return date.format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")
        );
    }

    private String calculerFraicheur(LocalDateTime synchro) {
        if (synchro == null) {
            return "⚫ Aucune synchronisation";
        }
        Duration duree = Duration.between(synchro, LocalDateTime.now());
        long jours = duree.toDays();
        if (jours > 0) {
            return "🔴 Il y a " + jours + " jour" + (jours > 1 ? "s" : "");
        }
        long heures = duree.toHours();
        if (heures > 0) {
            if (heures < 6) {
                return "🟠 Il y a " + heures + " heure" + (heures > 1 ? "s" : "");
            }
            return "🔴 Il y a " + heures + " heure" + (heures > 1 ? "s" : "");
        }
        long minutes = duree.toMinutes();
        if (minutes > 1) {
            return "🟢 Il y a " + minutes + " minutes";
        }
        return "🟢 À l'instant";
    }
    
    private String calculerCouleur(LocalDateTime synchro) {
        if (synchro == null) {
            return "danger";
        }
        Duration duree = Duration.between(synchro, LocalDateTime.now());
        if (duree.toMinutes() < 60) {
            return "success";
        }
        if (duree.toHours() < 6) {
            return "warning";
        }
        return "danger";
    }
    
    private LocalDateTime getDerniereMajJoueurs() {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT MAX(date_maj) FROM joueur",
                    LocalDateTime.class
            );
        } catch (Exception e) {
            return null;
        }
    }
}