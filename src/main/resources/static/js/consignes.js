(() => {
    document.querySelectorAll('.consigne-review-form').forEach(form => {
        form.addEventListener('submit', event => {
            if (!form.querySelector('input[name="evolutions"]:checked')) { event.preventDefault(); alert('Coche au moins une évolution à traiter.'); }
        });
    });
    const reopen = new URLSearchParams(window.location.search).get('joueur');
    if (reopen && /^\d+$/.test(reopen)) {
        const modal = document.getElementById('joueur-' + reopen);
        if (modal && window.bootstrap) new bootstrap.Modal(modal).show();
    }

    const exportModal = document.getElementById('consigneExportModal');
    const analysisForm = document.getElementById('consigneAnalyseForm');
    if (exportModal && analysisForm) {
        exportModal.addEventListener('show.bs.modal', event => {
            if (!analysisForm.reportValidity()) { event.preventDefault(); return; }
            const fields = new FormData(analysisForm);
            const start = fields.get('debut'), end = fields.get('fin');
            if (end < start) {
                event.preventDefault();
                const input = analysisForm.querySelector('[name="fin"]');
                input.setCustomValidity('La date de fin doit suivre la date de début.');
                input.reportValidity(); input.setCustomValidity(''); return;
            }
            const query = new URLSearchParams();
            query.set('debut', start); query.set('fin', end);
            fields.getAll('listes').forEach(id => query.append('listes', id));
            exportModal.querySelectorAll('.consigne-export-choice').forEach(link => {
                const params = new URLSearchParams(query); params.set('format', link.dataset.format);
                link.href = '/consignes/export?' + params.toString();
            });
            const count = fields.getAll('listes').length;
            document.getElementById('consigneExportContext').textContent = 'Période : ' + start + ' → ' + end + ' · ' + count + ' liste(s) autorisée(s).';
        });
    }

    const normalize = value => value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase();
    document.querySelectorAll('.consigne-editor').forEach(form => {
        const select = form.querySelector('.consigne-select');
        const search = form.querySelector('.consigne-search');
        const picked = form.querySelector('.consigne-picked');
        const add = form.querySelector('.consigne-add');
        const empty = form.querySelector('.consigne-empty');
        const feedback = form.querySelector('.consigne-feedback');
        const dirty = form.querySelector('.consigne-dirty');
        const catalogue = Array.from(select.options).filter(o => o.value).map(o => ({id:o.value, name:o.textContent}));
        const selected = new Map();
        form.querySelectorAll('.consigne-initial input').forEach(input => {
            selected.set(input.value, catalogue.find(p => p.id === input.value)?.name || input.value);
        });
        form.querySelector('.consigne-initial')?.remove();
        function markDirty() { if (dirty) dirty.hidden = false; }
        function options() {
            const previous = select.value;
            select.replaceChildren(new Option('Choisir un personnage', ''));
            catalogue.filter(p => !selected.has(p.id) && normalize(p.name).includes(normalize(search.value))).forEach(p => select.add(new Option(p.name, p.id)));
            if (Array.from(select.options).some(o => o.value === previous)) select.value = previous;
            add.disabled = !select.value;
        }
        function render() {
            picked.replaceChildren();
            [...selected].sort((a,b) => a[1].localeCompare(b[1],'fr')).forEach(([id,name]) => {
                const row = document.createElement('div'); row.className = 'consigne-personnage';
                const icon = document.createElement('i'); icon.className = 'bi bi-person-check'; icon.setAttribute('aria-hidden','true');
                const label = document.createElement('span'); label.textContent = name;
                const input = document.createElement('input'); input.type = 'hidden'; input.name = 'personnages'; input.value = id;
                const remove = document.createElement('button'); remove.type = 'button'; remove.className = 'consigne-remove'; remove.title = 'Retirer '+name; remove.setAttribute('aria-label','Retirer '+name);
                const trash = document.createElement('i'); trash.className = 'bi bi-trash3'; remove.append(trash);
                remove.addEventListener('click', () => { selected.delete(id); markDirty(); render(); options(); feedback.textContent = name+' retiré. Enregistre la liste pour appliquer le changement.'; select.focus(); });
                row.append(icon,label,input,remove); picked.append(row);
            });
            empty.hidden = selected.size > 0;
        }
        add.addEventListener('click', () => {
            const character = catalogue.find(p => p.id === select.value);
            if (!character || selected.has(character.id)) return;
            selected.set(character.id, character.name); markDirty(); search.value=''; render(); options();
            feedback.textContent = character.name+' ajouté. Enregistre la liste pour appliquer le changement.'; select.focus();
        });
        search.addEventListener('input', options);
        search.addEventListener('keydown', event => { if(event.key==='Enter') { event.preventDefault(); if(select.options.length===2) {select.selectedIndex=1;add.disabled=false;add.click();} } });
        select.addEventListener('change', () => { add.disabled=!select.value; });
        form.querySelector('input[name="nom"]').addEventListener('input', markDirty);
        render(); options();
    });
})();
