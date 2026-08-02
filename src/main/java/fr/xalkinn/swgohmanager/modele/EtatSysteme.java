package fr.xalkinn.swgohmanager.modele;

import java.time.Duration;
import java.time.LocalDateTime;

public class EtatSysteme {


    private boolean bddOk;
    private boolean comlinkOk;
    private LocalDateTime derniereSynchro;
    private String derniereSynchroFormatee;
    private String fraicheurDonnees;
    private String couleurFraicheur;

    public boolean isBddOk() {
        return bddOk;
    }

    public void setBddOk(boolean bddOk) {
        this.bddOk = bddOk;
    }

    public boolean isComlinkOk() {
        return comlinkOk;
    }

    public void setComlinkOk(boolean comlinkOk) {
        this.comlinkOk = comlinkOk;
    }

    public LocalDateTime getDerniereSynchro() {
        return derniereSynchro;
    }

    public void setDerniereSynchro(LocalDateTime derniereSynchro) {
        this.derniereSynchro = derniereSynchro;
    }
    
    @Override
    public String toString() {

        return "EtatSysteme {" +
                "bddOk=" + bddOk +
                ", comlinkOk=" + comlinkOk +
                ", derniereSynchro=" + derniereSynchro +
                '}';

    }
    public String getDerniereSynchroFormatee() {
        return derniereSynchroFormatee;
    }


    public void setDerniereSynchroFormatee(String derniereSynchroFormatee) {
        this.derniereSynchroFormatee = derniereSynchroFormatee;
    }

	public String getFraicheurDonnees() {
		return fraicheurDonnees;
	}

	public void setFraicheurDonnees(String fraicheurDonnees) {
		this.fraicheurDonnees = fraicheurDonnees;
	}

	public String getCouleurFraicheur() {
		return couleurFraicheur;
	}

	public void setCouleurFraicheur(String couleurFraicheur) {
		this.couleurFraicheur = couleurFraicheur;
	}
	
}