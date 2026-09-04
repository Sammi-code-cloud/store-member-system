// 极简静态服务器，用于本地预览员工端 H5 演示页
const http = require('http')
const fs = require('fs')
const path = require('path')

const PORT = 8081
const ROOT = __dirname

const server = http.createServer((req, res) => {
  let file = req.url === '/' ? '/index.html' : req.url.split('?')[0]
  const full = path.join(ROOT, decodeURIComponent(file))
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
