/* EdgeSpeech - the one speech engine for the whole app (Microsoft Edge "Natural" voices through the Web Speech API).
 *
 * Why the earlier builds were silent:
 *   1. the chosen voice was never put on the utterance (only utter.lang), so Edge used its default voice;
 *   2. getVoices() was read once, before Edge had finished loading its (asynchronous) voice list;
 *   3. text was cut to 600-800 characters, and long single utterances are dropped by online voices;
 *   4. there were no error / start-watchdog handlers, so a failed voice stayed silent with no explanation;
 *   5. the "translation" that got spoken was a placeholder label, not the page text.
 * Every one of those is handled here and covered by js-tests/speech.test.js.
 */
(function (root) {
  'use strict';

  function normLang(l) { return String(l || '').replace('_', '-').toLowerCase(); }
  function primary(l) { return normLang(l).split('-')[0]; }

  /* ---------------------------------------------------------------- text -> speakable chunks */
  var SENTENCE_END = /([.!?\u0964\u0965\u06D4\u061F\u2026]+["')\]]*)\s+/g;

  function chunkText(text, max) {
    max = max || 220;
    var clean = String(text == null ? '' : text).replace(/[\u0000-\u0008\u000B\u000C\u000E-\u001F]/g, ' ').replace(/[ \t]+/g, ' ').trim();
    if (!clean) return [];
    var sentences = clean.replace(/\r/g, '').replace(/\n+/g, '\u0001').replace(SENTENCE_END, '$1\u0001').split('\u0001');
    var out = [], cur = '', s, i;
    function push(p) { p = p.trim(); if (p) out.push(p); }
    for (i = 0; i < sentences.length; i++) {
      s = sentences[i].trim();
      if (!s) continue;
      if (s.length > max) {                                    // a very long sentence: split at commas, then spaces, then hard
        if (cur) { push(cur); cur = ''; }
        splitLong(s, max).forEach(push);
        continue;
      }
      if (cur && (cur.length + 1 + s.length) > max) { push(cur); cur = s; }
      else cur = cur ? cur + ' ' + s : s;
    }
    if (cur) push(cur);
    return out;
  }

  function splitLong(s, max) {
    var parts = [], rest = s, cut;
    while (rest.length > max) {
      var lim = max - 1;                                          // index of the last character that still fits
      cut = Math.max(rest.lastIndexOf(',', lim), rest.lastIndexOf(';', lim), rest.lastIndexOf('\u060C', lim));
      if (cut < max * 0.4) cut = rest.lastIndexOf(' ', lim);
      if (cut < max * 0.3) cut = lim;                          // scripts without spaces: hard cut
      parts.push(rest.slice(0, cut + 1));
      rest = rest.slice(cut + 1);
    }
    if (rest.trim()) parts.push(rest);
    return parts;
  }

  /* ---------------------------------------------------------------- voice selection */
  /* opts: { locales: ['kn-IN'], names: ['Sapna','Gagan'], preferNatural: true } -> ranked candidates (best first) */
  function rankVoices(voices, opts) {
    opts = opts || {};
    var locales = (opts.locales || []).map(normLang);
    var names = (opts.names || []).map(function (n) { return String(n).toLowerCase(); });
    var prim = locales.length ? primary(locales[0]) : '';
    var ranked = [];
    (voices || []).forEach(function (v) {
      var l = normLang(v.lang), score = 100, li = locales.indexOf(l), why = [];
      if (li >= 0) { score = li * 2; why.push('locale'); }
      else if (prim && primary(l) === prim) { score = locales.length * 2 + 4; why.push('language'); }
      else return;                                               // never pick a voice of another language
      var lname = String(v.name || '').toLowerCase();
      for (var i = 0; i < names.length; i++) {
        if (names[i] && lname.indexOf(names[i]) >= 0) { score -= 10 - Math.min(i, 5); why.push('name'); break; }
      }
      if (opts.preferNatural !== false && /natural|online/i.test(v.name || '')) { score -= 3; why.push('natural'); }
      ranked.push({ voice: v, score: score, why: why });
    });
    ranked.sort(function (a, b) { return a.score - b.score || String(a.voice.name).localeCompare(String(b.voice.name)); });
    return ranked;
  }

  function pickVoice(voices, opts) {
    var r = rankVoices(voices, opts);
    if (!r.length) return { voice: null, exact: false, candidates: [] };
    return { voice: r[0].voice, exact: r[0].why.indexOf('name') >= 0 && r[0].why.indexOf('locale') >= 0, candidates: r.map(function (x) { return x.voice; }) };
  }

  /* ---------------------------------------------------------------- friendly errors */
  var ERRORS = {
    'not-allowed': 'The browser blocks sound until you click a button on the page. Click Play again.',
    'network': 'Natural voices are online voices and need internet. Check the connection, or pick an offline voice.',
    'synthesis-failed': 'The voice engine failed to produce sound.',
    'synthesis-unavailable': 'The voice engine is not available in this browser.',
    'audio-busy': 'The sound device is busy. Close other apps that are playing audio.',
    'audio-hardware': 'No sound output device was found.',
    'language-unavailable': 'No voice for this language is installed.',
    'voice-unavailable': 'The selected voice is not available.',
    'no-start': 'The voice never started speaking (online voices need internet and a moment to load).',
    'no-voice': 'No voice for this language is installed. Open the app in Microsoft Edge to get the free Natural voices.',
    'unsupported': 'This browser cannot speak. Open the app in Microsoft Edge.'
  };
  function friendlyError(code) { return ERRORS[code] || ('The voice reported: ' + code); }

  /* ---------------------------------------------------------------- the speaker */
  function create(env) {
    env = env || {};
    var synth = env.synth || (typeof window !== 'undefined' ? window.speechSynthesis : null);
    var Utter = env.Utterance || (typeof SpeechSynthesisUtterance !== 'undefined' ? SpeechSynthesisUtterance : null);
    var setT = env.setTimeout || setTimeout, clearT = env.clearTimeout || clearTimeout;
    var startTimeoutMs = env.startTimeoutMs || 6000, cancelGapMs = env.cancelGapMs == null ? 60 : env.cancelGapMs, maxChunk = env.maxChunk || 220;
    var token = 0, active = null;

    function supported() { return !!(synth && Utter); }

    function voices() { return supported() ? (synth.getVoices() || []) : []; }

    /* Resolves with the voice list as soon as Edge has loaded it (or after timeoutMs with whatever there is). */
    function ready(timeoutMs) {
      timeoutMs = timeoutMs == null ? 3000 : timeoutMs;
      return new Promise(function (resolve) {
        if (!supported()) { resolve([]); return; }
        if (voices().length) { resolve(voices()); return; }
        var done = false, poll, limit;
        function finish() { if (done) return; done = true; clearT(poll); clearT(limit); resolve(voices()); }
        function tick() { if (voices().length) finish(); else poll = setT(tick, 150); }
        if (synth.addEventListener) synth.addEventListener('voiceschanged', function h() { if (voices().length) finish(); });
        else synth.onvoiceschanged = function () { if (voices().length) finish(); };
        poll = setT(tick, 150);
        limit = setT(finish, timeoutMs);
      });
    }

    function stop() {
      token++;
      if (active && active.watchdog) clearT(active.watchdog);
      active = null;
      if (supported()) synth.cancel();
    }

    /* opts: { voice, fallbacks:[voice...], lang, rate, pitch, volume, onChunk(i,n,text), onEnd(), onError({code,message}) } */
    function speak(text, opts) {
      opts = opts || {};
      if (!supported()) { if (opts.onError) opts.onError({ code: 'unsupported', message: friendlyError('unsupported') }); return { cancel: stop }; }
      var chunks = chunkText(text, maxChunk);
      stop();
      var my = token;
      if (!chunks.length) { if (opts.onEnd) setT(function () { if (my === token) opts.onEnd(); }, 0); return { cancel: stop }; }
      var queue = [opts.voice].concat(opts.fallbacks || []).filter(Boolean), vIdx = 0, tried = 0;

      function failed(code) {
        if (my !== token) return;
        if (active && active.watchdog) clearT(active.watchdog);
        if (vIdx + 1 < queue.length && tried < 3) { vIdx++; tried++; run(active ? active.i : 0, true); return; }
        stop();
        if (opts.onError) opts.onError({ code: code, message: friendlyError(code) });
      }

      function run(i, isRetry) {
        if (my !== token) return;
        if (i >= chunks.length) { if (opts.onEnd) opts.onEnd(); return; }
        var u = new Utter(chunks[i]);
        var v = queue[vIdx];
        if (v) { u.voice = v; u.lang = v.lang; } else if (opts.lang) { u.lang = opts.lang; }
        u.rate = opts.rate || 1; u.pitch = opts.pitch || 1; u.volume = opts.volume == null ? 1 : opts.volume;
        var started = false;
        active = { i: i, u: u, watchdog: null };
        active.watchdog = setT(function () { if (!started) failed('no-start'); }, startTimeoutMs);
        u.onstart = function () { if (my !== token) return; started = true; clearT(active.watchdog); if (opts.onChunk) opts.onChunk(i, chunks.length, chunks[i]); };
        u.onend = function () { if (my !== token) return; clearT(active.watchdog); run(i + 1, false); };
        u.onerror = function (e) {
          if (my !== token) return;
          var code = e && e.error ? e.error : 'synthesis-failed';
          if (code === 'canceled' || code === 'interrupted') return;     // our own stop() - not an error
          failed(code);
        };
        synth.speak(u);
      }

      var startFn = function () { if (my === token) run(0, false); };
      if (cancelGapMs > 0) setT(startFn, cancelGapMs); else startFn();   // Edge drops an utterance spoken right after cancel()
      return { cancel: stop };
    }

    function describe(locales) {
      var all = voices(), want = (locales || []).map(normLang), prim = want.length ? primary(want[0]) : '';
      var same = all.filter(function (v) { return prim && primary(v.lang) === prim; });
      return {
        supported: supported(), total: all.length,
        natural: all.filter(function (v) { return /natural/i.test(v.name); }).length,
        sameLanguage: same.map(function (v) { return v.name + ' (' + v.lang + ')'; })
      };
    }

    return { supported: supported, voices: voices, ready: ready, speak: speak, stop: stop, describe: describe,
             pause: function () { if (supported()) synth.pause(); }, resume: function () { if (supported()) synth.resume(); } };
  }

  var api = { chunkText: chunkText, rankVoices: rankVoices, pickVoice: pickVoice, friendlyError: friendlyError, create: create, normLang: normLang };
  if (typeof module !== 'undefined' && module.exports) module.exports = api; else root.EdgeSpeech = api;
})(typeof window !== 'undefined' ? window : this);
