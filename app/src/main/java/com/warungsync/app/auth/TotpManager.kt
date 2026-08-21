package com.warungsync.app.auth

import dev.turingcomplete.kotlinonetimepassword.HmacAlgorithm
import dev.turingcomplete.kotlinonetimepassword.TimeBasedOneTimePasswordConfig
import dev.turingcomplete.kotlinonetimepassword.TimeBasedOneTimePasswordGenerator
import org.apache.commons.codec.binary.Base32
import java.util.concurrent.TimeUnit

class TotpManager {

    private val generator: TimeBasedOneTimePasswordGenerator by lazy {
        val config = TimeBasedOneTimePasswordConfig(
            timeStep = 30,
            timeStepUnit = TimeUnit.SECONDS,
            codeDigits = 6,
            hmacAlgorithm = HmacAlgorithm.SHA1
        )
        // Decode Base32 secret string into ByteArray
        val secretBytes = try {
            Base32().decode(TotpConstants.MASTER_SECRET)
        } catch (e: Exception) {
            TotpConstants.MASTER_SECRET.toByteArray(Charsets.UTF_8)
        }
        TimeBasedOneTimePasswordGenerator(secretBytes, config)
    }

    /**
     * Verifikasi kode 6 digit TOTP dari user dengan toleransi ±1 time window (clock drift).
     */
    fun verifyMasterCode(code: String): Boolean {
        if (code.isBlank() || code.length != 6) return false
        val cleanCode = code.trim()
        val now = System.currentTimeMillis()
        val stepMillis = 30_000L

        // Cek interval saat ini, interval -30 detik, dan interval +30 detik
        for (offset in listOf(0L, -stepMillis, stepMillis)) {
            val generated = generator.generate(now + offset)
            if (generated == cleanCode) {
                return true
            }
        }
        return false
    }
}
