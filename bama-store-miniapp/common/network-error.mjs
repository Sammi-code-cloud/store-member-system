export function networkError(err = {}) {
  const detail = String(err.errMsg || err.message || '')
  if (/domain list|合法域名|url.*domain/i.test(detail)) return new Error('服务域名未获微信授权，请联系门店配置合法域名')
  if (/ssl|certificate|cert_|tls|handshake|hand shake/i.test(detail)) return new Error('服务安全连接失败，请联系门店检查 HTTPS 证书')
  if (/timeout|timed out/i.test(detail)) return new Error('连接超时，请切换网络后重试')
  if (/name_not_resolved|dns|resolve host/i.test(detail)) return new Error('无法解析服务地址，请切换网络后重试')
  if (/internet_disconnected|network.*unavailable|network.*offline/i.test(detail)) return new Error('网络未连接，请检查手机网络')
  return new Error('无法连接门店服务，请切换 Wi-Fi 或移动网络后重试')
}
