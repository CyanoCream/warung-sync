package com.warungsync.app.presentation.screen.additem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.warungsync.app.domain.model.Category
import com.warungsync.app.domain.model.Item

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditItemSheet(
    categories: List<Category>,
    editingItem: Item? = null,
    onDismiss: () -> Unit,
    onSave: (nama: String, deskripsi: String?, harga: Double, satuan: String, categoryId: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var namaBarang by remember { mutableStateOf(editingItem?.namaBarang ?: "") }
    var deskripsi by remember { mutableStateOf(editingItem?.deskripsi ?: "") }
    var hargaText by remember { mutableStateOf(editingItem?.harga?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
    var satuan by remember { mutableStateOf(editingItem?.satuan ?: "pcs") }
    var selectedCategoryId by remember {
        mutableStateOf(editingItem?.categoryId ?: categories.firstOrNull()?.id ?: "")
    }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var satuanDropdownExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val commonUnits = listOf("pcs", "kg", "gram", "liter", "dus", "pack", "ikat", "botol", "kaleng", "renceng", "sachet", "bungkus")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (editingItem != null) "Edit Barang" else "Tambah Barang Baru",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Nama Barang
            OutlinedTextField(
                value = namaBarang,
                onValueChange = {
                    namaBarang = it
                    errorMessage = null
                },
                label = { Text("Nama Barang *") },
                placeholder = { Text("Contoh: Beras Ramos 5kg") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Deskripsi (Opsional)
            OutlinedTextField(
                value = deskripsi,
                onValueChange = { deskripsi = it },
                label = { Text("Deskripsi (Opsional)") },
                placeholder = { Text("Contoh: Beras pulen wangi kualitas super") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            // Kategori Dropdown
            ExposedDropdownMenuBox(
                expanded = categoryDropdownExpanded,
                onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
            ) {
                val selectedCategoryName = categories.find { it.id == selectedCategoryId }?.namaKategori ?: "Pilih Kategori"
                OutlinedTextField(
                    value = selectedCategoryName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Kategori *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false }
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.namaKategori) },
                            onClick = {
                                selectedCategoryId = category.id
                                categoryDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Harga & Satuan
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = hargaText,
                    onValueChange = {
                        hargaText = it.filter { char -> char.isDigit() || char == '.' }
                        errorMessage = null
                    },
                    label = { Text("Harga (Rp) *") },
                    placeholder = { Text("15000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1.2f),
                    singleLine = true
                )

                // Satuan Dropdown / Input
                ExposedDropdownMenuBox(
                    expanded = satuanDropdownExpanded,
                    onExpandedChange = { satuanDropdownExpanded = !satuanDropdownExpanded },
                    modifier = Modifier.weight(0.8f)
                ) {
                    OutlinedTextField(
                        value = satuan,
                        onValueChange = { satuan = it },
                        label = { Text("Satuan *") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = satuanDropdownExpanded,
                        onDismissRequest = { satuanDropdownExpanded = false }
                    ) {
                        commonUnits.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit) },
                                onClick = {
                                    satuan = unit
                                    satuanDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Error message
            errorMessage?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = onDismiss) {
                    Text("Batal")
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = {
                        val harga = hargaText.toDoubleOrNull()
                        when {
                            namaBarang.isBlank() -> errorMessage = "Nama barang tidak boleh kosong"
                            selectedCategoryId.isBlank() -> errorMessage = "Pilih kategori terlebih dahulu"
                            harga == null || harga <= 0 -> errorMessage = "Harga harus berupa angka valid > 0"
                            satuan.isBlank() -> errorMessage = "Satuan tidak boleh kosong"
                            else -> {
                                onSave(namaBarang.trim(), deskripsi.trim().ifBlank { null }, harga, satuan.trim(), selectedCategoryId)
                                onDismiss()
                            }
                        }
                    }
                ) {
                    Text(if (editingItem != null) "Simpan Perubahan" else "Tambah Barang")
                }
            }
        }
    }
}
