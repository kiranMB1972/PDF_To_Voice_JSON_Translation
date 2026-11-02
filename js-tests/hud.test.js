const test = require('node:test');
const assert = require('node:assert');
const H = require('../src/main/resources/static/js/hud.js');

test('stamp format is HH:MM:SS DD/Mon/Year with zero padding', () => {
  assert.strictEqual(H.formatStamp(new Date(2026, 9, 27, 9, 5, 3)), '09:05:03 27/Oct/2026');
  assert.strictEqual(H.formatStamp(new Date(2027, 0, 1, 0, 0, 0)), '00:00:00 01/Jan/2027');
  assert.strictEqual(H.formatStamp(new Date(2026, 11, 31, 23, 59, 59)), '23:59:59 31/Dec/2026');
});
test('cardinal directions', () => {
  assert.deepStrictEqual([0, 44, 90, 135, 180, 225, 270, 315, 359, 360, -90].map(H.cardinal), ['N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW', 'N', 'N', 'W']);
});
test('heading from Android alpha (counter-clockwise) and iOS webkitCompassHeading (clockwise)', () => {
  assert.strictEqual(H.headingFromEvent({ alpha: 270 }), 90);
  assert.strictEqual(H.headingFromEvent({ alpha: 0 }), 0);
  assert.strictEqual(H.headingFromEvent({ alpha: 90 }), 270);
  assert.strictEqual(H.headingFromEvent({ webkitCompassHeading: 123, alpha: 5 }), 123);
  assert.strictEqual(H.headingFromEvent({ alpha: null }), null);
  assert.strictEqual(H.headingFromEvent({}), null);
});
test('smoothing takes the short way round 359 -> 1', () => {
  assert.strictEqual(H.smoothAngle(null, 40, 0.3), 40);
  const s = H.smoothAngle(359, 1, 0.5); assert.ok(s === 0 || s > 359 || s < 1, 'got ' + s);
  assert.ok(Math.abs(H.smoothAngle(10, 100, 1) - 100) < 1e-9);
});

test('flat device: top-edge heading, corrected for screen rotation', () => {
  assert.strictEqual(Math.round(H.headingFromAngles(270, 0, 0, 0)), 90);
  assert.strictEqual(Math.round(H.headingFromAngles(0, 0, 0, 0)), 0);
  assert.strictEqual(Math.round(H.headingFromAngles(270, 0, 0, 90)), 0);          // screen turned to landscape: screen-up is 90 deg left of the top edge
  assert.strictEqual(Math.round(H.headingFromEvent({ alpha: 270, beta: 2, gamma: 1 })), 90);
});
test('upright device: the direction the back of the device faces', () => {
  assert.strictEqual(Math.round(H.headingFromAngles(0, 90, 0, 0)), 0);            // facing north
  assert.strictEqual(Math.round(H.headingFromAngles(270, 90, 0, 0)), 90);         // turned to face east
  assert.strictEqual(Math.round(H.headingFromAngles(180, 90, 0, 0)), 180);
});
test('screen angle is applied on the plain alpha path too', () => {
  assert.strictEqual(H.headingFromEvent({ alpha: 270 }, 90), 0);
});
test('Sun position matches an independent astronomy library (astral) within 0.1 degree', () => {
  const cases = [
    [12.2958, 76.6394, '2026-10-29T06:30:00Z', 176.021, 64.180], [12.2958, 76.6394, '2026-06-21T09:00:00Z', 294.992, 58.393],
    [40.7, -74.0, '2026-03-20T15:00:00Z', 137.508, 40.632], [-33.9, 151.2, '2026-12-21T02:00:00Z', 351.306, 79.429],
  ];
  for (const [la, lo, t, az, el] of cases) {
    const s = H.sunPosition(new Date(t), la, lo);
    assert.ok(Math.abs(s.azimuth - az) < 0.1, `az ${s.azimuth} vs ${az}`);
    assert.ok(Math.abs(s.elevation - el) < 0.1, `el ${s.elevation} vs ${el}`);
  }
});

test('quaternion heading: identity faces north, 90 degrees counter-clockwise faces west, upright device uses its back', () => {
  const sin = Math.sin(Math.PI / 4), cos = Math.cos(Math.PI / 4);
  assert.strictEqual(Math.round(H.headingFromQuaternion([0, 0, 0, 1], 0)), 0);
  assert.strictEqual(Math.round(H.headingFromQuaternion([0, 0, sin, cos], 0)), 270);          // turned 90 deg CCW (to the west)
  assert.strictEqual(Math.round(H.headingFromQuaternion([0, 0, -sin, cos], 0)), 90);         // turned 90 deg CW (to the east)
  assert.strictEqual(Math.round(H.headingFromQuaternion([0, 0, 0, 1], 90)), 270);            // screen rotated: screen-up moved
  assert.strictEqual(Math.round(H.headingFromQuaternion([sin, 0, 0, cos], 0)), 0);           // stood upright facing north: back faces north
});
test('unwrap keeps the dial turning the short way across north', () => {
  assert.strictEqual(H.unwrap(null, 359), 359);
  assert.ok(Math.abs(H.unwrap(359, 1) - 361) < 1e-9);
  assert.ok(Math.abs(H.unwrap(361, 359) - 359) < 1e-9);
  assert.ok(Math.abs(H.unwrap(10, 350) - (-10)) < 1e-9);
  let shown = null; const path = [350, 355, 359, 3, 8, 3, 359, 354]; const out = path.map(h => (shown = H.unwrap(shown, h)));
  for (let i = 1; i < out.length; i++) assert.ok(Math.abs(out[i] - out[i - 1]) <= 10);
});
