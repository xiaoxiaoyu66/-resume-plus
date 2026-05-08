/**
 * 批量导入页面独立服务器
 * 同时代理 API 请求到后端，避免跨域问题
 *
 * 使用方式： node server-import.js
 * 访问：     http://localhost:3456
 */
const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 3456;
const BACKEND = 'http://localhost:8080';
const HTML_FILE = path.join(__dirname, 'public', 'import.html');

const server = http.createServer((req, res) => {
  const url = req.url;

  // 代理 API 请求到后端
  if (url.startsWith('/api/')) {
    const backendPath = url.replace(/^\/api/, '');
    const options = {
      hostname: 'localhost',
      port: 8080,
      path: backendPath,
      method: req.method,
      headers: { ...req.headers, host: 'localhost:8080' }
    };

    const proxyReq = http.request(options, (proxyRes) => {
      res.writeHead(proxyRes.statusCode, proxyRes.headers);
      proxyRes.pipe(res);
    });

    proxyReq.on('error', (e) => {
      res.writeHead(502, { 'Content-Type': 'text/html; charset=utf-8' });
      res.end('<h2>502 - 后端连接失败</h2><p>请确保后端服务运行在 http://localhost:8080</p>');
    });

    req.pipe(proxyReq);
    return;
  }

  // 提供静态页面
  if (url === '/' || url === '/import.html') {
    fs.readFile(HTML_FILE, 'utf-8', (err, content) => {
      if (err) {
        res.writeHead(500, { 'Content-Type': 'text/plain' });
        return res.end('500 Internal Server Error');
      }
      res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
      res.end(content);
    });
    return;
  }

  // 其他返回 404
  res.writeHead(404);
  res.end('Not Found');
});

server.listen(PORT, () => {
  console.log('═══════════════════════════════════════');
  console.log('  批量导入页面已启动');
  console.log('  ───────────────────────────────');
  console.log('  地址: http://localhost:' + PORT);
  console.log('  后端: ' + BACKEND);
  console.log('  ───────────────────────────────');
  console.log('  按 Ctrl+C 停止服务');
  console.log('═══════════════════════════════════════');
});
