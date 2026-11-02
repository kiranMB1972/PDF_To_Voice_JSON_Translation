const test = require('node:test');
const assert = require('node:assert');
const G = require('../src/main/resources/static/school/games/edu-games.js');

function evalExpr(q) {   // independent check of the displayed question text
  let t = q.text.replace(/\u2212/g, '-').replace(/\u00D7/g, '*').replace(/\u00F7/g, '/').replace(/\u00B2/g, '**2').replace(/\^/g, '**');
  let m;
  if ((m = /^(\d+)% of (\d+)$/.exec(t))) return +m[1] * +m[2] / 100;
  if ((m = /^\u221A(\d+)$/.exec(t))) return Math.sqrt(+m[1]);
  if ((m = /^x \+ (\d+) = (-?\d+),\s+x = \?$/.exec(t))) return +m[2] - +m[1];
  if ((m = /^(\d+)x \+ (\d+) = (-?\d+),\s+x = \?$/.exec(t))) return (+m[3] - +m[2]) / +m[1];
  return Function('"use strict"; return (' + t + ')')();
}

test('class bands: 1-2 k, 3-4 a, 5-6 b, 7-8 c, 9-10 d', () => {
  assert.deepStrictEqual([1, 2, 3, 4, 5, 6, 7, 8, 9, 10].map(G.band), ['k', 'k', 'a', 'a', 'b', 'b', 'c', 'c', 'd', 'd']);
  assert.strictEqual(G.band(0), 'k'); assert.strictEqual(G.band(99), 'd');
});

test('every generated sum has the right answer, 4 distinct choices that include it, for every class and many seeds', () => {
  for (let level = 1; level <= 10; level++) {
    const rnd = G.rng(level * 7919);
    for (let i = 0; i < 400; i++) {
      const q = G.mathQuestion(level, rnd);
      assert.strictEqual(evalExpr(q), q.answer, `class ${level}: ${q.text}`);
      assert.strictEqual(q.choices.length, 4, q.text); assert.strictEqual(new Set(q.choices).size, 4, q.text);
      assert.ok(q.choices.includes(q.answer), q.text);
      assert.ok(Number.isInteger(q.answer), q.text);
    }
  }
});

test('difficulty grows with the class: class 1 stays within 20, class 3 uses times tables', () => {
  const r1 = G.rng(1); for (let i = 0; i < 300; i++) { const q = G.mathQuestion(1, r1); assert.ok(q.answer >= 0 && q.answer <= 20, q.text); }
  const r3 = G.rng(2); const texts = new Set(); for (let i = 0; i < 300; i++) texts.add(G.mathQuestion(3, r3).text.includes('\u00D7'));
  assert.ok(texts.has(true));
});

test('streak bonus is capped', () => { assert.strictEqual(G.mathPoints(0), 10); assert.strictEqual(G.mathPoints(3), 16); assert.strictEqual(G.mathPoints(50), 30); });

test('scramble never returns the word itself (unless it cannot change) and keeps the same letters', () => {
  const rnd = G.rng(5);
  for (let level = 1; level <= 10; level++) G.wordList(level).forEach(w => {
    for (let i = 0; i < 20; i++) { const s = G.scramble(w, rnd); assert.notStrictEqual(s, w, w); assert.strictEqual(s.split('').sort().join(''), w.split('').sort().join('')); }
  });
  assert.strictEqual(G.scramble('a'), 'a'); assert.strictEqual(G.scramble('aaa'), 'aaa');
});

test('memory pairs: right number per class, ids unique, meanings unique so a match is never ambiguous', () => {
  const want = { 1: 6, 3: 8, 5: 8, 7: 10, 9: 10 };   // class 9-10 list has 12 entries but "1" repeats, so at most 12 and at least 10 stay unique
  for (const level of [1, 3, 5, 7, 9]) {
    const p = G.memoryPairs(level, G.rng(level));
    assert.ok(p.length >= want[level] - 2 && p.length <= 12, `class ${level}: ${p.length}`);
    assert.strictEqual(new Set(p.map(x => x.b)).size, p.length); assert.strictEqual(new Set(p.map(x => x.id)).size, p.length);
    p.forEach(x => { assert.ok(x.a && x.b); });
  }
});

test('quiz bank is valid: 4 options, answer in range, no duplicate options, every band present', () => {
  const bands = new Set();
  G.BANK.forEach(([q, o, a, subj, b]) => { assert.strictEqual(o.length, 4, q); assert.ok(a >= 0 && a < 4, q); assert.strictEqual(new Set(o).size, 4, q); assert.ok(subj && b); bands.add(b); });
  assert.deepStrictEqual([...bands].sort(), ['a', 'b', 'c', 'd', 'k']);
});

test('quizQuestions keeps the right answer after shuffling and uses the class band +/- 1', () => {
  for (let level = 1; level <= 10; level++) {
    const qs = G.quizQuestions(level, 8, G.rng(level + 100));
    assert.ok(qs.length >= 6, 'class ' + level);
    qs.forEach(q => {
      const orig = G.BANK.find(b => b[0] === q.q);
      assert.strictEqual(q.options[q.answer], orig[1][orig[2]], q.q);
      const order = ['k', 'a', 'b', 'c', 'd'], i = order.indexOf(G.band(level));
      assert.ok(Math.abs(order.indexOf(q.band) - i) <= 1, `${q.q} band ${q.band} for class ${level}`);
    });
  }
  assert.ok(G.quizPoints(10, 3) > G.quizPoints(0, 0));
});

test('the shown question has exactly one question mark (no "x = ? = ?")', () => {
  for (let level = 1; level <= 10; level++) { const rnd = G.rng(level); for (let i = 0; i < 300; i++) { const q = G.mathQuestion(level, rnd); assert.strictEqual((q.display.match(/\?/g) || []).length, 1, q.display); } }
});
