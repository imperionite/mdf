package com.imperionite.mdf.ui.prescriptions

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.imperionite.mdf.data.local.entity.PrescriptionMedicineEntity
import com.imperionite.mdf.data.local.entity.PurchaseBatchWithItems
import com.imperionite.mdf.data.repository.PrescriptionDetails
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

private data class RecordedPrice(
    val date: String,
    val batchId: Long,
    val quantity: Double,
    val unitPriceCentavos: Long
)

private data class BatchSpending(
    val batchId: Long,
    val date: String,
    val totalCentavos: Long
)

private val reportChartPalette = listOf(
    Color(0xFF527D8B),
    Color(0xFF86B0BD),
    Color(0xFFAEC4D4),
    Color(0xFFCFEBFF),
    Color(0xFF78909C),
    Color(0xFFB7C4CF),
    Color(0xFF607D8B),
    Color(0xFF9DBCC7),
    Color(0xFF7897A8),
    Color(0xFFD0DDE3)
)

private fun chartColor(index: Int): Color {
    return reportChartPalette[index % reportChartPalette.size]
}

@Composable
@Suppress("FunctionName")
fun PrescriptionInsightsScreen(
    details: PrescriptionDetails,
    onBack: () -> Unit
) {
    val prescription = details.prescription.prescription
    val medicines = details.prescription.medicines

    val batches = details.purchaseBatches.sortedWith(
        compareBy<PurchaseBatchWithItems> { it.batch.purchaseDate }
            .thenBy { it.batch.id }
    )

    val spending = batches.map { batch ->
        BatchSpending(
            batchId = batch.batch.id,
            date = batch.batch.purchaseDate,
            totalCentavos = batchTotal(batch)
        )
    }

    val totalSpending = spending.fold(0L) { total, item ->
        Math.addExact(total, item.totalCentavos)
    }

    val totalPurchasedQuantity = batches
        .flatMap { it.items }
        .sumOf { it.purchasedQuantity }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Prescription Insights & History",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            "Physician: ${prescription.physicianName}",
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            "Prescription date: ${prescription.prescriptionDate}",
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            "This report summarizes saved purchase transactions. " +
                    "It reflects recorded purchases, not independently verified " +
                    "medicine use or current physical stock.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            "Spending Overview",
            style = MaterialTheme.typography.titleLarge
        )

        SummaryCard(
            title = "Total recorded spending",
            value = formatReportMoney(totalSpending),
            detail = "Across ${batches.size} recorded purchase batch(es)"
        )

        SummaryCard(
            title = "Total quantity purchased",
            value = formatReportQuantity(totalPurchasedQuantity),
            detail = "Combined quantities across all recorded items; " +
                    "different medicine units are not directly comparable"
        )

        if (batches.isEmpty()) {
            SummaryCard(
                title = "Purchase history",
                value = "No recorded purchases",
                detail = "Record a purchase to begin building this report."
            )
        } else {
            Text(
                "Spending by Purchase Batch",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                "The chart shows each batch's share of total recorded " +
                        "spending. See the legend below for exact amounts and dates.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SpendingPieChart(
                spending = spending,
                totalSpending = totalSpending
            )

            spending.forEachIndexed { index, batch ->
                val previous = spending.getOrNull(index - 1)
                val difference = previous?.let {
                    batch.totalCentavos - it.totalCentavos
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Batch #${index + 1}",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text("Purchase date: ${batch.date}")
                        Text(
                            "Batch total: ${
                                formatReportMoney(batch.totalCentavos)
                            }"
                        )

                        if (totalSpending > 0L) {
                            val percentage =
                                batch.totalCentavos.toDouble() /
                                        totalSpending.toDouble() * 100.0

                            Text(
                                "Share of total spending: " +
                                        String.format(
                                            Locale.US,
                                            "%.2f%%",
                                            percentage
                                        )
                            )
                        }

                        if (difference != null) {
                            val description = when {
                                difference > 0L -> "Higher"
                                difference < 0L -> "Lower"
                                else -> "Unchanged"
                            }

                            Text(
                                "$description than previous batch by " +
                                        formatReportMoney(
                                            kotlin.math.abs(difference)
                                        )
                            )
                        } else {
                            Text("First recorded batch in this report.")
                        }
                    }
                }
            }
        }

        Text(
            "Medicine Price Analysis",
            style = MaterialTheme.typography.titleLarge
        )

        if (medicines.isEmpty()) {
            Text("No medicines are recorded for this prescription.")
        } else {
            medicines.forEach { medicine ->
                MedicinePriceCard(
                    medicine = medicine,
                    batches = batches
                )
            }
        }

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Prescription Details")
        }
    }
}

@Composable
@Suppress("FunctionName")
private fun SummaryCard(
    title: String,
    value: String,
    detail: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
@Suppress("FunctionName")
private fun SpendingPieChart(
    spending: List<BatchSpending>,
    totalSpending: Long
) {
    if (spending.isEmpty()) return

    val outlineColor = MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (totalSpending <= 0L) {
                Text(
                    "There is no positive recorded spending to display. " +
                            "Batch totals are zero.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                ) {
                    val diameter = minOf(size.width, size.height) * 0.88f
                    val topLeft = Offset(
                        x = (size.width - diameter) / 2f,
                        y = (size.height - diameter) / 2f
                    )

                    var startAngle = -90f

                    spending.forEachIndexed { index, batch ->
                        val sweepAngle =
                            batch.totalCentavos.toDouble() /
                                    totalSpending.toDouble() * 360.0

                        if (sweepAngle > 0.0) {
                            drawArc(
                                color = chartColor(index),
                                startAngle = startAngle,
                                sweepAngle = sweepAngle.toFloat(),
                                useCenter = true,
                                topLeft = topLeft,
                                size = androidx.compose.ui.geometry.Size(
                                    diameter,
                                    diameter
                                )
                            )

                            startAngle += sweepAngle.toFloat()
                        }
                    }

                    // Thin separators make adjacent slices easier to distinguish.
                    startAngle = -90f

                    spending.forEach { batch ->
                        val sweepAngle =
                            batch.totalCentavos.toDouble() /
                                    totalSpending.toDouble() * 360.0

                        if (sweepAngle > 0.0) {
                            drawArc(
                                color = outlineColor,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle.toFloat(),
                                useCenter = true,
                                topLeft = topLeft,
                                size = androidx.compose.ui.geometry.Size(
                                    diameter,
                                    diameter
                                ),
                                style = Stroke(width = 2.dp.toPx())
                            )

                            startAngle += sweepAngle.toFloat()
                        }
                    }
                }
            }

            Text(
                "Batch Legend",
                style = MaterialTheme.typography.titleMedium
            )

            spending.forEachIndexed { index, batch ->
                val percentage = if (totalSpending > 0L) {
                    batch.totalCentavos.toDouble() /
                            totalSpending.toDouble() * 100.0
                } else {
                    0.0
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .size(14.dp)
                    ) {
                        Canvas(modifier = Modifier.size(14.dp)) {
                            drawCircle(color = chartColor(index))
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            "Batch #${index + 1} · ${batch.date}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Text(
                            "${formatReportMoney(batch.totalCentavos)} · " +
                                    String.format(
                                        Locale.US,
                                        "%.2f%%",
                                        percentage
                                    ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                "Colors repeat after ${reportChartPalette.size} batches. " +
                        "The legend identifies every batch individually.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
@Suppress("FunctionName")
private fun MedicinePriceCard(
    medicine: PrescriptionMedicineEntity,
    batches: List<PurchaseBatchWithItems>
) {
    val records = batches.flatMap { batch ->
        batch.items
            .filter { it.prescriptionMedicineId == medicine.id }
            .map { item ->
                RecordedPrice(
                    date = batch.batch.purchaseDate,
                    batchId = batch.batch.id,
                    quantity = item.purchasedQuantity,
                    unitPriceCentavos = item.unitPriceCentavos
                )
            }
    }.sortedWith(
        compareBy<RecordedPrice> { it.date }
            .thenBy { it.batchId }
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                medicine.medicineName,
                style = MaterialTheme.typography.titleMedium
            )

            Text("Unit: ${medicine.unit}")

            if (records.isEmpty()) {
                Text(
                    "No purchase price history is available for this medicine."
                )
                Text(
                    "Current/original recorded unit price: " +
                            formatReportMoney(medicine.unitPriceCentavos)
                )
            } else {
                val minimum = records.minOf { it.unitPriceCentavos }
                val maximum = records.maxOf { it.unitPriceCentavos }
                val totalQuantity = records.sumOf { it.quantity }

                val weightedAverage = if (totalQuantity > 0.0) {
                    BigDecimal.valueOf(
                        records.sumOf {
                            it.quantity * it.unitPriceCentavos.toDouble()
                        } / totalQuantity
                    ).setScale(0, RoundingMode.HALF_UP).longValueExact()
                } else {
                    0L
                }

                val firstPrice = records.first().unitPriceCentavos
                val latestPrice = records.last().unitPriceCentavos
                val change = latestPrice - firstPrice

                Text("Recorded purchases: ${records.size}")
                Text("Minimum recorded unit price: ${formatReportMoney(minimum)}")
                Text("Maximum recorded unit price: ${formatReportMoney(maximum)}")
                Text("Quantity-weighted average: ${formatReportMoney(weightedAverage)}")
                Text("First recorded price: ${formatReportMoney(firstPrice)}")
                Text("Latest recorded price: ${formatReportMoney(latestPrice)}")

                Text(
                    "Absolute change: " +
                            (if (change > 0L) "+" else "") +
                            formatReportMoney(change)
                )

                if (firstPrice > 0L) {
                    val percentage =
                        change.toDouble() / firstPrice.toDouble() * 100.0

                    Text(
                        "Percentage change: " +
                                String.format(
                                    Locale.US,
                                    "%+.2f%%",
                                    percentage
                                )
                    )
                } else {
                    Text(
                        "Percentage change: unavailable because the first " +
                                "recorded price is zero."
                    )
                }

                Text(
                    "Recorded Unit-Price Trend",
                    style = MaterialTheme.typography.titleSmall
                )

                PriceTrendChart(records.map { it.unitPriceCentavos })

                records.forEachIndexed { index, record ->
                    Text(
                        "${index + 1}. ${record.date} — " +
                                "${formatReportMoney(record.unitPriceCentavos)} " +
                                "per ${medicine.unit}"
                    )
                }

                Text(
                    "Prices reflect saved transactions. Differences may " +
                            "reflect changes in supplier, product, package, " +
                            "discounts, or other factors; the records alone " +
                            "cannot establish the cause.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
@Suppress("FunctionName")
private fun PriceTrendChart(
    prices: List<Long>
) {
    if (prices.isEmpty()) return

    val lineColor = MaterialTheme.colorScheme.primary
    val pointColor = MaterialTheme.colorScheme.tertiary
    val axisColor = MaterialTheme.colorScheme.outlineVariant

    val minimum = prices.minOrNull()!!.toDouble()
    val maximum = prices.maxOrNull()!!.toDouble()
    val range = (maximum - minimum).coerceAtLeast(1.0)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
    ) {
        val left = 12f
        val right = size.width - 12f
        val top = 12f
        val bottom = size.height - 12f

        drawLine(
            color = axisColor,
            start = Offset(left, bottom),
            end = Offset(right, bottom),
            strokeWidth = 2f
        )

        val points = prices.mapIndexed { index, price ->
            val x = if (prices.size == 1) {
                (left + right) / 2f
            } else {
                left + (right - left) *
                        index.toFloat() / (prices.size - 1).toFloat()
            }

            val normalized = (price.toDouble() - minimum) / range
            val y = bottom - normalized.toFloat() * (bottom - top)

            Offset(x, y)
        }

        if (points.size > 1) {
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { point ->
                    lineTo(point.x, point.y)
                }
            }

            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 3f)
            )
        }

        points.forEach { point ->
            drawCircle(
                color = pointColor,
                radius = 5f,
                center = point
            )
        }
    }
}

private fun batchTotal(batch: PurchaseBatchWithItems): Long {
    return batch.items.fold(0L) { total, item ->
        val itemTotal = BigDecimal.valueOf(item.purchasedQuantity)
            .multiply(BigDecimal.valueOf(item.unitPriceCentavos))
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()

        Math.addExact(total, itemTotal)
    }
}

private fun formatReportMoney(centavos: Long): String {
    return NumberFormat.getCurrencyInstance(
        Locale.forLanguageTag("en-PH")
    ).format(centavos / 100.0)
}

private fun formatReportQuantity(quantity: Double): String {
    return BigDecimal.valueOf(quantity)
        .stripTrailingZeros()
        .toPlainString()
}
