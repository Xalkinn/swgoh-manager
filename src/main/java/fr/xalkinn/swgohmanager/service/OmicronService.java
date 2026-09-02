package fr.xalkinn.swgohmanager.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import fr.xalkinn.swgohmanager.dao.OmicronDAO;
import fr.xalkinn.swgohmanager.modele.Omicron;

@Service
public class OmicronService {

    private final OmicronDAO dao;
    private final PersonnageDefinitionService personnageDefinitionService;

    public OmicronService(
            OmicronDAO dao,
            PersonnageDefinitionService personnageDefinitionService) {
        this.dao = dao;
        this.personnageDefinitionService = personnageDefinitionService;
    }

    public List<Omicron> getOmicrons() {
        List<Omicron> omicrons = dao.findAll();
        Map<String, String> noms = personnageDefinitionService.chargerIndexNomsFrancais();

        for (Omicron omicron : omicrons) {
            String personnageBrut = omicron.getPersonnage();

            /*
             * La colonne omicron.personnage peut contenir soit un baseId,
             * soit le nom brut historique. L'index personnage_definition
             * connaît les deux formes, donc nomAffiche() retrouve la version
             * française sans modifier la donnée technique enregistrée.
             */
            String nomFrancais = personnageDefinitionService.nomAffiche(
                    noms,
                    personnageBrut,
                    personnageBrut);

            omicron.setNomFrancais(nomFrancais);
        }

        // Affichage plus naturel : joueur puis personnage en français.
        omicrons.sort(
                Comparator.comparing(
                        Omicron::getNomJoueur,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(
                        Omicron::getNomFrancais,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
        );

        return omicrons;
    }
}
