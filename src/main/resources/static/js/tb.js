(function () {
    "use strict";

    function initClickableKeyboard() {
        document.querySelectorAll(".tb-mission-clickable, .tb-phase-clickable").forEach((element) => {
            element.addEventListener("keydown", (event) => {
                if (event.key !== "Enter" && event.key !== " ") return;
                event.preventDefault();
                element.click();
            });
        });
    }

    function initPlayerSearch() {
        const input = document.getElementById("tbPlayerSearch");
        const table = document.getElementById("tbPlayerTable");
        if (!input || !table) return;

        const rows = Array.from(table.querySelectorAll("tbody tr"));
        input.addEventListener("input", function () {
            const q = this.value.trim().toLowerCase();
            rows.forEach((row) => {
                row.style.display = !q || row.textContent.toLowerCase().includes(q) ? "" : "none";
            });
        });
    }

    function init() {
        initClickableKeyboard();
        initPlayerSearch();
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
