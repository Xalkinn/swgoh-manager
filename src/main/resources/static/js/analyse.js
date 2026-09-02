/* =========================================================
   ANALYSE CANDIDAT
   ========================================================= */
(function () {
    "use strict";

    function initAnalyseForm() {
        const form = document.getElementById("analyseForm");
        const button = document.getElementById("analyseSubmit");
        if (!form || !button) return;

        form.addEventListener("submit", function () {
            button.classList.add("is-loading");
            button.disabled = true;
            button.innerHTML = '<span class="spinner-border spinner-border-sm" aria-hidden="true"></span><span>Analyse en cours…</span>';
        });
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initAnalyseForm);
    } else {
        initAnalyseForm();
    }
})();
