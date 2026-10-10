package com.imperionite.mdf.ui.prescriptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.imperionite.mdf.data.local.entity.PrescriptionMedicineEntity
import com.imperionite.mdf.data.repository.PrescriptionDetails
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PrescriptionDetailsScreen(
    details: PrescriptionDetails,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onNewPurchase: () -> Unit
) {
    val prescription = details.prescription.prescription
    val medicines = details.prescription.medicines

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

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors()
        ) {
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
                        text = "Prescription attachment is saved.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Text(
            text = "Prescribed Medicines",
            style = MaterialTheme.typography.titleMedium
        )

        if (medicines.isEmpty()) {
            Text("No medicines are recorded for this prescription.")
        } else {
            medicines.forEach { medicine ->
                MedicineQuantityCard(
                    medicine = medicine,
                    purchasedQuantity = details.purchasedQuantities[medicine.id] ?: 0.0
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
                Text("Edit")
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
            text = "Purchase History",
            style = MaterialTheme.typography.titleMedium
        )

        if (details.purchaseBatches.isEmpty()) {
            Text("No purchases have been recorded yet.")
        } else {
            details.purchaseBatches.forEach { batch ->
                val totalCentavos = batch.items.sumOf {
                    (it.purchasedQuantity * it.unitPriceCentavos).toLong()
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Purchase date: ${batch.batch.purchaseDate}")
                        Text("Items: ${batch.items.size}")
                        Text("Batch total: ${formatMoney(totalCentavos)}")

                        if (batch.batch.notes.isNotBlank()) {
                            Text("Notes: ${batch.batch.notes}")
                        }

                        batch.items.forEach { item ->
                            val medicineName = medicines
                                .firstOrNull {
                                    it.id == item.prescriptionMedicineId
                                }?.medicineName ?: "Medicine"

                            Text(
                                "$medicineName: ${item.purchasedQuantity} " +
                                        "× ${formatMoney(item.unitPriceCentavos)}"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MedicineQuantityCard(
    medicine: PrescriptionMedicineEntity,
    purchasedQuantity: Double
) {
    val remaining = (medicine.prescribedQuantity - purchasedQuantity)
        .coerceAtLeast(0.0)

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
            Text("Prescribed: ${medicine.prescribedQuantity} ${medicine.unit}")
            Text("Purchased: $purchasedQuantity ${medicine.unit}")
            Text("Remaining: $remaining ${medicine.unit}")
            Text("Current unit price: ${formatMoney(medicine.unitPriceCentavos)}")
        }
    }
}

private fun formatMoney(centavos: Long): String {
    return NumberFormat.getCurrencyInstance(Locale("en", "PH"))
        .format(centavos / 100.0)
}