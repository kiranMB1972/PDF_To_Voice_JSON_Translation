const test = require('node:test');
const assert = require('node:assert');
const { JSDOM } = require('jsdom');
const fs = require('fs'), path = require('path');
const SRC = fs.readFileSync(path.join(__dirname, '../src/main/resources/static/js/visual-errors.js'), 'utf8');

function setup() {
  const dom = new JSDOM('<div id="n" hidden></div>', { runScripts: 'outside-only' });
  dom.window.eval(SRC);
  return { w: dom.window, box: dom.window.document.getElementById('n'), VE: dom.window.VisualErrors };
}
const GUIDE = { code: 'NO_VOICE', category: 'VOICE', emoji: '\uD83D\uDD08', headline: 'No voice could speak', reason: 'No voice.', steps: ['Open Edge.', 'Press Voice check.'], tip: 'Edge carries free voices.', color1: '#f7971e', color2: '#ff512f' };

test('a problem is shown as a coloured card: emoji, reason, exact message, steps, idea, reference and what the person was doing', () => {
  const { box, VE } = setup();
  VE.render(box, { sev: 'warning', code: 'NO_VOICE', title: 'The voice could not speak', message: 'No voice for mr-IN', hint: 'Use Edge', op: 'Play / Pause', guide: GUIDE, ctx: { selection: 'Marathi / standard', lang: 'Marathi', langCode: 'mr' } });
  const t = box.textContent;
  assert.ok(!box.hidden);
  assert.match(box.className, /notice vx warning vx-voice/);
  assert.strictEqual(box.style.getPropertyValue('--c1'), '#f7971e');
  assert.match(t, /The voice could not speak/); assert.match(t, /No voice for mr-IN/); assert.match(t, /Doing: Play \/ Pause/); assert.match(t, /Selection: Marathi/);
  assert.strictEqual(box.querySelectorAll('.vx-steps li').length, 2);
  assert.match(t, /Aarohi or Manohar/, 'the tip is made for the chosen language');
  assert.match(t, /NO_VOICE/);
});

test('information is compact (no step list) and the text is never parsed as HTML', () => {
  const { box, VE } = setup();
  VE.render(box, { sev: 'info', code: 'TRANSLATION_OK', title: 'Works', message: '<img src=x onerror=alert(1)>', guide: Object.assign({}, GUIDE, { code: 'TRANSLATION_OK', category: 'INFO' }) });
  assert.strictEqual(box.querySelectorAll('.vx-steps').length, 0);
  assert.strictEqual(box.querySelectorAll('img').length, 0);
  assert.match(box.textContent, /<img src=x/);
});

test('without the server catalogue a built-in card still explains NETWORK failures; unknown codes fall back to a generic card', () => {
  const { box, VE } = setup();
  VE.render(box, { sev: 'error', code: 'NETWORK', op: 'Analyze PDF', ctx: { fileMb: 80 } });
  assert.match(box.textContent, /The app cannot be reached/); assert.match(box.textContent, /Application_run\.bat/); assert.match(box.textContent, /"Analyze PDF" action did not reach the server/);
  VE.render(box, { sev: 'error', code: 'SOMETHING_NEW' });
  assert.match(box.textContent, /Something went wrong/);
});

test('instance tips use the situation: file size, page number, chosen language', () => {
  const { VE } = setup();
  assert.match(VE.instanceTip('FILE_TOO_LARGE', { fileMb: 130 }), /130 MB.*4 parts/);
  assert.match(VE.instanceTip('NO_TEXT', { page: 7 }), /Page 7 is a picture/);
  assert.match(VE.instanceTip('TRANSLATION_FAILED', { lang: 'Hindi', op: 'Translate JSON & speak' }), /Hindi.*Translate JSON & speak.*Test translation/);
  assert.strictEqual(VE.instanceTip('NO_VOICE', {}), null);
});

test('dismiss hides the card', () => {
  const { box, VE } = setup();
  VE.render(box, { sev: 'error', code: 'NETWORK' });
  box.querySelector('.vx-close').click();
  assert.ok(box.hidden);
});

test('load() reads the server catalogue once and render prefers it', async () => {
  const { w, box, VE } = setup();
  let calls = 0; w.fetch = () => { calls++; return Promise.resolve({ ok: true, json: () => Promise.resolve({ NO_VOICE: GUIDE }) }); };
  await VE.load(); await VE.load();
  assert.strictEqual(calls, 1);
  VE.render(box, { sev: 'error', code: 'NO_VOICE' });
  assert.match(box.textContent, /No voice could speak/);
});
