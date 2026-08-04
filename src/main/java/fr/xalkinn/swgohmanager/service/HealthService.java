package fr.xalkinn.swgohmanager.service;


import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import fr.xalkinn.swgohmanager.modele.EtatSysteme;


@Service
public class HealthService {
    private final JdbcTemplate jdbcTemplate;
    public HealthService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public EtatSysteme getEtat() {
        EtatSysteme etat = new EtatSysteme();
        etat.setBddOk(verifierBDD());
        etat.setComlinkOk(verifierComlink());
        LocalDateTime synchro = getDerniereSynchro();
        etat.setDerniereSynchro(synchro);
        if(synchro != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");
            etat.setDerniereSynchroFormatee(
                    synchro.format(formatter));
        }
        etat.setFraicheurDonnees(calculerFraicheur(synchro));
        etat.setCouleurFraicheur(calculerCouleur(synchro));
        return etat;
    }

    private boolean verifierBDD() {
        try {
            jdbcTemplate.queryForObject(
                    "SELECT 1",
                    Integer.class
            );
            return true;
        } catch(Exception e) {
            return false;
        }
    }

    private boolean verifierComlink() {
        try {
            java.net.HttpURLConnection connexion =
                    (java.net.HttpURLConnection)
                    new java.net.URL(
                    "http://localhost:3000"
                    ).openConnection();
            connexion.setRequestMethod("GET");
            connexion.setConnectTimeout(2000);
            connexion.connect();
            int code = connexion.getResponseCode();
            return code >= 200 && code < 500;
        } catch(Exception e) {
            return false;
        }
    }
    private LocalDateTime getDerniereSynchro() {
        try {
            return jdbcTemplate.queryForObject(
                    """
                    SELECT MAX(date_maj)
                    FROM omicron
                    """,
                    LocalDateTime.class
            );
        } catch(Exception e) {
            return null;
        }
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
	        return "Danger";
	    }

	    Duration duree = Duration.between(synchro, LocalDateTime.now());

	    if (duree.toMinutes() < 60) {
	        return "Succes";
	    }

	    if (duree.toHours() < 6) {
	        return "Attention";
	    }

	    return "Danger";
	}
}