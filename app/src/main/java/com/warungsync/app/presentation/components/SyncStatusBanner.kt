package com.warungsync.app.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.warungsync.app.domain.model.SyncStatus
import com.warungsync.app.presentation.theme.EmeraldBgLight
import com.warungsync.app.presentation.theme.StatusConnected
import com.warungsync.app.presentation.theme.StatusOffline
import com.warungsync.app.presentation.theme.StatusSyncing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SyncStatusBanner(
    syncStatus: SyncStatus,
    onManualSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasPeers = syncStatus.connectedPeersCount > 0
    val isSyncing = syncStatus.isSyncing

    val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val lastSyncText = if (syncStatus.lastSyncTimestamp > 0) {
        "Sync: ${timeFormatter.format(Date(syncStatus.lastSyncTimestamp))}"
    } else {
        "Belum pernah sync"
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (hasPeers) EmeraldBgLight else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Indicator Dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isSyncing -> StatusSyncing
                                hasPeers -> StatusConnected
                                else -> StatusOffline
                            }
                        )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = if (hasPeers) Icons.Default.Wifi else Icons.Default.WifiOff,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (hasPeers) StatusConnected else StatusOffline
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = if (hasPeers) "${syncStatus.connectedPeersCount} Perangkat Terhubung" else "Mencari perangkat lokal...",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = lastSyncText,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.width(6.dp))

                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = StatusSyncing
                    )
                } else if (hasPeers) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sync Sekarang",
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onManualSyncClick() },
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
