// <!-- JS pour le comparateur-->
		console.log("SCRIPT COMPARATIF CHARGE");
		var ordreTri = {};
		/**
		 * Recherche joueur
		 */
		document
		.getElementById("rechercheJoueur")
		.addEventListener("keyup", function () {
		    let recherche = this.value.toLowerCase();
		    let lignes = document
		        .querySelectorAll("#tableComparatif tr");
		    lignes.forEach(function(ligne){
		        let joueur = ligne
		            .cells[0]
		            .textContent
		            .toLowerCase();
		        if(joueur.includes(recherche)){
		            ligne.style.display = "";
		        }
		        else{
		            ligne.style.display = "none";
		        }
		    });
		    mettreAJourCompteur();
		});
		/**
		 * Tri tableau
		 */
		 function trierTableau(colonne){
		     let tbody = document.getElementById("tableComparatif");
		     if(!tbody){
		         console.log("Tableau comparatif absent");
		         return;
		     }
		     let lignes = Array.from(tbody.rows);
		     ordreTri[colonne] = !ordreTri[colonne];
		     lignes.sort(function(a,b){
		         let A = a.cells[colonne]?.innerText ?? "";
		         let B = b.cells[colonne]?.innerText ?? "";
		         if(!isNaN(A) && !isNaN(B)){
		             return ordreTri[colonne]
		                 ? Number(A)-Number(B)
		                 : Number(B)-Number(A);
		         }
		         return ordreTri[colonne]
		             ? A.localeCompare(B)
		             : B.localeCompare(A);
		     });
		     lignes.forEach(ligne => tbody.appendChild(ligne));
		 }
		/**
		 * Reset
		 */
		 function resetComparatif(){
		     document.getElementById("rechercheJoueur").value="";

		     document
		     .querySelectorAll("#tableComparatif tr")
		     .forEach(ligne=>{
		         ligne.style.display="";
		     });

		     mettreAJourCompteur();
		 }
		/**
		 * Compteur
		 */
		 window.mettreAJourCompteur = function(){
		    let lignes =
		        document.querySelectorAll("#tableComparatif tr");
		    let visibles=0;
		    lignes.forEach(ligne=>{
		        if(ligne.style.display !== "none"){
		            visibles++;
		        }
		    });
		    document.getElementById("compteurJoueur")
		    .innerHTML =
		    "Affichage : "
		    + visibles
		    + " / "
		    + lignes.length
		    + " joueurs";
		}
		mettreAJourCompteur();