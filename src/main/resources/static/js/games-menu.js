/* Top-right "Entertainment Games" drop-down. The games live in the separate Games microservice (folder games-service,
   default http://localhost:8095); this file only lists them and opens one in a pop-out window (Esc closes). */
(function () {
  'use strict';
  var sel = document.getElementById('gamesSel');
  if (!sel) return;
  var base = '', frame = null, modal, box, closeBtn;

  function buildModal() {
    modal = document.createElement('div'); modal.id = 'gm-modal';
    box = document.createElement('div'); box.id = 'gm-box';
    closeBtn = document.createElement('button'); closeBtn.id = 'gm-close'; closeBtn.type = 'button'; closeBtn.textContent = 'Close (Esc)';
    box.appendChild(closeBtn); modal.appendChild(box); document.body.appendChild(modal);
    closeBtn.addEventListener('click', closeGame);
    modal.addEventListener('click', function (e) { if (e.target === modal) closeGame(); });
  }
  function openGame(file, title) {
    if (!modal) buildModal();
    if (frame) frame.remove();
    frame = document.createElement('iframe'); frame.title = title; frame.setAttribute('sandbox', 'allow-scripts allow-same-origin');
    frame.src = base + '/games/' + file; box.appendChild(frame); modal.classList.add('open');
    setTimeout(function () { try { frame.contentWindow.focus(); } catch (e) { /* ignore */ } }, 300);
  }
  function closeGame() { if (modal) modal.classList.remove('open'); if (frame) { frame.remove(); frame = null; } }

  function placeholder(text, disabled) {
    sel.innerHTML = ''; var o = document.createElement('option'); o.value = ''; o.textContent = text; sel.appendChild(o); sel.disabled = !!disabled;
  }

  function fill(list) {
    placeholder('Entertainment Games \u25BE', false);
    var rec = document.createElement('option'); rec.value = '__recommend'; rec.textContent = '\uD83E\uDD16 Surprise me (AI pick)'; sel.appendChild(rec);
    var groups = {}, order = [];
    list.forEach(function (g) {
      if (!groups[g.group]) { groups[g.group] = document.createElement('optgroup'); groups[g.group].label = g.group; order.push(g.group); }
      var o = document.createElement('option'); o.value = g.file; o.textContent = g.icon + '  ' + g.name; o.title = g.blurb + '  -  ' + g.controls; o.setAttribute('data-id', g.id);
      groups[g.group].appendChild(o);
    });
    order.forEach(function (k) { sel.appendChild(groups[k]); });
  }

  function recommend(list) {
    fetch(base + '/api/ai/recommend?mood=fun').then(function (r) { return r.json(); }).then(function (d) {
      var g = list.filter(function (x) { return x.id === d.recommendedGameId; })[0] || list[0];
      if (g) openGame(g.file, g.name);
    }).catch(function () { if (list[0]) openGame(list[0].file, list[0].name); });
  }

  function load() {
    placeholder('Entertainment Games - loading...', true);
    fetch('/api/config').then(function (r) { return r.json(); }).then(function (c) { base = c.gamesServiceUrl; return fetch(base + '/api/games'); })
      .then(function (r) { if (!r.ok) throw new Error('HTTP ' + r.status); return r.json(); })
      .then(function (list) {
        fill(list);
        sel.onchange = function () {
          var v = sel.value; if (!v) return;
          if (v === '__recommend') recommend(list);
          else { var g = list.filter(function (x) { return x.file === v; })[0]; openGame(v, g ? g.name : 'Game'); }
          sel.value = '';                                              // ready for the next choice
        };
      })
      .catch(function () {
        placeholder('Entertainment Games - start the games service', false);
        sel.title = 'Run games-service\\RunGames.bat (port 8095), then click here to retry';
        sel.onfocus = function () { sel.onfocus = null; load(); };
      });
  }

  document.addEventListener('keydown', function (e) { if (e.key === 'Escape') closeGame(); });
  window.addEventListener('message', function (e) { if (e.data && e.data.type === 'close-game') closeGame(); });
  load();
})();
