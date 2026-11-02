/* Visual errors for every screen: a coloured, animated card with the reason, the steps that fix it, an out-of-the-box idea
   and a tip made from what the person was doing (which button, which language / voice was selected).
   The texts come from the server catalogue (GET /api/error-guides) so pages, popups and server error pages all agree;
   a small built-in copy keeps the card useful even when the server cannot be reached. */
(function (root) {
  'use strict';
  var THEME = { FILE: ['#6a11cb', '#2575fc'], NETWORK: ['#0f9b8e', '#1fd1f9'], TRANSLATION: ['#b4207a', '#7b2ff7'], VOICE: ['#f7971e', '#ff512f'],
    SECURITY: ['#cb2d3e', '#ef473a'], DATABASE: ['#1d4ed8', '#06b6d4'], INPUT: ['#e58a1f', '#e8c13a'], NOTFOUND: ['#7f00ff', '#e100ff'],
    SERVER: ['#b91d47', '#ff5f6d'], SCHOOL: ['#11998e', '#38ef7d'], INFO: ['#2193b0', '#6dd5ed'] };
  var FALLBACK = {
    NETWORK: { code: 'NETWORK', category: 'NETWORK', emoji: '\uD83D\uDD0C', headline: 'The app cannot be reached', reason: 'The page could not talk to the server - it may have stopped.',
      steps: ['Check that the black window "PDF Voice And Games (8089)" is open.', 'If not, double-click Application_run.bat.', 'Reload this page.'], tip: 'The first red line in the black window is the real reason.' },
    INTERNAL_ERROR: { code: 'INTERNAL_ERROR', category: 'SERVER', emoji: '\uD83D\uDEE0\uFE0F', headline: 'Something went wrong', reason: 'An unexpected problem happened.',
      steps: ['Try the same action again.', 'Try a different file or choice.'], tip: 'Quote the reference code below if you report it.' }
  };
  var VOICES = { kn: 'Sapna or Gagan', ta: 'Pallavi or Valluvar', te: 'Shruti or Mohan', ml: 'Sobhana or Midhun', ur: 'Gul or Salman', hi: 'Swara or Madhur',
    mr: 'Aarohi or Manohar', es: 'Elvira or Alvaro', fr: 'Denise or Henri', en: 'Neerja or Prabhat' };
  var guides = null, loading = null;

  function load() {
    if (guides) return Promise.resolve(guides);
    if (loading) return loading;
    loading = root.fetch('/api/error-guides').then(function (r) { return r.ok ? r.json() : null; }).then(function (g) { guides = g; return g; }).catch(function () { return null; });
    return loading;
  }
  function guideFor(code, given) {
    return given || (guides && guides[code]) || FALLBACK[code] || (guides && guides.INTERNAL_ERROR) || FALLBACK.INTERNAL_ERROR;
  }
  function colors(g) { var t = THEME[g.category] || THEME.INFO; return [g.color1 || t[0], g.color2 || t[1]]; }

  /* A tip made from the situation: the button, the chosen language, the voice, the file. ctx = {op, lang, langCode, voice, fileName, fileMb, page}. */
  function instanceTip(code, ctx) {
    ctx = ctx || {};
    var lang = ctx.lang || 'the chosen language', v = VOICES[ctx.langCode] || null;
    switch (code) {
      case 'NO_VOICE': return v ? 'For ' + lang + ' Edge needs the Natural voice ' + v + '. Windows: Settings, Time & Language, Speech, Manage voices, Add voices. Then restart Edge and press Voice check.' : null;
      case 'TRANSLATION_ENGINE_OFFLINE': case 'TRANSLATION_FAILED': case 'TRANSLATION_TIMEOUT':
        return 'You were translating into ' + lang + (ctx.op ? ' with "' + ctx.op + '"' : '') + '. Press "Test translation": it names the route that fails (Google POST, Google GET, MyMemory, Ollama).';
      case 'CHOOSE_LANGUAGE': return 'The list "Read in" is empty. For voices of South India choose Kannada, Tamil, Telugu, Malayalam or Urdu.';
      case 'FILE_TOO_LARGE': return ctx.fileMb ? 'Your file is ' + ctx.fileMb + ' MB; the limit is 50 MB. Split it into ' + Math.ceil(ctx.fileMb / 40) + ' parts of about 40 MB.' : null;
      case 'NO_TEXT': case 'EMPTY_TEXT': return ctx.page ? 'Page ' + ctx.page + ' is a picture. Pages with text keep their number bright under the progress bar - tap one of those.' : null;
      case 'MISSING_FILE': return 'You pressed "' + (ctx.op || 'Analyze') + '" before choosing a file. The Choose file button is just above it.';
      case 'NETWORK': return ctx.op ? 'The "' + ctx.op + '" action did not reach the server. Nothing was lost - press it again after the server is back.' : null;
      default: return null;
    }
  }

  function el(tag, cls, text) { var e = root.document.createElement(tag); if (cls) e.className = cls; if (text != null) e.textContent = text; return e; }

  /* opts: {sev, code, title, message, hint, op, ctx, guide} */
  function render(box, opts) {
    var g = guideFor(opts.code, opts.guide), c = colors(g), ctx = {};
    Object.keys(opts.ctx || {}).forEach(function (k) { ctx[k] = opts.ctx[k]; });
    if (!ctx.op) ctx.op = opts.op;
    box.className = 'notice vx ' + (opts.sev || 'error') + ' vx-' + String(g.category || 'INFO').toLowerCase();
    box.style.setProperty('--c1', c[0]); box.style.setProperty('--c2', c[1]);
    box.hidden = false; box.innerHTML = '';
    var ico = el('div', 'ico vx-ico', g.emoji || '\u26A0\uFE0F'); ico.setAttribute('aria-hidden', 'true');
    var body = el('div', 'vx-body');
    body.appendChild(el('h3', null, opts.title || g.headline));
    if (g.reason && opts.title !== g.headline && !(opts.sev === 'info' && opts.message)) body.appendChild(el('p', 'vx-why', g.reason));
    if (opts.message) { var m = el('div', 'vx-msg'); m.appendChild(el('span', null, 'Exact message')); m.appendChild(root.document.createTextNode(opts.message)); body.appendChild(m); }
    if (opts.hint) body.appendChild(el('p', 'vx-hint', '\uD83D\uDCA1 ' + opts.hint));
    var doing = [];
    if (opts.op) doing.push('Doing: ' + opts.op);
    if (ctx.selection) doing.push('Selection: ' + ctx.selection);
    if (doing.length) body.appendChild(el('div', 'vx-ctx', doing.join('  \u2022  ')));
    var compact = opts.sev === 'info';                       // information is shown briefly; problems get the full how-to-fix card
    if (!compact && g.steps && g.steps.length) {
      body.appendChild(el('h4', null, 'How to fix it'));
      var ol = el('ol', 'vx-steps');
      g.steps.forEach(function (s, i) { var li = el('li', null, s); li.style.setProperty('--i', i + 1); ol.appendChild(li); });
      body.appendChild(ol);
    }
    var own = instanceTip(opts.code, ctx), tip = own || g.tip;
    if (tip && !compact) { var t = el('div', 'vx-tip'); t.appendChild(el('b', null, own ? '\uD83D\uDCA1 For this case' : '\uD83D\uDCA1 Out-of-the-box idea')); t.appendChild(el('span', null, tip)); body.appendChild(t); }
    var foot = el('div', 'vx-foot');
    foot.appendChild(el('span', 'ref', opts.code + ' \u00B7 ' + new Date().toLocaleTimeString()));
    var x = el('button', 'vx-close', 'Dismiss'); x.type = 'button'; x.addEventListener('click', function () { box.hidden = true; });
    foot.appendChild(x); body.appendChild(foot);
    box.appendChild(ico); box.appendChild(body);
    if (box.scrollIntoView) { try { box.scrollIntoView({ block: 'nearest', behavior: 'smooth' }); } catch (e) { /* older browsers */ } }
    return box;
  }

  var api = { load: load, render: render, guideFor: guideFor, instanceTip: instanceTip, THEME: THEME };
  if (typeof module !== 'undefined' && module.exports) module.exports = api; else root.VisualErrors = api;
})(typeof window !== 'undefined' ? window : this);
