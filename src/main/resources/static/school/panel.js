/* The "sprouting" School panel: slides in from the right of the main page and pushes the PDF reader blocks to the left.
   Logged out: Student / Admin login. Student: today's tests, PDFs (open, or listen + translate in the reader), live class, games, results.
   Admin: school name (editable here and only here), shortcuts into the pop-out Admin Console. */
(function (root) {
  'use strict';
  var S = root.School, el = S.el, doc = root.document, dock, body, view = 'out', loginAs = 'STUDENT', notice;

  function open(on) { doc.body.classList.toggle('school-open', on); if (on) refresh(); }
  function clear(n) { while (n.firstChild) n.removeChild(n.firstChild); }
  function err(e, op) { S.showError(e, op, notice); }

  function header(role, name, schoolName, editable) {
    var school = el('div', { class: 'sp-school' }, [el('span', { text: schoolName || '' })]);
    if (editable) school.appendChild(el('button', { type: 'button', class: 'sm', text: '\u270E Edit name', onclick: function () { rename(schoolName); } }));
    return el('div', { class: 'sp-head' }, [el('div', null, [el('h2', { class: 'sp-role gold3d', text: role }), school, name ? el('small', { text: name }) : null]),
      el('button', { type: 'button', class: 'sp-x', text: '\u2715', title: 'Close panel', onclick: function () { open(false); } })]);
  }
  function rename(current) {
    var v = root.prompt('School name (only the admin can change it):', current || '');
    if (v == null) return;
    S.api('PUT', '/school/api/admin/school', { schoolName: v }).then(refresh).catch(function (e) { err(e, 'Rename school'); });
  }

  function loginView(j) {
    clear(body);
    var u = el('input', { type: 'text', autocomplete: 'username', placeholder: 'User name' }), p = el('input', { type: 'password', autocomplete: 'current-password', placeholder: 'Password' });
    var tabs = el('div', { class: 'sp-tabs' }, ['STUDENT', 'ADMIN'].map(function (r) {
      return el('button', { type: 'button', class: r === loginAs ? 'on' : '', text: r === 'STUDENT' ? 'Student login' : 'Admin login', onclick: function () { loginAs = r; loginView(j); } });
    }));
    var go = function () { S.login(u.value, p.value, loginAs).then(refresh).catch(function (e) { err(e, loginAs === 'ADMIN' ? 'Admin login' : 'Student login'); }); };
    p.addEventListener('keydown', function (e) { if (e.key === 'Enter') go(); });
    body.appendChild(el('div', { class: 'sp' }, [header('SCHOOL', null, j && j.schoolName, false), tabs,
      el('label', { text: 'User name' }), u, el('label', { text: 'Password' }), p,
      el('p', { class: 'sp-msg', text: 'Your teacher or school admin gives you the user name and password.' }),
      el('button', { type: 'button', text: 'Log in', onclick: go })]));
  }

  function item(title, sub, pill, btns) {
    return el('div', { class: 'sp-item' }, [el('div', null, [el('b', { text: title }), sub ? el('small', { text: sub }) : null, pill ? el('span', { class: 'pill ' + pill, text: pill.replace('_', ' ') }) : null]),
      el('div', { class: 'row' }, btns || [])]);
  }

  function studentView(j) {
    S.api('GET', '/school/api/student/home').then(function (h) {
      clear(body);
      var tests = el('div', { class: 'sp-list' });
      (h.tests || []).slice(0, 8).forEach(function (t) {
        var b = [];
        if (t.status === 'OPEN' || t.status === 'IN_PROGRESS') b.push(el('button', { type: 'button', class: 'sm', text: '\u25B6 Start', onclick: function () { S.popout('/school/test.html?id=' + t.id, 'schooltest', 1000, 800); } }));
        if (t.status === 'DONE') b.push(el('button', { type: 'button', class: 'sm', text: t.score + '/' + t.total, onclick: function () { S.popout('/school/test.html?id=' + t.id + '&result=1', 'schooltest', 1000, 800); } }));
        tests.appendChild(item(t.title, t.subject + ' \u2022 ' + t.dateDMY + ' \u2022 ' + t.durationMinutes + ' min', t.status, b));
      });
      if (!h.tests || !h.tests.length) tests.appendChild(el('p', { class: 'sp-msg', text: 'No tests for you yet.' }));
      var grid = el('div', { class: 'sp-grid' }, [
        el('button', { type: 'button', text: '\uD83D\uDCDA My PDFs', onclick: function () { pdfs(); } }),
        el('button', { type: 'button', text: '\uD83C\uDFA5 Live class' + (h.liveActive ? ' \u25CF' : ''), onclick: function () { S.popout('/school/live.html', 'schoollive', 1180, 820); } }),
        el('button', { type: 'button', text: '\uD83C\uDFAE Games', onclick: function () { games(); } }),
        el('button', { type: 'button', text: '\uD83D\uDD11 Password', onclick: function () { pw(); } })]);
      body.appendChild(el('div', { class: 'sp' }, [header('STUDENT SCREEN', h.name + ' \u2022 Class ' + h.classLevel, j.schoolName, false), el('h2', { text: 'Today & upcoming tests' }), tests, grid,
        el('p', null, [el('button', { type: 'button', class: 'sm ghost', text: 'Log out', onclick: function () { S.logout().then(refresh); } })])]));
    }).catch(function (e) { err(e, 'Student screen'); });
  }

  function sub(title, content) {
    clear(body);
    body.appendChild(el('div', { class: 'sp' }, [el('div', { class: 'sp-head' }, [el('h2', { class: 'gold3d', text: title }),
      el('button', { type: 'button', class: 'sp-x', text: '\u2190 Back', onclick: refresh })]), content]));
  }
  function pdfs() {
    S.api('GET', '/school/api/student/materials').then(function (list) {
      var box = el('div', { class: 'sp-list' });
      list.forEach(function (m) {
        box.appendChild(item(m.title, m.subject + (m.targetStudentId ? ' \u2022 for you' : ' \u2022 class ' + m.classLevel) + ' \u2022 ' + S.fmtBytes(m.sizeBytes), null, [
          el('button', { type: 'button', class: 'sm', text: 'Open', onclick: function () { S.popout('/school/files/material/' + m.id, 'schoolpdf', 1000, 800); } }),
          el('button', { type: 'button', class: 'sm', text: '\uD83D\uDD0A Listen & translate', title: 'Loads this PDF into the reader: text, JSON, translation and the South Indian voices', onclick: function () { listen(m); } }),
          el('a', { class: 'sm', href: '/school/files/material/' + m.id + '?download=true', text: '\u2B07', title: 'Download' })]));
      });
      if (!list.length) box.appendChild(el('p', { class: 'sp-msg', text: 'No PDFs yet.' }));
      sub('My PDFs', box);
    }).catch(function (e) { err(e, 'Load my PDFs'); });
  }
  /* the school PDF goes through the existing reader: Analyze -> text preview -> translate -> voice -> JSON */
  function listen(m) {
    root.fetch('/school/files/material/' + m.id, { credentials: 'same-origin' }).then(function (r) { if (!r.ok) throw new Error('HTTP ' + r.status); return r.blob(); }).then(function (b) {
      var f = new root.File([b], m.fileName || 'school.pdf', { type: 'application/pdf' });
      if (root.WN_loadFile) { root.WN_loadFile(f); open(false); } else throw new Error('The reader is not ready');
    }).catch(function (e) { err(e, 'Listen & translate'); });
  }
  var GAMES = [['math-sprint', '\u2795 Math Sprint', '60 seconds of quick sums'], ['memory-match', '\uD83E\uDDE0 Memory Match', 'Match the pairs'], ['word-unscramble', '\uD83D\uDD24 Word Unscramble', 'Fix the jumbled words'], ['quiz-blitz', '\u26A1 Quiz Blitz', 'Fast questions, lifelines']];
  function games() {
    var box = el('div', { class: 'sp-list' });
    GAMES.forEach(function (g) { box.appendChild(item(g[1], g[2], null, [el('button', { type: 'button', class: 'sm', text: 'Play', onclick: function () { S.popout('/school/games/' + g[0] + '.html', 'game-' + g[0], 980, 820); } })])); });
    sub('Education games', box);
  }
  function pw() {
    var o = el('input', { type: 'password', placeholder: 'Current password' }), n = el('input', { type: 'password', placeholder: 'New password (6+ characters)' });
    sub('Change password', el('div', null, [o, n, el('p', null, [el('button', { type: 'button', text: 'Save', onclick: function () {
      S.api('POST', '/school/api/auth/change-password', { oldPassword: o.value, newPassword: n.value }).then(refresh).catch(function (e) { err(e, 'Change password'); }); } })])]));
  }

  function adminView(j) {
    S.api('GET', '/school/api/admin/overview').then(function (o) {
      clear(body);
      var go = function (tab) { return function () { S.popout('/school/admin.html#' + tab, 'schooladmin', 1240, 860); }; };
      body.appendChild(el('div', { class: 'sp' }, [header('ADMIN SCREEN', j.name, o.schoolName, true),
        el('p', { text: o.students + ' students \u2022 ' + o.materials + ' PDFs \u2022 ' + o.testsToday + ' tests today \u2022 ' + o.liveClasses + ' live class(es)' }),
        el('div', { class: 'sp-grid' }, [el('button', { type: 'button', text: '\uD83D\uDC69\u200D\uD83C\uDF93 Students', onclick: go('students') }), el('button', { type: 'button', text: '\uD83D\uDCC4 PDFs', onclick: go('pdfs') }),
          el('button', { type: 'button', text: '\uD83D\uDCDD Tests', onclick: go('tests') }), el('button', { type: 'button', text: '\uD83C\uDFA5 Live class', onclick: go('live') })]),
        el('p', null, [el('button', { type: 'button', text: '\u29C9 Open Admin Console', onclick: go('students') }), ' ', el('button', { type: 'button', class: 'sm', text: '\uD83D\uDD11 Password', onclick: pw }), ' ',
          el('button', { type: 'button', class: 'sm', text: 'Log out', onclick: function () { S.logout().then(refresh); } })])]));
    }).catch(function (e) { err(e, 'Admin screen'); });
  }

  function refresh() {
    if (!body) return;
    notice.hidden = true;
    S.loadMe().then(function (j) {
      if (!j.loggedIn) return loginView(j);
      if (j.mustChangePassword) { pw(); notice.hidden = false; notice.className = 'notice info'; notice.textContent = 'Please set your own password now.'; return; }
      return j.role === 'ADMIN' ? adminView(j) : studentView(j);
    }).catch(function (e) { err(e, 'Open the School panel'); });
  }

  function init() {
    dock = doc.getElementById('schoolPanel');
    if (!dock) return;
    notice = el('div', { class: 'notice', id: 'schoolNotice', hidden: true }); body = el('div', { id: 'schoolBody' });
    dock.appendChild(notice); dock.appendChild(body);
    S.onLoggedOut = refresh;
    var tab = doc.getElementById('schoolTab'); if (tab) tab.addEventListener('click', function () { open(true); });
  }
  root.SchoolPanel = { open: open, refresh: refresh };
  if (doc.readyState === 'loading') doc.addEventListener('DOMContentLoaded', init); else init();
})(window);
