// Only accept the public scene produced by the server. It never grants staff access.
export function parseStoreScene(scene) {
  if (typeof scene !== 'string' || scene.length > 96) return null
  let decoded
  try { decoded = decodeURIComponent(scene) } catch { return null }
  const match = /^s=([1-9]\d{0,15})$/.exec(decoded)
  if (!match) return null
  const id = Number(match[1])
  return Number.isSafeInteger(id) ? id : null
}

export function rememberScan(options, storage) {
  if (options?.path !== 'pages/customer/entry') return
  storage.setStorageSync('pending_store_scene', { scene: options.query?.scene || '' })
}
