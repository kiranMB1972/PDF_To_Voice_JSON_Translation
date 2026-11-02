/* PDF Voice And Games - upload, preview, translate, read aloud with Microsoft Edge Natural voices.
   Speech goes through EdgeSpeech (js/speech.js); everything spoken is the REAL page text or its real translation. */
(function () {
  'use strict';
  var $ = function (id) { return document.getElementById(id); };
  var ES = window.EdgeSpeech;
  var speaker = ES.create();

  var SAMPLE = {
    kn: '\u0CA8\u0CAE\u0CB8\u0CCD\u0C95\u0CBE\u0CB0. \u0C87\u0CA6\u0CC1 \u0CA7\u0CCD\u0CB5\u0CA8\u0CBF\u0CAF \u0CAA\u0CB0\u0CC0\u0C95\u0CCD\u0CB7\u0CC6.',
    ta: '\u0BB5\u0BA3\u0B95\u0BCD\u0B95\u0BAE\u0BCD. \u0B87\u0BA4\u0BC1 \u0B95\u0BC1\u0BB0\u0BB2\u0BCD \u0B9A\u0BCB\u0BA4\u0BA9\u0BC8.',
    te: '\u0C28\u0C2E\u0C38\u0C4D\u0C15\u0C3E\u0C30\u0C02. \u0C07\u0C26\u0C3F \u0C35\u0C3E\u0C2F\u0C3F\u0C38\u0C4D \u0C2A\u0C30\u0C40\u0C15\u0C4D\u0C37.',
    ml: '\u0D28\u0D2E\u0D38\u0D4D\u0D15\u0D3E\u0D30\u0D02. \u0D07\u0D24\u0D4D \u0D36\u0D2C\u0D4D\u0D26 \u0D2A\u0D30\u0D3F\u0D36\u0D4B\u0D27\u0D28\u0D2F\u0D3E\u0D23\u0D4D.',
    ur: '\u0622\u062F\u0627\u0628\u06D4 \u06CC\u06C1 \u0622\u0648\u0627\u0632 \u06A9\u06CC \u062C\u0627\u0646\u0686 \u06C1\u06D2\u06D4',
    hi: '\u0928\u092E\u0938\u094D\u0924\u0947. \u092F\u0939 \u0906\u0935\u093E\u091C\u093C \u0915\u0940 \u091C\u093E\u0901\u091A \u0939\u0948\u0964',
    mr: '\u0928\u092E\u0938\u094D\u0915\u093E\u0930. \u0939\u0940 \u0906\u0935\u093E\u091C\u093E\u091A\u0940 \u091A\u093E\u091A\u0923\u0940 \u0906\u0939\u0947.',
    es: 'Hola. Esta es una prueba de voz.', fr: 'Bonjour. Ceci est un test de voix.', en: 'Hello. This is a voice test.'
  };
  var NILA = { rate: 0.88, pitch: 0.98, volume: 0.8 }, WARM = { pitch: 1.1, volume: 1 };
  var ICONS = { TIMEOUT: '\u23F1\uFE0F', FILE_TOO_LARGE: '\uD83D\uDCE6', INVALID_PDF: '\uD83D\uDCC4', PASSWORD_PROTECTED: '\uD83D\uDD12',
                EMPTY_FILE: '\uD83D\uDCED', TOO_MANY_PAGES: '\uD83D\uDCDA', MISSING_FILE: '\uD83D\uDCCE', NETWORK: '\uD83D\uDD0C',
                NO_VOICE: '\uD83D\uDD08', NO_TEXT: '\uD83D\uDDBC\uFE0F', INTERNAL_ERROR: '\uD83D\uDEE0\uFE0F', TRANSLATION_ENGINE_OFFLINE: '\uD83D\uDCA4',
                INFO: '\u2139\uFE0F', EDGE: '\uD83C\uDF10' };

  var st = { jsonRun: 0, jsonOriginal: '', doc: null, page: 0, languages: [], accents: [], catalog: [], trans: {}, inflight: {}, engineUp: null,
             playing: false, run: 0, chunkNow: -1, readInfo: null, warnedFallback: false };

  /* ------------------------------------------------------------ notices */
  function selectionText() {
    var parts = [], l = langByCode(targetCode());
    if (l) parts.push(l.englishName);
    if ($('accentSel') && $('accentSel').value && l) parts.push($('accentSel').value);
    if ($('voiceSel') && $('voiceSel').value) parts.push($('voiceSel').value);
    return parts.join(' / ');
  }
  function setOp(name) { st.op = name; }
  /* Coloured, animated notice (reason, steps, idea) - see visual-errors.js. guide = the catalogue entry the server sent with the error. */
  function notice(sev, code, title, msg, hint, guide) {
    var l = langByCode(targetCode()), f = $('pdfFile') && $('pdfFile').files && $('pdfFile').files[0];
    VisualErrors.render($('notice'), { sev: sev, code: code, title: title, message: msg, hint: hint, op: st.op, guide: guide,
      ctx: { selection: selectionText(), lang: l && l.englishName, langCode: l && l.code, voice: $('voiceSel') && $('voiceSel').value,
             fileMb: f ? Math.round(f.size / 1048576) : null, page: st.doc ? st.page + 1 : null } });
  }
  function clearNotice() { $('notice').hidden = true; }
  function status(t) { $('status').textContent = t || ''; }

  function getJson(url) { return fetch(url).then(function (r) { return r.ok ? r.json() : Promise.reject(new Error('HTTP ' + r.status)); }); }
  function parse(res) { return res.text().then(function (t) { var j = null; try { j = t ? JSON.parse(t) : null; } catch (e) { /* not JSON */ } return { res: res, json: j }; }); }

  /* ------------------------------------------------------------ languages / accents / voices */
  function langByCode(c) { return st.languages.filter(function (l) { return l.code === c; })[0]; }
  function targetCode() { return $('targetSel').value; }                         // 'orig' or a language code
  function readLangCode() { return targetCode() === 'orig' || st.fallbackToOriginal ? (st.doc && st.doc.detectedLanguage) || 'en' : targetCode(); }

  function localesFor(code) {
    var l = langByCode(code), base = l ? l.voiceLocales.slice() : [code];
    if (targetCode() !== 'orig' && !st.fallbackToOriginal) {
      var a = st.accents.filter(function (x) { return x.code === $('accentSel').value; })[0];
      if (a) { a.voiceLocales.forEach(function (x, i) { var k = base.indexOf(x); if (k >= 0) base.splice(k, 1); base.splice(i, 0, x); }); }
    }
    return base;
  }

  function fillTargets() {
    var sel = $('targetSel'); sel.innerHTML = '';
    var o = document.createElement('option'); o.value = 'orig'; o.textContent = 'Original language (no translation)'; sel.appendChild(o);
    var groups = {};
    st.languages.forEach(function (l) {
      var label = l.familyLabel || l.family;
      var g = groups[label] || (groups[label] = document.createElement('optgroup')); g.label = label;
      var op = document.createElement('option'); op.value = l.code; op.textContent = l.englishName + ' - ' + l.nativeName; g.appendChild(op);
    });
    Object.keys(groups).forEach(function (k) { sel.appendChild(groups[k]); });
  }

  function fillAccents() {
    var sel = $('accentSel'); sel.innerHTML = '';
    var code = targetCode();
    $('accentWrap').style.display = $('modeWrap').style.display = code === 'orig' ? 'none' : '';
    if (code === 'orig') { st.accents = []; return Promise.resolve(); }
    return getJson('/api/accents?langCode=' + encodeURIComponent(code)).then(function (list) {
      st.accents = list;
      list.forEach(function (a) { var op = document.createElement('option'); op.value = a.code; op.textContent = a.displayName + ' - ' + a.description; sel.appendChild(op); });
      $('accentWrap').style.display = list.length > 1 ? '' : 'none';              // English / Spanish / French: standard only
    }).catch(function () { st.accents = []; });
  }

  function fillVoices() {
    var sel = $('voiceSel'), previous = sel.value; sel.innerHTML = '';
    var code = readLangCode();
    var list = st.catalog.filter(function (v) { return v.lang.split('-')[0] === code; });
    var locales = localesFor(code);
    list.sort(function (a, b) { var x = locales.indexOf(a.lang), y = locales.indexOf(b.lang); return (x < 0 ? 99 : x) - (y < 0 ? 99 : y); });
    if (!list.length) { var n = document.createElement('option'); n.value = ''; n.textContent = 'Automatic'; sel.appendChild(n); }
    list.forEach(function (v) { var op = document.createElement('option'); op.value = v.name.split('-')[2].replace('Neural', ''); op.textContent = v.label; sel.appendChild(op); });
    if (previous) { for (var k = 0; k < sel.options.length; k++) if (sel.options[k].value === previous) sel.value = previous; }   // keep the user's choice
    updateVoiceInfo();
  }

  /* The voice plan for whatever is about to be read: chosen voice first, other voices of the language as fall-backs. */
  function voicePlan() {
    var locales = localesFor(readLangCode()), chosen = $('voiceSel').value;
    var names = chosen ? [chosen] : [];
    $('voiceSel').querySelectorAll('option').forEach(function (o) { if (o.value && names.indexOf(o.value) < 0) names.push(o.value); });
    var pick = ES.pickVoice(speaker.voices(), { locales: locales, names: names });
    return { locales: locales, voice: pick.voice, fallbacks: pick.candidates.slice(1, 4), exact: pick.exact };
  }

  function updateVoiceInfo() {
    var info = $('voiceInfo');
    if (!speaker.supported()) { info.textContent = 'This browser cannot speak. Open the app in Microsoft Edge.'; return; }
    if (!speaker.voices().length) { info.textContent = 'Loading voices...'; return; }
    var plan = voicePlan(), langName = (langByCode(readLangCode()) || { englishName: readLangCode() }).englishName;
    if (plan.voice && plan.exact) info.textContent = 'Voice: ' + plan.voice.name;
    else if (plan.voice) info.textContent = 'Closest installed ' + langName + ' voice: ' + plan.voice.name + ' (the chosen Natural voice is not installed here)';
    else info.textContent = 'No ' + langName + ' voice is installed in this browser. Use Microsoft Edge (free Natural voices) and press "Voice check".';
  }

  /* ------------------------------------------------------------ translation engine status */
  function refreshEngine() {
    return getJson('/api/v1/translate/status').then(function (s) {
      st.engineUp = !!s.available; var c = $('engineChip');
      c.className = 'chip ' + (s.available ? 'up' : 'down');
      c.textContent = s.available ? 'Translation: ready (' + (s.label || s.model) + ')' : 'Translation engine: offline - original text will be read';
      c.title = s.hint || '';
    }).catch(function () { st.engineUp = false; $('engineChip').className = 'chip down'; $('engineChip').textContent = 'Translation engine: unknown'; });
  }

  /* ------------------------------------------------------------ upload */
  function showError(res, json) {
    if (json && json.code) notice(res && (res.status === 408 || res.status === 503) ? 'warning' : 'error', json.code, json.error || 'Problem', json.userGuidance, json.workaround, json.guide);
    else notice('error', 'INTERNAL_ERROR', 'Something went wrong', 'The server sent an unexpected answer (HTTP ' + (res ? res.status : '?') + ').', 'Try again.');
  }

  function upload() {
    setOp('Analyze PDF');
    clearNotice();
    var f = $('pdfFile').files[0];
    if (!f) { notice('warning', 'MISSING_FILE', 'Choose a PDF first', 'No file is selected.', 'Pick a PDF, then press Analyze.'); return; }
    var fd = new FormData(); fd.append('file', f);
    $('analyzeBtn').disabled = true; $('analyzeBtn').textContent = 'Reading...';
    fetch('/api/v1/documents/analyze-sync', { method: 'POST', body: fd }).then(parse)
      .then(function (o) { if (!o.res.ok) showError(o.res, o.json); else loadDoc(o.json); })
      .catch(function () { notice('error', 'NETWORK', 'Cannot reach the server', 'The connection failed before an answer came back.', 'Check that the application is running.'); })
      .finally(function () { $('analyzeBtn').disabled = false; $('analyzeBtn').textContent = 'Analyze'; });
  }

  function loadDoc(d) {
    stop(); st.doc = d; st.trans = {}; st.inflight = {}; st.fallbackToOriginal = false; st.warnedFallback = false;
    $('totalPages').textContent = d.totalPages;
    var bar = $('progressBar'); bar.min = 1; bar.max = Math.max(1, d.totalPages);
    $('skipInfo').textContent = (d.skippedContent || []).join(' | ');
    var preview = d.pages.filter(function (p) { return p.readable; }).slice(0, 3).map(function (p) { return { page: p.sequence, text: p.blocks[0].content.substring(0, 150) }; });
    st.jsonOriginal = JSON.stringify({ documentId: d.documentId, fileName: d.fileName, totalPages: d.totalPages,
      readablePages: d.readablePages, detectedLanguage: d.detectedLanguage, truncated: d.truncated, previewPages: preview, skipped: d.skippedContent }, null, 2);
    st.jsonRun++; $('jsonContainer').textContent = st.jsonOriginal; $('jsonTag').textContent = '(first 3 readable pages, short)'; $('jsonProgress').textContent = '';
    if (d.readablePages === 0) {
      notice('warning', 'NO_TEXT', 'This PDF has no readable text', (d.skippedContent && d.skippedContent[0]) || 'Every page is a picture or scan.', 'Use a PDF whose text can be selected.');
    }
    fillVoices();
    var first = firstReadable(0, 1);
    gotoPage(first >= 0 ? first : 0, false);
  }

  /* ------------------------------------------------------------ pages */
  function isReadable(i) { return !!(st.doc && st.doc.pages[i] && st.doc.pages[i].readable); }
  function firstReadable(from, dir) { if (!st.doc) return -1; for (var i = from; i >= 0 && i < st.doc.pages.length; i += dir) if (isReadable(i)) return i; return -1; }
  function pageText(i) { return isReadable(i) ? st.doc.pages[i].blocks[0].content : ''; }
  function transKey(i) { return [st.doc.documentId, i, targetCode(), $('accentSel').value, $('modeSel').value].join('|'); }

  function gotoPage(i, keepPlaying) {
    var was = st.playing;
    cancelRun();
    st.page = i; st.chunkNow = -1;
    $('currentPage').textContent = st.doc ? i + 1 : 0; $('progressBar').value = i + 1;
    renderLinks(); render();
    if (st.doc && isReadable(i) && targetCode() !== 'orig') ensureTranslation(i).then(render);
    if (keepPlaying !== false && was) startPlayback();
  }

  function renderLinks() {
    var box = $('pageLinks'); box.innerHTML = '';
    if (!st.doc || st.doc.totalPages < 2) return;
    var total = st.doc.totalPages, cur = st.page, from = Math.max(0, cur - 4), to = Math.min(total - 1, from + 9);
    from = Math.max(0, to - 9);
    function add(i, label) {
      var b = document.createElement('button'); b.type = 'button'; b.textContent = label || (i + 1);
      if (i === cur) b.className = 'cur'; else if (!isReadable(i)) b.className = 'dim';
      b.addEventListener('click', function () { gotoPage(i, true); }); box.appendChild(b);
    }
    if (from > 0) { add(0, '1'); if (from > 1) { var d = document.createElement('span'); d.textContent = '\u2026'; box.appendChild(d); } }
    for (var i = from; i <= to; i++) add(i);
    if (to < total - 1) { var d2 = document.createElement('span'); d2.textContent = '\u2026'; box.appendChild(d2); add(total - 1); }
  }

  /* ------------------------------------------------------------ translation */
  /* A successful translation is kept; a failed one is only remembered for 15 s, so pressing Play again (or clicking the
     engine chip) retries instead of staying stuck on "original (not translated)". */
  function fresh(key) { var t = st.trans[key]; return !!t && (t.ok || Date.now() - (t.at || 0) < 15000); }
  function ensureTranslation(i) {
    var code = targetCode();
    if (code === 'orig' || !isReadable(i)) return Promise.resolve(null);
    var key = transKey(i);
    if (fresh(key)) return Promise.resolve(st.trans[key]);
    if (st.inflight[key]) return st.inflight[key];
    st.inflight[key] = fetch('/api/v1/translate', { method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ text: pageText(i), target: code, accent: $('accentSel').value || 'standard', mode: $('modeSel').value, source: st.doc.detectedLanguage }) })
      .then(parse).then(function (o) {
        var r;
        if (o.res.ok) { r = { ok: true, text: o.json.translation, same: o.json.sameLanguage, engine: o.json.engine }; st.engineUp = true; st.lastEngine = o.json.engine; }
        else { r = { ok: false, code: o.json && o.json.code, message: (o.json && o.json.error) || ('HTTP ' + o.res.status), hint: o.json && (o.json.userGuidance) };
               if (r.code === 'TRANSLATION_ENGINE_OFFLINE') st.engineUp = false; }
        r.at = Date.now(); st.trans[key] = r; return r;
      }).catch(function () { var r = { ok: false, code: 'NETWORK', message: 'The translation request failed.', hint: 'Check that the application is running.', at: Date.now() }; st.trans[key] = r; return r; })
      .finally(function () { delete st.inflight[key]; refreshChip(); });
    return st.inflight[key];
  }
  function refreshChip() {
    var c = $('engineChip');
    if (st.engineUp === true) { c.className = 'chip up'; if (st.lastEngine) c.textContent = 'Translation: ready (' + (/^ollama/.test(st.lastEngine) ? st.lastEngine : 'free online translator') + ')'; }
    else if (st.engineUp === false) { c.className = 'chip down'; c.textContent = 'Translation engine: offline - original text will be read'; }
  }

  /* What will actually be spoken for page i (and in which language). */
  function readSource(i) {
    var orig = pageText(i);
    if (targetCode() === 'orig') return { text: orig, lang: (st.doc && st.doc.detectedLanguage) || 'en', translated: false };
    var t = st.trans[transKey(i)];
    if (t && t.ok) return { text: t.text, lang: t.same ? (st.doc.detectedLanguage || 'en') : targetCode(), translated: !t.same };
    return { text: orig, lang: (st.doc && st.doc.detectedLanguage) || 'en', translated: false, pending: !t, failed: t && !t.ok ? t : null };
  }

  /* ------------------------------------------------------------ rendering */
  function chunkSpans(box, text, nowIdx) {
    box.textContent = '';
    ES.chunkText(text, 220).forEach(function (c, k) {
      var s = document.createElement('span'); s.className = 'chunk' + (k === nowIdx ? ' now' : ''); s.textContent = c + ' '; box.appendChild(s);
    });
  }
  function render() {
    var orig = $('origPanel'), read = $('readingPreview');
    if (!st.doc) { orig.textContent = read.textContent = 'Waiting...'; return; }
    var i = st.page, src = readSource(i);
    $('origTag').textContent = '- page ' + (i + 1);
    if (!isReadable(i)) {
      var why = (st.doc.pages[i].blocks[0] || {}).type;
      var msg = why === 'GARBLED_FONT' ? 'This page uses an old font whose text cannot be decoded - skipped.'
              : why === 'NOT_READ' ? 'This page was beyond the size limit and was not read.' : 'This page is a picture/scan - nothing to read.';
      orig.textContent = read.textContent = msg; $('transTag').textContent = ''; return;
    }
    var readingOrig = !src.translated;
    chunkSpans(orig, pageText(i), readingOrig ? st.chunkNow : -1);
    if (targetCode() === 'orig') { read.innerHTML = ''; chunkSpans(read, pageText(i), st.chunkNow); $('transTag').textContent = '- original'; return; }
    var lang = (langByCode(targetCode()) || {}).englishName || targetCode();
    if (src.translated) { chunkSpans(read, src.text, st.chunkNow); $('transTag').textContent = '- ' + lang + (st.trans[transKey(i)] && st.trans[transKey(i)].engine ? ' (' + st.trans[transKey(i)].engine + ')' : ''); return; }
    if (src.pending) { read.innerHTML = ''; var m = document.createElement('div'); m.className = 'muted'; m.textContent = 'Translating to ' + lang + '...'; read.appendChild(m); $('transTag').textContent = ''; return; }
    if (src.failed) {
      read.textContent = '';
      var n = document.createElement('div'); n.className = 'muted';
      n.textContent = (src.failed.code === 'TRANSLATION_ENGINE_OFFLINE' ? 'No translation engine could be reached. ' : 'Translation failed. ') + (src.failed.hint || src.failed.message) + ' - the original text is shown and read instead.';
      read.appendChild(n); chunkSpans(read, pageText(i), readingOrig ? st.chunkNow : -1);
      $('transTag').textContent = '- original (not translated)'; return;
    }
    chunkSpans(read, pageText(i), st.chunkNow);
  }

  /* ------------------------------------------------------------ speaking */
  function cancelRun() { st.run++; speaker.stop(); }

  function opts() {
    var nila = $('styleSel').value === 'nila';
    return { rate: nila ? NILA.rate : parseFloat($('speedSel').value), pitch: nila ? NILA.pitch : WARM.pitch, volume: nila ? NILA.volume : WARM.volume };
  }

  function speechFailed(e) {
    st.playing = false; $('playBtn').textContent = 'Play'; status('');
    notice('warning', 'NO_VOICE', 'The voice could not speak', e.message, 'Open the app in Microsoft Edge, press "Voice check", or choose another voice.');
  }

  function startPlayback() {
    if (!st.doc) return;
    var my = ++st.run;
    speaker.stop();
    var i = st.page;
    if (!isReadable(i)) { var n = firstReadable(i + 1, 1); if (n < 0) return finish(); st.page = i = n; renderLinks(); }
    var needs = targetCode() !== 'orig' && !fresh(transKey(i));
    if (needs) { status('Translating page ' + (i + 1) + '...'); render(); }
    var p = needs ? ensureTranslation(i) : Promise.resolve();
    p.then(function () {
      if (my !== st.run || !st.playing) return;
      status('');
      var src = readSource(i);
      if (src.failed && !st.warnedFallback) {
        st.warnedFallback = true;
        notice('warning', src.failed.code === 'TRANSLATION_ENGINE_OFFLINE' ? 'TRANSLATION_ENGINE_OFFLINE' : 'INFO', 'Reading the original text',
               src.failed.message, src.failed.hint || 'Connect to the internet (free online translator) or start Ollama, then press Play again.');
      }
      st.fallbackToOriginal = !!src.failed;
      fillVoices();
      var next = firstReadable(i + 1, 1);
      if (next >= 0 && targetCode() !== 'orig') ensureTranslation(next);               // pre-translate the next page while this one is read
      $('currentPage').textContent = i + 1; $('progressBar').value = i + 1; render();
      var plan = voicePlan();
      var o = opts();
      speaker.speak(src.text, {
        voice: plan.voice, fallbacks: plan.fallbacks, lang: plan.locales[0], rate: o.rate, pitch: o.pitch, volume: o.volume,
        onChunk: function (k) { if (my !== st.run) return; st.chunkNow = k; render(); },
        onEnd: function () {
          if (my !== st.run || !st.playing) return;
          var n2 = firstReadable(st.page + 1, 1);
          if (n2 < 0) return finish();
          st.page = n2; st.chunkNow = -1; renderLinks(); startPlayback();
        },
        onError: function (e) { if (my === st.run) speechFailed(e); }
      });
    });
  }
  function finish() { st.playing = false; $('playBtn').textContent = 'Play'; status('Finished'); }

  function play() {
    setOp('Play / Pause');
    if (!st.doc || st.doc.readablePages === 0) { notice('warning', 'MISSING_FILE', 'Nothing to read yet', 'Analyze a PDF that has text first.', null); return; }
    if (!speaker.supported()) { speechFailed({ message: ES.friendlyError('unsupported') }); return; }
    if (st.playing) { st.playing = false; cancelRun(); $('playBtn').textContent = 'Resume'; status('Paused'); return; }
    clearNotice(); st.playing = true; $('playBtn').textContent = 'Pause';
    speaker.ready().then(function () { if (st.playing) startPlayback(); });
  }
  function stop() { st.playing = false; cancelRun(); st.chunkNow = -1; $('playBtn').textContent = 'Play'; status(''); if (st.doc) render(); }
  function step(dir) { if (!st.doc) return; var n = firstReadable(st.page + dir, dir); if (n >= 0) gotoPage(n, true); }

  /* ------------------------------------------------------------ JSON -> translated JSON -> voice */
  var JSON_PAGE_LIMIT = 40;       // pages translated per click (each page is one request; press again for more)

  function translateText(text, code) {      // one-off translation of a short text (not cached per page)
    return fetch('/api/v1/translate', { method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ text: text, target: code, accent: $('accentSel').value || 'standard', mode: $('modeSel').value, source: st.doc.detectedLanguage }) })
      .then(parse).then(function (o) { return o.res.ok ? { ok: true, text: o.json.translation, engine: o.json.engine } : { ok: false, message: (o.json && o.json.error) || ('HTTP ' + o.res.status) }; })
      .catch(function () { return { ok: false, message: 'The translation request failed.' }; });
  }

  /* Translates the document JSON (every readable page, in order) into the chosen language, shows the translated JSON,
     then reads it aloud with the language's Natural voice - the same South Indian voices the Play button uses. */
  function translateJson() {
    setOp('Translate JSON & speak');
    clearNotice();
    if (!st.doc || st.doc.readablePages === 0) { notice('warning', 'MISSING_FILE', 'Nothing to translate yet', 'Analyze a PDF that has text first.', null); return; }
    var code = targetCode();
    if (code === 'orig') { notice('warning', 'CHOOSE_LANGUAGE', 'Choose a language first', 'The JSON is translated into the language selected under "Read in".', 'Pick Kannada, Tamil, Telugu, Malayalam, Urdu, Hindi, Marathi, Spanish, French or English.'); return; }
    var my = ++st.jsonRun, lang = (langByCode(code) || {}).englishName || code;
    var idx = []; for (var i = 0; i < st.doc.pages.length && idx.length < JSON_PAGE_LIMIT; i++) if (isReadable(i)) idx.push(i);
    var out = [], engine = '', failed = null, btn = $('jsonTransBtn');
    btn.disabled = true;
    function progress(n) { $('jsonProgress').textContent = 'Translating page ' + n + ' of ' + idx.length + '...'; }
    var chain = Promise.resolve();
    idx.forEach(function (pi, n) {
      chain = chain.then(function () {
        if (my !== st.jsonRun || failed) return;
        progress(n + 1);
        return ensureTranslation(pi).then(function (r) {
          if (my !== st.jsonRun) return;
          if (!r || !r.ok) { failed = r || { message: 'No answer' }; return; }
          engine = r.engine || engine;
          out.push({ page: pi + 1, language: r.same ? (st.doc.detectedLanguage || 'en') : code, translated: !r.same, text: r.text });
        });
      });
    });
    chain.then(function () {
      if (my !== st.jsonRun) return null;
      if (failed) { return null; }
      var skipped = (st.doc.skippedContent || []);
      return skipped.length ? translateText(skipped.join('\n'), code) : { ok: true, text: '' };
    }).then(function (sk) {
      btn.disabled = false; $('jsonProgress').textContent = '';
      if (my !== st.jsonRun) return;
      if (failed) {
        notice('warning', failed.code || 'TRANSLATION_FAILED', 'The JSON could not be translated', failed.message || 'The translation failed.', failed.hint || 'Press "Test translation" to see which engine fails and why.');
        return;
      }
      var doc = { documentId: st.doc.documentId, fileName: st.doc.fileName, totalPages: st.doc.totalPages, readablePages: st.doc.readablePages,
                  detectedLanguage: st.doc.detectedLanguage, translatedTo: { code: code, language: lang, accent: $('accentSel').value || 'standard', engine: engine },
                  translatedPages: out, translatedPageCount: out.length, moreAvailable: st.doc.readablePages > out.length,
                  skipped: sk && sk.ok && sk.text ? sk.text.split('\n') : st.doc.skippedContent };
      $('jsonContainer').textContent = JSON.stringify(doc, null, 2);
      $('jsonTag').textContent = '(translated to ' + lang + ', ' + out.length + ' page' + (out.length === 1 ? '' : 's') + ')';
      var first = out.length ? out[0].page - 1 : st.page;
      st.page = first; st.playing = true; $('playBtn').textContent = 'Pause';
      gotoPage(first, false);
      speaker.ready().then(function () { if (st.playing) startPlayback(); });
    });
  }

  function showOriginalJson() { st.jsonRun++; $('jsonProgress').textContent = ''; $('jsonTransBtn').disabled = false; if (st.jsonOriginal) { $('jsonContainer').textContent = st.jsonOriginal; $('jsonTag').textContent = '(first 3 readable pages, short)'; } }

  /* "Test translation": which engine answers for the chosen language, or exactly why each one fails. */
  function selfTest() {
    setOp('Test translation');
    clearNotice();
    var code = targetCode() === 'orig' ? 'hi' : targetCode();
    status('Testing translation to ' + code + '...');
    getJson('/api/v1/translate/selftest?target=' + encodeURIComponent(code)).then(function (r) {
      status('');
      var lines = ['Ollama running: ' + (r.ollamaRunning ? 'yes' : 'no')].concat(r.online || []);
      if (r.ok) notice('info', 'TRANSLATION_OK', 'Translation to ' + code + ' works (' + r.engine + ')', lines.join(' | '), (r.result || '').slice(0, 160));
      else notice('warning', 'TRANSLATION_FAILED', 'Translation to ' + code + ' fails', (r.error || '') + ' || ' + lines.join(' | '), 'Fix the failing route, e.g. allow translate.googleapis.com in the firewall, or start Ollama.');
    }).catch(function () { status(''); notice('error', 'NETWORK', 'Test failed', 'The server did not answer.', 'Check that the application is running.'); });
  }

  function sample() {
    setOp('Hear sample');
    clearNotice();
    speaker.ready().then(function () {
      var plan = voicePlan(), o = opts(), code = readLangCode();
      var text = SAMPLE[code] || SAMPLE.en;
      if (st.doc && isReadable(st.page)) { var s = readSource(st.page); if (!s.pending) text = s.text.substring(0, 180); }
      st.playing = false; $('playBtn').textContent = 'Play'; st.run++;
      speaker.speak(text, { voice: plan.voice, fallbacks: plan.fallbacks, lang: plan.locales[0], rate: o.rate, pitch: o.pitch, volume: o.volume,
        onError: speechFailed });
      status('Playing sample...');
      setTimeout(function () { if (!st.playing) status(''); }, 4000);
    });
  }

  function voiceCheck() {
    setOp('Voice check');
    speaker.ready(4000).then(function () {
      var d = speaker.describe(localesFor(readLangCode()));
      var edge = /Edg\//.test(navigator.userAgent);
      var msg = d.total + ' voices installed, ' + d.natural + ' Natural. ' + (d.sameLanguage.length ? 'For this language: ' + d.sameLanguage.join('; ') : 'None for this language.');
      notice(d.sameLanguage.length ? 'info' : 'warning', 'INFO', 'Voice check', msg,
        d.sameLanguage.length ? (edge ? null : 'You are not in Microsoft Edge - Natural voices are an Edge feature.')
                              : 'Open this page in Microsoft Edge and make sure you are online - the Natural voices download on first use.');
      updateVoiceInfo();
    });
  }

  /* ------------------------------------------------------------ init */
  function onTargetChange() {
    setOp('Choose language');
    st.fallbackToOriginal = false; st.warnedFallback = false;
    fillAccents().then(function () { fillVoices(); if (st.doc) gotoPage(st.page, true); });
  }

  /* the School panel hands a school PDF to the reader: it goes through the normal Analyze flow (text, JSON, translation, voices) */
  window.WN_loadFile = function (file) {
    try { var dt = new DataTransfer(); dt.items.add(file); $('pdfFile').files = dt.files; } catch (e) { notice('error', 'INTERNAL_ERROR', 'This browser cannot hand the PDF to the reader', e.message, 'Use Microsoft Edge, or download the PDF and choose it with Choose file.'); return; }
    upload();
  };

  function init() {
    VisualErrors.load();
    getJson('/api/greeting').then(function (g) { $('greeting').textContent = g.greeting; }).catch(function () { /* optional */ });
    Promise.all([getJson('/api/languages'), getJson('/api/v1/voice/voices')]).then(function (r) {
      st.languages = r[0]; st.catalog = r[1]; fillTargets(); fillVoices();
    }).catch(function () { notice('error', 'NETWORK', 'Cannot load languages', 'The server did not answer.', 'Check that the application is running.'); });
    refreshEngine();
    speaker.ready().then(function () { updateVoiceInfo(); });
    if (!/Edg\//.test(navigator.userAgent)) {
      notice('info', 'EDGE', 'Use Microsoft Edge for the free Natural voices', 'This browser may not have the Kannada/Tamil/Telugu/Malayalam/Hindi/Urdu/Marathi Natural voices.', 'Open http://localhost:8089 in Microsoft Edge.');
    }
    $('targetSel').addEventListener('change', onTargetChange);
    ['accentSel', 'modeSel'].forEach(function (id) { $(id).addEventListener('change', function () { st.warnedFallback = false; if (st.doc) gotoPage(st.page, true); }); });
    ['voiceSel', 'styleSel', 'speedSel'].forEach(function (id) { $(id).addEventListener('change', function () { updateVoiceInfo(); if (st.playing) startPlayback(); }); });
    $('engineChip').addEventListener('click', function () { st.warnedFallback = false; st.fallbackToOriginal = false; refreshEngine().then(function () { if (st.doc) { Object.keys(st.trans).forEach(function (k) { if (!st.trans[k].ok) delete st.trans[k]; }); gotoPage(st.page, true); } }); });
    $('engineChip').title = 'Click to re-check the translation engine and retry';
    $('jsonTransBtn').addEventListener('click', translateJson);
    $('jsonOrigBtn').addEventListener('click', showOriginalJson);
    $('selfTestBtn').addEventListener('click', selfTest);
    $('analyzeBtn').addEventListener('click', upload);
    $('playBtn').addEventListener('click', play);
    $('stopBtn').addEventListener('click', stop);
    $('sampleBtn').addEventListener('click', sample);
    $('checkBtn').addEventListener('click', voiceCheck);
    $('prevBtn').addEventListener('click', function () { step(-1); });
    $('nextBtn').addEventListener('click', function () { step(1); });
    $('progressBar').addEventListener('input', function () {
      if (!st.doc) return;
      var i = parseInt(this.value, 10) - 1, n = isReadable(i) ? i : (firstReadable(i, 1) >= 0 ? firstReadable(i, 1) : firstReadable(i, -1));
      if (n >= 0) gotoPage(n, true);
    });
    window.addEventListener('beforeunload', function () { speaker.stop(); });
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init); else init();
})();
