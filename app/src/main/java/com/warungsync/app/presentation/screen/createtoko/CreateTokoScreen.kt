package com.warungsync.app.presentation.screen.createtoko

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTokoScreen(
    createdTokoCount: Int,
    isLoading: Boolean,
    errorMessage: String?,
    onBackClick: () -> Unit,
    onCreateToko: (namaToko: String, totpCode: String?) -> Unit
) {
    var namaToko by remember { mutableStateOf("") }
    var totpCode by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val isFirstToko = createdTokoCount == 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isFirstToko) "Buat Toko Pertama (Gratis)" else "Buat Toko Baru") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Icon(
                imageVector = if (isFirstToko) Icons.Default.Store else Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = if (isFirstToko) "Mulai Warung Digital Anda" else "Aktivasi Toko Tambahan",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (isFirstToko) {
                    "Sebagai pemilik toko (OWNER), Anda dapat mengelola kategori, barang, harga, dan menambahkan admin atau kasir untuk sinkronisasi lokal."
                } else {
                    "Perangkat ini sudah pernah membuat toko. Untuk membuat toko tambahan (#${createdTokoCount + 1}), masukkan kode OTP lisensi 6-digit dari pemilik aplikasi."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            OutlinedTextField(
                value = namaToko,
                onValueChange = {
                    namaToko = it
                    localError = null
                },
                label = { Text("Nama Toko / Warung *") },
                placeholder = { Text("Contoh: Toko Berkah Mulya") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Form TOTP jika toko ke-2 dst
            if (!isFirstToko) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "🔐 Lisensi Toko Tambahan",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Minta 6-digit kode OTP ke pengembang/pemilik aplikasi untuk mengotorisasi toko baru.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = totpCode,
                            onValueChange = {
                                if (it.length <= 6) {
                                    totpCode = it.filter { char -> char.isDigit() }
                                    localError = null
                                }
                            },
                            label = { Text("Kode OTP (6 digit) *") },
                            placeholder = { Text("123456") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }

            val displayError = localError ?: errorMessage
            if (displayError != null) {
                Text(
                    text = displayError,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    when {
                        namaToko.isBlank() -> localError = "Nama toko tidak boleh kosong"
                        !isFirstToko && totpCode.length != 6 -> localError = "Kode OTP harus 6 digit angka"
                        else -> onCreateToko(namaToko.trim(), if (isFirstToko) null else totpCode.trim())
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = if (isFirstToko) "Buat Toko Sekarang" else "Verifikasi & Buat Toko",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
