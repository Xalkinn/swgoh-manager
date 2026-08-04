package fr.xalkinn.swgohmanager.service;

import org.springframework.stereotype.Service;

import dao.OmicronDAO;
import fr.xalkinn.swgohmanager.modele.DashboardStats;
import fr.xalkinn.swgohmanager.repository.JoueurRepository;
import fr.xalkinn.swgohmanager.repository.OmicronRepository;
import fr.xalkinn.swgohmanager.repository.PersonnageRepository;

@Service
public class DashboardService {

    private final PersonnageRepository personnageRepository;
	private final JoueurRepository joueurRepository;
    private final OmicronRepository omicronRepository;

    public DashboardService(JoueurRepository joueurRepository,OmicronRepository omicronRepository, PersonnageRepository personnageRepository) {
        this.joueurRepository = joueurRepository;
        this.omicronRepository = omicronRepository;
        this.personnageRepository = personnageRepository;
    }

    public DashboardStats getDashboardStats() {

        DashboardStats stats = new DashboardStats();

        stats.setNombreJoueurs((int) joueurRepository.count());
        stats.setNombreOmicrons((int) omicronRepository.count());
        stats.setNombrePersonnages((int) personnageRepository.count());
        stats.setDerniereMajOmicrons(omicronRepository.findDerniereMaj());
        stats.setDerniereMajRoster(joueurRepository.findDerniereMaj());
        return stats;
    }

}