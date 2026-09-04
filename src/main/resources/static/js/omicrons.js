(function () {
    "use strict";

    function catalog() {
        return Array.from(document.querySelectorAll(".omicron-catalog-item")).map((el) => ({
            id: el.dataset.id,
            category: el.dataset.category,
            categoryLabel: el.dataset.categoryLabel,
            baseId: el.dataset.baseId,
            character: el.dataset.character,
            skill: el.dataset.skill,
            skillId: el.dataset.skillId,
            type: el.dataset.type,
            tier: el.dataset.tier
        }));
    }

    function initConfigurator() {
        const category = document.getElementById("omicronCategorie");
        const character = document.getElementById("omicronPersonnage");
        const definition = document.getElementById("omicronDefinition");
        const addButton = document.getElementById("omicronAddButton");
        if (!category || !character || !definition || !addButton) return;

        const items = catalog();

        function resetSelect(select, placeholder, disabled = true) {
            select.innerHTML = "";
            const option = document.createElement("option");
            option.value = "";
            option.textContent = placeholder;
            select.appendChild(option);
            select.disabled = disabled;
        }

        function updateButton() {
            addButton.disabled = !definition.value;
        }

        category.addEventListener("change", () => {
            resetSelect(definition, "Choisir d'abord un personnage", true);
            addButton.disabled = true;

            if (!category.value) {
                resetSelect(character, "Choisir d'abord une catégorie", true);
                return;
            }

            const byCharacter = new Map();
            items.filter((x) => x.category === category.value).forEach((x) => {
                if (!byCharacter.has(x.baseId)) byCharacter.set(x.baseId, x.character);
            });

            resetSelect(character, "Choisir un personnage", false);
            Array.from(byCharacter.entries())
                .sort((a, b) => a[1].localeCompare(b[1], "fr", { sensitivity: "base" }))
                .forEach(([baseId, name]) => {
                    const option = document.createElement("option");
                    option.value = baseId;
                    option.textContent = name;
                    character.appendChild(option);
                });
        });

        character.addEventListener("change", () => {
            addButton.disabled = true;
            if (!character.value) {
                resetSelect(definition, "Choisir d'abord un personnage", true);
                return;
            }

            const skills = items.filter((x) =>
                x.category === category.value && x.baseId === character.value);

            resetSelect(definition, skills.length === 1 ? "Capacité détectée" : "Choisir une capacité", false);
            skills.forEach((x) => {
                const option = document.createElement("option");
                option.value = x.id;
                option.textContent = x.skill;
                option.title = x.skillId;
                definition.appendChild(option);
            });

            if (skills.length === 1) {
                definition.value = skills[0].id;
            }
            updateButton();
        });

        definition.addEventListener("change", updateButton);
    }

    function init() {
        initConfigurator();
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
