(function () {
    "use strict";

    let detailRequestId = 0;

    function initPlayerSearch() {
        const input = document.getElementById("datacronPlayerSearch");
        const table = document.getElementById("datacronPlayerTable");
        if (!input || !table) return;

        const rows = Array.from(table.querySelectorAll("tbody tr"));
        input.addEventListener("input", function () {
            const query = input.value.trim().toLocaleLowerCase("fr");
            rows.forEach(function (row) {
                const name = (row.dataset.playerName || "").toLocaleLowerCase("fr");
                row.hidden = Boolean(query && !name.includes(query));
            });
        });
    }

    function initSetAccordion(root) {
        const scope = root || document;
        const accordions = Array.from(scope.querySelectorAll(".datacron-set-accordion"));
        if (!accordions.length) return;

        accordions.forEach(function (accordion) {
            const toggles = Array.from(accordion.querySelectorAll(".datacron-set-toggle"));

            function getPanel(toggle) {
                const panelId = toggle.getAttribute("aria-controls");
                return panelId ? document.getElementById(panelId) : null;
            }

            function closeSet(toggle) {
                const panel = getPanel(toggle);
                toggle.setAttribute("aria-expanded", "false");
                toggle.closest(".datacron-set-group")?.classList.remove("is-open");
                if (panel) panel.hidden = true;
            }

            function openSet(toggle) {
                const panel = getPanel(toggle);
                toggle.setAttribute("aria-expanded", "true");
                toggle.closest(".datacron-set-group")?.classList.add("is-open");
                if (panel) panel.hidden = false;
            }

            // Tous les sets sont fermés à chaque ouverture de la popin.
            toggles.forEach(closeSet);

            toggles.forEach(function (toggle) {
                if (toggle.dataset.accordionReady === "1") return;
                toggle.dataset.accordionReady = "1";

                toggle.addEventListener("click", function () {
                    const wasOpen = toggle.getAttribute("aria-expanded") === "true";
                    toggles.forEach(closeSet);
                    if (!wasOpen) openSet(toggle);
                });
            });
        });
    }

    function modalLoading() {
        return '<div class="datacron-modal-loading">' +
            '<div class="spinner-border" role="status" aria-hidden="true"></div>' +
            '<span>Chargement des Datacrons…</span>' +
            '</div>';
    }

    function modalError() {
        return '<div class="app-empty datacron-modal-empty">' +
            '<i class="bi bi-exclamation-triangle"></i>' +
            '<h3>Impossible de charger les Datacrons</h3>' +
            '<p>Réessaie en cliquant de nouveau sur le joueur.</p>' +
            '</div>';
    }

    function markSelectedPlayer(link) {
        const table = document.getElementById("datacronPlayerTable");
        if (!table) return;
        table.querySelectorAll("tbody tr.selected").forEach(function (row) {
            row.classList.remove("selected");
        });
        link.closest("tr")?.classList.add("selected");
    }

    function openPlayerModal(link) {
        const modalElement = document.getElementById("datacronPlayerModal");
        const modalBody = document.getElementById("datacronPlayerModalBody");
        const modalTitle = document.getElementById("datacronPlayerModalTitle");
        const url = link.dataset.detailUrl;
        const playerName = link.dataset.playerName || link.textContent.trim();

        if (!modalElement || !modalBody || !url || typeof bootstrap === "undefined") {
            window.location.href = link.href;
            return;
        }

        const requestId = ++detailRequestId;
        markSelectedPlayer(link);
        modalTitle.textContent = "Datacrons · " + playerName;
        modalBody.innerHTML = modalLoading();

        bootstrap.Modal.getOrCreateInstance(modalElement).show();

        fetch(url, {
            headers: { "X-Requested-With": "XMLHttpRequest" }
        })
            .then(function (response) {
                if (!response.ok) throw new Error("HTTP " + response.status);
                return response.text();
            })
            .then(function (html) {
                // Si l'utilisateur a cliqué sur un autre joueur entre-temps,
                // on ignore la réponse précédente.
                if (requestId !== detailRequestId) return;
                modalBody.innerHTML = html;
                initSetAccordion(modalBody);
            })
            .catch(function () {
                if (requestId !== detailRequestId) return;
                modalBody.innerHTML = modalError();
            });
    }

    function initPlayerModal() {
        const table = document.getElementById("datacronPlayerTable");
        if (!table) return;

        table.addEventListener("click", function (event) {
            const link = event.target.closest(".datacron-player-link");
            if (!link || !table.contains(link)) return;
            event.preventDefault();
            openPlayerModal(link);
        });

        // Compatibilité avec un ancien lien /datacrons?joueurId=...
        const selected = table.querySelector("tbody tr.selected .datacron-player-link");
        if (selected) openPlayerModal(selected);
    }

    function init() {
        initPlayerSearch();
        initPlayerModal();
        initSetAccordion(document);
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
