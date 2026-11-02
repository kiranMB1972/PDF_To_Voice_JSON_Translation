/* Education game logic for classes 1-10 (pure functions, no DOM - unit tested in js-tests/edu-games.test.js).
   Bands: 1-2 = k, 3-4 = a, 5-6 = b, 7-8 = c, 9-10 = d. */
(function (root) {
  'use strict';
  function rng(seed) { var a = seed >>> 0; return function () { a |= 0; a = a + 0x6D2B79F5 | 0; var t = Math.imul(a ^ a >>> 15, 1 | a); t = t + Math.imul(t ^ t >>> 7, 61 | t) ^ t; return ((t ^ t >>> 14) >>> 0) / 4294967296; }; }
  function rint(rnd, lo, hi) { return lo + Math.floor(rnd() * (hi - lo + 1)); }
  function pick(rnd, arr) { return arr[Math.floor(rnd() * arr.length)]; }
  function shuffle(rnd, arr) { var a = arr.slice(); for (var i = a.length - 1; i > 0; i--) { var j = Math.floor(rnd() * (i + 1)); var t = a[i]; a[i] = a[j]; a[j] = t; } return a; }
  function band(level) { level = Math.max(1, Math.min(10, level | 0)); return level <= 2 ? 'k' : level <= 4 ? 'a' : level <= 6 ? 'b' : level <= 8 ? 'c' : 'd'; }

  /* ---- Math Sprint ---- */
  function mathQuestion(level, rnd) {
    rnd = rnd || Math.random;
    var b = band(level), text, ans, a, c, d;
    if (b === 'k') {
      var max = level === 1 ? 10 : 20; a = rint(rnd, 1, max); c = rint(rnd, 1, max);
      if (rnd() < 0.5) { text = a + ' + ' + c; ans = a + c; } else { if (c > a) { d = a; a = c; c = d; } text = a + ' \u2212 ' + c; ans = a - c; }
    } else if (b === 'a') {
      var k = rint(rnd, 0, 2);
      if (k === 0) { a = rint(rnd, 2, 10); c = rint(rnd, 2, 10); text = a + ' \u00D7 ' + c; ans = a * c; }
      else if (k === 1) { a = rint(rnd, 10, 99); c = rint(rnd, 10, 99); text = a + ' + ' + c; ans = a + c; }
      else { a = rint(rnd, 30, 99); c = rint(rnd, 10, a - 1); text = a + ' \u2212 ' + c; ans = a - c; }
    } else if (b === 'b') {
      var m = rint(rnd, 0, 3);
      if (m === 0) { a = rint(rnd, 12, 99); c = rint(rnd, 2, 9); text = a + ' \u00D7 ' + c; ans = a * c; }
      else if (m === 1) { c = rint(rnd, 2, 12); ans = rint(rnd, 2, 12); a = c * ans; text = a + ' \u00F7 ' + c; }
      else if (m === 2) { d = pick(rnd, [10, 20, 25, 50]); a = rint(rnd, 1, 10) * 20; text = d + '% of ' + a; ans = a * d / 100; }
      else { a = rint(rnd, 100, 999); c = rint(rnd, 100, 999); text = a + ' + ' + c; ans = a + c; }
    } else if (b === 'c') {
      var n = rint(rnd, 0, 3);
      if (n === 0) { a = rint(rnd, 2, 15); text = a + '\u00B2'; ans = a * a; }
      else if (n === 1) { a = rint(rnd, -20, 20); c = rint(rnd, -20, 20); text = '(' + a + ') + (' + c + ')'; ans = a + c; }
      else if (n === 2) { ans = rint(rnd, -9, 15); c = rint(rnd, 2, 30); text = 'x + ' + c + ' = ' + (ans + c) + ',  x = ?'; }
      else { d = pick(rnd, [10, 15, 20, 25, 30, 40, 60, 75]); a = rint(rnd, 2, 20) * 20; text = d + '% of ' + a; ans = a * d / 100; }
    } else {
      var p = rint(rnd, 0, 3);
      if (p === 0) { a = rint(rnd, 2, 9); ans = rint(rnd, -6, 12); c = rint(rnd, 1, 20); text = a + 'x + ' + c + ' = ' + (a * ans + c) + ',  x = ?'; }
      else if (p === 1) { ans = rint(rnd, 4, 25); text = '\u221A' + (ans * ans); }
      else if (p === 2) { a = rint(rnd, 2, 5); c = rint(rnd, 2, 5); text = a + '^' + c; ans = Math.pow(a, c); }
      else { a = rint(rnd, 2, 9); c = rint(rnd, 2, 9); text = '(' + a + ' + ' + c + ')\u00B2 \u2212 ' + a + '\u00B2 \u2212 ' + c + '\u00B2'; ans = 2 * a * c; }
    }
    return { text: text, display: text.indexOf('?') >= 0 ? text : text + ' = ?', answer: ans, choices: choices(rnd, ans), band: b };
  }
  function choices(rnd, ans) {
    var set = [ans], spread = Math.max(3, Math.round(Math.abs(ans) * 0.15)), guard = 0;
    while (set.length < 4 && guard++ < 200) { var v = ans + rint(rnd, -spread, spread); if (v !== ans && set.indexOf(v) < 0 && (ans < 0 || v >= 0 || ans === 0 && v >= 0)) set.push(v); if (guard > 100) spread++; }
    return shuffle(rnd, set);
  }
  function mathPoints(streak) { return 10 + Math.min(streak, 10) * 2; }       // streak bonus: +2 per answer in a row, up to +20

  /* ---- Word Unscramble ---- */
  var WORDS = {
    k: ['cat', 'dog', 'sun', 'bus', 'pen', 'cow', 'hen', 'fish', 'tree', 'milk', 'book', 'ball'],
    a: ['planet', 'flower', 'school', 'rabbit', 'garden', 'monkey', 'orange', 'window', 'river', 'forest', 'animal', 'friend'],
    b: ['gravity', 'triangle', 'climate', 'country', 'compass', 'harvest', 'monsoon', 'volcano', 'mineral', 'islands', 'history', 'science'],
    c: ['photosynthesis', 'democracy', 'molecule', 'equation', 'parallel', 'civilization', 'electricity', 'perimeter', 'continent', 'ecosystem', 'fraction', 'adjective'],
    d: ['chromosome', 'trigonometry', 'constitution', 'metabolism', 'revolution', 'hypotenuse', 'magnetism', 'atmosphere', 'polynomial', 'democracy', 'inflation', 'biodiversity']
  };
  function wordList(level) { return WORDS[band(level)].slice(); }
  function scramble(word, rnd) {
    rnd = rnd || Math.random;
    var distinct = new Set(word.split('')).size;
    if (word.length < 2 || distinct < 2) return word;
    var s, guard = 0;
    do { s = shuffle(rnd, word.split('')).join(''); } while (s === word && guard++ < 50);
    if (s === word) s = word.slice(1) + word[0];
    return s;
  }

  /* ---- Memory Match: term <-> meaning ---- */
  var PAIRS = {
    k: [['\uD83D\uDC36', 'Dog'], ['\uD83D\uDC31', 'Cat'], ['\uD83D\uDC2E', 'Cow'], ['\uD83C\uDF4E', 'Apple'], ['\u2600\uFE0F', 'Sun'], ['\uD83C\uDF19', 'Moon'], ['\uD83D\uDE8C', 'Bus'], ['\uD83C\uDF33', 'Tree']],
    a: [['2 \u00D7 6', '12'], ['5 \u00D7 5', '25'], ['3 \u00D7 9', '27'], ['4 \u00D7 8', '32'], ['7 \u00D7 7', '49'], ['6 \u00D7 8', '48'], ['9 \u00D7 9', '81'], ['10 \u00D7 7', '70']],
    b: [['India', 'New Delhi'], ['Karnataka', 'Bengaluru'], ['Tamil Nadu', 'Chennai'], ['Kerala', 'Thiruvananthapuram'], ['Telangana', 'Hyderabad'], ['Maharashtra', 'Mumbai'], ['Rajasthan', 'Jaipur'], ['Gujarat', 'Gandhinagar']],
    c: [['H\u2082O', 'Water'], ['NaCl', 'Salt'], ['CO\u2082', 'Carbon dioxide'], ['Fe', 'Iron'], ['Au', 'Gold'], ['O\u2082', 'Oxygen'], ['Na', 'Sodium'], ['Ag', 'Silver'], ['Cu', 'Copper'], ['He', 'Helium']],
    d: [['\u03C0', '3.14159...'], ['sin 90\u00B0', '1'], ['cos 0\u00B0', '1'], ['\u221A144', '12'], ['2\u2075', '32'], ['log\u2081\u2080 1000', '3'], ['5!', '120'], ['tan 45\u00B0', '1 (angle)'], ['a\u00B2 \u2212 b\u00B2', '(a+b)(a\u2212b)'], ['Speed unit', 'm/s'], ['Force unit', 'Newton'], ['Work unit', 'Joule']]
  };
  function memoryPairs(level, rnd) {
    rnd = rnd || Math.random;
    var b = band(level), n = { k: 6, a: 8, b: 8, c: 10, d: 12 }[b], list = shuffle(rnd, PAIRS[b]).slice(0, n);
    // tan 45 and cos 0 / sin 90 all map to "1": keep only pairs whose meaning is unique so a match is never ambiguous
    var seen = {}; list = list.filter(function (p) { if (seen[p[1]]) return false; seen[p[1]] = 1; return true; });
    return list.map(function (p, i) { return { id: i, a: p[0], b: p[1] }; });
  }

  /* ---- Quiz Blitz: [question, [options], answerIndex, subject, band] ---- */
  var BANK = [
    ['How many legs does a spider have?', ['6', '8', '10', '4'], 1, 'Science', 'k'], ['Which animal gives us milk?', ['Cow', 'Lion', 'Snake', 'Eagle'], 0, 'EVS', 'k'],
    ['Which colour do you get by mixing blue and yellow?', ['Green', 'Red', 'Purple', 'Orange'], 0, 'Art', 'k'], ['How many days are there in a week?', ['5', '6', '7', '8'], 2, 'General Knowledge', 'k'],
    ['The Sun rises in the...', ['West', 'East', 'North', 'South'], 1, 'EVS', 'k'], ['Which one is a fruit?', ['Carrot', 'Mango', 'Potato', 'Onion'], 1, 'EVS', 'k'],
    ['What is the capital of India?', ['Mumbai', 'Kolkata', 'New Delhi', 'Chennai'], 2, 'Social Science', 'a'], ['Which gas do we breathe in to live?', ['Oxygen', 'Carbon dioxide', 'Nitrogen', 'Helium'], 0, 'Science', 'a'],
    ['7 \u00D7 8 = ?', ['54', '56', '58', '64'], 1, 'Maths', 'a'], ['Which is the national animal of India?', ['Lion', 'Elephant', 'Tiger', 'Peacock'], 2, 'General Knowledge', 'a'],
    ['How many months have 30 days or fewer?', ['4', '5', '7', '8'], 3, 'General Knowledge', 'a'], ['Which planet is closest to the Sun?', ['Venus', 'Mercury', 'Earth', 'Mars'], 1, 'Science', 'a'],
    ['Which is the largest planet in our solar system?', ['Saturn', 'Earth', 'Jupiter', 'Neptune'], 2, 'Science', 'b'], ['Who wrote the Indian national anthem?', ['Bankim Chandra Chatterjee', 'Rabindranath Tagore', 'Sarojini Naidu', 'Subramania Bharati'], 1, 'Social Science', 'b'],
    ['Which river is the longest in India?', ['Ganga', 'Yamuna', 'Godavari', 'Krishna'], 0, 'Social Science', 'b'], ['What is 25% of 200?', ['25', '40', '50', '75'], 2, 'Maths', 'b'],
    ['Plants make their food by...', ['Respiration', 'Photosynthesis', 'Digestion', 'Evaporation'], 1, 'Science', 'b'], ['Which is the smallest prime number?', ['0', '1', '2', '3'], 2, 'Maths', 'b'],
    ['What is the chemical symbol of gold?', ['Go', 'Gd', 'Au', 'Ag'], 2, 'Science', 'c'], ['The value of \u03C0 (pi) is about...', ['2.14', '3.14', '4.13', '3.41'], 1, 'Maths', 'c'],
    ['Who was the first Prime Minister of India?', ['Sardar Patel', 'Jawaharlal Nehru', 'Lal Bahadur Shastri', 'Dr. Rajendra Prasad'], 1, 'Social Science', 'c'], ['Which part of the cell contains the genetic material?', ['Nucleus', 'Cell wall', 'Vacuole', 'Ribosome'], 0, 'Science', 'c'],
    ['Solve: 3x = 27. x = ?', ['6', '8', '9', '12'], 2, 'Maths', 'c'], ['Which layer of the atmosphere has the ozone layer?', ['Troposphere', 'Stratosphere', 'Mesosphere', 'Exosphere'], 1, 'Science', 'c'],
    ['What is the SI unit of force?', ['Joule', 'Watt', 'Newton', 'Pascal'], 2, 'Science', 'd'], ['In which year did India become independent?', ['1942', '1947', '1950', '1952'], 1, 'Social Science', 'd'],
    ['The sum of angles in a triangle is...', ['90\u00B0', '180\u00B0', '270\u00B0', '360\u00B0'], 1, 'Maths', 'd'], ['What is the powerhouse of the cell?', ['Nucleus', 'Mitochondria', 'Golgi body', 'Lysosome'], 1, 'Science', 'd'],
    ['Which article of the Indian Constitution abolishes untouchability?', ['Article 14', 'Article 17', 'Article 21', 'Article 32'], 1, 'Social Science', 'd'], ['\u221A169 = ?', ['11', '12', '13', '14'], 2, 'Maths', 'd'],
    ['What is the pH of pure water?', ['0', '5', '7', '14'], 2, 'Science', 'd'], ['Who discovered the law of gravitation?', ['Albert Einstein', 'Isaac Newton', 'Galileo Galilei', 'C. V. Raman'], 1, 'Science', 'd']
  ];
  /* questions for the player's band plus one band either side, in random order, answers shuffled but kept correct */
  function quizQuestions(level, count, rnd) {
    rnd = rnd || Math.random;
    var order = ['k', 'a', 'b', 'c', 'd'], i = order.indexOf(band(level)), allowed = order.slice(Math.max(0, i - 1), i + 2);
    var pool = shuffle(rnd, BANK.filter(function (q) { return allowed.indexOf(q[4]) >= 0; }));
    return pool.slice(0, count || 12).map(function (q) {
      var idx = shuffle(rnd, [0, 1, 2, 3]), options = idx.map(function (k) { return q[1][k]; });
      return { q: q[0], options: options, answer: idx.indexOf(q[2]), subject: q[3], band: q[4] };
    });
  }
  function quizPoints(secondsLeft, streak) { return 100 + Math.max(0, secondsLeft) * 5 + Math.min(streak, 5) * 20; }

  var api = { rng: rng, band: band, mathQuestion: mathQuestion, mathPoints: mathPoints, wordList: wordList, scramble: scramble, memoryPairs: memoryPairs, quizQuestions: quizQuestions, quizPoints: quizPoints, BANK: BANK, shuffle: shuffle };
  if (typeof module !== 'undefined' && module.exports) module.exports = api; else root.EduGames = api;
})(typeof window !== 'undefined' ? window : this);
