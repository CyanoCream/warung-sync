package com.warungsync.app.presentation.screen.tokolist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import com.warungsync.app.domain.model.MemberRole
import com.warungsync.app.domain.model.Toko

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TokoListScreen(
    tokos: List<Toko>,
    activeTokoId: String?,
    defaultTokoId: String?,
    autoOpenDefault: Boolean,
    deviceName: String,
    onSelectToko: (Toko) -> Unit,
    onSetDefaultToko: (String?) -> Unit,
    onToggleAutoOpen: (Boolean) -> Unit,
    onCreateTokoClick: () -> Unit,
    onJoinTokoClick: () -> Unit,
    onRestoreBackupClick: () -> Unit,
    isRestoringBackup: Boolean,
    backupStatusMessage: String?,
    onLeaveTokoClick: (String) -> Unit
) {
    var tokoToLeave by remember { mutableStateOf<Toko?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("WarungSync", fontWeight = FontWeight.Bold)
                        Text(
                            text = "Perangkat: $deviceName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onJoinTokoClick) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Gabung Toko via WiFi",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateTokoClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Buat Toko Baru") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { paddingValues ->
        if (tokos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Store,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Belum Ada Toko",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Buat toko baru atau gabung ke toko rekan melalui WiFi lokal.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = onCreateTokoClick) {
                            Text("Buat Toko")
                        }
                        OutlinedButton(onClick = onJoinTokoClick) {
                            Text("Gabung Toko")
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onRestoreBackupClick,
                        enabled = !isRestoringBackup
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isRestoringBackup) "Memulihkan…" else "Pulihkan Backup CSV")
                    }
                    backupStatusMessage?.let { message ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (message.contains("gagal", ignoreCase = true)) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pengaturan Toko Default jika punya lebih dari 1 toko
                if (tokos.size > 1) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Buka Toko Utama Otomatis",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (defaultTokoId != null) {
                                            val defaultName = tokos.find { it.id == defaultTokoId }?.namaToko ?: "Toko Utama"
                                            "Langsung buka '$defaultName' saat app dijalankan"
                                        } else {
                                            "Tandai salah satu toko dengan bintang sebagai toko utama"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = autoOpenDefault && defaultTokoId != null,
                                    onCheckedChange = { onToggleAutoOpen(it) },
                                    enabled = defaultTokoId != null
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Daftar Toko (${tokos.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(tokos, key = { it.id }) { toko ->
                    val isDefault = toko.id == defaultTokoId
                    TokoCard(
                        toko = toko,
                        isActive = toko.id == activeTokoId,
                        isDefault = isDefault,
                        showStar = tokos.size > 1,
                        onToggleDefault = {
                            if (isDefault) onSetDefaultToko(null)
                            else onSetDefaultToko(toko.id)
                        },
                        onOpenClick = { onSelectToko(toko) },
                        onLeaveClick = { tokoToLeave = toko }
                    )
                }
            }
        }
    }

    // Dialog Konfirmasi Keluar Toko
    tokoToLeave?.let { toko ->
        AlertDialog(
            onDismissRequest = { tokoToLeave = null },
            title = { Text("Tinggalkan Toko?") },
            text = {
                Text("Apakah Anda yakin ingin keluar dari toko '${toko.namaToko}'? Anda tidak akan menerima sinkronisasi data dari toko ini lagi.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onLeaveTokoClick(toko.id)
                        tokoToLeave = null
                    }
                ) {
                    Text("Keluar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { tokoToLeave = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun TokoCard(
    toko: Toko,
    isActive: Boolean,
    isDefault: Boolean,
    showStar: Boolean,
    onToggleDefault: () -> Unit,
    onOpenClick: () -> Unit,
    onLeaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = toko.namaToko,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    RoleBadge(role = toko.myRole)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Owner: ${toko.ownerDeviceName} • ${toko.memberCount} member",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showStar) {
                    IconButton(onClick = onToggleDefault) {
                        Icon(
                            imageVector = if (isDefault) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Jadikan Toko Utama",
                            tint = if (isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (toko.myRole != MemberRole.OWNER) {
                    IconButton(onClick = onLeaveClick) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Leave toko",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Buka Toko",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun RoleBadge(role: MemberRole) {
    val (bgColor, textColor, label) = when (role) {
        MemberRole.OWNER -> Triple(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary,
            "OWNER"
        )
        MemberRole.ADMIN -> Triple(
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.onSecondary,
            "ADMIN"
        )
        MemberRole.USER -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "VIEWER"
        )
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
