package com.warungsync.app.auth

object TotpConstants {
    // Master secret key (Base32 encoded) - hanya developer/pemilik app yang tahu
    // Digunakan untuk verifikasi pembuatan toko ke-2 dst
    const val MASTER_SECRET = "JBSWY3DPEHPK3PXP"
    const val ISSUER = "WarungSync"
    const val ACCOUNT = "MasterLicense"
}
