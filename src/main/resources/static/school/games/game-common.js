/* Shared by the education games: login check (class level comes from the student), score saving, leaderboard, confetti. */
(function (root) {
  'use strict';
  var S = root.School, el = S.el, doc = root.document, ctx = { level: 5, canSave: false, id: '' };

  /* HD confetti: the canvas is scaled by devicePixelRatio so every dot is crisp on Retina / 4K screens */
  var cv, g, parts = [], raf = 0, dpr = 1;
  function fxInit() {
    cv = doc.createElement('canvas'); cv.id = 'fx'; doc.body.appendChild(cv); g = cv.getContext('2d');
    function size() { dpr = root.devicePixelRatio || 1; cv.width = Math.round(root.innerWidth * dpr); cv.height = Math.round(root.innerHeight * dpr); g.setTransform(dpr, 0, 0, dpr, 0, 0); }
    size(); root.addEventListener('resize', size);
  }
  function confetti(x, y, n) {
    if (!g) return;
    var cols = ['#ffd700', '#38ef7d', '#ff5f6d', '#6dd5ed', '#e100ff', '#fff'];
    for (var i = 0; i < (n || 36); i++) parts.push({ x: x, y: y, vx: (Math.random() - .5) * 9, vy: -Math.random() * 8 - 2, r: 2 + Math.random() * 4, c: cols[i % cols.length], life: 60 + Math.random() * 30 });
    if (!raf) raf = root.requestAnimationFrame(tick);
  }
  function tick() {
    g.clearRect(0, 0, root.innerWidth, root.innerHeight);
    parts = parts.filter(function (p) { return p.life > 0; });
    parts.forEach(function (p) { p.x += p.vx; p.y += p.vy; p.vy += .35; p.life--; g.globalAlpha = Math.max(0, p.life / 60); g.fillStyle = p.c; g.beginPath(); g.arc(p.x, p.y, p.r, 0, 6.283); g.fill(); });
    g.globalAlpha = 1;
    raf = parts.length ? root.requestAnimationFrame(tick) : 0;
  }
  function burstAt(elm) { var r = elm.getBoundingClientRect(); confetti(r.left + r.width / 2, r.top + r.height / 2); }

  function board() {
    var box = doc.getElementById('board'); if (!box) return;
    S.api('GET', '/school/api/student/games/leaderboard?game=' + ctx.id).then(function (list) {
      while (box.firstChild) box.removeChild(box.firstChild);
      box.appendChild(el('h2', { class: 'gold', text: '🏆 Top 10 in the school' }));
      var ol = el('ol'); list.forEach(function (r) { ol.appendChild(el('li', { text: r.name + (r.classLevel ? ' (class ' + r.classLevel + ')' : '') + ' - ' + r.score })); });
      box.appendChild(list.length ? ol : el('p', { text: 'Be the first on the board!' }));
    }).catch(function () { /* the board is a bonus - the game works without it */ });
  }

  function boot(id, title) {
    ctx.id = id; fxInit();
    doc.getElementById('gtitle').textContent = title;
    return S.loadMe().then(function (me) {
      if (me.loggedIn && me.role === 'STUDENT') { ctx.level = me.classLevel || 5; ctx.canSave = true; }
      else if (me.loggedIn) { ctx.level = 5; }
      else { S.showError({ status: 401, message: 'Log in as a student to save scores', json: { code: 'AUTH_REQUIRED' } }, 'Play ' + title); }
      doc.getElementById('glevel').textContent = 'Class ' + ctx.level;
      board();
      return ctx;
    }).catch(function (e) { S.showError(e, 'Open ' + title); return ctx; });
  }

  /* returns a promise of the student's best score (or the score itself when not logged in) */
  function finish(score) {
    if (!ctx.canSave) return Promise.resolve(score);
    return S.api('POST', '/school/api/student/games/score', { game: ctx.id, score: score }).then(function (r) { board(); return r.best; }).catch(function (e) { S.showError(e, 'Save score'); return score; });
  }

  root.GameKit = { boot: boot, finish: finish, confetti: confetti, burstAt: burstAt, ctx: ctx, el: el };
})(window);
