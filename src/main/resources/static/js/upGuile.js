function trierTableau(colonne) {

    const table = document.getElementById("tableauUps");
    let lignes = Array.from(table.rows).slice(1);

    let asc = table.dataset.ordre !== "asc";

    lignes.sort((a, b) => {

        let A = a.cells[colonne].innerText;
        let B = b.cells[colonne].innerText;

        let numA = parseInt(A);
        let numB = parseInt(B);

        if (!isNaN(numA) && !isNaN(numB)) {
            return asc ? numA - numB : numB - numA;
        }

        return asc 
            ? A.localeCompare(B)
            : B.localeCompare(A);
    });

    lignes.forEach(ligne => table.tBodies[0].appendChild(ligne));

    table.dataset.ordre = asc ? "asc" : "desc";
}