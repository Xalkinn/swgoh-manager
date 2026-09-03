package fr.xalkinn.swgohmanager.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Décompose les identifiants techniques des statistiques de Territory Battle.
 *
 * Les valeurs brutes restent enregistrées en BDD ; ce parser ajoute uniquement
 * des colonnes pratiques (type, phase, conflit, bonus, mission, round) pour les
 * agrégations de l'interface.
 */
public final class MapStatIdParser {

    private static final Pattern COVERT_MISSION = Pattern.compile(
            "^(covert_complete|covert_round_attempted)_mission_tb3_mixed_phase(\\d{2})_conflict(\\d{2})(_bonus)?_covert(\\d{2})$");

    private static final Pattern ZONE = Pattern.compile(
            "^(power|disobey|strike_attempt|strike_encounter|summary|unit_donated)_zone_tb3_mixed_phase(\\d{2})_conflict(\\d{2})(_bonus)?$");

    private static final Pattern ROUND = Pattern.compile(
            "^(power|disobey|strike_attempt|strike_encounter|summary|unit_donated|covert_attempt)_round_(\\d+)$");

    private MapStatIdParser() {
    }

    public record ParsedMapStat(
            String statType,
            Integer phase,
            Integer conflict,
            boolean bonus,
            Integer covertNum,
            Integer roundNum) {
    }

    public static ParsedMapStat parse(String mapStatId) {
        if (mapStatId == null || mapStatId.isBlank()) {
            return new ParsedMapStat("inconnu", null, null, false, null, null);
        }

        Matcher m = COVERT_MISSION.matcher(mapStatId);
        if (m.matches()) {
            return new ParsedMapStat(
                    m.group(1),
                    Integer.parseInt(m.group(2)),
                    Integer.parseInt(m.group(3)),
                    m.group(4) != null,
                    Integer.parseInt(m.group(5)),
                    null);
        }

        m = ZONE.matcher(mapStatId);
        if (m.matches()) {
            return new ParsedMapStat(
                    m.group(1),
                    Integer.parseInt(m.group(2)),
                    Integer.parseInt(m.group(3)),
                    m.group(4) != null,
                    null,
                    null);
        }

        m = ROUND.matcher(mapStatId);
        if (m.matches()) {
            return new ParsedMapStat(
                    m.group(1),
                    null,
                    null,
                    false,
                    null,
                    Integer.parseInt(m.group(2)));
        }

        // Totaux globaux (power, summary, strike_encounter...) ou futur type
        // encore inconnu : on garde toujours l'identifiant original.
        return new ParsedMapStat(mapStatId, null, null, false, null, null);
    }
}
