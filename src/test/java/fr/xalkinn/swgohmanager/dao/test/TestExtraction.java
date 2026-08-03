package fr.xalkinn.swgohmanager.dao.test;

import dao.ExtractionDAO;

public class TestExtraction {

    public static void main(String[] args) throws Exception {

        ExtractionDAO dao = new ExtractionDAO();

        int id = dao.startExtraction();

        System.out.println("Extraction créée : " + id);

        dao.updateProgress(id, 50);

        System.out.println("Progression mise à jour");

        dao.finishExtraction(id);

        System.out.println("Extraction terminée");
    }
}