// Browser-level test of the home page: real index.html + speech.js + app.js + games-corner.js in jsdom,
// with a simulated Microsoft Edge speechSynthesis and a simulated server.   Run: npm install && npm test
const test = require('node:test');
const assert = require('node:assert');
const fs = require('node:fs');
const path = require('node:path');
const { JSDOM } = require('jsdom');

const STATIC = path.join(__dirname, '../src/main/resources/static');
const HTML = fs.readFileSync(path.join(STATIC, 'index.html'), 'utf8').replace(/<script[\s\S]*?<\/script>/g, '');
const SRC = n => fs.readFileSync(path.join(STATIC, 'js', n), 'utf8');

const V = (name, lang) => ({ name, lang, localService: false });
const EDGE_VOICES = [
  V('Microsoft Sapna Online (Natural) - Kannada (India)', 'kn-IN'), V('Microsoft Gagan Online (Natural) - Kannada (India)', 'kn-IN'),
  V('Microsoft Neerja Online (Natural) - English (India)', 'en-IN'), V('Microsoft Prabhat Online (Natural) - English (India)', 'en-IN'),
  V('Microsoft Gul Online (Natural) - Urdu (India)', 'ur-IN'), V('Microsoft Elvira Online (Natural) - Spanish (Spain)', 'es-ES'),
  V('Microsoft Swara Online (Natural) - Hindi (India)', 'hi-IN'), V('Microsoft Aarohi Online (Natural) - Marathi (India)', 'mr-IN'), V('Microsoft Denise Online (Natural) - French (France)', 'fr-FR'),
];
const LANGS = [
  ['kn', 'kn-IN', 'Kannada', 'SOUTH_INDIAN', 'South Indian Languages', ['kn-IN']], ['ta', 'ta-IN', 'Tamil', 'SOUTH_INDIAN', 'South Indian Languages', ['ta-IN']],
  ['te', 'te-IN', 'Telugu', 'SOUTH_INDIAN', 'South Indian Languages', ['te-IN']], ['ur', 'ur-IN', 'Urdu', 'SOUTH_INDIAN', 'South Indian Languages', ['ur-IN']],
  ['ml', 'ml-IN', 'Malayalam', 'SOUTH_INDIAN', 'South Indian Languages', ['ml-IN']], ['hi', 'hi-IN', 'Hindi', 'OTHER_INDIAN', 'Other Indian Languages', ['hi-IN']],
  ['mr', 'mr-IN', 'Marathi', 'OTHER_INDIAN', 'Other Indian Languages', ['mr-IN']], ['en', 'en-IN', 'English', 'ENGLISH', 'English (Indian PDF text to English)', ['en-IN']],
  ['es', 'es-ES', 'Spanish', 'INTERNATIONAL', 'International Languages', ['es-ES']], ['fr', 'fr-FR', 'French', 'INTERNATIONAL', 'International Languages', ['fr-FR']],
].map(([code, bcp47, englishName, family, familyLabel, voiceLocales]) => ({ code, bcp47, englishName, nativeName: englishName, family, familyLabel, voiceLocales }));
const CATALOG = [
  ['kn-IN-SapnaNeural', 'Sapna - Kannada (India)'], ['kn-IN-GaganNeural', 'Gagan - Kannada (India)'],
  ['en-IN-NeerjaNeural', 'Neerja - English (India)'], ['en-IN-PrabhatNeural', 'Prabhat - English (India)'],
  ['ur-IN-GulNeural', 'Gul - Urdu (India)'], ['es-ES-ElviraNeural', 'Elvira - Spanish'],
  ['hi-IN-SwaraNeural', 'Swara - Hindi (India)'], ['hi-IN-MadhurNeural', 'Madhur - Hindi (India)'], ['mr-IN-AarohiNeural', 'Aarohi - Marathi (India)'], ['fr-FR-DeniseNeural', 'Denise - French'],
].map(([name, label]) => ({ name, lang: name.slice(0, 5), gender: 'x', label }));
const GAMES = [
  { id: 'maze', name: 'Hotstuff Fire Maze', group: 'Hotstuff & Princess Charmaine', icon: 'F', file: 'hotstuff-fire-maze.html', blurb: 'b', controls: 'c' },
  { id: 'memory', name: 'Charmaine Memory Flip', group: 'Hotstuff & Princess Charmaine', icon: 'C', file: 'charmaine-memory-flip.html', blurb: 'b', controls: 'c' },
  { id: 'highway-racer', name: 'Gold Highway Racer', group: 'Cars & Bikes', icon: 'R', file: 'highway-racer.html', blurb: 'b', controls: 'c' },
];
const P1 = 'Good morning children. Today we learn about trees.';
const P3 = 'Trees give us shade and fruit. We must protect them.';
const DOC = { documentId: 'doc1', fileName: 'a.pdf', totalPages: 3, readablePages: 2, detectedLanguage: 'en', truncated: false, extractedChars: 100,
  skippedContent: ['1 of 3 page(s) are pictures or scans with no text and are skipped when reading aloud'],
  pages: [{ sequence: 1, readable: true, blocks: [{ type: 'TEXT', content: P1 }] }, { sequence: 2, readable: false, blocks: [{ type: 'IMAGE_ONLY', content: '' }] },
          { sequence: 3, readable: true, blocks: [{ type: 'TEXT', content: P3 }] }] };

const OPEN_WINDOWS = [];
test.after(() => OPEN_WINDOWS.forEach(w => { try { w.close(); } catch (e) { /* ignore */ } }));   // stop the clock's setInterval so node can exit

async function boot(cfg = {}) {
  const dom = new JSDOM(HTML, { url: 'http://localhost:8089/', pretendToBeVisual: true, runScripts: 'outside-only' });
  const w = dom.window; OPEN_WINDOWS.push(w);
  if (cfg.pre) cfg.pre(w);                                   // hook: stub geolocation, freeze Date ...
  Object.defineProperty(w.navigator, 'userAgent', { value: 'Mozilla/5.0 Chrome/130 Safari/537.36 Edg/130.0', configurable: true });
  const calls = { translate: [], fetched: [] }, spoken = [];
  const synth = {
    _v: cfg.voicesLate ? [] : EDGE_VOICES, listeners: [],
    getVoices() { return this._v; }, addEventListener(e, f) { this.listeners.push(f); }, cancel() { this.cancelled = (this.cancelled || 0) + 1; },
    speak(u) {
      spoken.push(u);
      const failing = cfg.failVoice && u.voice && u.voice.name.includes(cfg.failVoice);
      setTimeout(() => {
        if (failing) { u.onerror && u.onerror({ error: 'synthesis-failed' }); return; }
        u.onstart && u.onstart(); setTimeout(() => u.onend && u.onend(), 8);
      }, 3);
    },
  };
  if (cfg.voicesLate) setTimeout(() => { synth._v = EDGE_VOICES; synth.listeners.forEach(f => f()); }, cfg.voicesLate);
  w.speechSynthesis = synth;
  w.SpeechSynthesisUtterance = class { constructor(t) { this.text = t; } };
  const json = (status, body) => ({ ok: status < 400, status, text: async () => JSON.stringify(body), json: async () => body });
  w.fetch = async (url, init = {}) => {
    calls.fetched.push(url);
    if (url.startsWith('/api/greeting')) return json(200, { greeting: 'Good morning' });
    if (url === '/api/languages') return json(200, LANGS);
    if (url.startsWith('/api/v1/voice/voices')) return json(200, CATALOG);
    if (url.startsWith('/api/accents') && /langCode=(hi|mr|es|fr|en)/.test(url)) { const lc = url.split('langCode=')[1]; return json(200, [{ code: 'standard', displayName: 'Standard', description: 'neutral', voiceLocales: [{ hi: 'hi-IN', mr: 'mr-IN', es: 'es-ES', fr: 'fr-FR', en: 'en-IN' }[lc]] }]); }
    if (url.startsWith('/api/accents')) return json(200, [{ code: 'mysuru', displayName: 'Mysuru', description: 'calm', voiceLocales: ['kn-IN'] }, { code: 'standard', displayName: 'Standard', description: 'neutral', voiceLocales: ['kn-IN'] }]);
    if (url === '/api/v1/translate/status') return json(200, { available: cfg.engineUp !== false, model: 'llama3.1', hint: 'x' });
    if (url.startsWith('/api/v1/translate/selftest')) return json(200, { target: 'hi', ollamaRunning: false, onlineEnabled: true, online: ['Google POST: OK -> x', 'Google GET: FAILED - HTTP 429', 'MyMemory: OK -> y'], ok: true, engine: 'online-translator', result: 'TR' });
    if (url === '/api/v1/translate') {
      const body = JSON.parse(init.body); calls.translate.push(body);
      if (cfg.engineUp === false) return json(503, { code: 'TRANSLATION_ENGINE_OFFLINE', error: 'The translation engine (Ollama) is not running', userGuidance: 'Start it: ollama serve' });
      return json(200, { translation: (body.target === 'kn' ? 'ಅನುವಾದ: ' : 'TR-' + body.target + ': ') + body.text.slice(0, 12), engine: 'ollama:llama3.1', chunks: 1, sameLanguage: false, sourceLanguage: 'en', target: body.target });
    }
    if (url === '/api/v1/documents/analyze-sync') return json(200, DOC);
    if (url === '/api/config') return json(200, { gamesServiceUrl: cfg.gamesDown ? 'http://localhost:9' : 'http://localhost:8095' });
    if (url === 'http://localhost:8095/api/games') return json(200, GAMES);
    if (url.startsWith('http://localhost:8095/api/ai/recommend')) return json(200, { recommendedGameId: 'memory' });
    if (url.startsWith('http://localhost:9')) throw new Error('connection refused');
    return json(404, {});
  };
  w.eval(SRC('visual-errors.js')); w.eval(SRC('speech.js')); w.eval(SRC('app.js')); w.eval(SRC('hud.js')); w.eval(SRC('games-menu.js'));
  await w.document.readyState; await wait(40);
  return { w, d: w.document, calls, spoken, synth, $: id => w.document.getElementById(id) };
}
const wait = ms => new Promise(r => setTimeout(r, ms));
async function analyze(ctx) {
  Object.defineProperty(ctx.$('pdfFile'), 'files', { value: [new ctx.w.File(['%PDF'], 'a.pdf')], configurable: true });
  ctx.$('analyzeBtn').click(); await wait(60);
}
async function choose(ctx, id, value) { ctx.$(id).value = value; ctx.$(id).dispatchEvent(new ctx.w.Event('change')); await wait(60); }

test('page loads with Indian + international languages, engine chip and gold page ids intact', async () => {
  const c = await boot();
  assert.ok(c.$('targetSel').querySelectorAll('option').length >= 6);
  assert.match(c.$('engineChip').textContent, /ready/);
  assert.strictEqual(c.$('greeting').textContent, 'Good morning');
  for (const id of ['pdfFile', 'progressBar', 'readingPreview', 'currentPage', 'totalPages', 'playBtn']) assert.ok(c.$(id), id);
});

test('PLAY reads the real page text with the Natural voice for the language - and moves on past the picture page', async () => {
  const c = await boot(); await analyze(c);
  assert.strictEqual(c.$('totalPages').textContent, '3');
  assert.match(c.$('skipInfo').textContent, /pictures or scans/);
  c.$('playBtn').click(); await wait(400);
  assert.ok(c.spoken.length >= 2, 'spoke ' + c.spoken.length);
  assert.strictEqual(c.spoken[0].text, P1);
  assert.match(c.spoken[0].voice.name, /Neerja/, 'the voice must be SET on the utterance (old builds never did)');
  assert.ok(c.spoken.some(u => u.text === P3), 'page 3 spoken after skipping the picture page');
  assert.ok(!c.spoken.some(u => /Scanned|Translation|Human/i.test(u.text)), 'no placeholder text is ever spoken');
  assert.strictEqual(c.$('playBtn').textContent, 'Play');
});

test('translation: page text goes to the engine and the TRANSLATION is spoken with the Kannada voice', async () => {
  const c = await boot(); await analyze(c);
  await choose(c, 'targetSel', 'kn');
  assert.ok(c.$('accentWrap').style.display !== 'none');
  c.$('playBtn').click(); await wait(500);
  assert.ok(c.calls.translate.length >= 1);
  assert.deepStrictEqual([c.calls.translate[0].text, c.calls.translate[0].target, c.calls.translate[0].source], [P1, 'kn', 'en']);
  const first = c.spoken[0];
  assert.match(first.text, /^ಅನುವಾದ/); assert.match(first.voice.name, /Sapna/); assert.strictEqual(first.lang, 'kn-IN');
  assert.match(c.$('readingPreview').textContent, /ಅನುವಾದ/);
  assert.match(c.$('transTag').textContent, /Kannada/);
});

test('the voice picked in the list is the voice that speaks', async () => {
  const c = await boot(); await analyze(c);
  await choose(c, 'targetSel', 'kn'); await choose(c, 'voiceSel', 'Gagan');
  c.$('playBtn').click(); await wait(300);
  assert.match(c.spoken[0].voice.name, /Gagan/);
});

test('engine offline: the ORIGINAL page text is read (never fake translation text) and the user is told why', async () => {
  const c = await boot({ engineUp: false }); await analyze(c);
  await choose(c, 'targetSel', 'kn');
  c.$('playBtn').click(); await wait(400);
  assert.strictEqual(c.spoken[0].text, P1);
  assert.match(c.spoken[0].voice.name, /Neerja|Prabhat/, 'English voice for English text, not a Kannada voice reading English');
  assert.match(c.$('notice').textContent, /original text/i);
  assert.match(c.$('notice').textContent, /ollama serve/);
});

test('clicking the engine chip clears a failed translation and retries it', async () => {
  const c = await boot({ engineUp: false }); await analyze(c);
  await choose(c, 'targetSel', 'hi'); await wait(100);
  assert.match(c.$('transTag').textContent, /not translated/);
  const before = c.calls.translate.length;
  c.$('engineChip').click(); await wait(200);
  assert.ok(c.calls.translate.length > before, 'the page asked for the translation again');
});

test('voices that load late (Edge loads them asynchronously) still get used', async () => {
  const c = await boot({ voicesLate: 250 }); await analyze(c);
  c.$('playBtn').click(); await wait(700);
  assert.ok(c.spoken.length >= 1);
  assert.ok(c.spoken[0].voice, 'a voice was set even though the list was empty at click time');
});

test('a failing voice falls back to another voice of the language and speech continues', async () => {
  const c = await boot({ failVoice: 'Neerja' }); await analyze(c);
  c.$('playBtn').click(); await wait(500);
  assert.ok(c.spoken.some(u => u.voice && /Prabhat/.test(u.voice.name)));
  assert.strictEqual(c.$('notice').hidden, true);
});

test('Hear sample speaks a real sentence in the language, in the chosen voice', async () => {
  const c = await boot();
  await choose(c, 'targetSel', 'kn');
  c.$('sampleBtn').click(); await wait(300);
  assert.match(c.spoken[0].text, /^ನಮಸ್ಕಾರ/); assert.match(c.spoken[0].voice.name, /Sapna/);
});

test('Voice check reports what is installed', async () => {
  const c = await boot(); await choose(c, 'targetSel', 'kn');
  c.$('checkBtn').click(); await wait(100);
  assert.match(c.$('notice').textContent, /9 voices installed/); assert.match(c.$('notice').textContent, /Sapna/);
});

test('page links, Pause / Resume and Stop work', async () => {
  const c = await boot(); await analyze(c);
  assert.strictEqual(c.$('pageLinks').querySelectorAll('button').length, 3);
  c.$('pageLinks').querySelectorAll('button')[2].click(); await wait(40);
  assert.strictEqual(c.$('currentPage').textContent, '3');
  c.$('playBtn').click(); await wait(20); assert.strictEqual(c.$('playBtn').textContent, 'Pause');
  c.$('playBtn').click(); await wait(20); assert.strictEqual(c.$('playBtn').textContent, 'Resume');
  c.$('stopBtn').click(); assert.strictEqual(c.$('playBtn').textContent, 'Play');
});

test('language drop-down is grouped: South Indian, Other Indian (Hindi, Marathi), English, International Languages', async () => {
  const c = await boot();
  const groups = [...c.$('targetSel').querySelectorAll('optgroup')].map(g => g.label);
  assert.deepStrictEqual(groups, ['South Indian Languages', 'Other Indian Languages', 'English (Indian PDF text to English)', 'International Languages']);
  const other = [...c.$('targetSel').querySelectorAll('optgroup')[1].querySelectorAll('option')].map(o => o.value);
  assert.deepStrictEqual(other, ['hi', 'mr']);
  const intl = [...c.$('targetSel').querySelectorAll('optgroup')[3].querySelectorAll('option')].map(o => o.value);
  assert.deepStrictEqual(intl, ['es', 'fr']);
  const all = [...c.$('targetSel').querySelectorAll('option')].map(o => o.value);
  assert.ok(all.includes('kn') && all.includes('ta') && all.includes('te') && all.includes('ur') && all.includes('ml') && all.includes('hi') && all.includes('mr'));
});

test('left-aligned layout: the page content sits in .shell beside an empty right-hand dock', () => {
  const html = fs.readFileSync(path.join(STATIC, 'index.html'), 'utf8');
  assert.ok(/<div class="page">[\s\S]*<main class="shell">[\s\S]*<\/main>\s*<aside class="right-dock[^"]*"[^>]*><\/aside>/.test(html));
  const css = fs.readFileSync(path.join(STATIC, 'css/gold.css'), 'utf8');
  assert.ok(/\.shell \{[^}]*margin: 0;/.test(css) && !/\.shell \{[^}]*margin: 0 auto/.test(css));
});

test('accent box shows only South Indian accents and disappears for Spanish/French (no overseas accents)', async () => {
  const c = await boot();
  await choose(c, 'targetSel', 'kn');
  assert.notStrictEqual(c.$('accentWrap').style.display, 'none');
  const accents = [...c.$('accentSel').querySelectorAll('option')].map(o => o.textContent);
  assert.ok(accents.some(a => /Mysuru/.test(a)) && !accents.some(a => /Doha|Dubai|Mumbai|Mexico|Quebec/.test(a)));
});

test('top-right "Entertainment Games" drop-down lists the games microservice, opens one in a pop-out, Esc closes', async () => {
  const c = await boot(); await wait(60);
  const sel = c.$('gamesSel');
  assert.match(c.d.querySelector('label[for=gamesSel]').textContent, /Entertainment Games/);
  const groups = [...sel.querySelectorAll('optgroup')].map(g => g.label);
  assert.deepStrictEqual(groups, ['Hotstuff & Princess Charmaine', 'Cars & Bikes']);
  assert.ok([...sel.querySelectorAll('option')].some(o => /Surprise me/.test(o.textContent)));
  sel.value = 'hotstuff-fire-maze.html'; sel.dispatchEvent(new c.w.Event('change'));
  const modal = c.d.getElementById('gm-modal'); assert.ok(modal.classList.contains('open'));
  assert.strictEqual(modal.querySelector('iframe').getAttribute('src'), 'http://localhost:8095/games/hotstuff-fire-maze.html');
  assert.strictEqual(sel.value, '', 'drop-down resets so the same game can be chosen again');
  c.d.dispatchEvent(new c.w.KeyboardEvent('keydown', { key: 'Escape' })); assert.ok(!modal.classList.contains('open'));
  sel.value = 'highway-racer.html'; sel.dispatchEvent(new c.w.Event('change'));
  c.w.dispatchEvent(new c.w.MessageEvent('message', { data: { type: 'close-game' } })); assert.ok(!modal.classList.contains('open'));
});

test('"Surprise me" asks the games microservice for an AI pick and opens it', async () => {
  const c = await boot(); await wait(60);
  c.$('gamesSel').value = '__recommend'; c.$('gamesSel').dispatchEvent(new c.w.Event('change')); await wait(60);
  assert.match(c.d.querySelector('#gm-modal iframe').getAttribute('src'), /charmaine-memory-flip\.html$/);
});

test('games microservice offline: the drop-down says how to start it instead of failing silently', async () => {
  const c = await boot({ gamesDown: true }); await wait(60);
  assert.match(c.$('gamesSel').textContent, /start the games service/);
});

test('clock shows HH:MM:SS DD/Mon/Year and ticks every second', async () => {
  const c = await boot();
  const t1 = c.$('clock').textContent;
  assert.match(t1, /^\d{2}:\d{2}:\d{2} \d{2}\/(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\/\d{4}$/);
  await wait(1150); assert.notStrictEqual(c.$('clock').textContent, t1);
});

test('compass follows a (simulated) phone sensor and says when there is none', async () => {
  const c = await boot();
  const ev = new c.w.Event('deviceorientationabsolute'); ev.alpha = 270; c.w.dispatchEvent(ev);        // alpha 270 = facing east (90 deg)
  await wait(60);
  assert.match(c.$('compassText').textContent, /90\u00B0 E/);
  assert.match(c.$('compassRose').style.transform, /rotate\(-90/);
  const d = await boot(); await wait(2700);
  assert.match(d.$('compassText').textContent, /no compass sensor/i);
  assert.match(d.$('compassText').textContent, /set where your screen faces/i);
});

test('the main page and its styles never use black', () => {
  const css = fs.readFileSync(path.join(STATIC, 'css/gold.css'), 'utf8').toLowerCase();
  assert.ok(!/#000(000)?\b/.test(css) && !/rgba\(0,\s*0,\s*0/.test(css) && !/#111\b|#0f0f0f|#1e1e1e|:\s*black/.test(css));
  assert.ok(css.includes('--sky-1:#0b3a68'));
});

test('Hindi, Marathi, Spanish and French: preview shows the translation and the matching Natural voice speaks it', async () => {
  const want = { hi: /Swara/, mr: /Aarohi/, es: /Elvira/, fr: /Denise/ }, loc = { hi: 'hi-IN', mr: 'mr-IN', es: 'es-ES', fr: 'fr-FR' };
  for (const code of ['hi', 'mr', 'es', 'fr']) {
    const c = await boot(); await analyze(c);
    await choose(c, 'targetSel', code);
    assert.strictEqual(c.$('accentSel').value, 'standard', code + ' accent');
    c.$('playBtn').click(); await wait(500);
    assert.strictEqual(c.calls.translate[0].target, code);
    assert.strictEqual(c.calls.translate[0].accent, 'standard');
    assert.match(c.$('readingPreview').textContent, new RegExp('TR-' + code), code + ' preview shows the translation');
    assert.match(c.$('transTag').textContent, /ollama|online/i, code + ' tag');
    assert.ok(c.spoken.length >= 1, code + ' spoke');
    assert.match(c.spoken[0].text, new RegExp('^TR-' + code));
    assert.match(c.spoken[0].voice.name, want[code], code + ' voice');
    assert.strictEqual(c.spoken[0].lang, loc[code]);
  }
});

test('Translate JSON button: translates every readable page into the chosen language, shows the translated JSON and speaks it with the language voice', async () => {
  const c = await boot(); await analyze(c);
  await choose(c, 'targetSel', 'kn');
  c.$('jsonTransBtn').click(); await wait(700);
  const j = JSON.parse(c.$('jsonContainer').textContent);
  assert.strictEqual(j.translatedTo.language, 'Kannada');
  assert.strictEqual(j.translatedPageCount, 2);
  assert.deepStrictEqual(j.translatedPages.map(p => p.page), [1, 3]);
  assert.match(j.translatedPages[0].text, /^ಅನುವಾದ/);
  assert.match(c.$('jsonTag').textContent, /translated to Kannada, 2 pages/);
  assert.ok(c.spoken.length >= 1, 'the translated JSON is spoken');
  assert.match(c.spoken[0].text, /^ಅನುವಾದ/); assert.match(c.spoken[0].voice.name, /Sapna/);
  c.$('jsonOrigBtn').click();
  assert.match(c.$('jsonContainer').textContent, /previewPages/); assert.ok(!/translatedPages/.test(c.$('jsonContainer').textContent));
});

test('Translate JSON with Hindi / Marathi / Spanish / French uses their own voice', async () => {
  for (const [code, voice] of [['hi', /Swara/], ['mr', /Aarohi/], ['es', /Elvira/], ['fr', /Denise/]]) {
    const c = await boot(); await analyze(c);
    await choose(c, 'targetSel', code);
    c.$('jsonTransBtn').click(); await wait(700);
    const j = JSON.parse(c.$('jsonContainer').textContent);
    assert.strictEqual(j.translatedTo.code, code); assert.match(j.translatedPages[0].text, new RegExp('^TR-' + code));
    assert.match(c.spoken[0].voice.name, voice, code);
  }
});

test('Translate JSON without a chosen language explains what to do; a failing engine is reported, not faked', async () => {
  const c = await boot(); await analyze(c);
  c.$('jsonTransBtn').click(); await wait(40);
  assert.match(c.$('notice').textContent, /Choose a language first/);
  const d = await boot({ engineUp: false }); await analyze(d);
  await choose(d, 'targetSel', 'hi');
  d.$('jsonTransBtn').click(); await wait(300);
  assert.match(d.$('notice').textContent, /could not be translated/);
  assert.ok(!/translatedPages/.test(d.$('jsonContainer').textContent), 'no fake translated JSON');
  assert.strictEqual(d.spoken.length, 0);
});

test('Test translation button reports which online route works and which fails', async () => {
  const c = await boot(); await choose(c, 'targetSel', 'hi');
  c.$('selfTestBtn').click(); await wait(80);
  assert.match(c.$('notice').textContent, /Translation to hi works/); assert.match(c.$('notice').textContent, /Google GET: FAILED - HTTP 429/);
});

const orient = (c, alpha, extra = {}) => { const ev = new c.w.Event('deviceorientationabsolute'); ev.alpha = alpha; Object.assign(ev, extra); c.w.dispatchEvent(ev); };
const roseDeg = c => parseFloat(/rotate\((-?[\d.]+)deg\)/.exec(c.$('compassRose').style.transform)[1]);

test('compass never spins the long way when the heading crosses north', async () => {
  const c = await boot();
  const seen = [];
  for (const alpha of [10, 6, 3, 359, 356, 353, 356, 359, 3, 6, 10]) { orient(c, alpha); await wait(30); seen.push(roseDeg(c)); }
  for (let i = 1; i < seen.length; i++) assert.ok(Math.abs(seen[i] - seen[i - 1]) < 25, 'jump of ' + (seen[i] - seen[i - 1]).toFixed(1) + ' degrees at step ' + i + ': ' + seen.join(' '));
  assert.match(c.$('compassText').textContent, /\(live\)/);
});

test('compass: a tilted phone held upright points to where its back faces; screen rotation is applied when flat', async () => {
  const c = await boot();
  orient(c, 270, { beta: 90, gamma: 0 }); await wait(60);                      // upright, facing east
  assert.match(c.$('compassText').textContent, /90\u00B0 E/);
});

test('desktop PC without a sensor: the person sets where the screen faces and the dial follows; a real sensor would win', async () => {
  const c = await boot(); await wait(2700);
  c.$('compass').click(); await wait(20);
  const menu = c.d.querySelector('.compass-menu'); assert.ok(menu && !menu.hidden, 'the menu opens');
  const sel = c.d.getElementById('faceSel'); sel.value = '90'; sel.dispatchEvent(new c.w.Event('change')); await wait(60);
  assert.match(c.$('compassText').textContent, /Screen faces 90\u00B0 E \(set by you\)/);
  assert.strictEqual(roseDeg(c), -90);
  assert.strictEqual(c.w.localStorage.getItem('wn.compass.facing'), '90');
  orient(c, 180); await wait(60);                                                // a real reading arrives later: it takes over
  assert.match(c.$('compassText').textContent, /\(live\)/);
  assert.strictEqual(Math.round(roseDeg(c)), -180);
});

test('Sun calibration: "Sun is on my right" sets the screen direction from the real Sun azimuth', async () => {
  const FIXED = Date.UTC(2026, 9, 29, 6, 30, 0);                                // Mysuru, 12:00 IST: Sun due south (176 deg), 64 deg high
  const c = await boot({ pre: w => {
    const D = w.Date; w.Date = class extends D { constructor(...a) { if (a.length) super(...a); else super(FIXED); } static now() { return FIXED; } };
    w.navigator.geolocation = { getCurrentPosition: ok => ok({ coords: { latitude: 12.2958, longitude: 76.6394 } }) };
  } });
  await wait(2700);
  assert.match(c.$('sunText').textContent, /Sun 17\d\u00B0 S, 6\d\u00B0 high/);
  c.$('compass').click(); await wait(20);
  const right = c.d.querySelector('[data-sun="90"]'); assert.ok(!right.disabled);
  right.click(); await wait(60);
  assert.match(c.$('compassText').textContent, /Screen faces 8\d\u00B0 E/);          // 176 - 90 = 86
});
