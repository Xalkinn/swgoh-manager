package fr.xalkinn.swgohmanager.service;

import org.springframework.stereotype.Service;

import fr.xalkinn.swgohmanager.modele.DashboardStats;
import fr.xalkinn.swgohmanager.repository.JoueurRepository;
import fr.xalkinn.swgohmanager.repository.OmicronRepository;

@Service
public class DashboardService {

    private final JoueurRepository joueurRepository;
    private final OmicronRepository omicronRepository;

    public DashboardService(JoueurRepository joueurRepository,OmicronRepository omicronRepository) {
        this.joueurRepository = joueurRepository;
        this.omicronRepository = omicronRepository;
    }

    public DashboardStats getDashboardStats() {

        DashboardStats stats = new DashboardStats();

        stats.setNombreJoueurs((int) joueurRepository.count());
        stats.setNombreOmicrons((int) omicronRepository.count()
        );

        return stats;
    }

}