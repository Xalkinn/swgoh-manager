/* =========================================================
   DASHBOARD JOUEUR
   Les comportements communs (thème + sélecteur joueur)
   sont volontairement gérés dans dashboard.js.
   ========================================================= */
(function () {
    "use strict";

    function initRosterSearch() {
        const search = document.getElementById("rosterSearch");
        const table = document.getElementById("rosterTable");
        if (!search || !table) return;

        const rows = Array.from(table.querySelectorAll("tbody tr"));
        const counter = document.getElementById("rosterVisibleCount");

        function filterRoster() {
            const query = search.value.toLowerCase().trim();
            let visible = 0;

            rows.forEach(row => {
                const show = row.textContent.toLowerCase().includes(query);
                row.style.display = show ? "" : "none";
                if (show) visible++;
            });

            if (counter) counter.textContent = visible;
        }

        search.addEventListener("input", filterRoster);
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initRosterSearch);
    } else {
        initRosterSearch();
    }
})();

// Ouvre la popin Omicron au clavier sur une ligne de priorité.
document.querySelectorAll('.player-priority-clickable').forEach((row) => {
    row.addEventListener('keydown', (event) => {
        if (event.key !== 'Enter' && event.key !== ' ') return;
        event.preventDefault();
        const target = row.getAttribute('data-bs-target');
        if (!target || typeof bootstrap === 'undefined') return;
        const modalElement = document.querySelector(target);
        if (modalElement) bootstrap.Modal.getOrCreateInstance(modalElement).show();
    });
});
