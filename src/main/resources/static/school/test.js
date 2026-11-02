/* A student takes today's test (pop-out): live timer, check-box questions built from the test file, autosave, auto-submit,
   result with the correct answers, and read-aloud / translated preview of each question with the language's Natural voice. */
(function (root) {
  'use strict';
  var S = root.School, el = S.el, doc = root.document, view = doc.getElementById('view'), P = S.params(), id = P.id;
  var LOCALE = { kn: 'kn-IN', ta: 'ta-IN', te: 'te-IN', ml: 'ml-IN', ur: 'ur-IN', hi: 'hi-IN', mr: 'mr-IN', es: 'es-ES', fr: 'fr-FR' };
  var test = null, answers = {}, timerId = 0, left = 0, submitted = false, storeKey = '';

  function err(e, op) { S.showError(e, op); }
  function clear(n) { while (n.firstChild) n.removeChild(n.firstChild); }
  function mmss(s) { s = Math.max(0, s); return String(Math.floor(s / 60)).padStart(2, '0') + ':' + String(s % 60).padStart(2, '0'); }

  // ------------------------------------------------------------------------------------------ read aloud / translate
  function questionText(q) { return q.text + '\n' + q.options.map(function (o) { return o.letter + '. ' + o.text; }).join('\n'); }
  function pickVoice(code) {
    var loc = LOCALE[code], vs = root.speechSynthesis ? root.speechSynthesis.getVoices() : [];
    var same = vs.filter(function (v) { return v.lang === loc; }), prim = vs.filter(function (v) { return v.lang.split('-')[0] === loc.split('-')[0]; });
    var pool = same.length ? same : prim;
    return pool.filter(function (v) { return /natural|online/i.test(v.name); })[0] || pool[0] || null;
  }
  function speak(text, code) {
    var u = new root.SpeechSynthesisUtterance(text), v = pickVoice(code);
    if (!v) { err({ message: 'No voice for ' + LOCALE[code], status: 400, json: { code: 'NO_VOICE' } }, 'Read aloud'); return; }
    u.voice = v; u.lang = v.lang; root.speechSynthesis.cancel(); root.speechSynthesis.speak(u);
  }
  function readQuestion(q, box) {
    var code = doc.getElementById('lang').value, text = questionText(q);
    if (!code) { if (root.speechSynthesis) { var u = new root.SpeechSynthesisUtterance(text); u.lang = 'en-IN'; root.speechSynthesis.cancel(); root.speechSynthesis.speak(u); } return; }
    box.textContent = 'Translating...';
    root.fetch('/api/v1/translate', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ text: text, target: code, accent: 'standard', mode: 'translate', source: 'en' }) })
      .then(function (r) { return r.json().then(function (j) { if (!r.ok) { var e = new Error(j.error || 'HTTP ' + r.status); e.status = r.status; e.json = j; throw e; } return j; }); })
      .then(function (j) { box.textContent = j.translation; box.hidden = false; speak(j.translation, code); })
      .catch(function (e) { box.textContent = ''; err(e, 'Translate question'); });
  }

  // ------------------------------------------------------------------------------------------ taking the test
  function save() { try { root.sessionStorage.setItem(storeKey, JSON.stringify(answers)); } catch (e) { /* storage blocked */ } }
  function build(t) {
    test = t; storeKey = 'schooltest.' + t.testId + '.' + t.attemptKey;
    try { answers = JSON.parse(root.sessionStorage.getItem(storeKey) || '{}'); } catch (e) { answers = {}; }
    doc.getElementById('testTitle').textContent = t.title + ' - ' + t.subject;
    doc.getElementById('readbar').hidden = false;
    clear(view);
    t.questions.forEach(function (q) {
      var tr = el('div', { class: 'tq', style: 'margin-top:8px', hidden: true });
      var card = el('div', { class: 'tq' }, [el('h3', null, [q.number + '. ' + q.text + '  ', el('small', { text: '(' + q.marks + ' mark' + (q.marks > 1 ? 's' : '') + (q.multiple ? ', tick all correct' : '') + ')' }), ' ',
        el('button', { type: 'button', class: 'sm', text: '🔊', title: 'Read aloud / translate', onclick: function () { readQuestion(q, tr); } })])]);
      q.options.forEach(function (o) {
        card.appendChild(el('label', null, [el('input', { type: 'checkbox', checked: (answers[q.number] || []).indexOf(o.letter) >= 0, onchange: function (e) {
          var set = answers[q.number] || []; set = set.filter(function (x) { return x !== o.letter; }); if (e.target.checked) set.push(o.letter); answers[q.number] = set.sort(); save(); } }), o.letter + '.  ' + o.text]));
      });
      card.appendChild(tr); view.appendChild(card);
    });
    view.appendChild(el('p', null, [el('button', { type: 'button', text: '✔ Submit test', onclick: function () { if (root.confirm('Submit your answers now?')) submit(); } })]));
    left = t.secondsLeft; var timer = doc.getElementById('timer'); timer.hidden = false;
    var started = Date.now(), base = left;
    timerId = root.setInterval(function () {
      left = base - Math.floor((Date.now() - started) / 1000);
      timer.textContent = '⏱ ' + mmss(left); timer.classList.toggle('low', left <= 60);
      if (left <= 0) { root.clearInterval(timerId); submit(); }
    }, 500);
  }
  function submit() {
    if (submitted) return; submitted = true; root.clearInterval(timerId);
    S.api('POST', '/school/api/student/tests/' + id + '/submit', { answers: answers }).then(function () { try { root.sessionStorage.removeItem(storeKey); } catch (e) { /* ignore */ } showResult(); })
      .catch(function (e) { submitted = false; err(e, 'Submit test'); });
  }

  // ------------------------------------------------------------------------------------------ result
  function showResult() {
    doc.getElementById('timer').hidden = true; doc.getElementById('readbar').hidden = true;
    S.api('GET', '/school/api/student/tests/' + id + '/result').then(function (r) {
      doc.getElementById('testTitle').textContent = r.title + ' - ' + r.subject;
      clear(view);
      var pct = r.total ? Math.round(100 * r.score / r.total) : 0;
      view.appendChild(el('div', { class: 'card' }, [el('h2', { text: 'Your marks: ' + r.score + ' / ' + r.total + '  (' + pct + '%)' }), el('div', { class: 'bar-big' }, [el('i', { style: 'width:' + pct + '%' })])]));
      r.questions.forEach(function (q) {
        var card = el('div', { class: 'tq' }, [el('h3', { text: (q.earned ? '✅ ' : '❌ ') + q.number + '. ' + q.text })]);
        q.options.forEach(function (o) {
          var cls = o.correct === true ? 'right' : (o.selected && o.correct === false ? 'wrong' : '');
          card.appendChild(el('label', { class: cls }, [el('input', { type: 'checkbox', checked: o.selected, disabled: true }), o.letter + '.  ' + o.text + (o.correct === true ? '   ✔ correct' : '')]));
        });
        view.appendChild(card);
      });
    }).catch(function (e) { err(e, 'Show result'); });
  }

  function init() {
    doc.getElementById('stopSpeech').addEventListener('click', function () { if (root.speechSynthesis) root.speechSynthesis.cancel(); });
    S.loadMe().then(function (me) {
      if (!me.loggedIn || me.role !== 'STUDENT') { err({ status: 401, message: 'Not logged in as a student', json: { code: me.loggedIn ? 'WRONG_PORTAL' : 'AUTH_REQUIRED' } }, 'Take a test'); return; }
      if (P.result) return showResult();
      return S.api('GET', '/school/api/student/tests/' + id + '/start').then(build);
    }).catch(function (e) { err(e, 'Take a test'); });
  }
  if (doc.readyState === 'loading') doc.addEventListener('DOMContentLoaded', init); else init();
})(window);
