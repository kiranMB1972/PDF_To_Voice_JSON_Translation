/* Live learning session (pop-out). Teacher: opens a PDF for a class and moves the shared page; students follow it automatically
   and can ask questions in the chat. Updates are polled every 2 seconds. */
(function (root) {
  'use strict';
  var S = root.School, el = S.el, doc = root.document, body = doc.getElementById('body'), P = S.params(), me = null;
  var frame, chat, pageBox, lastPage = null, lastMat = null, sid = null, timer = 0, lastMsgCount = -1;

  function err(e, op) { S.showError(e, op); }
  function stateUrl() { return me.role === 'ADMIN' ? '/school/api/admin/live' : '/school/api/student/live/state'; }

  function draw(s) {
    doc.getElementById('liveTitle').textContent = s.title + ' (class ' + s.classLevel + ')';
    sid = s.sessionId;
    if (pageBox && me.role === 'ADMIN') pageBox.value = s.page;
    if (s.materialId !== lastMat || s.page !== lastPage) {                 // a new ?t= forces the PDF viewer to jump to the page
      frame.src = '/school/files/material/' + s.materialId + '?t=' + Date.now() + '#page=' + s.page;
      lastMat = s.materialId; lastPage = s.page;
    }
    if (s.messages.length !== lastMsgCount) {
      lastMsgCount = s.messages.length;
      while (chat.firstChild) chat.removeChild(chat.firstChild);
      s.messages.forEach(function (m) { chat.appendChild(el('p', { class: m.role === 'ADMIN' ? 'teacher' : '' }, [el('b', { text: m.who + ': ' }), m.text])); });
      chat.scrollTop = chat.scrollHeight;
    }
  }
  function poll() {
    S.api('GET', stateUrl()).then(function (j) {
      var s = me.role === 'ADMIN' ? (j || []).filter(function (x) { return String(x.sessionId) === String(P.id); })[0] : j;
      if (!s || s.active === false) { frame.removeAttribute('src'); lastMat = null; lastPage = null; doc.getElementById('liveTitle').textContent = me.role === 'ADMIN' ? 'ended' : 'waiting for the teacher to start...'; return; }
      draw(s);
    }).catch(function (e) { root.clearInterval(timer); err(e, 'Live class'); });
  }

  function build() {
    frame = el('iframe', { class: 'pdfframe', title: 'Lesson PDF' });
    chat = el('div', { class: 'chat' });
    var text = el('input', { type: 'text', placeholder: me.role === 'ADMIN' ? 'Write to the class...' : 'Ask a question...', maxlength: '600' });
    var send = function () {
      if (!text.value.trim()) return;
      var url = me.role === 'ADMIN' ? '/school/api/admin/live/' + P.id + '/message' : '/school/api/student/live/ask';
      S.api('POST', url, { text: text.value }).then(function () { text.value = ''; poll(); }).catch(function (e) { err(e, 'Send message'); });
    };
    text.addEventListener('keydown', function (e) { if (e.key === 'Enter') send(); });
    var side = el('div', { class: 'card' }, [el('h2', { text: 'Questions & chat' }), chat, el('div', { class: 'row' }, [text, el('button', { type: 'button', class: 'sm', text: 'Send', onclick: send })])]);
    if (me.role === 'ADMIN') {
      pageBox = el('input', { type: 'number', min: '1', style: 'width:90px', value: '1' });
      var go = function (n) { S.api('POST', '/school/api/admin/live/' + P.id + '/page', { page: Math.max(1, n) }).then(poll).catch(function (e) { err(e, 'Change page'); }); };
      var c = doc.getElementById('controls');
      c.appendChild(el('button', { type: 'button', class: 'sm', text: '◀ Prev', onclick: function () { go((+pageBox.value || 1) - 1); } })); c.appendChild(doc.createTextNode(' Page '));
      c.appendChild(pageBox); c.appendChild(el('button', { type: 'button', class: 'sm', text: 'Go', onclick: function () { go(+pageBox.value || 1); } }));
      c.appendChild(el('button', { type: 'button', class: 'sm', text: 'Next ▶', onclick: function () { go((+pageBox.value || 1) + 1); } }));
      c.appendChild(el('button', { type: 'button', class: 'sm danger', text: 'End class', onclick: function () { S.api('POST', '/school/api/admin/live/' + P.id + '/stop').then(poll); } }));
    }
    body.appendChild(el('div', { class: 'row', style: 'align-items:flex-start' }, [el('div', { style: 'flex:2 1 560px' }, [frame]), el('div', { style: 'flex:1 1 280px' }, [side])]));
    doc.getElementById('role').textContent = me.role === 'ADMIN' ? 'ADMIN SCREEN - LIVE CLASS' : 'STUDENT SCREEN - LIVE CLASS';
    poll(); timer = root.setInterval(poll, 2000);
  }

  function init() {
    S.loadMe().then(function (j) {
      me = j;
      if (!j.loggedIn) { err({ status: 401, message: 'Not logged in', json: { code: 'AUTH_REQUIRED' } }, 'Live class'); return; }
      if (j.role === 'ADMIN' && !P.id) { err({ status: 400, message: 'Open this screen from Admin > Live class', json: { code: 'BAD_REQUEST' } }, 'Live class'); return; }
      build();
    }).catch(function (e) { err(e, 'Live class'); });
  }
  if (doc.readyState === 'loading') doc.addEventListener('DOMContentLoaded', init); else init();
})(window);
