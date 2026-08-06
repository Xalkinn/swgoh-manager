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

function chargerStatutMods() {

    fetch('/api/mods/status')
        .then(r => r.json())
        .then(data => {

            let etat = document.getElementById("modsEtat");
            let etape = document.getElementById("modsEtape");
            let progress = document.getElementById("modsProgress");

            if(!etat) return;

            if(data.enCours){
                etat.innerHTML="En cours";
                etat.className="badge bg-warning";
            }
            else if(data.fin != null){
                etat.innerHTML="Terminé";
                etat.className="badge bg-success";
            }
            else{
                etat.innerHTML="Repos";
                etat.className="badge bg-secondary";
            }

            etape.innerHTML = data.etapeActuelle ?? "-";

            progress.style.width = data.pourcentage + "%";
            progress.innerHTML = data.pourcentage + "%";
        });

}

setInterval(chargerStatutMods,2000);
chargerStatutMods();