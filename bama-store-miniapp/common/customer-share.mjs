const imageUrl = '/static/share-logo.png'

function storeId(value) {
  const text = String(value ?? '')
  return /^[1-9]\d*$/.test(text) && Number.isSafeInteger(Number(text)) ? text : ''
}

// Share only public branch context, never the current page's account or scan query.
export function customerShare(page, store) {
  const id = storeId(store?.id)
  const query = id ? `storeId=${id}` : ''
  const target = page === 'rooms' ? 'rooms' : 'home'
  const name = typeof store?.name === 'string' ? store.name.slice(0, 40) : '八马茶业'
  return {
    title: `${name || '八马茶业'} · ${target === 'rooms' ? '邀你一起品茶，预订茶室' : '一盏好茶，一刻闲适'}`,
    path: `/pages/customer/${target}${query ? '?' + query : ''}`,
    query,
    imageUrl
  }
}

export function receiveSharedStore(options, platform) {
  const id = storeId(options?.storeId)
  if (id) platform.setStorageSync('customer_store_id', Number(id))
}
