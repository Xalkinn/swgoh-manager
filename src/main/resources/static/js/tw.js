(function () {
    "use strict";

    function formatNumber(value) {
        const n = Number(value || 0);
        return new Intl.NumberFormat("fr-FR", { maximumFractionDigits: 0 }).format(n);
    }

    function initMatchModal() {
        const modal = document.getElementById("twMatchModal");
        if (!modal) return;

        document.querySelectorAll(".tw-match-row").forEach((row) => {
            row.addEventListener("keydown", (event) => {
                if (event.key !== "Enter" && event.key !== " ") return;
                event.preventDefault();
                row.click();
            });
        });

        modal.addEventListener("show.bs.modal", (event) => {
            const row = event.relatedTarget;
            if (!row) return;

            const result = row.dataset.result || "Résultat";
            const code = row.dataset.resultCode || "DRAW";
            const resultEl = document.getElementById("twModalResult");
            resultEl.textContent = result;
            resultEl.className = "tw-result-pill " + (code === "WIN" ? "win" : code === "LOSS" ? "loss" : "draw");

            document.getElementById("twModalOpponent").textContent = row.dataset.opponent || "Adversaire";
            document.getElementById("twModalDate").textContent = row.dataset.date || "-";
            document.getElementById("twModalOurScore").textContent = row.dataset.score || "0";
            document.getElementById("twModalOpponentScore").textContent = row.dataset.opponentScore || "0";

            const margin = Number(row.dataset.margin || 0);
            document.getElementById("twModalMargin").textContent = "Écart : " + (margin > 0 ? "+" : "") + margin;
            const opponentGp = Number(row.dataset.opponentGp || 0);
            const power = Number(row.dataset.power || 0);
            document.getElementById("twModalOpponentGp").textContent = opponentGp > 0 ? formatNumber(opponentGp) + " PG" : "-";
            document.getElementById("twModalPower").textContent = power > 0 ? formatNumber(power) : "-";
            document.getElementById("twModalDuration").textContent = (row.dataset.duration || "0") + " h";
            document.getElementById("twModalType").textContent = row.dataset.twId || "-";
        });
    }


    function initOmicronMissingModal() {
        const modal = document.getElementById("twOmicronMissingModal");
        const dataRoot = document.getElementById("twOmicronMissingData");
        if (!modal || !dataRoot) return;

        document.querySelectorAll(".tw-clickable-kpi").forEach((card) => {
            card.addEventListener("keydown", (event) => {
                if (event.key !== "Enter" && event.key !== " ") return;
                event.preventDefault();
                card.click();
            });
        });

        const allRows = Array.from(dataRoot.querySelectorAll(".tw-missing-source")).map((el) => ({
            priority: Number(el.dataset.priority || 0),
            joueurId: el.dataset.joueurId || "",
            playerId: el.dataset.playerId || "",
            player: el.dataset.player || "Joueur",
            personnage: el.dataset.personnage || "Personnage",
            capacite: el.dataset.capacite || "Omicron"
        }));

        modal.addEventListener("show.bs.modal", (event) => {
            const trigger = event.relatedTarget;
            if (!trigger) return;

            const priority = trigger.dataset.priority || "ALL";
            const label = trigger.dataset.priorityLabel || "Plan TW global";
            const planTotal = Number(trigger.dataset.planTotal || 0);
            const rows = priority === "ALL"
                ? allRows
                : allRows.filter((row) => String(row.priority) === priority);

            document.getElementById("twOmicronMissingTitle").textContent = label + " · Omicrons manquants";
            document.getElementById("twOmicronMissingSubtitle").textContent =
                priority === "ALL"
                    ? "Toutes les priorités TW P1 à P4"
                    : "Priorité TW P" + priority;

            const grouped = new Map();
            rows.forEach((row) => {
                const key = row.joueurId || row.playerId || row.player;
                if (!grouped.has(key)) {
                    grouped.set(key, {
                        joueurId: row.joueurId,
                        player: row.player,
                        items: []
                    });
                }
                grouped.get(key).items.push(row);
            });

            document.getElementById("twOmicronMissingPlayers").textContent = grouped.size;
            document.getElementById("twOmicronMissingCount").textContent = rows.length;

            const list = document.getElementById("twOmicronMissingList");
            const empty = document.getElementById("twOmicronMissingEmpty");
            list.replaceChildren();

            if (!rows.length) {
                list.classList.add("d-none");
                empty.classList.remove("d-none");
                const strong = empty.querySelector("strong");
                const span = empty.querySelector("span");
                if (planTotal <= 0) {
                    strong.textContent = "Aucun Omicron configuré";
                    span.textContent = priority === "ALL"
                        ? "Le plan TW ne contient encore aucune priorité Omicron."
                        : "Aucun Omicron TW n'est configuré pour cette priorité.";
                } else {
                    strong.textContent = "Tout est complet 🎉";
                    span.textContent = "Aucun Omicron TW manquant pour cette sélection.";
                }
                return;
            }

            empty.classList.add("d-none");
            list.classList.remove("d-none");

            Array.from(grouped.values())
                .sort((a, b) => a.player.localeCompare(b.player, "fr", { sensitivity: "base" }))
                .forEach((group) => {
                    const playerCard = document.createElement("article");
                    playerCard.className = "tw-missing-player-card";

                    const header = document.createElement("div");
                    header.className = "tw-missing-player-header";

                    const playerBlock = document.createElement("div");
                    playerBlock.className = "tw-missing-player-name";

                    const avatar = document.createElement("span");
                    avatar.className = "tw-player-avatar";
                    avatar.textContent = (group.player || "J").trim().charAt(0).toUpperCase() || "J";
                    playerBlock.appendChild(avatar);

                    const nameWrap = document.createElement("span");
                    const name = document.createElement(group.joueurId ? "a" : "strong");
                    name.textContent = group.player;
                    if (group.joueurId) {
                        name.href = "/dashboard/joueur/" + encodeURIComponent(group.joueurId);
                        name.className = "tw-missing-player-link";
                    }
                    nameWrap.appendChild(name);

                    const count = document.createElement("small");
                    count.textContent = group.items.length + " Omicron" + (group.items.length > 1 ? "s" : "") + " manquant" + (group.items.length > 1 ? "s" : "");
                    nameWrap.appendChild(count);
                    playerBlock.appendChild(nameWrap);

                    header.appendChild(playerBlock);
                    playerCard.appendChild(header);

                    const omicronList = document.createElement("div");
                    omicronList.className = "tw-missing-omicron-list";
                    group.items
                        .sort((a, b) => a.priority - b.priority || a.personnage.localeCompare(b.personnage, "fr", { sensitivity: "base" }))
                        .forEach((item) => {
                            const line = document.createElement("div");
                            line.className = "tw-missing-omicron-row";

                            const priorityBadge = document.createElement("span");
                            priorityBadge.className = "tw-priority-badge p" + item.priority;
                            priorityBadge.textContent = "P" + item.priority;

                            const detail = document.createElement("span");
                            detail.className = "tw-missing-omicron-detail";
                            const charName = document.createElement("strong");
                            charName.textContent = item.personnage;
                            const ability = document.createElement("small");
                            ability.textContent = item.capacite;
                            detail.appendChild(charName);
                            detail.appendChild(ability);

                            line.appendChild(priorityBadge);
                            line.appendChild(detail);
                            omicronList.appendChild(line);
                        });
                    playerCard.appendChild(omicronList);
                    list.appendChild(playerCard);
                });
        });
    }

    function initPrepSearch() {
        const input = document.getElementById("twPrepSearch");
        const table = document.getElementById("twPrepTable");
        if (!input || !table) return;

        const rows = Array.from(table.querySelectorAll("tbody tr"));
        input.addEventListener("input", function () {
            const q = this.value.trim().toLowerCase();
            rows.forEach((row) => {
                row.style.display = !q || row.textContent.toLowerCase().includes(q) ? "" : "none";
            });
        });
    }

    function initScoreChart() {
        const canvas = document.getElementById("twScoreChart");
        const table = document.getElementById("twHistoryTable");
        if (!canvas || !table) return;

        const rows = Array.from(table.querySelectorAll("tbody tr")).slice(0, 12).reverse();
        if (!rows.length) return;

        const points = rows.map((row) => ({
            label: row.dataset.chartDate || "-",
            ours: Number(row.dataset.score || 0),
            opponent: Number(row.dataset.opponentScore || 0)
        }));

        const ctx = canvas.getContext("2d");

        function draw() {
            const ratio = window.devicePixelRatio || 1;
            const cssWidth = canvas.parentElement.clientWidth - 20;
            const cssHeight = 280;
            canvas.style.width = cssWidth + "px";
            canvas.style.height = cssHeight + "px";
            canvas.width = Math.max(320, cssWidth) * ratio;
            canvas.height = cssHeight * ratio;
            ctx.setTransform(ratio, 0, 0, ratio, 0, 0);
            ctx.clearRect(0, 0, cssWidth, cssHeight);

            const dark = document.documentElement.classList.contains("dark-mode");
            const grid = dark ? "rgba(148,163,184,.18)" : "rgba(100,116,139,.16)";
            const text = dark ? "#9aa5b8" : "#7d879b";
            const oursColor = "#2563eb";
            const opponentColor = "#dc2626";

            const left = 48;
            const right = 18;
            const top = 18;
            const bottom = 38;
            const width = cssWidth - left - right;
            const height = cssHeight - top - bottom;
            const maxValue = Math.max(...points.flatMap((p) => [p.ours, p.opponent]), 1);
            const minValue = Math.min(...points.flatMap((p) => [p.ours, p.opponent]), 0);
            const range = Math.max(1, maxValue - minValue);
            const paddedMin = Math.max(0, minValue - range * .08);
            const paddedMax = maxValue + range * .08;
            const paddedRange = Math.max(1, paddedMax - paddedMin);

            ctx.font = "11px system-ui, sans-serif";
            ctx.textAlign = "right";
            ctx.textBaseline = "middle";
            ctx.strokeStyle = grid;
            ctx.fillStyle = text;
            ctx.lineWidth = 1;

            for (let i = 0; i <= 4; i++) {
                const y = top + (height * i / 4);
                const value = paddedMax - (paddedRange * i / 4);
                ctx.beginPath();
                ctx.moveTo(left, y);
                ctx.lineTo(left + width, y);
                ctx.stroke();
                ctx.fillText(Math.round(value).toLocaleString("fr-FR"), left - 8, y);
            }

            function xAt(index) {
                return points.length === 1 ? left + width / 2 : left + width * index / (points.length - 1);
            }
            function yAt(value) {
                return top + height - ((value - paddedMin) / paddedRange) * height;
            }

            ctx.textAlign = "center";
            ctx.textBaseline = "top";
            ctx.fillStyle = text;
            points.forEach((p, i) => {
                if (points.length <= 8 || i % 2 === 0 || i === points.length - 1) {
                    ctx.fillText(p.label, xAt(i), top + height + 10);
                }
            });

            function drawLine(key, color) {
                ctx.strokeStyle = color;
                ctx.lineWidth = 2.5;
                ctx.beginPath();
                points.forEach((p, i) => {
                    const x = xAt(i);
                    const y = yAt(p[key]);
                    if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
                });
                ctx.stroke();

                ctx.fillStyle = color;
                points.forEach((p, i) => {
                    ctx.beginPath();
                    ctx.arc(xAt(i), yAt(p[key]), 3.5, 0, Math.PI * 2);
                    ctx.fill();
                });
            }

            drawLine("ours", oursColor);
            drawLine("opponent", opponentColor);
        }

        draw();
        let resizeTimer;
        window.addEventListener("resize", () => {
            clearTimeout(resizeTimer);
            resizeTimer = setTimeout(draw, 120);
        });

        const themeToggle = document.getElementById("themeToggle");
        if (themeToggle) themeToggle.addEventListener("click", () => setTimeout(draw, 0));
    }

    function init() {
        initMatchModal();
        initOmicronMissingModal();
        initPrepSearch();
        initScoreChart();
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
