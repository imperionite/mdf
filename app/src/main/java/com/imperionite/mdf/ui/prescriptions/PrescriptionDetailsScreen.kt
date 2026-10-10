package com.imperionite.mdf.ui.prescriptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.imperionite.mdf.data.local.entity.PrescriptionMedicineEntity
import com.imperionite.mdf.data.repository.PrescriptionDetails
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val trackingDateFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

@Composable
fun PrescriptionDetailsScreen(
    details: PrescriptionDetails,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onNewPurchase: () -> Unit,
    onEditBatch: (Long) -> Unit,
    onDeleteBatch: (Long) -> Unit
) {
    val prescription = details.prescription.prescription
    val medicines = details.prescription.medicines
    val batches = details.purchaseBatches
    val latestBatchId = batches.maxOfOrNull { it.batch.id }

    var batchPendingDeletion by remember {
        mutableStateOf<Long?>(null)
    }

    if (batchPendingDeletion != null) {
        AlertDialog(
            onDismissRequest = { batchPendingDeletion = null },
            title = { Text("Delete purchase batch?") },
            text = {
                Text(
                    "This will permanently delete the selected purchase " +
                            "transaction and its items. The remaining medicine " +
                            "quantities will be recalculated."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = batchPendingDeletion
                        batchPendingDeletion = null
                        if (id != null) onDeleteBatch(id)
                    }
                ) {
                    Text(
                        "Delete",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { batchPendingDeletion = null }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Prescription Details",
            style = MaterialTheme.typography.headlineSmall
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Physician: ${prescription.physicianName}")
                Text("Prescription date: ${prescription.prescriptionDate}")

                if (prescription.notes.isNotBlank()) {
                    Text("Notes: ${prescription.notes}")
                }

                if (prescription.attachmentUri != null) {
                    Text(
                        "Prescription attachment is saved.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Text(
            "Prescribed Medicines",
            style = MaterialTheme.typography.titleMedium
        )

        if (medicines.isEmpty()) {
            Text("No medicines are recorded for this prescription.")
        } else {
            medicines.forEach { medicine ->
                MedicineQuantityCard(
                    medicine = medicine,
                    purchasedQuantity =
                        details.purchasedQuantities[medicine.id] ?: 0.0
                )
            }
        }

        // New advisory tracking report.
        Text(
            "Next Purchase Tracking",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            "These dates are estimates based on the recorded dosage " +
                    "regimen and purchase history. Verify actual stock before " +
                    "planning a purchase.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (medicines.isEmpty()) {
            Text("Add a medicine to see its tracking report.")
        } else {
            medicines.forEach { medicine ->
                NextPurchaseTrackingCard(
                    medicine = medicine,
                    batches = batches
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }

            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.weight(1f)
            ) {
                Text("Edit Prescription")
            }
        }

        Button(
            onClick = onNewPurchase,
            modifier = Modifier.fillMaxWidth(),
            enabled = medicines.any {
                val purchased = details.purchasedQuantities[it.id] ?: 0.0
                it.prescribedQuantity - purchased > 0.000001
            }
        ) {
            Text("Record Purchase")
        }

        Text(
            "Purchase History",
            style = MaterialTheme.typography.titleMedium
        )

        if (batches.isEmpty()) {
            Text("No purchases have been recorded yet.")
        } else {
            batches.forEach { batch ->
                val batchId = batch.batch.id
                val isLatest = batchId == latestBatchId

                val totalCentavos = batch.items.fold(0L) { total, item ->
                    val itemTotal = BigDecimal.valueOf(
                        item.purchasedQuantity
                    ).multiply(
                        BigDecimal.valueOf(item.unitPriceCentavos)
                    ).setScale(0, RoundingMode.HALF_UP).longValueExact()

                    Math.addExact(total, itemTotal)
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Batch #$batchId",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text("Purchase date: ${batch.batch.purchaseDate}")
                        Text("Items: ${batch.items.size}")
                        Text("Batch total: ${formatMoney(totalCentavos)}")

                        if (batch.batch.notes.isNotBlank()) {
                            Text("Notes: ${batch.batch.notes}")
                        }

                        batch.items.forEach { item ->
                            val medicineName = medicines.firstOrNull {
                                it.id == item.prescriptionMedicineId
                            }?.medicineName ?: "Medicine"

                            Text(
                                "$medicineName: " +
                                        "${formatQuantity(item.purchasedQuantity)} × " +
                                        "${formatMoney(item.unitPriceCentavos)}"
                            )
                        }

                        if (isLatest) {
                            Text(
                                "This is the latest saved batch. " +
                                        "You can edit or delete it.",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onEditBatch(batchId) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Edit Batch")
                                }

                                OutlinedButton(
                                    onClick = {
                                        batchPendingDeletion = batchId
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        "Delete",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        } else {
                            Text(
                                "Locked: a newer purchase batch exists.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NextPurchaseTrackingCard(
    medicine: PrescriptionMedicineEntity,
    batches: List<com.imperionite.mdf.data.local.entity.PurchaseBatchWithItems>
) {
    val report = calculateNextPurchaseTracking(
        medicine = medicine,
        batches = batches
    )

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                medicine.medicineName,
                style = MaterialTheme.typography.titleMedium
            )

            Text("Posology: ${medicine.dosageRegimen}")

            Text(
                "Status: ${report.status}",
                style = MaterialTheme.typography.titleSmall,
                color = when (report.status) {
                    "Purchase Soon", "Possible Run-Out" ->
                        MaterialTheme.colorScheme.error

                    "Manual Review Required", "Review Supply History" ->
                        MaterialTheme.colorScheme.tertiary

                    else -> MaterialTheme.colorScheme.primary
                }
            )

            report.dailyConsumption?.let { daily ->
                Text(
                    "Estimated daily consumption: " +
                            "${formatQuantity(daily)} ${medicine.unit}/day"
                )
            }

            report.estimatedCurrentStock?.let { stock ->
                Text(
                    "Estimated current stock: " +
                            "${formatQuantity(stock)} ${medicine.unit}"
                )
            }

            report.estimatedRunOutDate?.let { date ->
                Text(
                    "Estimated run-out date: " +
                            date.format(trackingDateFormatter)
                )
            }

            report.suggestedPurchaseDate?.let { date ->
                Text(
                    "Suggested next purchase: " +
                            date.format(trackingDateFormatter),
                    style = MaterialTheme.typography.titleSmall
                )
            }

            Text(
                report.detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MedicineQuantityCard(
    medicine: PrescriptionMedicineEntity,
    purchasedQuantity: Double
) {
    val remaining = (
            medicine.prescribedQuantity - purchasedQuantity
            ).coerceAtLeast(0.0)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                medicine.medicineName,
                style = MaterialTheme.typography.titleMedium
            )

            Text("Dosage/strength: ${medicine.dosage}")
            Text("Dosage regimen: ${medicine.dosageRegimen}")
            Text(
                "Prescribed: ${formatQuantity(medicine.prescribedQuantity)} " +
                        medicine.unit
            )
            Text(
                "Purchased: ${formatQuantity(purchasedQuantity)} " +
                        medicine.unit
            )
            Text(
                "Remaining: ${formatQuantity(remaining)} ${medicine.unit}"
            )
            Text(
                "Original unit price: " +
                        formatMoney(medicine.unitPriceCentavos)
            )
        }
    }
}

private fun formatQuantity(quantity: Double): String {
    return BigDecimal.valueOf(quantity)
        .stripTrailingZeros()
        .toPlainString()
}

private fun formatMoney(centavos: Long): String {
    return NumberFormat.getCurrencyInstance(Locale("en", "PH"))
        .format(centavos / 100.0)
}
