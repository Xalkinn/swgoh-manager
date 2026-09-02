package fr.xalkinn.swgohmanager.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import fr.xalkinn.swgohmanager.dto.PersonnageDisponible;
import fr.xalkinn.swgohmanager.modele.ComparatifObjectif;
import fr.xalkinn.swgohmanager.modele.ObjectifPersonnage;
import fr.xalkinn.swgohmanager.repository.ObjectifPersonnageRepository;
import fr.xalkinn.swgohmanager.repository.PersonnageRepository;

@Service
public class ObjectifService {

    private final ObjectifPersonnageRepository objectifRepository;
    private final PersonnageRepository personnageRepository;
    private final PersonnageDefinitionService personnageDefinitionService;

    public ObjectifService(
            ObjectifPersonnageRepository objectifRepository,
            PersonnageRepository personnageRepository,
            PersonnageDefinitionService personnageDefinitionService) {

        this.objectifRepository = objectifRepository;
        this.personnageRepository = personnageRepository;
        this.personnageDefinitionService = personnageDefinitionService;
    }

    public List<PersonnageDisponible> getPersonnagesDisponibles() {
        return personnageDefinitionService.listerPersonnagesDisponibles();
    }

    public void ajouterObjectif(ObjectifPersonnage objectif) {
        String nomBrut = objectif.getNom() == null
                ? null
                : objectif.getNom().replace(",", "").trim();

        objectif.setNom(nomBrut);

        // Les anciens écrans travaillaient uniquement avec nom.
        // On complète désormais également base_id pour les nouveaux objectifs.
        if ((objectif.getBaseId() == null || objectif.getBaseId().isBlank())
                && nomBrut != null && !nomBrut.isBlank()) {
            objectif.setBaseId(
                    personnageDefinitionService.trouverBaseIdParNomBrut(nomBrut));
        }

        objectif.setActif(true);
        objectifRepository.save(objectif);
    }

    public void supprimerObjectif(Integer id) {
        objectifRepository.deleteById(id);
    }

    public List<ComparatifObjectif> getComparatif(String nom) {
        List<ComparatifObjectif> resultat = personnageRepository.getComparatifObjectif(nom);
        nom = nom.replace(",", "").trim();
        System.out.println("Nom recherché : " + nom);
        System.out.println("Nombre résultats : " + resultat.size());

        for (ComparatifObjectif c : resultat) {
            System.out.println(
                c.getJoueur()
                + " R" + c.getRelicActuelle()
                + " -> R" + c.getRelicCible()
            );
        }

        return resultat;
    }

    public List<ObjectifPersonnage> getObjectifs() {
        List<ObjectifPersonnage> objectifs = new ArrayList<>();
        objectifRepository.findAll().forEach(objectifs::add);

        // Le nom technique reste inchangé. Pour les anciens objectifs créés
        // avant l'arrivée de personnage_definition, on renseigne une fois leur
        // base_id afin que l'affichage français soit désormais fiable.
        Map<String, String> nomsFrancais =
                personnageDefinitionService.chargerIndexNomsFrancais();

        for (ObjectifPersonnage objectif : objectifs) {
            if ((objectif.getBaseId() == null || objectif.getBaseId().isBlank())
                    && objectif.getNom() != null && !objectif.getNom().isBlank()) {

                String baseId =
                        personnageDefinitionService.trouverBaseIdParNomBrut(objectif.getNom());

                if (baseId != null && !baseId.isBlank()) {
                    objectif.setBaseId(baseId);
                    // Migration transparente des anciens objectifs.
                    objectifRepository.save(objectif);
                }
            }

            objectif.setNomFrancais(
                    personnageDefinitionService.nomAffiche(
                            nomsFrancais,
                            objectif.getBaseId(),
                            objectif.getNom()));
        }

        return objectifs;
    }
}
