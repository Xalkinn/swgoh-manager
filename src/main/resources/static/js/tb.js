(function () {
    "use strict";

    function initMissionKeyboard() {
        document.querySelectorAll(".tb-mission-clickable").forEach((card) => {
            card.addEventListener("keydown", (event) => {
                if (event.key !== "Enter" && event.key !== " ") return;
                event.preventDefault();
                card.click();
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
        initMissionKeyboard();
        initPlayerSearch();
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
