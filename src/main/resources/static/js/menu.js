/* =========================================================
   MENU PRINCIPAL
   - état actif selon l'URL
   - activation automatique du menu parent d'un sous-menu actif
   ========================================================= */
(function () {
    "use strict";

    function normalizePath(path) {
        if (!path) return "/";
        const clean = path.replace(/\/+$/, "");
        return clean || "/";
    }

    function markParentGroupActive(link) {
        const dropdown = link.closest(".dropdown[data-menu-group]");
        if (!dropdown) return;

        const trigger = dropdown.querySelector(":scope > .swgoh-nav-link");
        if (trigger) {
            trigger.classList.add("active");
        }
    }

    function initMenu() {
        const currentPath = normalizePath(window.location.pathname);
        const links = document.querySelectorAll("[data-menu-path]");

        links.forEach((link) => {
            const targetPath = normalizePath(link.dataset.menuPath);
            const isPlayerDashboard = currentPath.startsWith("/dashboard/joueur/");
            const isDashboard = targetPath === "/" &&
                (currentPath === "/" || (currentPath.startsWith("/dashboard") && !isPlayerDashboard));
            const isPlayerTracking = targetPath === "/joueurs" && isPlayerDashboard;
            const isMatch = targetPath !== "/" &&
                (isPlayerTracking || currentPath === targetPath || currentPath.startsWith(targetPath + "/"));

            if (isDashboard || isMatch) {
                link.classList.add("active");
                link.setAttribute("aria-current", "page");

                if (link.classList.contains("swgoh-dropdown-item")) {
                    markParentGroupActive(link);
                }
            }
        });
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initMenu);
    } else {
        initMenu();
    }
})();
