/* =========================================================
   MENU PRINCIPAL
   - état actif selon l'URL
   - ouverture automatique de la section Outils si nécessaire
   ========================================================= */
(function () {
    "use strict";

    function normalizePath(path) {
        if (!path) return "/";
        const clean = path.replace(/\/+$/, "");
        return clean || "/";
    }

    function initMenu() {
        const currentPath = normalizePath(window.location.pathname);
        const links = document.querySelectorAll("[data-menu-path]");
        let activeInTools = false;

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
                    activeInTools = true;
                }
            }
        });

        if (activeInTools) {
            const trigger = document.querySelector(".swgoh-tools-trigger");
            if (trigger) trigger.classList.add("active");
        }
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initMenu);
    } else {
        initMenu();
    }
})();
