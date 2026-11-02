/* Shared by the School panel (main page) and every pop-out: API calls with the CSRF token, visual errors, small DOM helpers. */
(function (root) {
  'use strict';
  var csrf = null, me = null;

  function el(tag, attrs, kids) {                        // safe DOM builder: text is never parsed as HTML
    var e = root.document.createElement(tag);
    Object.keys(attrs || {}).forEach(function (k) {
      var v = attrs[k];
      if (v == null || v === false) return;
      if (k === 'class') e.className = v; else if (k === 'text') e.textContent = v; else if (k.slice(0, 2) === 'on') e.addEventListener(k.slice(2), v);
      else if (k === 'value') e.value = v; else if (k === 'checked' || k === 'disabled' || k === 'selected') e[k] = !!v; else e.setAttribute(k, v === true ? '' : v);
    });
    (kids || []).forEach(function (c) { if (c != null) e.appendChild(typeof c === 'string' ? root.document.createTextNode(c) : c); });
    return e;
  }

  function api(method, url, body) {
    var opt = { method: method, credentials: 'same-origin', headers: {} };
    if (csrf) opt.headers['X-CSRF-Token'] = csrf;
    if (body instanceof root.FormData) opt.body = body;
    else if (body !== undefined) { opt.headers['Content-Type'] = 'application/json'; opt.body = JSON.stringify(body); }
    return root.fetch(url, opt).then(function (res) {
      return res.text().then(function (t) {
        var j = null; try { j = t ? JSON.parse(t) : null; } catch (e) { /* not JSON */ }
        if (res.ok) return j;
        var err = new Error((j && j.error) || ('HTTP ' + res.status)); err.status = res.status; err.json = j; throw err;
      });
    });
  }

  /* Any failure -> the coloured, animated card (reason, steps, idea). op = what the person was doing. */
  function showError(err, op, box) {
    var target = box || root.document.getElementById('notice');
    var j = err && err.json, code = j && j.code ? j.code : (err && err.status ? 'INTERNAL_ERROR' : 'NETWORK');
    var VE = root.VisualErrors;
    if (!target || !VE) { if (root.console) root.console.error(err); return; }
    VE.render(target, { sev: err && err.status && err.status < 500 ? 'warning' : 'error', code: code, title: undefined, message: err && err.message, hint: j && j.userGuidance,
      op: op, guide: j && j.guide, ctx: {} });
    if (err && err.status === 401 && root.School && root.School.onLoggedOut) root.School.onLoggedOut();
  }

  function loadMe() {
    return api('GET', '/school/api/auth/me').then(function (j) { me = j; csrf = j && j.csrf ? j.csrf : null; return j; });
  }
  function login(username, password, loginAs) {
    return api('POST', '/school/api/auth/login', { username: username, password: password, loginAs: loginAs }).then(function (j) { me = j; csrf = j.csrf; return j; });
  }
  function logout() { return api('POST', '/school/api/auth/logout').then(function () { me = null; csrf = null; }); }

  /* open a pop-out window (a "new screen"); falls back to a tab when pop-ups are blocked */
  function popout(url, name, w, h) {
    var feat = 'popup=yes,width=' + (w || 1100) + ',height=' + (h || 780) + ',left=' + Math.max(0, ((root.screen && root.screen.width) || 1280) - (w || 1100) - 30) + ',top=40,resizable=yes,scrollbars=yes';
    var win = root.open(url, name || '_blank', feat);
    if (win && win.focus) win.focus();
    return win;
  }

  function fmtBytes(n) { return n == null ? '' : n < 1048576 ? Math.round(n / 1024) + ' KB' : (n / 1048576).toFixed(1) + ' MB'; }
  function todayIso() { var d = new Date(); return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0'); }
  function params() { var o = {}; new URLSearchParams(root.location.search).forEach(function (v, k) { o[k] = v; }); return o; }

  root.School = { el: el, api: api, showError: showError, loadMe: loadMe, login: login, logout: logout, popout: popout, fmtBytes: fmtBytes, todayIso: todayIso, params: params,
                  get me() { return me; }, onLoggedOut: null };
  if (typeof module !== 'undefined' && module.exports) module.exports = root.School;
})(typeof window !== 'undefined' ? window : this);
