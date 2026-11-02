/* GameKit (GK): shared plumbing for the four gold games - canvas sizing, keyboard + touch input,
   start / game-over overlay, best score, pause, and ESC -> close the pop-out window. */
(function () {
  'use strict';
  var GK = window.GK = { keys: {}, edges: {}, taps: [], ptr: { x: 0, y: 0, down: false }, state: 'ready', w: 800, h: 600, ctx: null, best: 0, score: 0, cfg: null, time: 0 };
  var KEYMAP = { ArrowLeft: 'left', KeyA: 'left', ArrowRight: 'right', KeyD: 'right', ArrowUp: 'up', KeyW: 'up',
                 ArrowDown: 'down', KeyS: 'down', Space: 'act', ShiftLeft: 'act', ShiftRight: 'act',
                 Digit1: 'k1', Numpad1: 'k1', Digit2: 'k2', Numpad2: 'k2', Digit3: 'k3', Numpad3: 'k3' };
  var overlay, ovTitle, ovText, canvas, last = 0, dpr = 1;

  function store(get, key, val) {
    try { if (get) return window.localStorage.getItem(key); window.localStorage.setItem(key, String(val)); } catch (e) { /* storage blocked */ }
    return null;
  }

  function resize() {
    dpr = Math.min(window.devicePixelRatio || 1, 2);
    GK.w = Math.max(320, window.innerWidth); GK.h = Math.max(240, window.innerHeight);
    canvas.width = Math.floor(GK.w * dpr); canvas.height = Math.floor(GK.h * dpr);
    canvas.style.width = GK.w + 'px'; canvas.style.height = GK.h + 'px';
    GK.ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    if (GK.cfg && GK.cfg.onResize) GK.cfg.onResize(GK.w, GK.h);
  }

  function showOverlay(title, text) {
    ovTitle.textContent = title; ovText.textContent = text; overlay.style.display = 'flex';
  }

  function begin() {
    if (GK.state === 'play') return;
    GK.cfg.reset();
    GK.score = 0; GK.time = 0; GK.taps = []; GK.edges = {}; GK.state = 'play'; overlay.style.display = 'none';
  }

  GK.gameOver = function (message) {
    if (GK.state !== 'play') return;
    GK.state = 'over';
    var s = Math.floor(GK.score);
    var record = s > GK.best;
    if (record) { GK.best = s; store(false, 'best-' + GK.cfg.id, s); }
    showOverlay(record ? 'New best!  ' + s : (message || 'Game over') + '  ' + s,
                (record ? 'You set a new record.  ' : 'Best ' + GK.best + '.  ') + 'Press Space or tap to play again');
  };

  function onKey(down) {
    return function (e) {
      if (e.code === 'Escape') {
        if (down) { try { if (window.parent && window.parent !== window) window.parent.postMessage({ type: 'close-game' }, '*'); } catch (x) { /* ignore */ } }
        return;
      }
      var a = KEYMAP[e.code];
      if (a) { if (down && !GK.keys[a]) GK.edges[a] = true; GK.keys[a] = down; if (e.preventDefault) e.preventDefault(); }
      if (down && (e.code === 'Space' || e.code === 'Enter') && GK.state !== 'play' && GK.state !== 'paused') begin();
      if (down && e.code === 'KeyP') {
        if (GK.state === 'play') { GK.state = 'paused'; showOverlay('Paused', 'Press P to continue'); }
        else if (GK.state === 'paused') { GK.state = 'play'; overlay.style.display = 'none'; }
      }
    };
  }

  function bindPointer() {
    function pos(e) { var r = canvas.getBoundingClientRect ? canvas.getBoundingClientRect() : { left: 0, top: 0 }; return { x: e.clientX - r.left, y: e.clientY - r.top }; }
    canvas.addEventListener('pointerdown', function (e) { var p = pos(e); GK.ptr = { x: p.x, y: p.y, down: true }; if (GK.state === 'play') GK.taps.push(p); });
    canvas.addEventListener('pointermove', function (e) { if (GK.ptr.down) { var p = pos(e); GK.ptr.x = p.x; GK.ptr.y = p.y; } });
    var up = function () { GK.ptr.down = false; };
    canvas.addEventListener('pointerup', up); canvas.addEventListener('pointercancel', up); canvas.addEventListener('pointerleave', up);
  }

  function bindTouch() {
    var btns = document.querySelectorAll('[data-act]');
    for (var i = 0; i < btns.length; i++) {
      (function (b) {
        var a = b.getAttribute('data-act');
        var on = function (e) { if (!GK.keys[a]) GK.edges[a] = true; GK.keys[a] = true; if (GK.state !== 'play' && GK.state !== 'paused') begin(); if (e.preventDefault) e.preventDefault(); };
        var off = function (e) { GK.keys[a] = false; if (e.preventDefault) e.preventDefault(); };
        b.addEventListener('pointerdown', on); b.addEventListener('pointerup', off);
        b.addEventListener('pointerleave', off); b.addEventListener('pointercancel', off);
      })(btns[i]);
    }
    overlay.addEventListener('pointerdown', function () { if (GK.state !== 'play' && GK.state !== 'paused') begin(); });
  }

  function drawHud(ctx) {
    ctx.save();
    ctx.font = 'bold 15px Georgia, serif'; ctx.textBaseline = 'top';
    ctx.fillStyle = 'rgba(10,55,100,.78)'; ctx.fillRect(0, 0, GK.w, 34);
    ctx.fillStyle = '#ffd700'; ctx.textAlign = 'left';
    ctx.fillText(GK.cfg.title + '   Score ' + Math.floor(GK.score), 12, 9);
    ctx.textAlign = 'right';
    var extra = GK.cfg.hud ? GK.cfg.hud() : '';
    ctx.fillText((extra ? extra + '   ' : '') + 'Best ' + GK.best, GK.w - 70, 9);
    ctx.restore();
  }

  function loop(ts) {
    var dt = Math.min(0.05, Math.max(0, (ts - last) / 1000));
    last = ts;
    if (GK.state === 'play') { GK.time += dt; GK.cfg.update(dt); }
    GK.cfg.draw(GK.ctx, GK.w, GK.h);
    drawHud(GK.ctx);
    window.requestAnimationFrame(loop);
  }

  GK.start = function (cfg) {
    GK.cfg = cfg;
    canvas = document.getElementById('c'); GK.ctx = canvas.getContext('2d');
    overlay = document.getElementById('overlay'); ovTitle = document.getElementById('ovTitle'); ovText = document.getElementById('ovText');
    GK.best = parseInt(store(true, 'best-' + cfg.id), 10) || 0;
    resize(); window.addEventListener('resize', resize);
    window.addEventListener('keydown', onKey(true)); window.addEventListener('keyup', onKey(false));
    window.addEventListener('blur', function () { GK.keys = {}; });
    bindTouch(); bindPointer();
    cfg.reset();
    showOverlay(cfg.title, cfg.tip + '   -   Press Space or tap to start.   P = pause, Esc = close');
    window.requestAnimationFrame(function (ts) { last = ts; loop(ts); });
  };

  /* small drawing helpers shared by the games */
  GK.rr = function (ctx, x, y, w, h, r) {
    ctx.beginPath(); ctx.moveTo(x + r, y); ctx.lineTo(x + w - r, y); ctx.quadraticCurveTo(x + w, y, x + w, y + r);
    ctx.lineTo(x + w, y + h - r); ctx.quadraticCurveTo(x + w, y + h, x + w - r, y + h); ctx.lineTo(x + r, y + h);
    ctx.quadraticCurveTo(x, y + h, x, y + h - r); ctx.lineTo(x, y + r); ctx.quadraticCurveTo(x, y, x + r, y); ctx.closePath();
  };
  /** true once per fresh press of an action key (or touch button) */
  GK.pressed = function (a) { var v = !!GK.edges[a]; GK.edges[a] = false; return v; };
  GK.takeTap = function () { return GK.taps.length ? GK.taps.shift() : null; };
  GK.EMOJI = '"Segoe UI Emoji","Apple Color Emoji","Noto Color Emoji",sans-serif';
  GK.rand = function (a, b) { return a + Math.random() * (b - a); };
  GK.clamp = function (v, a, b) { return v < a ? a : (v > b ? b : v); };
})();
