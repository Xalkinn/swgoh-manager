package fr.xalkinn.swgohmanager;

import fr.xalkinn.swgohmanager.dao.ModQDAOImpl;
import fr.xalkinn.swgohmanager.modele.ModQ;


public class TestModQ {


    public static void main(String[] args) {


        ModQDAOImpl dao = new ModQDAOImpl();


        for(ModQ modQ : dao.findAll()) {

            System.out.println(
                modQ.getJoueur()
                + " | "
                + modQ.getNb15_19()
                + " | "
                + modQ.getNb20_24()
                + " | "
                + modQ.getNb25()
                + " | ModQ = "
                + modQ.getModQ()
            );
        }
    }
}