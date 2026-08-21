package com.warungsync.app.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Interactive SearchBar yang langsung memfilter data saat user mengetik (real-time/on-the-fly)
 * tanpa perlu menekan tombol submit/search di keyboard.
 */
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Cari...",
    containerColor: Color? = null,
    accentColor: Color? = null,
    onSearchDone: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val resolvedContainerColor = containerColor ?: MaterialTheme.colorScheme.surface
    val resolvedAccentColor = accentColor ?: MaterialTheme.colorScheme.primary

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        placeholder = {
            Text(
                text = placeholder,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = resolvedAccentColor,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Hapus",
                    modifier = Modifier.clickable { onQueryChange("") },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = { onSearchDone?.invoke() }
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = resolvedAccentColor,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor = resolvedContainerColor,
            unfocusedContainerColor = resolvedContainerColor
        )
    )
}
