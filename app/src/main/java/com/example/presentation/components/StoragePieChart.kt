package com.example.presentation.components

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.CategoryStat
import com.example.data.model.FileCategory
import com.example.utils.FileUtils
import org.json.JSONArray
import org.json.JSONObject

data class PieSliceData(
    val category: FileCategory,
    val label: String,
    val sizeBytes: Long,
    val count: Int,
    val color: Color,
    val sweepAngle: Float,
    val percentage: Float
)

/**
 * Storage Distribution Pie Chart:
 * Features a dual rendering approach:
 * 1. Interactive D3.js SVG Pie Chart rendered in a lightweight WebView with fluid enter animation,
 *    curved slices, tooltip callouts, and center hole stats.
 * 2. High-performance native Jetpack Compose Donut Chart with animated sweep transitions and interactive category selection.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StoragePieChart(
    categoryStats: Map<FileCategory, CategoryStat>,
    totalUsedBytes: Long,
    modifier: Modifier = Modifier
) {
    var useD3View by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<FileCategory?>(null) }

    val categories = listOf(
        FileCategory.IMAGES,
        FileCategory.VIDEOS,
        FileCategory.AUDIO,
        FileCategory.DOCUMENTS,
        FileCategory.DOWNLOADS,
        FileCategory.ARCHIVES,
        FileCategory.APK,
        FileCategory.OTHER
    )

    val validStats = categories.mapNotNull { cat ->
        val stat = categoryStats[cat]
        val size = stat?.totalSizeBytes ?: 0L
        val count = stat?.count ?: 0
        if (size > 0L) {
            Triple(cat, size, count)
        } else null
    }

    val totalBytes = if (totalUsedBytes > 0L) totalUsedBytes else validStats.sumOf { it.second }.coerceAtLeast(1L)

    val slices = remember(validStats, totalBytes) {
        validStats.map { (cat, size, count) ->
            val fraction = size.toFloat() / totalBytes.toFloat()
            val sweep = fraction * 360f
            PieSliceData(
                category = cat,
                label = cat.title,
                sizeBytes = size,
                count = count,
                color = cat.color,
                sweepAngle = sweep,
                percentage = fraction * 100f
            )
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Toggle header between Native Donut & D3 Interactive SVG
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PieChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Storage Distribution",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Mode toggle pill
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Row(modifier = Modifier.padding(2.dp)) {
                    val activeBg = MaterialTheme.colorScheme.primary
                    val activeText = MaterialTheme.colorScheme.onPrimary
                    val inactiveText = MaterialTheme.colorScheme.onSurfaceVariant

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!useD3View) activeBg else Color.Transparent)
                            .clickable { useD3View = false }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Chart",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (!useD3View) activeText else inactiveText
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (useD3View) activeBg else Color.Transparent)
                            .clickable { useD3View = true }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "D3 Engine",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (useD3View) activeText else inactiveText
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (slices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No storage data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            if (useD3View) {
                // D3-based Pie Chart rendered via HTML5 SVG
                D3PieChartWebView(
                    slices = slices,
                    totalBytes = totalBytes,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                )
            } else {
                // Native animated Compose Donut Chart
                ComposeDonutChart(
                    slices = slices,
                    totalBytes = totalBytes,
                    selectedCategory = selectedCategory,
                    onSelectSlice = { cat ->
                        selectedCategory = if (selectedCategory == cat) null else cat
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Legend FlowRow
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                slices.forEach { slice ->
                    val isSelected = selectedCategory == slice.category
                    val bg = if (isSelected) slice.color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(bg)
                            .clickable {
                                selectedCategory = if (selectedCategory == slice.category) null else slice.category
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(slice.color)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = slice.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${slice.percentage.toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComposeDonutChart(
    slices: List<PieSliceData>,
    totalBytes: Long,
    selectedCategory: FileCategory?,
    onSelectSlice: (FileCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "donut_animation"
    )

    val selectedSlice = slices.firstOrNull { it.category == selectedCategory }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(190.dp)) {
            val strokeWidth = 26.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val radius = diameter / 2f
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)

            var startAngle = -90f

            slices.forEach { slice ->
                val sweep = slice.sweepAngle * animatedProgress
                val isSelected = selectedCategory == slice.category
                val currentStroke = if (isSelected) strokeWidth * 1.25f else strokeWidth

                drawArc(
                    color = slice.color,
                    startAngle = startAngle,
                    sweepAngle = (sweep - 1.5f).coerceAtLeast(0.1f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = currentStroke, cap = StrokeCap.Round)
                )

                startAngle += sweep
            }
        }

        // Center Details
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (selectedSlice != null) {
                Text(
                    text = selectedSlice.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = selectedSlice.color,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = FileUtils.formatFileSize(selectedSlice.sizeBytes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${selectedSlice.count} files (${selectedSlice.percentage.toInt()}%)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Total Used",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = FileUtils.formatFileSize(totalBytes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${slices.size} categories",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun D3PieChartWebView(
    slices: List<PieSliceData>,
    totalBytes: Long,
    modifier: Modifier = Modifier
) {
    val formattedTotal = FileUtils.formatFileSize(totalBytes)

    val jsonArray = JSONArray()
    slices.forEach { slice ->
        val obj = JSONObject()
        obj.put("label", slice.label)
        obj.put("value", slice.sizeBytes)
        obj.put("formatted", FileUtils.formatFileSize(slice.sizeBytes))
        obj.put("count", slice.count)
        val colorHex = String.format("#%06X", (0xFFFFFF and slice.color.toArgb()))
        obj.put("color", colorHex)
        jsonArray.put(obj)
    }

    val html = remember(slices, totalBytes) {
        generateD3PieChartHtml(jsonArray.toString(), formattedTotal)
    }

    AndroidView(
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                webViewClient = WebViewClient()
                loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        }
    )
}

private fun generateD3PieChartHtml(dataJson: String, totalFormatted: String): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
          <script src="https://d3js.org/d3.v7.min.js"></script>
          <style>
            * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, Roboto, sans-serif; }
            body {
              background-color: transparent;
              display: flex;
              flex-direction: column;
              align-items: center;
              justify-content: center;
              height: 100vh;
              overflow: hidden;
            }
            #chart {
              position: relative;
              width: 100%;
              height: 100%;
              display: flex;
              align-items: center;
              justify-content: center;
            }
            .slice {
              cursor: pointer;
              transition: transform 0.2s cubic-bezier(0.4, 0, 0.2, 1);
            }
            .slice:hover {
              filter: brightness(1.1);
            }
            .center-label {
              font-size: 11px;
              font-weight: 500;
              fill: #6c757d;
              text-anchor: middle;
            }
            .center-val {
              font-size: 15px;
              font-weight: 700;
              fill: #212529;
              text-anchor: middle;
            }
            .tooltip {
              position: absolute;
              bottom: 8px;
              left: 50%;
              transform: translateX(-50%);
              background: rgba(33, 37, 41, 0.9);
              color: white;
              padding: 4px 10px;
              border-radius: 6px;
              font-size: 11px;
              pointer-events: none;
              white-space: nowrap;
              opacity: 0;
              transition: opacity 0.2s;
            }
          </style>
        </head>
        <body>
          <div id="chart">
            <div class="tooltip" id="tooltip"></div>
          </div>
          <script>
            const data = $dataJson;
            const width = 230;
            const height = 230;
            const radius = Math.min(width, height) / 2 - 10;
            const innerRadius = radius * 0.62;

            const svg = d3.select("#chart")
              .append("svg")
              .attr("width", "100%")
              .attr("height", "100%")
              .attr("viewBox", `0 0 ${'$'}{width} ${'$'}{height}`)
              .append("g")
              .attr("transform", `translate(${'$'}{width/2}, ${'$'}{height/2})`);

            const pie = d3.pie()
              .value(d => d.value)
              .sort(null)
              .padAngle(0.025);

            const arc = d3.arc()
              .innerRadius(innerRadius)
              .outerRadius(radius)
              .cornerRadius(5);

            const arcHover = d3.arc()
              .innerRadius(innerRadius)
              .outerRadius(radius + 6)
              .cornerRadius(6);

            const tooltip = document.getElementById("tooltip");

            // Center group
            const centerGroup = svg.append("g");
            const labelText = centerGroup.append("text")
              .attr("class", "center-label")
              .attr("y", -6)
              .text("Storage Used");

            const valueText = centerGroup.append("text")
              .attr("class", "center-val")
              .attr("y", 14)
              .text("$totalFormatted");

            // Draw paths
            const path = svg.selectAll("path")
              .data(pie(data))
              .enter()
              .append("path")
              .attr("class", "slice")
              .attr("fill", d => d.data.color)
              .attr("d", arc)
              .each(function(d) { this._current = d; })
              .on("touchstart click mouseenter", function(event, d) {
                d3.select(this).transition().duration(150).attr("d", arcHover);
                labelText.text(d.data.label);
                valueText.text(d.data.formatted);
                tooltip.style.opacity = 1;
                tooltip.innerHTML = `<strong>${'$'}{d.data.label}</strong>: ${'$'}{d.data.formatted} (${'$'}{d.data.count} files)`;
              })
              .on("touchend mouseleave", function() {
                d3.select(this).transition().duration(200).attr("d", arc);
                labelText.text("Storage Used");
                valueText.text("$totalFormatted");
                tooltip.style.opacity = 0;
              });

            // Smooth enter animation
            path.transition()
              .duration(850)
              .attrTween("d", function(d) {
                const interpolate = d3.interpolate({ startAngle: 0, endAngle: 0 }, d);
                return function(t) {
                  return arc(interpolate(t));
                };
              });
          </script>
        </body>
        </html>
    """.trimIndent()
}
