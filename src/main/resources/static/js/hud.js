/* Top-left HUD: live clock "HH:MM:SS DD/Mon/Year" and a compass.
   REAL compass: on a phone / tablet / 2-in-1 PC with a magnetometer the rose follows the device, live.
   Fixes over the earlier build: (1) only NORTH-referenced readings are used - Chrome/Edge's plain "deviceorientation"
   alpha is relative to an arbitrary start, which made the needle wrong; (2) the device tilt and the screen rotation are
   taken into account; (3) the needle is smoothed along the short arc.
   A desktop PC has NO compass hardware, so no software can read magnetic north there. In that case the rose stays
   North-up and a Sun marker shows the Sun's real direction, computed live from the clock and the browser location. */
(function (root) {
  'use strict';
  var MON = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
  var DIRS = ['N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW'];
  var RAD = Math.PI / 180;
  function p2(n) { return (n < 10 ? '0' : '') + n; }

  function formatStamp(d) {
    return p2(d.getHours()) + ':' + p2(d.getMinutes()) + ':' + p2(d.getSeconds()) + ' ' + p2(d.getDate()) + '/' + MON[d.getMonth()] + '/' + d.getFullYear();
  }
  function norm360(a) { return ((a % 360) + 360) % 360; }
  function cardinal(deg) { return DIRS[Math.round(norm360(deg) / 45) % 8]; }
  function num(x) { return typeof x === 'number' && isFinite(x); }

  /* Heading (clockwise from north) from the W3C alpha/beta/gamma angles.
     Device lying flat -> the direction of the device's top edge, corrected for the screen rotation.
     Device held upright -> the direction the back of the device faces (like a camera). */
  function headingFromAngles(alpha, beta, gamma, screenAngle) {
    var a = alpha * RAD, b = beta * RAD, g = gamma * RAD;
    var cA = Math.cos(a), sA = Math.sin(a), cB = Math.cos(b), sB = Math.sin(b), cG = Math.cos(g), sG = Math.sin(g);
    var zUp = cB * cG;                                           // +1 flat face-up, -1 flat face-down, 0 upright
    if (Math.abs(zUp) > 0.7) {
      var yx = -sA * cB, yy = cA * cB;                           // top edge, horizontal part
      return norm360(Math.atan2(yx, yy) / RAD - (screenAngle || 0));
    }
    var vx = -cA * sG - sA * sB * cG, vy = -sA * sG + cA * sB * cG;   // back of the device, horizontal part
    return norm360(Math.atan2(vx, vy) / RAD);
  }

  /* iOS gives webkitCompassHeading (clockwise from north). Android / Edge give alpha (counter-clockwise).
     Plain 'deviceorientation' events are only trusted when they say absolute===true. */
  function headingFromEvent(e, screenAngle) {
    if (!e) return null;
    if (num(e.webkitCompassHeading)) return norm360(e.webkitCompassHeading);
    if (!num(e.alpha)) return null;
    if (num(e.beta) && num(e.gamma)) return headingFromAngles(e.alpha, e.beta, e.gamma, screenAngle);
    return norm360(360 - e.alpha - (screenAngle || 0));
  }
  /* smooth along the shortest arc so the needle never spins the long way round at 359 -> 1 */
  function smoothAngle(prev, next, k) {
    if (prev == null) return next;
    var d = ((next - prev + 540) % 360) - 180;
    return norm360(prev + d * k);
  }

  /* Sun direction (azimuth clockwise from true north, elevation above the horizon) for a place and moment.
     Standard low-precision astronomical formulas (about 0.1 degree). */
  function sunPosition(date, latDeg, lonDeg) {
    var d = date.getTime() / 86400000 - 10957.5;                 // days since J2000.0
    var L = norm360(280.460 + 0.9856474 * d);
    var g = norm360(357.528 + 0.9856003 * d) * RAD;
    var lambda = (L + 1.915 * Math.sin(g) + 0.020 * Math.sin(2 * g)) * RAD;
    var eps = (23.439 - 0.0000004 * d) * RAD;
    var ra = Math.atan2(Math.cos(eps) * Math.sin(lambda), Math.cos(lambda));
    var dec = Math.asin(Math.sin(eps) * Math.sin(lambda));
    var gmst = norm360(280.46061837 + 360.98564736629 * d);
    var H = (gmst + lonDeg) * RAD - ra;
    var lat = latDeg * RAD;
    var el = Math.asin(Math.sin(lat) * Math.sin(dec) + Math.cos(lat) * Math.cos(dec) * Math.cos(H));
    var az = Math.atan2(-Math.cos(dec) * Math.sin(H), Math.sin(dec) * Math.cos(lat) - Math.cos(dec) * Math.sin(lat) * Math.cos(H));
    return { azimuth: norm360(az / RAD), elevation: el / RAD };
  }

  /* Heading from the sensor quaternion [x, y, z, w] (AbsoluteOrientationSensor, reference frame "screen" = the screen's own axes).
     Flat device: where the top of the screen points. Upright device: where its back faces (like a camera). */
  function headingFromQuaternion(q, screenAngle) {
    var x = q[0], y = q[1], z = q[2], w = q[3];
    var yE = 2 * (x * y - w * z), yN = 1 - 2 * (x * x + z * z);        // device top axis, East / North parts
    var zE = 2 * (x * z + w * y), zN = 2 * (y * z - w * x), zU = 1 - 2 * (x * x + y * y);   // device face axis
    if (Math.abs(zU) > 0.7) return norm360(Math.atan2(yE, yN) / RAD - (screenAngle || 0));
    return norm360(Math.atan2(-zE, -zN) / RAD);
  }

  /* Keeps the dial turning the SHORT way: 359 -> 1 must turn +2 degrees, not -358 (that was the "spins backwards when crossing north" bug). */
  function unwrap(prevShown, headingDeg) {
    if (prevShown == null) return headingDeg;
    var d = ((headingDeg - norm360(prevShown) + 540) % 360) - 180;
    return prevShown + d;
  }

  var api = { formatStamp: formatStamp, cardinal: cardinal, headingFromEvent: headingFromEvent, headingFromAngles: headingFromAngles,
              headingFromQuaternion: headingFromQuaternion, unwrap: unwrap, smoothAngle: smoothAngle, norm360: norm360, sunPosition: sunPosition };
  if (typeof module !== 'undefined' && module.exports) { module.exports = api; return; }
  root.Hud = api;

  var doc = root.document;
  var FACING_KEY = 'wn.compass.facing';
  function init() {
    var clock = doc.getElementById('clock'), rose = doc.getElementById('compassRose'), text = doc.getElementById('compassText'),
        box = doc.getElementById('compass'), sunMark = doc.getElementById('sunMark'), sunText = doc.getElementById('sunText');
    if (!clock) return;
    function tick() { clock.textContent = formatStamp(new Date()); }
    tick(); root.setInterval(tick, 1000);

    var heading = null, shown = null, source = null, listening = false, place = null, geoAsked = false, facing = null, frame = 0, sensor = null, sunTimer = 0;
    try { var f = root.localStorage.getItem(FACING_KEY); if (f !== null && f !== '' && isFinite(+f)) facing = norm360(+f); } catch (e) { /* storage blocked */ }

    function screenAngle() {
      var o = root.screen && root.screen.orientation;
      if (o && num(o.angle)) return o.angle;
      return num(root.orientation) ? norm360(root.orientation) : 0;
    }
    function paint() {                                              // one DOM write per animation frame
      frame = 0;
      if (shown != null) rose.style.transform = 'rotate(' + (-shown).toFixed(1) + 'deg)';
    }
    function show(h) {
      shown = unwrap(shown, h);
      if (!frame) frame = (root.requestAnimationFrame || function (fn) { return root.setTimeout(fn, 16); })(paint);
    }
    /* a reading from a sensor source; the first source that delivers wins, so two sources never fight */
    function reading(h, src) {
      if (source && source !== src) return;
      source = src;
      heading = smoothAngle(heading, h, 0.35);
      show(heading);
      box.classList.add('live');
      text.textContent = 'Compass: ' + Math.round(heading) + '\u00B0 ' + cardinal(heading) + ' (live)';
      showSun();
    }
    function onAbsolute(e) { var h = headingFromEvent(e, screenAngle()); if (h != null) reading(h, 'events'); }
    function onPlain(e) { if (e.absolute === true || num(e.webkitCompassHeading)) onAbsolute(e); }   // relative alpha is NOT north

    function startGeneric() {                                       // Chromium / Edge Generic Sensor API: Windows tablets, 2-in-1s, Android
      if (sensor || !root.AbsoluteOrientationSensor) return;
      try {
        sensor = new root.AbsoluteOrientationSensor({ frequency: 30, referenceFrame: 'screen' });
        sensor.addEventListener('reading', function () { if (sensor.quaternion) reading(headingFromQuaternion(sensor.quaternion, 0), 'sensor'); });
        sensor.addEventListener('error', function () { try { sensor.stop(); } catch (e) { /* ignore */ } sensor = null; });
        sensor.start();
      } catch (e) { sensor = null; }
    }
    function listen() {
      if (listening) return;
      listening = true;
      startGeneric();
      root.addEventListener('deviceorientationabsolute', onAbsolute, true);
      root.addEventListener('deviceorientation', onPlain, true);
    }
    function enable() {                                             // iOS needs a tap to allow motion sensors
      var D = root.DeviceOrientationEvent;
      if (D && typeof D.requestPermission === 'function') {
        D.requestPermission().then(function (r) { if (r === 'granted') listen(); else text.textContent = 'Compass: permission refused - allow Motion & Orientation in Settings'; })
          .catch(function () { text.textContent = 'Compass: tap the compass to allow motion sensors'; });
      } else listen();
    }

    function sunNow() { return place ? sunPosition(new Date(), place.lat, place.lon) : null; }
    function showSun() {
      var s = sunNow();
      if (!s || !sunMark) return;
      sunMark.style.display = '';
      sunMark.setAttribute('transform', 'rotate(' + s.azimuth.toFixed(1) + ' 32 32)');
      sunMark.style.opacity = s.elevation > -0.8 ? '1' : '0.35';
      sunText.textContent = (s.elevation > -0.8 ? 'Sun ' : 'Sun (below horizon) ') + Math.round(s.azimuth) + '\u00B0 ' + cardinal(s.azimuth) + ', ' + Math.round(s.elevation) + '\u00B0 high';
    }
    function askPlace() {
      if (place || geoAsked || !root.navigator || !root.navigator.geolocation) return;
      geoAsked = true;
      root.navigator.geolocation.getCurrentPosition(function (pos) {
        place = { lat: pos.coords.latitude, lon: pos.coords.longitude };
        showSun(); if (!sunTimer) sunTimer = root.setInterval(showSun, 5000);
      }, function () { sunText.textContent = 'Allow location to see the Sun\u2019s direction'; geoAsked = false; }, { timeout: 10000, maximumAge: 3600000 });
    }

    /* ---- desktop PC without a sensor: the person tells where the screen faces (or calibrates with the Sun) ---- */
    function applyFacing() {
      if (source) return;                                           // a real sensor always wins
      if (facing == null) {
        show(0);
        text.textContent = 'No compass sensor on this PC - click to set where your screen faces';
      } else {
        show(facing);
        text.textContent = 'Screen faces ' + Math.round(facing) + '\u00B0 ' + cardinal(facing) + ' (set by you) - click to change';
      }
      showSun();
    }
    function setFacing(v) {
      facing = v == null ? null : norm360(v);
      try { if (facing == null) root.localStorage.removeItem(FACING_KEY); else root.localStorage.setItem(FACING_KEY, String(facing)); } catch (e) { /* storage blocked */ }
      applyFacing(); refreshMenu();
    }
    var menu = null, sel = null, sunNote = null;
    function buildMenu() {
      menu = doc.createElement('div'); menu.className = 'compass-menu'; menu.hidden = true;
      menu.innerHTML = '<b>Where does your screen face?</b><label>Direction <select id="faceSel"><option value="">Not set (North up)</option></select></label>'
        + '<div class="cm-sun"><span id="sunNote"></span><div class="cm-row"><button type="button" data-sun="0">Sun is ahead</button><button type="button" data-sun="90">on my right</button>'
        + '<button type="button" data-sun="180">behind me</button><button type="button" data-sun="270">on my left</button></div></div>'
        + '<div class="cm-row"><button type="button" id="faceClear">Clear</button><button type="button" id="faceClose">Close</button></div>';
      (box.parentNode || doc.body).appendChild(menu);
      sel = menu.querySelector('#faceSel'); sunNote = menu.querySelector('#sunNote');
      [['N', 0], ['NE', 45], ['E', 90], ['SE', 135], ['S', 180], ['SW', 225], ['W', 270], ['NW', 315]].forEach(function (d) {
        var o = doc.createElement('option'); o.value = String(d[1]); o.textContent = d[0] + ' (' + d[1] + '\u00B0)'; sel.appendChild(o);
      });
      sel.addEventListener('change', function () { setFacing(sel.value === '' ? null : +sel.value); });
      menu.querySelectorAll('[data-sun]').forEach(function (b) {
        b.addEventListener('click', function () { var s = sunNow(); if (s && s.elevation > 3) setFacing(s.azimuth - (+b.getAttribute('data-sun'))); });
      });
      menu.querySelector('#faceClear').addEventListener('click', function () { setFacing(null); });
      menu.querySelector('#faceClose').addEventListener('click', function () { menu.hidden = true; });
    }
    function refreshMenu() {
      if (!menu) return;
      sel.value = facing == null ? '' : (String(Math.round(facing / 45) * 45 % 360) === String(Math.round(facing)) ? String(Math.round(facing)) : '');
      var s = sunNow(), ok = !!s && s.elevation > 3;
      sunNote.textContent = !s ? 'Allow location to calibrate with the Sun.' : ok ? 'Look outside: where is the Sun, seen from your seat? Tap it.' : 'The Sun is below the horizon - use the list.';
      menu.querySelectorAll('[data-sun]').forEach(function (b) { b.disabled = !ok; });
    }
    function toggleMenu() { if (!menu) buildMenu(); menu.hidden = !menu.hidden; refreshMenu(); }

    box.addEventListener('click', function () {
      enable();
      if (source) return;                                           // live sensor: nothing to set
      askPlace(); toggleMenu();
    });
    enable();
    root.setTimeout(function () {
      if (source) return;
      box.title = 'This computer has no magnetometer, so nothing can sense north. Click to tell the app where your screen faces; the Sun marker is real and live. A phone or tablet gives a live sensor compass.';
      applyFacing();
      askPlace();
    }, 2500);
  }
  if (doc.readyState === 'loading') doc.addEventListener('DOMContentLoaded', init); else init();
})(typeof window !== 'undefined' ? window : this);
