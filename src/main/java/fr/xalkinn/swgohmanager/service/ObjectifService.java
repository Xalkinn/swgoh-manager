package fr.xalkinn.swgohmanager.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import fr.xalkinn.swgohmanager.modele.ComparatifObjectif;
import fr.xalkinn.swgohmanager.modele.ObjectifJoueurResultat;
import fr.xalkinn.swgohmanager.modele.ObjectifPersonnage;
import fr.xalkinn.swgohmanager.modele.Personnage;
import fr.xalkinn.swgohmanager.repository.JoueurRepository;
import fr.xalkinn.swgohmanager.repository.ObjectifPersonnageRepository;
import fr.xalkinn.swgohmanager.repository.PersonnageRepository;

@Service
public class ObjectifService {


    private final ObjectifPersonnageRepository objectifRepository;
    private final JoueurRepository joueurRepository;
    private final PersonnageRepository personnageRepository;


    public ObjectifService(
            ObjectifPersonnageRepository objectifRepository,
            JoueurRepository joueurRepository,
            PersonnageRepository personnageRepository) {

        this.objectifRepository = objectifRepository;
        this.joueurRepository = joueurRepository;
        this.personnageRepository = personnageRepository;
    }


    
    public List<Personnage> getPersonnagesDisponibles() {
        return personnageRepository.trouverPersonnagesDisponibles();
    }


    public void ajouterObjectif(ObjectifPersonnage objectif) {

        objectif.setNom(
            objectif.getNom()
                .replace(",", "")
                .trim()
        );

        objectif.setActif(true);

        objectifRepository.save(objectif);
    }
    
    
    
    public void supprimerObjectif(Integer id) {
        objectifRepository.deleteById(id);
    }
    
    public List<ComparatifObjectif> getComparatif(String nom){

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

        objectifRepository.findAll()
            .forEach(objectifs::add);

        return objectifs;
    }
}