export const minutes = time => { const [h,m] = time.split(':').map(Number); return h*60+m }
export const timeText = value => String(Math.floor(value/60)).padStart(2,'0')+':'+String(value%60).padStart(2,'0')
// Start at the room minimum, then offer half-hour extensions up to the next occupied interval.
export function endOptions(room, store, start, occupied = []) {
  if (!start) return []
  const from = minutes(start), minimum = Math.round(Number(room.minHours || 1)*60)
  let limit = Math.min(minutes(room.closeTime), minutes(store?.closeTime || room.closeTime))
  for (const slot of occupied) {
    const a = minutes(slot.start), b = minutes(slot.end)
    if (a <= from && b > from) return []
    if (a > from) limit = Math.min(limit, a)
  }
  const result=[]
  for(let end=from+minimum;end<=limit;end+=30) {
    const hours=(end-from)/60
    result.push({time:timeText(end),hours,label:`${timeText(end)} · ${Number(hours.toFixed(1))} 小时`})
  }
  return result
}
