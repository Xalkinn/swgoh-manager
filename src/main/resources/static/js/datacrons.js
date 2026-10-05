(function () {
    "use strict";

    let detailRequestId = 0;

    function planCatalog() {
        return Array.from(document.querySelectorAll(".datacron-plan-catalog-item")).map(function (el) {
            return {
                id: el.dataset.id,
                setId: Number(el.dataset.set || 0),
                tier: Number(el.dataset.tier || 0),
                label: el.dataset.label || "Mécanique Datacron",
                characterId: el.dataset.characterId || "",
                character: el.dataset.character || "",
                target: el.dataset.target || "",
                ability: el.dataset.ability || ""
            };
        });
    }

    function resetSelect(select, placeholder, disabled) {
        if (!select) return;
        select.innerHTML = "";
        const option = document.createElement("option");
        option.value = "";
        option.textContent = placeholder;
        select.appendChild(option);
        select.disabled = disabled !== false;
    }

    function appendOptions(select, items) {
        items.forEach(function (item) {
            const option = document.createElement("option");
            option.value = item.id;
            option.textContent = item.label;
            option.title = item.label;
            select.appendChild(option);
        });
    }

    function initPlanConfigurator() {
        const set = document.getElementById("datacronPlanSet");
        const t3 = document.getElementById("datacronPlanT3");
        const t6 = document.getElementById("datacronPlanT6");
        const t9 = document.getElementById("datacronPlanT9");
        const addButton = document.getElementById("datacronPlanAddButton");
        if (!set || !t3 || !t6 || !t9 || !addButton) return;

        const items = planCatalog();

        function bySetAndTier(setId, tier) {
            return items
                .filter(function (x) { return x.setId === setId && x.tier === tier; })
                .sort(function (a, b) {
                    const aCharacter = a.character || a.target || "";
                    const bCharacter = b.character || b.target || "";
                    if (tier === 9 && aCharacter !== bCharacter) {
                        return aCharacter.localeCompare(bCharacter, "fr", { sensitivity: "base" });
                    }
                    return a.label.localeCompare(b.label, "fr", { sensitivity: "base" });
                });
        }

        function updateButton() {
            addButton.disabled = !(set.value && t3.value && t6.value && t9.value);
        }

        function loadSet() {
            const setId = Number(set.value || 0);
            addButton.disabled = true;
            resetSelect(t3, setId ? "Choisir le T3" : "Choisir d'abord un Set", !setId);
            resetSelect(t6, "Choisir d'abord le T3", true);
            resetSelect(t9, "Choisir d'abord le T6", true);
            if (!setId) return;

            const options = bySetAndTier(setId, 3);
            appendOptions(t3, options);
            if (!options.length) resetSelect(t3, "Aucun T3 détecté pour ce Set", true);
        }

        function loadT3() {
            const setId = Number(set.value || 0);
            addButton.disabled = true;
            resetSelect(t6, t3.value ? "Choisir le T6" : "Choisir d'abord le T3", !t3.value);
            resetSelect(t9, "Choisir d'abord le T6", true);
            if (!setId || !t3.value) return;

            const options = bySetAndTier(setId, 6);
            appendOptions(t6, options);
            if (!options.length) resetSelect(t6, "Aucun T6 détecté pour ce Set", true);
        }

        function loadT6() {
            const setId = Number(set.value || 0);
            addButton.disabled = true;
            resetSelect(t9, t6.value ? "Choisir le T9" : "Choisir d'abord le T6", !t6.value);
            if (!setId || !t6.value) return;

            const options = bySetAndTier(setId, 9);
            appendOptions(t9, options);
            if (!options.length) resetSelect(t9, "Aucun T9 détecté pour ce Set", true);
        }

        set.addEventListener("change", loadSet);
        t3.addEventListener("change", loadT3);
        t6.addEventListener("change", loadT6);
        t9.addEventListener("change", updateButton);
    }

    function initInventorySetFilter() {
        const select = document.getElementById("datacronInventorySet");
        const form = document.getElementById("datacronInventorySetForm");
        if (!select || !form || select.disabled) return;
        select.addEventListener("change", function () {
            form.submit();
        });
    }

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
    }

    function init() {
        initPlanConfigurator();
        initInventorySetFilter();
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
