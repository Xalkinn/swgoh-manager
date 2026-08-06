let ordreEvolutionJoueur = {};


function trierEvolutionJoueur(colonne) {

    const table = document.getElementById("tableEvolutionJoueur");

    if (!table) {
        console.log("Table introuvable");
        return;
    }


    const tbody = table.querySelector("tbody");

    const lignes = Array.from(tbody.rows);


    ordreEvolutionJoueur[colonne] =
        !ordreEvolutionJoueur[colonne];


    lignes.sort((a, b) => {

        let valeurA = a.cells[colonne].innerText.trim();
        let valeurB = b.cells[colonne].innerText.trim();


        return ordreEvolutionJoueur[colonne]
            ? valeurA.localeCompare(valeurB)
            : valeurB.localeCompare(valeurA);

    });


    lignes.forEach(ligne => tbody.appendChild(ligne));

}