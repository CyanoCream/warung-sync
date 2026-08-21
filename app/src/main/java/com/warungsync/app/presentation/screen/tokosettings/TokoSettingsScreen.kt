package com.warungsync.app.presentation.screen.tokosettings

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.warungsync.app.domain.model.TokoMember
import com.warungsync.app.presentation.screen.tokolist.RoleBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TokoSettingsScreen(
    toko: Toko,
    members: List<TokoMember>,
    isOwner: Boolean,
    onBackClick: () -> Unit,
    onUpdateNamaToko: (String) -> Unit,
    onUpdateMemberRole: (memberDeviceId: String, newRole: MemberRole) -> Unit,
    onKickMember: (memberDeviceId: String) -> Unit,
    onDeleteToko: () -> Unit
) {
    var isEditingName by remember { mutableStateOf(false) }
    var newTokoName by remember(toko.namaToko) { mutableStateOf(toko.namaToko) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var memberToKick by remember { mutableStateOf<TokoMember?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kelola Toko") },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Toko Info & Name Change (Owner only)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Informasi Toko",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (isEditingName && isOwner) {
                            OutlinedTextField(
                                value = newTokoName,
                                onValueChange = { newTokoName = it },
                                label = { Text("Nama Toko") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(onClick = {
                                    newTokoName = toko.namaToko
                                    isEditingName = false
                                }) {
                                    Text("Batal")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = {
                                    if (newTokoName.isNotBlank()) {
                                        onUpdateNamaToko(newTokoName.trim())
                                        isEditingName = false
                                    }
                                }) {
                                    Text("Simpan")
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = toko.namaToko,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Role Anda: ${toko.myRole.name}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isOwner) {
                                    IconButton(onClick = { isEditingName = true }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Nama Toko")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Members List
            item {
                Text(
                    text = "Daftar Perangkat Terhubung (${members.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(members, key = { it.deviceId }) { member ->
                MemberCard(
                    member = member,
                    isOwnerViewer = isOwner,
                    onRoleChange = { newRole -> onUpdateMemberRole(member.deviceId, newRole) },
                    onKick = { memberToKick = member }
                )
            }

            // Section 3: Delete Toko (Owner only)
            if (isOwner) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showDeleteConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Hapus Toko Permanen", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Dialog Konfirmasi Hapus Toko
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Hapus Toko?") },
            text = {
                Text("Semua data barang, kategori, dan riwayat harga toko '${toko.namaToko}' akan dihapus. Aksi ini tidak dapat dibatalkan.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteToko()
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog Konfirmasi Keluarkan Member
    memberToKick?.let { member ->
        AlertDialog(
            onDismissRequest = { memberToKick = null },
            title = { Text("Keluarkan Member?") },
            text = {
                Text("Apakah Anda yakin ingin mengeluarkan perangkat '${member.deviceName}' dari toko?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onKickMember(member.deviceId)
                        memberToKick = null
                    }
                ) {
                    Text("Keluarkan", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToKick = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun MemberCard(
    member: TokoMember,
    isOwnerViewer: Boolean,
    onRoleChange: (MemberRole) -> Unit,
    onKick: () -> Unit
) {
    var roleMenuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = member.deviceName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ID: ${member.deviceId.take(8)}...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isOwnerViewer && member.role != MemberRole.OWNER) {
                    Box {
                        OutlinedButton(
                            onClick = { roleMenuExpanded = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(member.role.name)
                        }

                        DropdownMenu(
                            expanded = roleMenuExpanded,
                            onDismissRequest = { roleMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("ADMIN (Bisa Tambah/Edit Barang)") },
                                onClick = {
                                    onRoleChange(MemberRole.ADMIN)
                                    roleMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("USER (Hanya Lihat Harga)") },
                                onClick = {
                                    onRoleChange(MemberRole.USER)
                                    roleMenuExpanded = false
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(onClick = onKick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Kick Member",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    RoleBadge(role = member.role)
                }
            }
        }
    }
}
