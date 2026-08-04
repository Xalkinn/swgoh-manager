package fr.xalkinn.swgohmanager.modele;

import java.time.LocalDateTime;

public class DashboardStats {

    private int nombreJoueurs;
    private int nombreOmicrons;
    private int joueursListe1;
    private int joueursListe2;
    private int nombrePersonnages;
    private int nombreRosters;
    private LocalDateTime derniereMajOmicrons;
    private LocalDateTime derniereMajRoster;

	public int getNombreJoueurs() {
		return nombreJoueurs;
	}

	public void setNombreJoueurs(int nombreJoueurs) {
		this.nombreJoueurs = nombreJoueurs;
	}

	public int getNombreOmicrons() {
		return nombreOmicrons;
	}

	public void setNombreOmicrons(int nombreOmicrons) {
		this.nombreOmicrons = nombreOmicrons;
	}

	public int getJoueursListe1() {
		return joueursListe1;
	}

	public void setJoueursListe1(int joueursListe1) {
		this.joueursListe1 = joueursListe1;
	}

	public int getJoueursListe2() {
		return joueursListe2;
	}

	public void setJoueursListe2(int joueursListe2) {
		this.joueursListe2 = joueursListe2;
	}

	public int getNombrePersonnages() {
		return nombrePersonnages;
	}

	public void setNombrePersonnages(int nombrePersonnages) {
		this.nombrePersonnages = nombrePersonnages;
	}

	public int getNombreRosters() {
		return nombreRosters;
	}

	public void setNombreRosters(int nombreRosters) {
		this.nombreRosters = nombreRosters;
	}

	public LocalDateTime getDerniereMajOmicrons() {
		return derniereMajOmicrons;
	}

	public void setDerniereMajOmicrons(LocalDateTime derniereMajOmicrons) {
		this.derniereMajOmicrons = derniereMajOmicrons;
	}

	public LocalDateTime getDerniereMajRoster() {
		return derniereMajRoster;
	}

	public void setDerniereMajRoster(LocalDateTime derniereMajRoster) {
		this.derniereMajRoster = derniereMajRoster;
	}
}