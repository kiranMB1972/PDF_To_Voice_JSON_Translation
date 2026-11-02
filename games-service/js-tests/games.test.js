// Plays every game headlessly: real HTML + common.js + game script in jsdom, with a recording canvas stub, a manual
// frame clock and a SEEDED random generator (so results are repeatable).  Run: npm install && node --test games.test.js
const test = require('node:test');
const assert = require('node:assert');
const fs = require('node:fs');
const path = require('node:path');
const { JSDOM } = require('jsdom');

const GAMES = path.join(__dirname, '../src/main/resources/static/games');
const ALL = ['highway-racer', 'moto-dash', 'hill-climb-bike', 'neon-circuit', 'hotstuff-fire-maze', 'charmaine-memory-flip',
             'hotstuff-fire-runner', 'hotstuff-charmaine-quiz', 'charmaine-star-catcher'];

function ctxStub() {
  const calls = { n: 0, bad: 0 };
  const grad = { addColorStop() {} };
  const base = { measureText: () => ({ width: 10 }), createLinearGradient: () => grad, createRadialGradient: () => grad };
  return new Proxy(base, {
    get(t, k) { if (k in t) return t[k]; if (k === '__calls') return calls; return (...a) => { calls.n++; a.forEach(v => { if (typeof v === 'number' && !Number.isFinite(v)) calls.bad++; }); }; },
    set(t, k, v) { t[k] = v; return true; },
  });
}
function seeded(seed) { let a = seed >>> 0; return () => { a |= 0; a = (a + 0x6D2B79F5) | 0; let t = Math.imul(a ^ (a >>> 15), 1 | a); t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t; return ((t ^ (t >>> 14)) >>> 0) / 4294967296; }; }

function load(id, seed = 1) {
  const file = id + '.html';
  const html = fs.readFileSync(path.join(GAMES, file), 'utf8');
  const inline = html.match(/<script>([\s\S]*?)<\/script>/)[1];
  const dom = new JSDOM(html.replace(/<script[\s\S]*?<\/script>/g, ''), { url: 'http://localhost:8095/games/' + file, pretendToBeVisual: true, runScripts: 'outside-only' });
  const w = dom.window, ctx = ctxStub();
  w.Math.random = seeded(seed);
  w.HTMLCanvasElement.prototype.getContext = () => ctx;
  const q = []; w.requestAnimationFrame = f => { q.push(f); return q.length; };
  w.innerWidth = 900; w.innerHeight = 600;
  const messages = []; Object.defineProperty(w, 'parent', { value: { postMessage: m => messages.push(JSON.parse(JSON.stringify(m))) } });
  w.eval(fs.readFileSync(path.join(GAMES, 'common.js'), 'utf8'));
  w.eval(inline);
  let ts = 1000;
  const step = (frames, dtMs = 16) => { for (let i = 0; i < frames; i++) { ts += dtMs; const f = q.shift(); if (f) f(ts); } };
  const key = (code, down = true) => w.dispatchEvent(new w.KeyboardEvent(down ? 'keydown' : 'keyup', { code, bubbles: true }));
  const start = () => { key('Space'); key('Space', false); };
  return { w, GK: w.GK, ctx, step, key, start, messages };
}

for (const id of ALL) {
  test(`${id}: starts from the overlay, draws every frame without errors or NaN`, () => {
    const g = load(id);
    assert.strictEqual(g.GK.state, 'ready'); g.step(5);
    g.start(); assert.strictEqual(g.GK.state, 'play');
    g.step(600);
    assert.ok(g.ctx.__calls.n > 500, 'it draws');
    assert.strictEqual(g.ctx.__calls.bad, 0, 'no NaN / Infinity reaches the canvas');
    assert.ok(['play', 'over'].includes(g.GK.state));
  });

  test(`${id}: Esc closes the pop-out, P pauses and resumes`, () => {
    const g = load(id); g.key('Escape');
    assert.deepStrictEqual(g.messages, [{ type: 'close-game' }]);
    g.start(); g.step(10); g.key('KeyP'); assert.strictEqual(g.GK.state, 'paused');
    const s = g.GK.score; g.step(60); assert.strictEqual(g.GK.score, s, 'frozen while paused');
    g.key('KeyP'); assert.strictEqual(g.GK.state, 'play');
  });

  test(`${id}: works offline, never uses a black background, has touch input where needed`, () => {
    const html = fs.readFileSync(path.join(GAMES, id + '.html'), 'utf8');
    assert.ok(!/https?:\/\//.test(html.replace(/xmlns='[^']*'/g, '')), 'nothing loaded from the internet');
    assert.ok(!/background:\s*#000[;"'}]/.test(html) && !/background:\s*black/i.test(html), 'no black background');
    assert.ok(!/rgba\(0,\s*0,\s*0/.test(html), 'no black translucent overlays');
    assert.ok(html.includes('<script src="common.js"></script>'));
  });
}

/* ----------------------------------------------------------------- the vehicle games (unchanged behaviour) */
test('Highway Racer: scores, sitting still is not safe, crash saves the best score, nitro is faster', () => {
  const g = load('highway-racer'); g.start(); g.step(300);
  assert.ok(g.GK.score > 20);
  let f = 0; while (g.GK.state === 'play' && f < 60 * 90) { g.step(1); f++; }
  assert.strictEqual(g.GK.state, 'over'); assert.ok(g.GK.best > 0);
  assert.strictEqual(g.w.localStorage.getItem('best-highway-racer'), String(g.GK.best));
  const n = load('highway-racer'); n.start(); n.step(120); const km1 = parseInt(n.GK.cfg.hud(), 10); n.key('Space'); n.step(120);
  assert.ok(parseInt(n.GK.cfg.hud(), 10) > km1);
});

test('Moto Dash: idle crashes, jumping survives far longer', () => {
  const idle = load('moto-dash'); idle.start(); let f = 0; while (idle.GK.state === 'play' && f < 3600) { idle.step(1); f++; }
  assert.strictEqual(idle.GK.state, 'over');
  const bot = load('moto-dash'); bot.start(); let alive = 0;
  for (let i = 0; i < 1200 && bot.GK.state === 'play'; i++) { if (i % 54 === 0) bot.key('ArrowUp'); if (i % 54 === 30) bot.key('ArrowUp', false); bot.step(1); alive++; }
  assert.ok(alive > 240 && bot.GK.score > 100);
});

test('Hill Climb Bike: gas moves it, idle ends, full gas eventually crashes after real progress', () => {
  const g = load('hill-climb-bike'); g.start(); g.key('ArrowRight'); g.step(180); assert.ok(g.GK.score > 5);
  const idle = load('hill-climb-bike'); idle.start(); let f = 0; while (idle.GK.state === 'play' && f < 12000) { idle.step(1); f++; }
  assert.strictEqual(idle.GK.state, 'over');
  const full = load('hill-climb-bike'); full.start(); full.key('ArrowRight'); f = 0; while (full.GK.state === 'play' && f < 7200) { full.step(1); f++; }
  assert.strictEqual(full.GK.state, 'over'); assert.ok(full.GK.score > 20);
});

test('Neon Circuit: laps score and the 120 s clock ends the game', () => {
  const g = load('neon-circuit'); g.start(); g.key('ArrowUp'); g.step(120); assert.ok(g.GK.score > 1);
  let f = 0; while (g.GK.state === 'play' && f < 7800) { g.step(1); f++; }
  assert.strictEqual(g.GK.state, 'over'); assert.ok(f >= 60 * 118 && f <= 60 * 125);
});

/* ----------------------------------------------------------------- Hotstuff & Princess Charmaine */
test('Fire Maze: the diamond is always reachable; following the path wins level 1 and scores; the clock can run out', () => {
  const g = load('hotstuff-fire-maze', 7); g.start(); g.step(2);
  const path = g.w.GK.test.solve(); assert.ok(path.length > 4, 'a real path exists: ' + path.length);
  const code = d => (d[0] === 1 ? 'ArrowRight' : d[0] === -1 ? 'ArrowLeft' : d[1] === 1 ? 'ArrowDown' : 'ArrowUp');
  for (const d of path) { g.key(code(d)); g.step(1); g.key(code(d), false); g.step(1); }
  const st = g.w.GK.test.state();
  assert.strictEqual(st.level, 2, 'level 2 reached'); assert.ok(g.GK.score >= 100, 'score ' + g.GK.score);

  const idle = load('hotstuff-fire-maze', 3); idle.start(); let f = 0; while (idle.GK.state === 'play' && f < 60 * 120) { idle.step(1); f++; }
  assert.strictEqual(idle.GK.state, 'over');
});

test('Fire Maze: walls block, and the crown gives bonus time', () => {
  const g = load('hotstuff-fire-maze', 11); g.start(); g.step(2);
  const before = g.w.GK.test.state();
  g.key('ArrowLeft'); g.step(1); g.key('ArrowLeft', false); g.step(1);        // start is at the left edge: wall
  const after = g.w.GK.test.state(); assert.deepStrictEqual([after.px, after.py], [before.px, before.py]);
});

test('Memory Flip: matching pairs wins and scores; a mismatch flips back and costs a move', () => {
  const g = load('charmaine-memory-flip', 5); g.start(); g.step(3);
  const deck = g.w.GK.test.deck(); assert.strictEqual(deck.length, 16);
  const t = g.w.GK.test;
  const tap = i => { const s = t.slot(i); t.tap(s.x, s.y); g.step(3); };
  const a = 0, b = deck.findIndex((e, i) => i > 0 && e !== deck[0]);
  tap(a); tap(b); assert.match(g.GK.cfg.hud(), /Moves 1/); g.step(70);
  assert.strictEqual(g.GK.state, 'play'); assert.match(g.GK.cfg.hud(), /Pairs 0\/8/);

  const seen = {};
  deck.forEach((e, i) => { (seen[e] = seen[e] || []).push(i); });
  for (const pair of Object.values(seen)) { tap(pair[0]); tap(pair[1]); g.step(35); }
  assert.strictEqual(g.GK.state, 'over'); assert.ok(g.GK.score >= 100 && g.GK.score <= 1000, 'score ' + g.GK.score);
});

test('Fire Runner: idle crashes, a jumping bot collects diamonds and lives longer', () => {
  const idle = load('hotstuff-fire-runner', 2); idle.start(); let f = 0; while (idle.GK.state === 'play' && f < 3600) { idle.step(1); f++; }
  assert.strictEqual(idle.GK.state, 'over');
  const bot = load('hotstuff-fire-runner', 2); bot.start(); let alive = 0;
  for (let i = 0; i < 1500 && bot.GK.state === 'play'; i++) { if (i % 50 === 0) bot.key('Space'); if (i % 50 === 25) bot.key('Space', false); bot.step(1); alive++; }
  assert.ok(alive > 200, 'bot survived ' + alive); assert.ok(bot.GK.score > 50);
});

test('Quiz: right answers score more than wrong ones, ten questions end the game, the timer moves on by itself', () => {
  const g = load('hotstuff-charmaine-quiz', 9); g.start(); g.step(2);
  for (let i = 0; i < 10; i++) { g.w.GK.test.answer(g.w.GK.test.right()); g.step(100); }
  assert.strictEqual(g.GK.state, 'over'); const st = g.w.GK.test.state();
  assert.strictEqual(st.correct, 10); assert.ok(g.GK.score >= 1000, 'score ' + g.GK.score);

  const wrong = load('hotstuff-charmaine-quiz', 9); wrong.start(); wrong.step(2);
  const bad = () => (wrong.w.GK.test.right() + 1) % 3;
  for (let i = 0; i < 10; i++) { wrong.w.GK.test.answer(bad()); wrong.step(100); }
  assert.strictEqual(wrong.GK.state, 'over'); assert.strictEqual(wrong.w.GK.test.state().correct, 0); assert.strictEqual(wrong.GK.score, 0);

  const lazy = load('hotstuff-charmaine-quiz', 4); lazy.start(); lazy.step(60 * 20);
  assert.ok(lazy.w.GK.test.state().qi >= 1, 'unanswered questions time out');
});

test('Quiz: keys 1/2/3 answer too', () => {
  const g = load('hotstuff-charmaine-quiz', 6); g.start(); g.step(2);
  g.key('Digit' + (g.w.GK.test.right() + 1)); g.step(2); g.key('Digit1', false); g.key('Digit2', false); g.key('Digit3', false);
  assert.strictEqual(g.w.GK.test.state().correct, 1);
});

test('Star Catcher: a bot that follows stars scores; sparks cost lives; the game can be lost', () => {
  const g = load('charmaine-star-catcher', 8); g.start();
  for (let i = 0; i < 60 * 25 && g.GK.state === 'play'; i++) {
    const items = g.w.GK.test.items().filter(t => t.kind !== 'ember').sort((a, b) => b.y - a.y);
    const px = g.w.GK.test.player(); g.key('ArrowLeft', false); g.key('ArrowRight', false);
    if (items[0]) { if (items[0].x < px - 8) g.key('ArrowLeft'); else if (items[0].x > px + 8) g.key('ArrowRight'); }
    g.step(1);
  }
  assert.ok(g.GK.score >= 100, 'bot caught things: ' + g.GK.score);
  const idle = load('charmaine-star-catcher', 8); idle.start(); let f = 0; while (idle.GK.state === 'play' && f < 60 * 200) { idle.step(1); f++; }
  assert.strictEqual(idle.GK.state, 'over', 'standing still in the corner eventually loses');
});
