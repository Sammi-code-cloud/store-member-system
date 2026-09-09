import { readFileSync, writeFileSync, existsSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const target = fileURLToPath(new URL('../manifest.json', import.meta.url))
if (existsSync(target)) {
  console.log('manifest.json 已存在，保留本地配置。')
} else {
  const manifest = JSON.parse(readFileSync(new URL('../manifest.example.json', import.meta.url), 'utf8'))
  const appId = process.env.WECHAT_MINI_APP_ID || ''
  if (appId && !/^wx[0-9a-f]{16}$/i.test(appId)) {
    throw new Error('WECHAT_MINI_APP_ID 格式不正确')
  }
  manifest['mp-weixin'].appid = appId
  writeFileSync(target, JSON.stringify(manifest, null, 2) + '\n', { flag: 'wx' })
  console.log('已从公开模板生成本地 manifest.json；请配置自己的微信 AppID。')
}
