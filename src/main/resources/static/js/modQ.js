function trierTableauModQ(colonne) {

    const table = document.getElementById("tableauModQ");

    let lignes = Array.from(table.tBodies[0].rows);

    let asc = table.dataset.ordre !== "asc";


    lignes.sort((a, b) => {

        let A = a.cells[colonne].innerText.trim();
        let B = b.cells[colonne].innerText.trim();


        let numA = parseFloat(A.replace(",", "."));
        let numB = parseFloat(B.replace(",", "."));


        if (!isNaN(numA) && !isNaN(numB)) {

            return asc 
                ? numA - numB
                : numB - numA;
        }


        return asc
            ? A.localeCompare(B)
            : B.localeCompare(A);

    });


    lignes.forEach(ligne => {
        table.tBodies[0].appendChild(ligne);
    });


    table.dataset.ordre = asc ? "asc" : "desc";
}