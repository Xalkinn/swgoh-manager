/* =========================================================
   DASHBOARD - comportements communs
   - thème clair / sombre
   - sélection d'un joueur
   - pagination du tableau des ups (dashboard guilde uniquement)
   ========================================================= */
(function () {
    "use strict";

    const root = document.documentElement;

    function initTheme() {
        const toggle = document.getElementById("themeToggle");
        const icon = toggle ? toggle.querySelector("i") : null;
        const label = toggle ? toggle.querySelector("span") : null;

        function applyTheme(dark) {
            root.classList.toggle("dark-mode", dark);

            if (!toggle) return;

            if (icon) {
                icon.className = dark ? "bi bi-sun-fill" : "bi bi-moon-stars-fill";
            }
            if (label) {
                label.textContent = dark ? "Mode clair" : "Mode sombre";
            }

            toggle.setAttribute(
                "aria-label",
                dark ? "Activer le mode clair" : "Activer le mode sombre"
            );
            toggle.setAttribute(
                "title",
                dark ? "Passer en mode clair" : "Passer en mode sombre"
            );
        }

        const savedTheme = localStorage.getItem("swgoh-dashboard-theme");
        const prefersDark = window.matchMedia &&
            window.matchMedia("(prefers-color-scheme: dark)").matches;

        applyTheme(savedTheme ? savedTheme === "dark" : prefersDark);

        if (toggle) {
            toggle.addEventListener("click", function () {
                const dark = !root.classList.contains("dark-mode");
                applyTheme(dark);
                localStorage.setItem("swgoh-dashboard-theme", dark ? "dark" : "light");
            });
        }
    }

    function initPlayerSelector() {
        const selector = document.getElementById("playerSelector");
        if (!selector) return;

        selector.addEventListener("change", function () {
            if (!this.value) return;
            window.location.href = "/dashboard/joueur/" + encodeURIComponent(this.value);
        });
    }

    function initUpsPagination() {
        const table = document.getElementById("tableauUps");
        const pagination = document.getElementById("paginationUps");
        if (!table || !pagination) return;

        const rows = Array.from(table.querySelectorAll("tbody tr"));
        const perPage = 20;
        let page = 1;
        const totalPages = Math.max(1, Math.ceil(rows.length / perPage));

        function render() {
            const start = (page - 1) * perPage;
            const end = start + perPage;

            rows.forEach((row, index) => {
                row.style.display = index >= start && index < end ? "" : "none";
            });

            pagination.innerHTML = "";
            if (rows.length <= perPage) return;

            const previous = document.createElement("button");
            previous.className = "page-btn";
            previous.type = "button";
            previous.innerHTML = '<i class="bi bi-chevron-left"></i>';
            previous.disabled = page === 1;
            previous.addEventListener("click", () => {
                page--;
                render();
            });
            pagination.appendChild(previous);

            for (let i = 1; i <= totalPages; i++) {
                const btn = document.createElement("button");
                btn.className = "page-btn" + (i === page ? " active" : "");
                btn.type = "button";
                btn.textContent = i;
                btn.addEventListener("click", () => {
                    page = i;
                    render();
                });
                pagination.appendChild(btn);
            }

            const next = document.createElement("button");
            next.className = "page-btn";
            next.type = "button";
            next.innerHTML = '<i class="bi bi-chevron-right"></i>';
            next.disabled = page === totalPages;
            next.addEventListener("click", () => {
                page++;
                render();
            });
            pagination.appendChild(next);
        }

        render();
    }

    function initEquipmentDashboard() {
        const modal = document.getElementById("dashboardEquipmentModal");
        if (!modal) return;

        const label = document.getElementById("equipmentModalMetricLabel");
        const search = document.getElementById("equipmentSearch");
        const filterButtons = Array.from(modal.querySelectorAll("[data-equipment-sort]"));
        const openButtons = Array.from(document.querySelectorAll("[data-equipment-metric]"));
        const tables = [
            document.getElementById("equipmentPlayersTable"),
            document.getElementById("equipmentCharactersTable")
        ].filter(Boolean);

        const labels = {
            g13: "G13",
            r5Plus: "R5+",
            r7Plus: "R7+",
            r9Plus: "R9+",
            r10: "R10"
        };

        let currentMetric = "r9Plus";

        function normalize(value) {
            return (value || "")
                .toString()
                .normalize("NFD")
                .replace(/[\u0300-\u036f]/g, "")
                .toLowerCase();
        }

        function sortTable(table, metric) {
            const tbody = table.tBodies[0];
            if (!tbody) return;

            const rows = Array.from(tbody.rows);
            rows.sort((a, b) => {
                const av = Number(a.dataset[metric] || 0);
                const bv = Number(b.dataset[metric] || 0);
                if (bv !== av) return bv - av;
                return (a.dataset.name || "").localeCompare(
                    b.dataset.name || "",
                    "fr",
                    { sensitivity: "base" }
                );
            });
            rows.forEach(row => tbody.appendChild(row));
        }

        function applyMetric(metric) {
            if (!labels[metric]) metric = "r9Plus";
            currentMetric = metric;

            if (label) label.textContent = labels[metric];

            filterButtons.forEach(button => {
                button.classList.toggle("active", button.dataset.equipmentSort === metric);
            });

            modal.querySelectorAll("[data-equipment-column]").forEach(cell => {
                cell.classList.toggle("metric-active", cell.dataset.equipmentColumn === metric);
            });

            tables.forEach(table => sortTable(table, metric));
        }

        function applySearch() {
            const query = normalize(search ? search.value : "");
            tables.forEach(table => {
                Array.from(table.tBodies[0]?.rows || []).forEach(row => {
                    const matches = !query || normalize(row.dataset.name).includes(query);
                    row.classList.toggle("equipment-row-hidden", !matches);
                });
            });
        }

        openButtons.forEach(button => {
            button.addEventListener("click", function () {
                applyMetric(this.dataset.equipmentMetric || "r9Plus");
            });
        });

        filterButtons.forEach(button => {
            button.addEventListener("click", function () {
                applyMetric(this.dataset.equipmentSort || "r9Plus");
            });
        });

        if (search) {
            search.addEventListener("input", applySearch);
        }

        modal.addEventListener("shown.bs.modal", function () {
            applyMetric(currentMetric);
            applySearch();
        });

        applyMetric(currentMetric);
    }

    function init() {
        initTheme();
        initPlayerSelector();
        initUpsPagination();
        initEquipmentDashboard();
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
