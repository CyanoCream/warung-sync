package com.warungsync.app.auth

import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class TotpManager {

    /**
     * Verifikasi kode 6 digit TOTP dari user dengan toleransi ±1 time window (clock drift).
     */
    fun verifyMasterCode(code: String): Boolean {
        if (code.isBlank() || code.length != 6) return false
        val cleanCode = code.trim()
        val now = System.currentTimeMillis() / 1000L
        val timeStepSeconds = 30L

        val currentCounter = now / timeStepSeconds

        // Cek interval saat ini, interval -30 detik, dan interval +30 detik
        for (counterOffset in listOf(0L, -1L, 1L)) {
            val generated = generateTotp(TotpConstants.MASTER_SECRET, currentCounter + counterOffset)
            if (generated == cleanCode) {
                return true
            }
        }
        return false
    }

    companion object {
        fun generateTotp(base32Secret: String, counter: Long, digits: Int = 6): String {
            val key = decodeBase32(base32Secret)
            val data = ByteBuffer.allocate(8).putLong(counter).array()

            val mac = Mac.getInstance("HmacSHA1")
            mac.init(SecretKeySpec(key, "HmacSHA1"))
            val hash = mac.doFinal(data)

            val offset = (hash[hash.size - 1].toInt() and 0x0F)
            val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
                    ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                    ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                    (hash[offset + 3].toInt() and 0xFF)

            val otp = binary % Math.pow(10.0, digits.toDouble()).toInt()
            return otp.toString().padStart(digits, '0')
        }

        private fun decodeBase32(secret: String): ByteArray {
            val base32Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
            val cleanSecret = secret.trim().uppercase().filter { it in base32Chars }
            var buffer = 0
            var bitsLeft = 0
            val bytes = mutableListOf<Byte>()

            for (c in cleanSecret) {
                val value = base32Chars.indexOf(c)
                buffer = (buffer shl 5) or value
                bitsLeft += 5
                if (bitsLeft >= 8) {
                    bytes.add(((buffer shr (bitsLeft - 8)) and 0xFF).toByte())
                    bitsLeft -= 8
                }
            }
            return bytes.toByteArray()
        }
    }
}

