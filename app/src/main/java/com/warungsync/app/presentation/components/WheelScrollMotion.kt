package com.warungsync.app.presentation.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Keeps the three items nearest the viewport center steady, then progressively
 * fades and shrinks items toward the top and bottom edges. The work stays
 * inside a graphics layer so scrolling does not trigger layout.
 */
fun Modifier.wheelScrollMotion(
    listState: LazyListState,
    itemKey: String,
    enabled: Boolean
): Modifier {
    if (!enabled) return this

    return graphicsLayer {
        val layoutInfo = listState.layoutInfo
        val viewportCenter =
            (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
        val halfViewport =
            (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2f

        val currentItem = layoutInfo.visibleItemsInfo.firstOrNull { it.key == itemKey }

        val item = currentItem
        if (item == null || item.size <= 0 || halfViewport <= 0f) {
            alpha = 0f
            return@graphicsLayer
        }

        val itemCenter = item.offset + item.size / 2f
        val itemDistance = abs(itemCenter - viewportCenter)
        val stableBoundary = item.size * 1.15f
        val effectRange = (halfViewport - stableBoundary).coerceAtLeast(1f)
        val distanceEffect = ((itemDistance - stableBoundary) / effectRange)
            .coerceIn(0f, 1f)

        val visibleStart = max(item.offset, layoutInfo.viewportStartOffset)
        val visibleEnd = min(item.offset + item.size, layoutInfo.viewportEndOffset)
        val visibleFraction = ((visibleEnd - visibleStart).toFloat() / item.size)
            .coerceIn(0f, 1f)
        val clippedEffect = (1f - visibleFraction).coerceIn(0f, 1f)
        val rawEffect = max(distanceEffect, clippedEffect)
        val wheelEffect = rawEffect * (2f - rawEffect)
        val isAboveCenter = itemCenter < viewportCenter

        alpha = 1f - 0.82f * wheelEffect
        val scale = 1f - 0.28f * wheelEffect
        scaleX = scale
        scaleY = scale
        rotationX = 0f
        translationY = if (isAboveCenter) {
            -item.size * 0.05f * wheelEffect
        } else {
            item.size * 0.05f * wheelEffect
        }
        transformOrigin = TransformOrigin.Center
        compositingStrategy = CompositingStrategy.ModulateAlpha
    }
}
