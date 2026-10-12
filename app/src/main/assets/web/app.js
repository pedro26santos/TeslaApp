(function () {
  'use strict';
  var defaults = {
    stremio: 'https://web.stremio.com/',
    rtp: 'https://www.rtp.pt/play/',
    vodafone: 'https://tv.vodafone.pt/'
  };
  var urls = loadUrls();
  var latestState = null;
  var latestReport = null;
  var capabilityPromise = null;
  var reportTimer = null;
  var toastTimer = null;

  function byId(id) { return document.getElementById(id); }
  function showToast(message) {
    var el = byId('toast');
    el.textContent = message;
    el.classList.add('visible');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () { el.classList.remove('visible'); }, 3500);
  }
  function showView(id) {
    document.querySelectorAll('.view').forEach(function (view) { view.classList.toggle('active', view.id === id); });
    if (id === 'diagnostics') refreshState();
    window.scrollTo(0, 0);
  }
  function loadUrls() {
    try {
      var stored = JSON.parse(localStorage.getItem('teslahub.urls') || '{}');
      return { stremio: validUrl(stored.stremio) || defaults.stremio,
        rtp: validUrl(stored.rtp) || defaults.rtp, vodafone: validUrl(stored.vodafone) || defaults.vodafone };
    } catch (_) { return Object.assign({}, defaults); }
  }
  function validUrl(value) {
    try {
      var url = new URL(value);
      if ((url.protocol === 'https:' || url.protocol === 'http:') && !url.username && !url.password) return url.href;
    } catch (_) { }
    return null;
  }
  function fillSettings() {
    byId('stremio-url').value = urls.stremio;
    byId('rtp-url').value = urls.rtp;
    byId('vodafone-url').value = urls.vodafone;
  }
  function openUrl(key) { window.location.assign(urls[key]); }
  function value(value) {
    if (value === null || value === undefined || value === '') return 'Unavailable';
    if (typeof value === 'boolean') return value ? 'Yes' : 'No';
    return String(value);
  }
  function addRows(id, rows) {
    var target = byId(id);
    target.replaceChildren();
    rows.forEach(function (row) {
      var dt = document.createElement('dt');
      var dd = document.createElement('dd');
      dt.textContent = row[0]; dd.textContent = value(row[1]);
      target.append(dt, dd);
    });
  }
  function render() {
    var state = latestState || {};
    var browser = state.browser || latestReport || {};
    var phone = state.phone || {};
    var connected = !!state.browserConnected;
    byId('connection').textContent = connected ? 'Local browser connected' : 'Waiting for phone server';
    byId('connection').classList.toggle('online', connected);
    var addressRows = (state.addresses || []).map(function (item) { return item.interface + ': ' + item.url; }).join(' · ');
    addRows('phone-data', [
      ['Device', [phone.manufacturer, phone.model].filter(Boolean).join(' ')],
      ['Android', phone.android], ['ColorOS', phone.colorOs],
      ['Hotspot', state.hotspot], ['Local server', state.serverRunning ? 'Running' : state.serverError],
      ['Last request error', state.lastRequestError || 'None'],
      ['Interfaces / URLs', addressRows], ['Preferred mDNS URL', state.preferredAddress],
      ['mDNS', state.mdnsStatus], ['Tesla Bluetooth', state.bluetooth]
    ]);
    addRows('browser-data', [
      ['Connected', connected], ['Viewport', value(browser.innerWidth) + ' × ' + value(browser.innerHeight)],
      ['Device pixel ratio', browser.devicePixelRatio], ['Screen', value(browser.screenWidth) + ' × ' + value(browser.screenHeight)],
      ['Orientation', browser.orientation], ['Platform', browser.platform],
      ['User agent', browser.userAgent], ['Secure context', browser.secureContext]
    ]);
    addRows('feature-data', [
      ['WebSocket', browser.webSocket], ['WebRTC', browser.webRTC],
      ['WebCodecs', browser.webCodecs], ['H.264 WebCodecs', browser.h264WebCodecs],
      ['MediaSource / MSE', browser.mediaSource], ['EME', browser.eme],
      ['Widevine API', browser.widevine], ['ClearKey API', browser.clearKey], ['H.264 video', browser.h264],
      ['H.265 video', browser.h265], ['AAC audio', browser.aac],
      ['Cookies enabled', browser.cookiesEnabled], ['localStorage', browser.localStorage],
      ['Service workers', browser.serviceWorker]
    ]);
    addRows('future-data', [
      ['Receiver', state.androidAuto], ['Stream', state.stream],
      ['Frame rate', 'Unavailable'], ['Encoder', 'Unavailable'], ['Bitrate', 'Unavailable'],
      ['Touch mapping', 'Unavailable']
    ]);
    byId('vodafone-note').textContent = browser.eme === false || browser.widevine === 'Unavailable' ?
      'Protected media APIs are unavailable in this local browser context. Open the provider site to confirm its own compatibility result.' :
      'API presence does not confirm Widevine security level, license access, HDCP, or provider support.';
  }
  function refreshState() {
    fetch('/api/state', { cache: 'no-store' }).then(function (response) {
      if (!response.ok) throw Error('HTTP ' + response.status);
      return response.json();
    }).then(function (data) { latestState = data; render(); })
      .catch(function (error) { byId('connection').textContent = 'Server unavailable: ' + error.message; });
  }
  function storageAvailable() {
    try { var key = 'teslahub.probe'; localStorage.setItem(key, '1'); localStorage.removeItem(key); return true; }
    catch (_) { return false; }
  }
  function reportBase() {
    var video = document.createElement('video');
    var audio = document.createElement('audio');
    var orientation = window.screen && screen.orientation ? screen.orientation.type :
      (typeof window.orientation !== 'undefined' ? String(window.orientation) : 'Unavailable');
    return {
      innerWidth: window.innerWidth, innerHeight: window.innerHeight,
      devicePixelRatio: window.devicePixelRatio, screenWidth: screen.width, screenHeight: screen.height,
      userAgent: navigator.userAgent, platform: navigator.platform, orientation: orientation,
      language: navigator.language, secureContext: window.isSecureContext === true,
      webSocket: typeof WebSocket !== 'undefined', webRTC: typeof RTCPeerConnection !== 'undefined',
      webCodecs: typeof VideoDecoder !== 'undefined', mediaSource: typeof MediaSource !== 'undefined',
      eme: typeof navigator.requestMediaKeySystemAccess === 'function',
      h264: video.canPlayType('video/mp4; codecs="avc1.42E01E"') || 'No',
      h265: video.canPlayType('video/mp4; codecs="hvc1.1.6.L93.B0"') || 'No',
      aac: audio.canPlayType('audio/mp4; codecs="mp4a.40.2"') || 'No',
      cookiesEnabled: navigator.cookieEnabled, localStorage: storageAvailable(),
      serviceWorker: 'serviceWorker' in navigator,
      widevine: 'Unavailable', clearKey: 'Unavailable', h264WebCodecs: 'Unavailable'
    };
  }
  function probeCapabilities(report) {
    var tasks = [];
    if (typeof VideoDecoder !== 'undefined' && typeof VideoDecoder.isConfigSupported === 'function') {
      tasks.push(VideoDecoder.isConfigSupported({ codec: 'avc1.42E01E', codedWidth: 1280, codedHeight: 720 })
        .then(function (result) { report.h264WebCodecs = result.supported ? 'Supported' : 'Unsupported'; })
        .catch(function (error) { report.h264WebCodecs = 'Error: ' + error.name; }));
    }
    if (report.eme) {
      var keyConfig = [{
        initDataTypes: ['cenc'], videoCapabilities: [{ contentType: 'video/mp4; codecs="avc1.42E01E"' }]
      }];
      tasks.push(navigator.requestMediaKeySystemAccess('com.widevine.alpha', keyConfig)
        .then(function () { report.widevine = 'API access granted; playback unverified'; })
        .catch(function (error) { report.widevine = 'Unavailable: ' + error.name; }));
      tasks.push(navigator.requestMediaKeySystemAccess('org.w3.clearkey', keyConfig)
        .then(function () { report.clearKey = 'API access granted'; })
        .catch(function (error) { report.clearKey = 'Unavailable: ' + error.name; }));
    }
    return Promise.all(tasks).then(function () { return report; });
  }
  function capabilitiesOnce() {
    if (!capabilityPromise) {
      capabilityPromise = probeCapabilities(reportBase()).then(function (report) {
        return { widevine: report.widevine, clearKey: report.clearKey, h264WebCodecs: report.h264WebCodecs };
      });
    }
    return capabilityPromise;
  }
  function sendReport() {
    capabilitiesOnce().then(function (capabilities) {
      var report = Object.assign(reportBase(), capabilities);
      latestReport = report;
      render();
      return fetch('/api/report', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(report) });
    }).then(refreshState).catch(function (error) { showToast('Diagnostic report failed: ' + error.message); });
  }
  function downloadReport() {
    var data = { generatedAt: new Date().toISOString(), phoneAndServer: latestState, currentBrowser: latestReport };
    var blob = new Blob([JSON.stringify(data, null, 2)], { type: 'text/plain' });
    var link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = 'teslahub-diagnostics.txt';
    document.body.appendChild(link);
    link.click(); link.remove();
    setTimeout(function () { URL.revokeObjectURL(link.href); }, 2000);
  }

  document.querySelectorAll('[data-view]').forEach(function (button) {
    button.addEventListener('click', function () { showView(button.dataset.view); });
  });
  byId('android-auto').addEventListener('click', function () { showToast('Android Auto module not installed yet (Phase 3).'); });
  byId('stremio').addEventListener('click', function () { openUrl('stremio'); });
  byId('rtp').addEventListener('click', function () { openUrl('rtp'); });
  byId('vodafone').addEventListener('click', function () { showView('diagnostics'); });
  byId('open-vodafone').addEventListener('click', function () { openUrl('vodafone'); });
  byId('refresh').addEventListener('click', function () { sendReport(); });
  byId('save-settings').addEventListener('click', function () {
    var next = { stremio: validUrl(byId('stremio-url').value), rtp: validUrl(byId('rtp-url').value),
      vodafone: validUrl(byId('vodafone-url').value) };
    if (!next.stremio || !next.rtp || !next.vodafone) { byId('settings-result').textContent = 'Enter valid HTTP or HTTPS URLs without embedded credentials.'; return; }
    try { localStorage.setItem('teslahub.urls', JSON.stringify(next)); urls = next; byId('settings-result').textContent = 'Destinations saved in this browser.'; }
    catch (error) { byId('settings-result').textContent = 'Could not save destinations: ' + error.message; }
  });
  byId('reset-settings').addEventListener('click', function () {
    try { localStorage.removeItem('teslahub.urls'); } catch (_) { }
    urls = Object.assign({}, defaults); fillSettings(); byId('settings-result').textContent = 'Default destinations restored.';
  });
  byId('download-report').addEventListener('click', downloadReport);
  fillSettings(); render(); sendReport();
  setInterval(refreshState, 5000);
  setInterval(sendReport, 15000);
  window.addEventListener('resize', function () { clearTimeout(reportTimer); reportTimer = setTimeout(sendReport, 400); });
  window.addEventListener('orientationchange', function () { clearTimeout(reportTimer); reportTimer = setTimeout(sendReport, 400); });
})();
