package fr.xalkinn.swgohmanager.modele;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Référentiel d'un personnage SWGOH.
 * Une seule ligne par base_id, indépendamment des joueurs et des extractions.
 */
@Table("personnage_definition")
public class PersonnageDefinition {

    @Id
    private String baseId;
    private String nomBrut;
    private String nomFrancais;
    private String cleLocalisation;
    private String versionLocalisation;
    private LocalDateTime dateMaj;

    public String getBaseId() {
        return baseId;
    }

    public void setBaseId(String baseId) {
        this.baseId = baseId;
    }

    public String getNomBrut() {
        return nomBrut;
    }

    public void setNomBrut(String nomBrut) {
        this.nomBrut = nomBrut;
    }

    public String getNomFrancais() {
        return nomFrancais;
    }

    public void setNomFrancais(String nomFrancais) {
        this.nomFrancais = nomFrancais;
    }

    public String getCleLocalisation() {
        return cleLocalisation;
    }

    public void setCleLocalisation(String cleLocalisation) {
        this.cleLocalisation = cleLocalisation;
    }

    public String getVersionLocalisation() {
        return versionLocalisation;
    }

    public void setVersionLocalisation(String versionLocalisation) {
        this.versionLocalisation = versionLocalisation;
    }

    public LocalDateTime getDateMaj() {
        return dateMaj;
    }

    public void setDateMaj(LocalDateTime dateMaj) {
        this.dateMaj = dateMaj;
    }
}
