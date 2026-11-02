// Run:  node --test js-tests/speech.test.js   (Node 20+; no npm install needed)
const test = require('node:test');
const assert = require('node:assert');
const ES = require('../src/main/resources/static/js/speech.js');

const V = (name, lang, local = false) => ({ name, lang, localService: local });
const EDGE = [
  V('Microsoft Sapna Online (Natural) - Kannada (India)', 'kn-IN'),
  V('Microsoft Gagan Online (Natural) - Kannada (India)', 'kn-IN'),
  V('Microsoft Shruti Online (Natural) - Telugu (India)', 'te-IN'),
  V('Microsoft Gul Online (Natural) - Urdu (India)', 'ur-IN'),
  V('Microsoft Uzma Online (Natural) - Urdu (Pakistan)', 'ur-PK'),
  V('Microsoft Heera - English (India)', 'en-IN', true),
  V('Microsoft Elvira Online (Natural) - Spanish (Spain)', 'es-ES'),
  V('Microsoft Dalia Online (Natural) - Spanish (Mexico)', 'es-MX'),
];

/* A fake speechSynthesis that behaves like Edge: async voices, async events, scripted failures. */
function fakeSynth(opts = {}) {
  const s = {
    spoken: [], cancelCalls: 0, listeners: {}, _voices: opts.voicesNow ? EDGE : [],
    getVoices() { return this._voices; },
    addEventListener(ev, fn) { (this.listeners[ev] = this.listeners[ev] || []).push(fn); },
    loadVoices() { this._voices = EDGE; (this.listeners.voiceschanged || []).forEach(f => f()); },
    cancel() { this.cancelCalls++; this.pending && (this.pending.cancelled = true); },
    pause() {}, resume() {},
    speak(u) {
      this.spoken.push(u);
      const mode = opts.behaviour ? opts.behaviour(u, this.spoken.length) : 'ok';
      const state = { cancelled: false }; this.pending = state;
      setTimeout(() => {
        if (state.cancelled) { u.onerror && u.onerror({ error: 'canceled' }); return; }
        if (mode === 'silent') return;                                    // never starts
        if (mode && mode.error) { u.onerror && u.onerror({ error: mode.error }); return; }
        u.onstart && u.onstart();
        setTimeout(() => { if (!state.cancelled) u.onend && u.onend(); }, opts.endDelay || 5);
      }, 3);
    },
  };
  return s;
}
class Utter { constructor(text) { this.text = text; } }
const mk = (synth, extra = {}) => ES.create({ synth, Utterance: Utter, startTimeoutMs: 80, cancelGapMs: 5, ...extra });
const wait = (ms) => new Promise(r => setTimeout(r, ms));

/* ------------------------------------------------------------------ chunking */
test('chunkText keeps ALL text (old build cut at 600/800 chars) and never exceeds the limit', () => {
  const long = Array.from({ length: 60 }, (_, i) => `This is sentence number ${i + 1}, it has a comma, and some words.`).join(' ');
  const chunks = ES.chunkText(long, 220);
  assert.ok(chunks.length > 10);
  assert.ok(chunks.every(c => c.length <= 220), 'chunk too long');
  assert.strictEqual(chunks.join(' ').replace(/\s+/g, ' '), long.replace(/\s+/g, ' '));
});

test('chunkText splits Indic and Urdu sentence marks and spaceless runs', () => {
  assert.deepStrictEqual(ES.chunkText('ಇದು ಮೊದಲ ವಾಕ್ಯ। ಇದು ಎರಡನೇ ವಾಕ್ಯ।', 12).length >= 2, true);
  assert.deepStrictEqual(ES.chunkText('یہ پہلا جملہ ہے۔ یہ دوسرا جملہ ہے۔', 20).length >= 2, true);
  const spaceless = 'ಅ'.repeat(500);
  const parts = ES.chunkText(spaceless, 200);
  assert.ok(parts.every(p => p.length <= 200) && parts.join('') === spaceless);
});

test('chunkText of blank/null gives nothing to speak (never a placeholder like "[Scanned]")', () => {
  assert.deepStrictEqual(ES.chunkText('   \n  '), []);
  assert.deepStrictEqual(ES.chunkText(null), []);
});

/* ------------------------------------------------------------------ voice choice */
test('pickVoice honours the chosen name, prefers Natural, and matches the locale', () => {
  const p = ES.pickVoice(EDGE, { locales: ['kn-IN'], names: ['Gagan'] });
  assert.match(p.voice.name, /Gagan/); assert.strictEqual(p.exact, true);
  const q = ES.pickVoice(EDGE, { locales: ['kn-IN'], names: [] });
  assert.match(q.voice.name, /Natural/);
});

test('pickVoice never returns a voice of another language', () => {
  assert.strictEqual(ES.pickVoice(EDGE, { locales: ['ta-IN'], names: ['Pallavi'] }).voice, null);
  assert.strictEqual(ES.pickVoice([], { locales: ['kn-IN'] }).voice, null);
});

test('Urdu falls back between India and Pakistan voices; Spanish accent picks the right country', () => {
  assert.match(ES.pickVoice(EDGE, { locales: ['ur-IN', 'ur-PK'], names: [] }).voice.name, /Gul/);
  assert.match(ES.pickVoice([EDGE[4]], { locales: ['ur-IN', 'ur-PK'], names: [] }).voice.name, /Uzma/);
  assert.match(ES.pickVoice(EDGE, { locales: ['es-MX', 'es-ES'], names: [] }).voice.name, /Dalia/);
});

test('underscore locales from other platforms (kn_IN) still match', () => {
  assert.ok(ES.pickVoice([V('Some Kannada voice', 'kn_IN')], { locales: ['kn-IN'] }).voice);
});

/* ------------------------------------------------------------------ voices load late */
test('ready() waits for Edge to finish loading voices (old build read the list once, empty)', async () => {
  const synth = fakeSynth(); const sp = mk(synth);
  setTimeout(() => synth.loadVoices(), 60);
  const list = await sp.ready(1000);
  assert.strictEqual(list.length, EDGE.length);
});

test('ready() gives up cleanly when no voices ever appear', async () => {
  const sp = mk(fakeSynth()); const t0 = Date.now();
  const list = await sp.ready(250);
  assert.deepStrictEqual(list, []); assert.ok(Date.now() - t0 >= 240);
});

/* ------------------------------------------------------------------ speaking */
test('speak() puts the CHOSEN VOICE on every utterance and reads the whole text in order', async () => {
  const synth = fakeSynth({ voicesNow: true }); const sp = mk(synth);
  const text = Array.from({ length: 30 }, (_, i) => `Sentence ${i + 1} of the page.`).join(' ');   // ~900 chars
  let ended = 0; const seen = [];
  sp.speak(text, { voice: EDGE[0], rate: 0.9, pitch: 1.1, onChunk: (i, n, t) => seen.push(t), onEnd: () => ended++ });
  await wait(600);
  assert.strictEqual(ended, 1);
  assert.ok(synth.spoken.length >= 4);
  assert.ok(synth.spoken.every(u => u.voice === EDGE[0] && u.lang === 'kn-IN' && u.rate === 0.9));
  assert.strictEqual(synth.spoken.map(u => u.text).join(' '), text);
  assert.strictEqual(seen.length, synth.spoken.length);
});

test('stop() mid-way ends the chain: no further chunks, no onEnd, no error', async () => {
  const synth = fakeSynth({ voicesNow: true, endDelay: 40 }); const sp = mk(synth);
  let ended = 0, errors = 0;
  const text = Array.from({ length: 40 }, (_, i) => `Sentence ${i + 1} is here.`).join(' ');
  sp.speak(text, { voice: EDGE[0], onEnd: () => ended++, onError: () => errors++ });
  await wait(70); sp.stop(); const n = synth.spoken.length;
  await wait(300);
  assert.strictEqual(synth.spoken.length, n); assert.strictEqual(ended, 0); assert.strictEqual(errors, 0);
});

test('a new speak() cancels the old one first and waits a moment (Edge drops instant speak after cancel)', async () => {
  const synth = fakeSynth({ voicesNow: true }); const sp = mk(synth);
  sp.speak('First one.', { voice: EDGE[0] });
  const before = synth.cancelCalls;
  sp.speak('Second one.', { voice: EDGE[0] });
  assert.ok(synth.cancelCalls > before);
  await wait(100);
  assert.ok(synth.spoken.some(u => u.text === 'Second one.'));
});

test('a voice that errors is replaced by the next fallback voice, and speech still completes', async () => {
  const synth = fakeSynth({ voicesNow: true, behaviour: (u) => (u.voice === EDGE[0] ? { error: 'synthesis-failed' } : 'ok') });
  const sp = mk(synth); let ended = 0, err = null;
  sp.speak('Hello there. Second sentence.', { voice: EDGE[0], fallbacks: [EDGE[1]], onEnd: () => ended++, onError: e => (err = e) });
  await wait(300);
  assert.strictEqual(err, null); assert.strictEqual(ended, 1);
  assert.ok(synth.spoken.some(u => u.voice === EDGE[1]));
});

test('a voice that never starts is caught by the watchdog and falls back', async () => {
  const synth = fakeSynth({ voicesNow: true, behaviour: (u) => (u.voice === EDGE[0] ? 'silent' : 'ok') });
  const sp = mk(synth); let ended = 0;
  sp.speak('Only one sentence.', { voice: EDGE[0], fallbacks: [EDGE[1]], onEnd: () => ended++ });
  await wait(400);
  assert.strictEqual(ended, 1);
});

test('when every voice fails the user gets ONE clear message (never silence)', async () => {
  const synth = fakeSynth({ voicesNow: true, behaviour: () => ({ error: 'network' }) });
  const sp = mk(synth); const errs = [];
  sp.speak('Anything at all.', { voice: EDGE[0], fallbacks: [EDGE[1]], onError: e => errs.push(e) });
  await wait(400);
  assert.strictEqual(errs.length, 1);
  assert.strictEqual(errs[0].code, 'network');
  assert.match(errs[0].message, /internet/i);
});

test('"canceled"/"interrupted" caused by our own stop() is not reported as an error', async () => {
  const synth = fakeSynth({ voicesNow: true }); const sp = mk(synth); let errs = 0;
  sp.speak('A long enough sentence to be speaking.', { voice: EDGE[0], onError: () => errs++ });
  await wait(20); sp.stop(); await wait(100);
  assert.strictEqual(errs, 0);
});

test('without speechSynthesis the app says so instead of failing silently', () => {
  const sp = ES.create({ synth: null, Utterance: null }); let err = null;
  sp.speak('hi', { onError: e => (err = e) });
  assert.strictEqual(err.code, 'unsupported'); assert.strictEqual(sp.supported(), false);
});

test('empty text ends immediately without calling the engine', async () => {
  const synth = fakeSynth({ voicesNow: true }); const sp = mk(synth); let ended = 0;
  sp.speak('   ', { voice: EDGE[0], onEnd: () => ended++ });
  await wait(50);
  assert.strictEqual(ended, 1); assert.strictEqual(synth.spoken.length, 0);
});

test('describe() reports what is installed so a missing voice can be explained', async () => {
  const synth = fakeSynth({ voicesNow: true }); const sp = mk(synth);
  const d = sp.describe(['kn-IN']);
  assert.strictEqual(d.total, EDGE.length); assert.strictEqual(d.sameLanguage.length, 2);
  assert.ok(d.natural >= 6);
});
