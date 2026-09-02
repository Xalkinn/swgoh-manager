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

    function init() {
        initTheme();
        initPlayerSelector();
        initUpsPagination();
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
