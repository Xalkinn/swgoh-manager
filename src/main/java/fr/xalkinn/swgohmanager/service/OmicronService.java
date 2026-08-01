package fr.xalkinn.swgohmanager.service;


import java.util.List;
import org.springframework.stereotype.Service;
import fr.xalkinn.swgohmanager.dao.OmicronDAO;
import fr.xalkinn.swgohmanager.modele.Omicron;

@Service
public class OmicronService {

    private final OmicronDAO dao;
    public OmicronService(OmicronDAO dao) {
        this.dao = dao;
    }

    public List<Omicron> getOmicrons(){
        return dao.findAll();
    }
}