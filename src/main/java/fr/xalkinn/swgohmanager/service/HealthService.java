package fr.xalkinn.swgohmanager.service;


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
}