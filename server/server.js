/**
 * ScreenCast Server
 * Запуск: node server.js
 * Порт по умолчанию: 53347
 * 
 * В Termux:
 *   pkg install nodejs
 *   node server.js
 * 
 * Для туннеля через bore:
 *   bore local 53347 --to bore.pub
 */

const http = require('http');
const WebSocket = require('ws');
const path = require('path');

const PORT = process.env.PORT || 53347;

const HTML = `<!DOCTYPE html>
<html lang="ru">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>ScreenCast Viewer</title>
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; }
  body { background: #1a1a2e; color: #fff; font-family: sans-serif; display: flex; flex-direction: column; align-items: center; min-height: 100vh; }
  h1 { color: #e94560; padding: 16px; font-size: 22px; }
  #status { color: #888; font-size: 13px; margin-bottom: 12px; }
  #screen-container { position: relative; cursor: crosshair; border: 2px solid #e94560; border-radius: 8px; overflow: hidden; }
  #screen { display: block; max-width: 90vw; max-height: 80vh; width: auto; height: auto; }
  #controls { margin-top: 16px; display: flex; gap: 12px; }
  button { background: #16213e; color: #fff; border: 1px solid #e94560; padding: 8px 20px; border-radius: 6px; cursor: pointer; font-size: 14px; }
  button:hover { background: #e94560; }
  #fps { color: #888; font-size: 12px; margin-top: 8px; }
  #no-stream { color: #e94560; padding: 40px; font-size: 18px; text-align: center; }
</style>
</head>
<body>
<h1>ScreenCast Remote</h1>
<div id="status">Подключение...</div>
<div id="screen-container">
  <img id="screen" src="" alt="" style="display:none"/>
  <div id="no-stream">Ожидание трансляции...<br><small style="font-size:13px;color:#888;margin-top:8px;display:block">Запустите приложение на телефоне</small></div>
</div>
<div id="controls">
  <button onclick="sendCmd('back')">← Назад</button>
  <button onclick="sendCmd('home')">⌂ Домой</button>
  <button onclick="sendCmd('recents')">▣ Задачи</button>
</div>
<div id="fps">FPS: 0</div>

<script>
const img = document.getElementById('screen');
const noStream = document.getElementById('no-stream');
const status = document.getElementById('status');
const fpsEl = document.getElementById('fps');
const container = document.getElementById('screen-container');

let ws;
let frameCount = 0;
let lastFpsTime = Date.now();
let isStreaming = false;

function connect() {
  const proto = location.protocol === 'https:' ? 'wss:' : 'ws:';
  ws = new WebSocket(proto + '//' + location.host + '/ws');
  ws.binaryType = 'blob';

  ws.onopen = () => { status.textContent = 'Соединение установлено'; };
  ws.onclose = () => {
    status.textContent = 'Соединение разорвано. Переподключение...';
    setTimeout(connect, 2000);
  };
  ws.onerror = () => { status.textContent = 'Ошибка соединения'; };

  ws.onmessage = (event) => {
    try {
      const msg = JSON.parse(event.data);
      if (msg.type === 'frame') {
        const src = 'data:image/jpeg;base64,' + msg.data;
        img.src = src;
        if (!isStreaming) {
          isStreaming = true;
          img.style.display = 'block';
          noStream.style.display = 'none';
          status.textContent = 'Трансляция активна';
        }
        frameCount++;
      }
    } catch(e) {}
  };
}

setInterval(() => {
  const now = Date.now();
  const elapsed = (now - lastFpsTime) / 1000;
  const fps = Math.round(frameCount / elapsed);
  fpsEl.textContent = 'FPS: ' + fps;
  frameCount = 0;
  lastFpsTime = now;
}, 1000);

function sendCmd(type, extra) {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({ type, ...extra }));
  }
}

img.addEventListener('click', (e) => {
  const rect = img.getBoundingClientRect();
  const x = (e.clientX - rect.left) / rect.width;
  const y = (e.clientY - rect.top) / rect.height;
  sendCmd('click', { x, y });
  showTouchIndicator(e.clientX, e.clientY);
});

let touchStartX, touchStartY, touchStartTime;

img.addEventListener('touchstart', (e) => {
  e.preventDefault();
  const t = e.touches[0];
  touchStartX = t.clientX;
  touchStartY = t.clientY;
  touchStartTime = Date.now();
}, { passive: false });

img.addEventListener('touchend', (e) => {
  e.preventDefault();
  const t = e.changedTouches[0];
  const rect = img.getBoundingClientRect();
  const dx = t.clientX - touchStartX;
  const dy = t.clientY - touchStartY;
  const dt = Date.now() - touchStartTime;
  const dist = Math.sqrt(dx*dx + dy*dy);

  if (dt > 500 && dist < 10) {
    const x = (touchStartX - rect.left) / rect.width;
    const y = (touchStartY - rect.top) / rect.height;
    sendCmd('longclick', { x, y });
  } else if (dist > 30) {
    sendCmd('swipe', {
      x1: (touchStartX - rect.left) / rect.width,
      y1: (touchStartY - rect.top) / rect.height,
      x2: (t.clientX - rect.left) / rect.width,
      y2: (t.clientY - rect.top) / rect.height
    });
  } else {
    const x = (touchStartX - rect.left) / rect.width;
    const y = (touchStartY - rect.top) / rect.height;
    sendCmd('click', { x, y });
  }
}, { passive: false });

function showTouchIndicator(cx, cy) {
  const dot = document.createElement('div');
  dot.style.cssText = 'position:fixed;width:20px;height:20px;border-radius:50%;background:rgba(233,69,96,0.7);pointer-events:none;transform:translate(-50%,-50%);top:' + cy + 'px;left:' + cx + 'px;z-index:9999;transition:opacity 0.3s';
  document.body.appendChild(dot);
  setTimeout(() => { dot.style.opacity = '0'; setTimeout(() => dot.remove(), 300); }, 200);
}

connect();
</script>
</body>
</html>`;

const server = http.createServer((req, res) => {
  if (req.url === '/' || req.url === '/index.html') {
    res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
    res.end(HTML);
  } else {
    res.writeHead(404);
    res.end('Not found');
  }
});

const wss = new WebSocket.Server({ server, path: '/ws' });

let phoneSocket = null;
const browserSockets = new Set();

wss.on('connection', (ws, req) => {
  const ua = req.headers['user-agent'] || '';
  const isPhone = req.headers['x-client-type'] === 'phone';

  console.log('[+] Client connected:', req.socket.remoteAddress);

  ws.on('message', (data) => {
    try {
      const msg = JSON.parse(data.toString());

      if (msg.type === 'frame') {
        phoneSocket = ws;
        browserSockets.forEach(bws => {
          if (bws.readyState === WebSocket.OPEN) {
            bws.send(data.toString());
          }
        });
      } else {
        if (phoneSocket && phoneSocket.readyState === WebSocket.OPEN) {
          phoneSocket.send(data.toString());
        }
      }
    } catch(e) {
      const text = data.toString();
      if (text.startsWith('{"type":"frame"')) {
        browserSockets.forEach(bws => {
          if (bws.readyState === WebSocket.OPEN) bws.send(text);
        });
      }
    }
  });

  ws.on('close', () => {
    console.log('[-] Client disconnected');
    if (ws === phoneSocket) phoneSocket = null;
    browserSockets.delete(ws);
  });

  browserSockets.add(ws);
});

server.listen(PORT, '0.0.0.0', () => {
  console.log('=================================');
  console.log('ScreenCast Server running on port', PORT);
  console.log('Open in browser: http://localhost:' + PORT);
  console.log('=================================');
});
