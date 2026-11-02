/* Admin console (pop-out): students, PDFs, tests (builder -> Test<DD-MM-YYYY>.properties + .json), live class, results. */
(function (root) {
  'use strict';
  var S = root.School, el = S.el, doc = root.document, view = doc.getElementById('view'), tabsBox = doc.getElementById('tabs');
  var SUBJECTS = ['Maths', 'Science', 'Social Science', 'English', 'Hindi', 'Kannada', 'EVS', 'Computer', 'General Knowledge'];
  var TABS = [['students', 'Students'], ['pdfs', 'PDFs'], ['tests', 'Tests'], ['live', 'Live class'], ['results', 'Results']];
  var students = [], materials = [], current = 'students';

  function err(e, op) { S.showError(e, op); }
  function clear(n) { while (n.firstChild) n.removeChild(n.firstChild); }
  function classSelect(sel, withAll, onchange) {
    var s = el('select', { onchange: onchange });
    if (withAll) s.appendChild(el('option', { value: '', text: 'All classes' }));
    for (var i = 1; i <= 10; i++) s.appendChild(el('option', { value: String(i), text: 'Class ' + i, selected: sel === i }));
    return s;
  }
  function field(label, input) { return el('label', null, [label, input]); }
  function datalist(id, items) { return el('datalist', { id: id }, items.map(function (x) { return el('option', { value: x }); })); }
  function loadLists() {
    return Promise.all([S.api('GET', '/school/api/admin/students'), S.api('GET', '/school/api/admin/materials')]).then(function (r) { students = r[0]; materials = r[1]; });
  }

  // ------------------------------------------------------------------------------------------------- students
  function credsBox(list) {
    return el('div', { class: 'creds' }, [el('b', { text: 'Note these now - passwords are shown only once' })].concat(list.map(function (c) {
      return el('div', { text: c.username + (c.fullName ? ' (' + c.fullName + (c.classLevel ? ', class ' + c.classLevel : '') + ')' : '') + '   password: ' + c.password });
    })).concat([el('button', { type: 'button', class: 'sm', text: 'Copy', onclick: function () { root.navigator.clipboard && root.navigator.clipboard.writeText(list.map(function (c) { return c.username + '\t' + c.password; }).join('\n')); } })]));
  }
  function studentsTab(filter, created) {
    clear(view);
    var box = el('div');
    if (created) box.appendChild(credsBox(created));
    var un = el('input', { type: 'text', placeholder: 'e.g. anita7a' }), nm = el('input', { type: 'text', placeholder: 'Full name' }), cl = classSelect(1), rn = el('input', { type: 'text', placeholder: 'Roll no' }), pw = el('input', { type: 'text', placeholder: 'Leave empty = auto password' });
    box.appendChild(el('div', { class: 'card' }, [el('h2', { text: 'Add a student' }), el('div', { class: 'row' }, [field('User name', un), field('Full name', nm), field('Class', cl), field('Roll no', rn), field('Password', pw),
      el('button', { type: 'button', text: 'Add', onclick: function () {
        S.api('POST', '/school/api/admin/students', { username: un.value, fullName: nm.value, classLevel: +cl.value, rollNo: rn.value, password: pw.value })
          .then(function (r) { return loadLists().then(function () { studentsTab(filter, r.created); }); }).catch(function (e) { err(e, 'Add student'); }); } })])]));
    var bulk = el('textarea', { rows: '4', placeholder: 'username,Full Name,class,roll   (one student per line)\nanita7a,Anita Rao,7,12\nravi7a,Ravi K,7,13' });
    box.appendChild(el('div', { class: 'card' }, [el('h2', { text: 'Add many students at once' }), bulk, el('p', null, [el('button', { type: 'button', text: 'Add all', onclick: function () {
      S.api('POST', '/school/api/admin/students/bulk', { lines: bulk.value }).then(function (r) {
        return loadLists().then(function () { studentsTab(filter, r.created); if (r.problems && r.problems.length) err({ message: r.problems.join(' | '), status: 400, json: { code: 'VALIDATION' } }, 'Add many students'); });
      }).catch(function (e) { err(e, 'Add many students'); }); } })])]));
    var rows = students.filter(function (s) { return !filter || s.classLevel === filter; });
    var table = el('table', { class: 't' }, [el('tr', null, ['Class', 'Roll', 'Name', 'User name', 'Status', ''].map(function (h) { return el('th', { text: h }); }))]);
    rows.forEach(function (s) {
      table.appendChild(el('tr', null, [el('td', { text: String(s.classLevel) }), el('td', { text: s.rollNo || '' }), el('td', { text: s.fullName }), el('td', { text: s.username }), el('td', { text: s.active ? 'Active' : 'Switched off' }),
        el('td', null, [el('div', { class: 'row' }, [
          el('button', { type: 'button', class: 'sm', text: 'New password', onclick: function () { S.api('POST', '/school/api/admin/students/' + s.id + '/reset').then(function (r) { studentsTab(filter, r.created); }).catch(function (e) { err(e, 'Reset password'); }); } }),
          el('button', { type: 'button', class: 'sm ghost2', text: s.active ? 'Switch off' : 'Switch on', onclick: function () { S.api('POST', '/school/api/admin/students/' + s.id + '/active', { active: !s.active }).then(function () { return loadLists(); }).then(function () { studentsTab(filter); }).catch(function (e) { err(e, 'Switch account'); }); } }),
          el('button', { type: 'button', class: 'sm danger', text: 'Delete', onclick: function () { if (root.confirm('Delete ' + s.fullName + ' with all results?')) S.api('DELETE', '/school/api/admin/students/' + s.id).then(function () { return loadLists(); }).then(function () { studentsTab(filter); }).catch(function (e) { err(e, 'Delete student'); }); } })])])]));
    });
    box.appendChild(el('div', { class: 'card' }, [el('h2', { text: 'Students (' + rows.length + ')' }), el('div', { class: 'row' }, [field('Show', classSelect(filter || null, true, function (e) { studentsTab(e.target.value ? +e.target.value : null); }))]), table]));
    view.appendChild(box);
  }

  // ------------------------------------------------------------------------------------------------- PDFs
  function pdfsTab() {
    clear(view);
    var file = el('input', { type: 'file', accept: 'application/pdf,.pdf' }), subject = el('input', { type: 'text', list: 'subs', placeholder: 'Subject' }), title = el('input', { type: 'text', placeholder: 'Title (optional)' });
    var target = el('select');
    var cl = classSelect(1, false, fillTarget);
    function fillTarget() {
      clear(target); target.appendChild(el('option', { value: '', text: 'Whole class' }));
      students.filter(function (s) { return s.classLevel === +cl.value; }).forEach(function (s) { target.appendChild(el('option', { value: String(s.id), text: 'Only ' + s.fullName })); });
    }
    fillTarget();
    var up = el('div', { class: 'card' }, [el('h2', { text: 'Upload a subject PDF (max 25 MB)' }), datalist('subs', SUBJECTS), el('div', { class: 'row' }, [field('PDF file', file), field('Class', cl), field('Subject', subject), field('Title', title), field('For', target),
      el('button', { type: 'button', text: 'Upload', onclick: function () {
        if (!file.files[0]) { err({ message: 'No PDF chosen', status: 400, json: { code: 'MISSING_FILE' } }, 'Upload PDF'); return; }
        var fd = new root.FormData(); fd.append('file', file.files[0]); fd.append('classLevel', cl.value); fd.append('subject', subject.value); fd.append('title', title.value); if (target.value) fd.append('targetStudentId', target.value);
        S.api('POST', '/school/api/admin/materials', fd).then(function () { return loadLists(); }).then(pdfsTab).catch(function (e) { err(e, 'Upload PDF'); }); } })])]);
    var table = el('table', { class: 't' }, [el('tr', null, ['Class', 'Subject', 'Title', 'For', 'Size', ''].map(function (h) { return el('th', { text: h }); }))]);
    materials.forEach(function (m) {
      table.appendChild(el('tr', null, [el('td', { text: String(m.classLevel) }), el('td', { text: m.subject }), el('td', { text: m.title }), el('td', { text: m.targetName ? m.targetName : 'Whole class' }), el('td', { text: S.fmtBytes(m.sizeBytes) }),
        el('td', null, [el('div', { class: 'row' }, [el('button', { type: 'button', class: 'sm', text: 'Open', onclick: function () { S.popout('/school/files/material/' + m.id, 'adminpdf', 1000, 800); } }),
          el('a', { class: 'sm', href: '/school/files/material/' + m.id + '?download=true', text: 'Download' }),
          el('button', { type: 'button', class: 'sm danger', text: 'Delete', onclick: function () { if (root.confirm('Delete this PDF?')) S.api('DELETE', '/school/api/admin/materials/' + m.id).then(function () { return loadLists(); }).then(pdfsTab).catch(function (e) { err(e, 'Delete PDF'); }); } })])])]));
    });
    view.appendChild(up); view.appendChild(el('div', { class: 'card' }, [el('h2', { text: 'PDF library (' + materials.length + ')' }), table]));
  }

  // ------------------------------------------------------------------------------------------------- tests
  function testsTab(saved) {
    clear(view);
    if (saved) view.appendChild(el('div', { class: 'okbox' }, ['Test saved. Files generated: ' + saved.propertiesFile + ' and ' + saved.jsonFile + '  ',
      el('a', { class: 'sm', href: '/school/api/admin/tests/' + saved.id + '/properties', text: '⬇ .properties' }), ' ', el('a', { class: 'sm', href: '/school/api/admin/tests/' + saved.id + '/json', text: '⬇ .json' })]));
    view.appendChild(el('p', null, [el('button', { type: 'button', text: '＋ New test', onclick: function () { builder(null); } })]));
    S.api('GET', '/school/api/admin/tests').then(function (list) {
      var table = el('table', { class: 't' }, [el('tr', null, ['Date', 'Class', 'Subject', 'Title', 'Min', 'File', 'On', ''].map(function (h) { return el('th', { text: h }); }))]);
      list.forEach(function (t) {
        table.appendChild(el('tr', null, [el('td', { text: t.dateDMY }), el('td', { text: String(t.classLevel) }), el('td', { text: t.subject }), el('td', { text: t.title }), el('td', { text: String(t.durationMinutes) }), el('td', { text: t.propertiesFile }),
          el('td', null, [el('input', { type: 'checkbox', checked: t.active, title: 'Students can open it on its date', onchange: function (e) { S.api('POST', '/school/api/admin/tests/' + t.id + '/active', { active: e.target.checked }).catch(function (x) { err(x, 'Switch test'); }); } })]),
          el('td', null, [el('div', { class: 'row' }, [el('button', { type: 'button', class: 'sm', text: 'Edit', onclick: function () { builder(t.id); } }), el('button', { type: 'button', class: 'sm', text: 'Results', onclick: function () { current = 'results'; select('results', t.id); } }),
            el('a', { class: 'sm', href: '/school/api/admin/tests/' + t.id + '/properties', text: '.properties' }), el('a', { class: 'sm', href: '/school/api/admin/tests/' + t.id + '/json', text: '.json' }),
            el('button', { type: 'button', class: 'sm danger', text: 'Delete', onclick: function () { if (root.confirm('Delete this test and its results?')) S.api('DELETE', '/school/api/admin/tests/' + t.id).then(function () { testsTab(); }).catch(function (e) { err(e, 'Delete test'); }); } })])])]));
      });
      view.appendChild(el('div', { class: 'card' }, [el('h2', { text: 'Tests (' + list.length + ')' }), table]));
    }).catch(function (e) { err(e, 'Load tests'); });
  }

  function builder(id) {
    (id ? S.api('GET', '/school/api/admin/tests/' + id) : Promise.resolve(null)).then(function (t) {
      var st = { id: id, title: t ? t.title : '', classLevel: t ? t.classLevel : 1, subject: t ? t.subject : '', date: t ? t.date : S.todayIso(), duration: t ? t.durationMinutes : 30,
        materialId: t ? t.materialId : null, selected: new Set(t ? t.studentIds : []),
        qs: t ? t.questions.map(function (q) { return { text: q.text, options: q.options.slice(), correct: new Set(q.correct), marks: q.marks }; }) : [newQ()] };
      drawBuilder(st);
    }).catch(function (e) { err(e, 'Open test'); });
  }
  function newQ() { return { text: '', options: ['', '', '', ''], correct: new Set(), marks: 1 }; }
  var L = function (i) { return String.fromCharCode(65 + i); };

  function drawBuilder(st) {
    clear(view);
    var title = el('input', { type: 'text', value: st.title, placeholder: 'e.g. Unit Test 1 - Fractions', oninput: function (e) { st.title = e.target.value; } });
    var subject = el('input', { type: 'text', list: 'subs', value: st.subject, placeholder: 'Subject', onchange: function (e) { st.subject = e.target.value; drawBuilder(st); }, oninput: function (e) { st.subject = e.target.value; } });
    var cls = classSelect(st.classLevel, false, function (e) { st.classLevel = +e.target.value; st.materialId = null; drawBuilder(st); });
    var date = el('input', { type: 'date', value: st.date, onchange: function (e) { st.date = e.target.value; } }), dur = el('input', { type: 'number', min: '1', max: '300', value: String(st.duration), onchange: function (e) { st.duration = +e.target.value; } });
    var mats = materials.filter(function (m) { return m.classLevel === st.classLevel && !m.targetStudentId && (!st.subject || m.subject.toLowerCase() === st.subject.toLowerCase()); });
    var matSel = el('select', { onchange: function (e) { st.materialId = e.target.value ? +e.target.value : null; drawBuilder(st); } }, [el('option', { value: '', text: '- none -' })].concat(mats.map(function (m) { return el('option', { value: String(m.id), text: m.title, selected: st.materialId === m.id }); })));
    var head = el('div', { class: 'card' }, [el('h2', { text: st.id ? 'Edit test' : 'New test' }), datalist('subs', SUBJECTS), el('div', { class: 'row' }, [field('Title', title), field('Class', cls), field('Subject', subject), field('Date (the test opens on this day)', date), field('Minutes', dur), field('Subject PDF (to read while composing)', matSel),
      st.materialId ? el('button', { type: 'button', class: 'sm', text: 'View PDF', onclick: function () { S.popout('/school/files/material/' + st.materialId, 'adminpdf', 1000, 800); } }) : null])]);
    var inClass = students.filter(function (s) { return s.classLevel === st.classLevel && s.active; });
    var studs = el('div', { class: 'studs' }, inClass.map(function (s) {
      return el('label', null, [el('input', { type: 'checkbox', checked: st.selected.has(s.id), onchange: function (e) { if (e.target.checked) st.selected.add(s.id); else st.selected.delete(s.id); } }), (s.rollNo ? s.rollNo + '. ' : '') + s.fullName]);
    }));
    var who = el('div', { class: 'card' }, [el('h2', { text: 'Students who get this test (class ' + st.classLevel + ')' }), el('p', null, [el('button', { type: 'button', class: 'sm', text: 'Select all', onclick: function () { inClass.forEach(function (s) { st.selected.add(s.id); }); drawBuilder(st); } }), ' ',
      el('button', { type: 'button', class: 'sm ghost2', text: 'Select none', onclick: function () { inClass.forEach(function (s) { st.selected.delete(s.id); }); drawBuilder(st); } })]), inClass.length ? studs : el('p', { text: 'No active students in this class yet - add them in Students.' })]);
    var qBox = el('div', { class: 'card' }, [el('h2', { text: 'Questions (tick every correct answer - students answer with check boxes)' })]);
    st.qs.forEach(function (q, qi) {
      var card = el('div', { class: 'qcard' }, [el('b', { text: 'Question ' + (qi + 1) }), el('textarea', { rows: '2', value: q.text, placeholder: 'Question text', oninput: function (e) { q.text = e.target.value; } })]);
      q.options.forEach(function (o, oi) {
        card.appendChild(el('div', { class: 'opt' }, [el('span', { class: 'ltr', text: L(oi) }), el('input', { type: 'checkbox', checked: q.correct.has(L(oi)), title: 'Correct answer', onchange: function (e) { if (e.target.checked) q.correct.add(L(oi)); else q.correct.delete(L(oi)); } }),
          el('input', { type: 'text', value: o, placeholder: 'Option ' + L(oi), oninput: function (e) { q.options[oi] = e.target.value; } }),
          q.options.length > 2 ? el('button', { type: 'button', class: 'sm danger', text: '✕', onclick: function () { q.options.splice(oi, 1); q.correct = new Set(Array.from(q.correct).filter(function (l) { return l.charCodeAt(0) - 65 < q.options.length; })); drawBuilder(st); } }) : null]));
      });
      card.appendChild(el('div', { class: 'row' }, [q.options.length < 6 ? el('button', { type: 'button', class: 'sm', text: '＋ Option', onclick: function () { q.options.push(''); drawBuilder(st); } }) : null,
        el('label', null, ['Marks ', el('input', { type: 'number', min: '1', max: '100', value: String(q.marks), style: 'width:70px', onchange: function (e) { q.marks = +e.target.value; } })]),
        el('button', { type: 'button', class: 'sm danger', text: 'Remove question', onclick: function () { st.qs.splice(qi, 1); drawBuilder(st); } })]));
      qBox.appendChild(card);
    });
    qBox.appendChild(el('button', { type: 'button', text: '＋ Add question', onclick: function () { st.qs.push(newQ()); drawBuilder(st); } }));
    var save = el('p', null, [el('button', { type: 'button', text: '💾 Save test & generate Test' + st.date.split('-').reverse().join('-') + '.properties', onclick: function () {
      var body = { id: st.id, title: st.title, classLevel: st.classLevel, subject: st.subject, testDate: st.date, durationMinutes: st.duration, materialId: st.materialId,
        studentIds: Array.from(st.selected).filter(function (sid) { return inClass.some(function (s) { return s.id === sid; }); }),
        questions: st.qs.map(function (q) { return { text: q.text, options: q.options, correct: Array.from(q.correct).sort(), marks: q.marks }; }) };
      S.api('POST', '/school/api/admin/tests', body).then(function (r) { testsTab(r); }).catch(function (e) { err(e, 'Save test'); }); } }), ' ',
      el('button', { type: 'button', class: 'ghost2', text: 'Cancel', onclick: function () { testsTab(); } })]);
    [head, who, qBox, save].forEach(function (n) { view.appendChild(n); });
  }

  // ------------------------------------------------------------------------------------------------- live class
  function liveTab() {
    clear(view);
    var cl = classSelect(1, false, fill), mat = el('select'), title = el('input', { type: 'text', placeholder: 'Lesson title (optional)' });
    function fill() { clear(mat); materials.filter(function (m) { return m.classLevel === +cl.value && !m.targetStudentId; }).forEach(function (m) { mat.appendChild(el('option', { value: String(m.id), text: m.subject + ' - ' + m.title })); }); }
    fill();
    view.appendChild(el('div', { class: 'card' }, [el('h2', { text: 'Start a live class' }), el('div', { class: 'row' }, [field('Class', cl), field('PDF to teach from', mat), field('Title', title),
      el('button', { type: 'button', text: '▶ Start & open control room', onclick: function () {
        if (!mat.value) { err({ message: 'There is no PDF for this class yet', status: 400, json: { code: 'VALIDATION' } }, 'Start live class'); return; }
        S.api('POST', '/school/api/admin/live/start', { classLevel: +cl.value, materialId: +mat.value, title: title.value }).then(function (r) { S.popout('/school/live.html?id=' + r.sessionId, 'schoollive', 1180, 820); liveTab(); }).catch(function (e) { err(e, 'Start live class'); }); } })])]));
    S.api('GET', '/school/api/admin/live').then(function (list) {
      var box = el('div', { class: 'card' }, [el('h2', { text: 'Running now (' + list.length + ')' })]);
      list.forEach(function (s) { box.appendChild(el('div', { class: 'row' }, [el('span', { text: 'Class ' + s.classLevel + ' - ' + s.title + ' (' + s.materialTitle + ', page ' + s.page + ')' }),
        el('button', { type: 'button', class: 'sm', text: 'Control room', onclick: function () { S.popout('/school/live.html?id=' + s.sessionId, 'schoollive', 1180, 820); } }),
        el('button', { type: 'button', class: 'sm danger', text: 'Stop', onclick: function () { S.api('POST', '/school/api/admin/live/' + s.sessionId + '/stop').then(liveTab).catch(function (e) { err(e, 'Stop live class'); }); } })])); });
      view.appendChild(box);
    }).catch(function (e) { err(e, 'Load live classes'); });
  }

  // ------------------------------------------------------------------------------------------------- results
  function resultsTab(testId) {
    clear(view);
    S.api('GET', '/school/api/admin/tests').then(function (list) {
      var sel = el('select', { onchange: function (e) { resultsTab(+e.target.value); } }, [el('option', { value: '', text: '- choose a test -' })].concat(list.map(function (t) { return el('option', { value: String(t.id), text: t.dateDMY + '  ' + t.title + ' (class ' + t.classLevel + ')', selected: t.id === testId }); })));
      view.appendChild(el('div', { class: 'card' }, [el('h2', { text: 'Results' }), field('Test', sel)]));
      if (!testId) return null;
      return S.api('GET', '/school/api/admin/tests/' + testId + '/results').then(function (rows) {
        var table = el('table', { class: 't' }, [el('tr', null, ['Roll', 'Name', 'Status', 'Score', '%'].map(function (h) { return el('th', { text: h }); }))]);
        rows.forEach(function (r) { table.appendChild(el('tr', null, [el('td', { text: r.rollNo || '' }), el('td', { text: r.name }), el('td', { text: r.status.replace('_', ' ') }), el('td', { text: r.score == null ? '' : r.score + '/' + r.total }), el('td', { text: r.percent == null ? '' : r.percent + '%' })])); });
        view.appendChild(el('div', { class: 'card' }, [el('p', null, [el('a', { class: 'sm', href: '/school/api/admin/tests/' + testId + '/results.csv', text: '⬇ Download CSV (Excel)' })]), table]));
      });
    }).catch(function (e) { err(e, 'Load results'); });
  }

  function select(tab, arg) {
    current = tab;
    root.location.hash = tab;
    Array.prototype.forEach.call(tabsBox.children, function (b) { b.classList.toggle('on', b.getAttribute('data-t') === tab); });
    doc.getElementById('notice').hidden = true;
    loadLists().then(function () {
      if (tab === 'students') studentsTab(); else if (tab === 'pdfs') pdfsTab(); else if (tab === 'tests') testsTab(); else if (tab === 'live') liveTab(); else resultsTab(arg);
    }).catch(function (e) { err(e, 'Open ' + tab); });
  }

  function init() {
    S.onLoggedOut = function () { clear(view); err({ status: 401, message: 'Not logged in', json: { code: 'AUTH_REQUIRED' } }, 'Admin Console'); };
    S.loadMe().then(function (me) {
      if (!me.loggedIn || me.role !== 'ADMIN') { err({ status: 401, message: me.loggedIn ? 'This account is not an admin' : 'Not logged in', json: { code: me.loggedIn ? 'WRONG_PORTAL' : 'AUTH_REQUIRED' } }, 'Admin Console'); return; }
      doc.getElementById('schoolName').textContent = me.schoolName;
      doc.getElementById('renameBtn').addEventListener('click', function () {
        var v = root.prompt('School name (only the admin can change it):', doc.getElementById('schoolName').textContent);
        if (v != null) S.api('PUT', '/school/api/admin/school', { schoolName: v }).then(function (r) { doc.getElementById('schoolName').textContent = r.schoolName; }).catch(function (e) { err(e, 'Rename school'); });
      });
      doc.getElementById('logoutBtn').addEventListener('click', function () { S.logout().then(function () { root.close(); }); });
      TABS.forEach(function (t) { tabsBox.appendChild(el('button', { type: 'button', 'data-t': t[0], text: t[1], onclick: function () { select(t[0]); } })); });
      var h = (root.location.hash || '').replace('#', '');
      select(TABS.some(function (t) { return t[0] === h; }) ? h : 'students');
    }).catch(function (e) { err(e, 'Admin Console'); });
  }
  if (doc.readyState === 'loading') doc.addEventListener('DOMContentLoaded', init); else init();
})(window);
