import qrcode from './vendor/qrcode-generator/qrcode.js'

// Encode the exact server-issued credential locally; never send it to a QR service.
export function paymentQrRows(code) {
  if (!/^BM[A-F0-9]{16}$/.test(code)) throw new Error('付款码格式异常，请刷新重试')
  const qr = qrcode(0, 'M')
  qr.addData(code, 'Alphanumeric')
  qr.make()
  return Array.from({ length: qr.getModuleCount() }, (_, row) =>
    Array.from({ length: qr.getModuleCount() }, (_, col) => qr.isDark(row, col)))
}
