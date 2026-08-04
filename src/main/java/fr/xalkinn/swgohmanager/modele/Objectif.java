package fr.xalkinn.swgohmanager.modele;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("objectif")
public class Objectif {

    @Id
    private Integer id;

    private String personnageBaseId;

    private String nomPersonnage;

    private Integer relicCible;

    private Boolean actif;

}