package com.example.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CategoryStat
import com.example.data.model.FileCategory
import com.example.data.model.StorageInfo
import com.example.utils.FileUtils

@Composable
fun StorageBar(
    storageInfo: StorageInfo,
    categoryStats: Map<FileCategory, CategoryStat>,
    modifier: Modifier = Modifier
) {
    val total = storageInfo.totalBytes.coerceAtLeast(1L)
    val used = storageInfo.usedBytes
    val free = storageInfo.freeBytes

    val usedFormatted = FileUtils.formatFileSize(used)
    val totalFormatted = FileUtils.formatFileSize(total)
    val freeFormatted = FileUtils.formatFileSize(free)
    val usedRatio = (used.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    val animatedUsedRatio by animateFloatAsState(
        targetValue = usedRatio,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "storage_used_ratio"
    )
    val usedPercent = (animatedUsedRatio * 100).toInt().coerceIn(0, 100)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Internal Storage",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$usedFormatted used of $totalFormatted",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                            )
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "$usedPercent%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Segmented Visual Bar with smooth rounded styling
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                val relevantCategories = listOf(
                    FileCategory.IMAGES,
                    FileCategory.VIDEOS,
                    FileCategory.AUDIO,
                    FileCategory.DOCUMENTS,
                    FileCategory.ARCHIVES,
                    FileCategory.APK
                )

                var accumulatedWeight = 0f
                for (cat in relevantCategories) {
                    val catBytes = categoryStats[cat]?.totalSizeBytes ?: 0L
                    val weight = (catBytes.toFloat() / total.toFloat()).coerceAtLeast(0f)
                    if (weight > 0.005f) {
                        accumulatedWeight += weight
                        Box(
                            modifier = Modifier
                                .weight(weight)
                                .height(14.dp)
                                .background(cat.color)
                        )
                    }
                }

                // Other used space
                val otherUsedBytes = (used - relevantCategories.sumOf { categoryStats[it]?.totalSizeBytes ?: 0L }).coerceAtLeast(0L)
                val otherWeight = (otherUsedBytes.toFloat() / total.toFloat()).coerceAtLeast(0f)
                if (otherWeight > 0.005f) {
                    accumulatedWeight += otherWeight
                    Box(
                        modifier = Modifier
                            .weight(otherWeight)
                            .height(14.dp)
                            .background(FileCategory.OTHER.color)
                    )
                }

                // Remaining free space
                val freeWeight = (1f - accumulatedWeight).coerceAtLeast(0.01f)
                Box(
                    modifier = Modifier
                        .weight(freeWeight)
                        .height(14.dp)
                        .background(Color.Transparent)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Free Space Indicator & Legend Dots
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Free space: $freeFormatted",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "$totalFormatted total",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Quick Category Key Dots
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StorageLegendItem(color = FileCategory.IMAGES.color, label = "Images")
            StorageLegendItem(color = FileCategory.VIDEOS.color, label = "Videos")
            StorageLegendItem(color = FileCategory.DOCUMENTS.color, label = "Docs")
            StorageLegendItem(color = FileCategory.APK.color, label = "Apps")
            StorageLegendItem(color = FileCategory.ARCHIVES.color, label = "ZIP")
        }
    }
}

@Composable
private fun StorageLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
        )
    }
}
