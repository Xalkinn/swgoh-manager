package fr.xalkinn.swgohmanager.repository;

import java.util.List;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import fr.xalkinn.swgohmanager.modele.ComparatifObjectif;
import fr.xalkinn.swgohmanager.modele.EvolutionGuilde;
import fr.xalkinn.swgohmanager.modele.EvolutionPersonnage;
import fr.xalkinn.swgohmanager.modele.Personnage;

public interface PersonnageRepository extends CrudRepository<Personnage, Integer> {

    @Query("""
        SELECT relic
        FROM personnage
        WHERE joueur_id = :joueurId
          AND SUBSTRING_INDEX(base_id, ':', 1) = SUBSTRING_INDEX(:baseId, ':', 1)
        """)
    Integer trouverRelic(Integer joueurId, String baseId);

    @Query("""
        SELECT
            j.nom AS joueur,
            p.relic AS relic_actuelle,
            o.relic_cible AS relic_cible
        FROM objectif_personnage o
        JOIN personnage p
          ON TRIM(TRAILING ',' FROM o.nom) = p.nom
        JOIN joueur j
          ON j.id = p.joueur_id
        JOIN (
            SELECT joueur_id, nom, MAX(id) AS dernier_id
            FROM personnage
            GROUP BY joueur_id, nom
        ) dernier
          ON dernier.dernier_id = p.id
        WHERE o.nom = :nom
          AND j.dans_guilde = 1
        ORDER BY
            CASE WHEN p.relic >= o.relic_cible THEN 1 ELSE 0 END ASC,
            p.relic ASC,
            j.nom ASC
        """)
    List<ComparatifObjectif> getComparatifObjectif(@Param("nom") String nom);

    @Query("""
        SELECT MAX(extraction_id)
        FROM personnage
        WHERE joueur_id = :joueurId
        """)
    Integer trouverDerniereExtraction(Integer joueurId);

    @Query("""
        SELECT MAX(extraction_id)
        FROM personnage
        WHERE joueur_id = :joueurId
          AND extraction_id < :derniereExtraction
        """)
    Integer trouverAncienneExtraction(Integer joueurId, Integer derniereExtraction);

    @Query("""
        SELECT
            COALESCE(pd.nom_francais, nouveau.nom) AS nom,
            ancien.gear AS ancien_gear,
            nouveau.gear AS nouveau_gear,
            ancien.relic AS ancienne_relique,
            nouveau.relic AS nouvelle_relique,
            ancien.etoiles AS anciennes_etoiles,
            nouveau.etoiles AS nouvelles_etoiles
        FROM personnage ancien
        JOIN personnage nouveau
          ON ancien.joueur_id = nouveau.joueur_id
         AND ancien.nom = nouveau.nom
        LEFT JOIN personnage_definition pd
          ON pd.base_id = SUBSTRING_INDEX(nouveau.base_id, ':', 1)
        WHERE ancien.joueur_id = :joueurId
          AND ancien.extraction_id = :ancienneExtraction
          AND nouveau.extraction_id = :nouvelleExtraction
          AND (
              ancien.gear <> nouveau.gear
              OR ancien.relic <> nouveau.relic
              OR ancien.etoiles <> nouveau.etoiles
          )
        ORDER BY COALESCE(pd.nom_francais, nouveau.nom)
        """)
    List<EvolutionPersonnage> trouverEvolution(
            Integer joueurId,
            Integer ancienneExtraction,
            Integer nouvelleExtraction);

    @Query("""
        SELECT
            j.nom AS joueur,
            COALESCE(pd.nom_francais, nouveau.nom) AS personnage,
            ancien.gear AS ancien_gear,
            nouveau.gear AS nouveau_gear,
            ancien.relic AS ancienne_relique,
            nouveau.relic AS nouvelle_relique
        FROM (
            SELECT *
            FROM personnage
            WHERE extraction_id = (
                SELECT MAX(extraction_id) - 1
                FROM personnage
            )
        ) ancien
        JOIN (
            SELECT *
            FROM personnage
            WHERE extraction_id = (
                SELECT MAX(extraction_id)
                FROM personnage
            )
        ) nouveau
          ON ancien.joueur_id = nouveau.joueur_id
         AND ancien.nom = nouveau.nom
        JOIN joueur j
          ON j.id = nouveau.joueur_id
        LEFT JOIN personnage_definition pd
          ON pd.base_id = SUBSTRING_INDEX(nouveau.base_id, ':', 1)
        WHERE (
            ancien.gear <> nouveau.gear
            OR ancien.relic <> nouveau.relic
        )
          AND j.dans_guilde = 1
        ORDER BY j.nom, COALESCE(pd.nom_francais, nouveau.nom)
        """)
    List<EvolutionGuilde> trouverDerniersUpsGuilde();

    @Query("""
        SELECT
            COALESCE(pd.nom_francais, nouveau.nom) AS nom,
            ancien.gear AS ancien_gear,
            nouveau.gear AS nouveau_gear,
            ancien.relic AS ancienne_relique,
            nouveau.relic AS nouvelle_relique,
            ancien.etoiles AS anciennes_etoiles,
            nouveau.etoiles AS nouvelles_etoiles
        FROM personnage ancien
        JOIN personnage nouveau
          ON ancien.joueur_id = nouveau.joueur_id
         AND ancien.nom = nouveau.nom
        LEFT JOIN personnage_definition pd
          ON pd.base_id = SUBSTRING_INDEX(nouveau.base_id, ':', 1)
        WHERE ancien.joueur_id = :joueurId
          AND ancien.extraction_id = :ancienneExtraction
          AND nouveau.extraction_id = :nouvelleExtraction
          AND (
              ancien.gear <> nouveau.gear
              OR ancien.relic <> nouveau.relic
              OR ancien.etoiles <> nouveau.etoiles
          )
        ORDER BY COALESCE(pd.nom_francais, nouveau.nom)
        """)
    List<EvolutionPersonnage> comparerExtraction(
            Integer joueurId,
            Integer ancienneExtraction,
            Integer nouvelleExtraction);

    @Query("""
        SELECT
            j.nom AS joueur,
            COALESCE(pd.nom_francais, nouveau.nom) AS nom,
            ancien.gear AS ancien_gear,
            nouveau.gear AS nouveau_gear,
            ancien.relic AS ancienne_relique,
            nouveau.relic AS nouvelle_relique,
            ancien.etoiles AS anciennes_etoiles,
            nouveau.etoiles AS nouvelles_etoiles
        FROM personnage ancien
        JOIN personnage nouveau
          ON ancien.joueur_id = nouveau.joueur_id
         AND ancien.nom = nouveau.nom
        LEFT JOIN personnage_definition pd
          ON pd.base_id = SUBSTRING_INDEX(nouveau.base_id, ':', 1)
        JOIN joueur j
          ON j.id = nouveau.joueur_id
        WHERE ancien.extraction_id = :ancienneExtraction
          AND j.dans_guilde = 1
          AND nouveau.extraction_id = :nouvelleExtraction
          AND (
              ancien.gear <> nouveau.gear
              OR ancien.relic <> nouveau.relic
              OR ancien.etoiles <> nouveau.etoiles
          )
        ORDER BY j.nom, COALESCE(pd.nom_francais, nouveau.nom)
        """)
    List<EvolutionPersonnage> comparerTousLesJoueurs(
            Integer ancienneExtraction,
            Integer nouvelleExtraction);
}
