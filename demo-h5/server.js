// 极简静态服务器，用于本地预览员工端 H5 演示页
const http = require('http')
const fs = require('fs')
const path = require('path')

const PORT = Number(process.env.PORT || 8081)
const API_TARGET = new URL(process.env.BAMA_API_TARGET || 'http://127.0.0.1:8080')
const ROOT = __dirname

const server = http.createServer((req, res) => {
  if (req.url.startsWith('/api/')) {
    const upstream = http.request({ hostname: API_TARGET.hostname, port: API_TARGET.port || 80, path: req.url, method: req.method, headers: { ...req.headers, host: API_TARGET.host } }, response => { res.writeHead(response.statusCode, response.headers); response.pipe(res) })
    upstream.on('error', () => { res.writeHead(502, { 'Content-Type': 'application/json' }); res.end(JSON.stringify({ code: 502, message: '后端服务未连接' })) })
    req.pipe(upstream)
    return
  }
  let file = req.url === '/' ? '/index.html' : req.url.split('?')[0]
  const wxAssets=['/wechat-login.html','/wechat-login.js','/wechat-callback.js']
  const full = wxAssets.includes(file) ? path.join(ROOT,'../bama-store-web/public',file.slice(1)) : path.join(ROOT, decodeURIComponent(file))
  fs.readFile(full, (err, data) => {
    if (err) {
      res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' })
      res.end('404 Not Found')
      return
    }
    const ext = path.extname(full).toLowerCase()
    const type = ext === '.html' ? 'text/html; charset=utf-8'
      : ext === '.js' ? 'application/javascript; charset=utf-8'
      : ext === '.css' ? 'text/css; charset=utf-8' : 'application/octet-stream'
    res.writeHead(200, { 'Content-Type': type })
    res.end(data)
  })
})

server.listen(PORT, () => {
  console.log(`八马员工端 H5 演示: http://localhost:${PORT}`)
})
