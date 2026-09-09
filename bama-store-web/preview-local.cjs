// Local preview for the built administration UI and customer registration flow.
const http = require('node:http')
const fs = require('node:fs')
const path = require('node:path')
const root = path.join(__dirname, 'dist')
const port = Number(process.env.PORT || 5175)
const api = new URL(process.env.BAMA_API_TARGET || 'http://127.0.0.1:8092')
const server = http.createServer((req, res) => {
  if (req.url.startsWith('/api/')) {
    const upstream = http.request({ hostname: api.hostname, port: api.port || 80, path: req.url, method: req.method, headers: { ...req.headers, host: api.host } }, response => {
      res.writeHead(response.statusCode, response.headers); response.pipe(res)
    })
    upstream.on('error', () => { res.writeHead(502, { 'Content-Type': 'application/json' }); res.end(JSON.stringify({ code: 502, message: '后端服务未连接' })) })
    req.pipe(upstream); return
  }
  let pathname
  try { pathname = decodeURIComponent(new URL(req.url, 'http://localhost').pathname) } catch { res.writeHead(400); res.end(); return }
  let file
  if (['/customer.html', '/customer-auth.js'].includes(pathname)) file = path.join(__dirname, '..', 'docs', 'demo-h5', pathname.slice(1))
  else {
    file = path.resolve(root, '.' + (pathname === '/' ? '/index.html' : pathname))
    if (!file.startsWith(root + path.sep)) { res.writeHead(403); res.end(); return }
  }
  fs.readFile(file, (error, data) => {
    if (error) { res.writeHead(404); res.end('Not found. Run npm run build first.'); return }
    const types = { '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8', '.css': 'text/css; charset=utf-8', '.svg': 'image/svg+xml', '.png': 'image/png', '.woff2': 'font/woff2' }
    res.writeHead(200, { 'Content-Type': types[path.extname(file)] || 'application/octet-stream', 'Cache-Control': 'no-store' }); res.end(data)
  })
})
server.listen(port, '127.0.0.1', () => console.log(`Administration: http://127.0.0.1:${port}/ | Customer: http://127.0.0.1:${port}/customer.html`))
