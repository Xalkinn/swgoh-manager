package fr.xalkinn.swgohmanager.modele;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("extraction")
public class Extraction {

    @Id
    private Integer id;

    private LocalDateTime dateDebut;

    private LocalDateTime dateFin;

    private String statut;

    private Integer nbJoueurs;

    private Integer nbPersonnages;

    private Integer nbOmicrons;

    private String commentaire;

    private Integer progression;


    public Integer getId() {
        return id;
    }

    public LocalDateTime getDateFin() {
        return dateFin;
    }

    public LocalDateTime getDateDebut() {
        return dateDebut;
    }

    public String getStatut() {
        return statut;
    }

	public Integer getNbJoueurs() {
		return nbJoueurs;
	}

	public void setNbJoueurs(Integer nbJoueurs) {
		this.nbJoueurs = nbJoueurs;
	}

	public Integer getNbPersonnages() {
		return nbPersonnages;
	}

	public void setNbPersonnages(Integer nbPersonnages) {
		this.nbPersonnages = nbPersonnages;
	}

	public Integer getNbOmicrons() {
		return nbOmicrons;
	}

	public void setNbOmicrons(Integer nbOmicrons) {
		this.nbOmicrons = nbOmicrons;
	}

	public String getCommentaire() {
		return commentaire;
	}

	public void setCommentaire(String commentaire) {
		this.commentaire = commentaire;
	}

	public Integer getProgression() {
		return progression;
	}

	public void setProgression(Integer progression) {
		this.progression = progression;
	}
	
	@Override
	public String toString() {
	    return "Extraction{" +
	            "id=" + id +
	            ", dateDebut=" + dateDebut +
	            ", dateFin=" + dateFin +
	            ", statut='" + statut + '\'' +
	            ", nbJoueurs=" + nbJoueurs +
	            '}';
	}
	
	public String getDateFinFormatee() {

	    if(dateFin == null) {
	        return "En cours";
	    }

	    return dateFin.format(
	        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
	    );
	}
}