/* =========================================================
   COMPARATIF OBJECTIF
   Le contenu est chargé dynamiquement dans une modale.
   ========================================================= */
(function () {
    "use strict";

    const ordreTri = {};

    window.initialiserComparatif = function () {
        const recherche = document.getElementById("rechercheJoueur");
        if (!recherche || recherche.dataset.initialized === "true") {
            window.mettreAJourCompteur?.();
            return;
        }

        recherche.dataset.initialized = "true";
        recherche.addEventListener("keyup", function () {
            const valeur = this.value.toLowerCase();
            document.querySelectorAll("#tableComparatif tr").forEach(function (ligne) {
                const joueur = ligne.cells[0]?.textContent?.toLowerCase() ?? "";
                ligne.style.display = joueur.includes(valeur) ? "" : "none";
            });
            window.mettreAJourCompteur();
        });

        window.mettreAJourCompteur();
    };

    window.trierTableau = function (colonne) {
        const tbody = document.getElementById("tableComparatif");
        if (!tbody) return;

        const lignes = Array.from(tbody.rows);
        ordreTri[colonne] = !ordreTri[colonne];

        lignes.sort(function (a, b) {
            const A = a.cells[colonne]?.innerText?.trim() ?? "";
            const B = b.cells[colonne]?.innerText?.trim() ?? "";
            const numA = parseFloat(A.replace(/[^0-9,.-]/g, "").replace(",", "."));
            const numB = parseFloat(B.replace(/[^0-9,.-]/g, "").replace(",", "."));

            if (!Number.isNaN(numA) && !Number.isNaN(numB)) {
                return ordreTri[colonne] ? numA - numB : numB - numA;
            }
            return ordreTri[colonne] ? A.localeCompare(B) : B.localeCompare(A);
        });

        lignes.forEach((ligne) => tbody.appendChild(ligne));
    };

    window.resetComparatif = function () {
        const recherche = document.getElementById("rechercheJoueur");
        if (recherche) recherche.value = "";

        document.querySelectorAll("#tableComparatif tr").forEach((ligne) => {
            ligne.style.display = "";
        });
        window.mettreAJourCompteur();
    };

    window.mettreAJourCompteur = function () {
        const compteur = document.getElementById("compteurJoueur");
        if (!compteur) return;

        const lignes = Array.from(document.querySelectorAll("#tableComparatif tr"));
        const visibles = lignes.filter((ligne) => ligne.style.display !== "none").length;
        compteur.textContent = `Affichage : ${visibles} / ${lignes.length} joueurs`;
    };

    document.addEventListener("DOMContentLoaded", window.initialiserComparatif);
})();
