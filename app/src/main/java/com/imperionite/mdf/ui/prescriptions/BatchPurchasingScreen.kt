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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.imperionite.mdf.data.local.entity.PrescriptionMedicineEntity
import com.imperionite.mdf.data.repository.PurchaseItemDraft
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

private data class PurchaseMedicineForm(
    val medicine: PrescriptionMedicineEntity,
    val remainingQuantity: Double,
    var selected: Boolean = false,
    var quantityText: String = "",
    var priceText: String = ""
)

@Composable
fun BatchPurchasingScreen(
    prescriptionId: Long,
    physicianName: String,
    medicines: List<PrescriptionMedicineEntity>,
    purchasedQuantities: Map<Long, Double>,
    isSaving: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onSave: (purchaseDate: String, notes: String, items: List<PurchaseItemDraft>) -> Unit
) {
    val forms = remember(prescriptionId, medicines, purchasedQuantities) {
        mutableStateListOf<PurchaseMedicineForm>().apply {
            medicines.forEach { medicine ->
                val purchased = purchasedQuantities[medicine.id] ?: 0.0
                val remaining = (medicine.prescribedQuantity - purchased)
                    .coerceAtLeast(0.0)

                add(
                    PurchaseMedicineForm(
                        medicine = medicine,
                        remainingQuantity = remaining,
                        priceText = BigDecimal.valueOf(
                            medicine.unitPriceCentavos, 2
                        ).toPlainString()
                    )
                )
            }
        }
    }

    var purchaseDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var notes by remember { mutableStateOf("") }
    var validationMessage by remember { mutableStateOf<String?>(null) }

    val selectedForms = forms.filter { it.selected }

    val estimatedTotalCentavos = selectedForms.sumOf { form ->
        val quantity = form.quantityText.toDoubleOrNull() ?: 0.0
        val price = parseMoneyToCentavos(form.priceText) ?: 0L
        (quantity * price).toLong()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Batch Purchasing",
            style = MaterialTheme.typography.headlineSmall
        )
        Text("Prescription for: $physicianName")

        OutlinedTextField(
            value = purchaseDate,
            onValueChange = {
                purchaseDate = it
                validationMessage = null
            },
            label = { Text("Purchase date (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Text(
            text = "Select the medicines included in this purchase.",
            style = MaterialTheme.typography.bodyMedium
        )

        forms.forEachIndexed { index, form ->
            key(form.medicine.id) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = form.selected,
                                onCheckedChange = { checked ->
                                    forms[index] = form.copy(selected = checked)
                                    validationMessage = null
                                },
                                enabled = form.remainingQuantity > 0.000001
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    form.medicine.medicineName,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    "Remaining: ${form.remainingQuantity} " +
                                            form.medicine.unit
                                )
                            }
                        }

                        Text("Dosage/strength: ${form.medicine.dosage}")
                        Text("Regimen: ${form.medicine.dosageRegimen}")

                        if (form.remainingQuantity <= 0.000001) {
                            Text("This medicine has been fully purchased.")
                        } else {
                            OutlinedTextField(
                                value = form.quantityText,
                                onValueChange = { newValue ->
                                    forms[index] = form.copy(
                                        quantityText = newValue
                                    )
                                    validationMessage = null
                                },
                                label = {
                                    Text("Quantity to purchase (${form.medicine.unit})")
                                },
                                supportingText = {
                                    Text("Maximum: ${form.remainingQuantity}")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = form.priceText,
                                onValueChange = { newValue ->
                                    forms[index] = form.copy(
                                        priceText = newValue
                                    )
                                    validationMessage = null
                                },
                                label = { Text("Actual unit price (₱)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Purchase notes (optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Selected medicines: ${selectedForms.size}")
                Text(
                    text = "Estimated batch total: ${formatCentavos(estimatedTotalCentavos)}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "The saved total uses each item's recorded quantity and unit-price snapshot."
                )
            }
        }

        validationMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onBack,
                enabled = !isSaving,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }

            Button(
                onClick = {
                    val parsedDate = runCatching {
                        LocalDate.parse(purchaseDate)
                    }.getOrNull()

                    if (parsedDate == null) {
                        validationMessage =
                            "Enter a valid date in YYYY-MM-DD format."
                        return@Button
                    }

                    if (selectedForms.isEmpty()) {
                        validationMessage =
                            "Select at least one medicine."
                        return@Button
                    }

                    val drafts = mutableListOf<PurchaseItemDraft>()

                    for (form in selectedForms) {
                        val quantity = form.quantityText.toDoubleOrNull()
                        val priceCentavos = parseMoneyToCentavos(form.priceText)

                        if (quantity == null || !quantity.isFinite() || quantity <= 0.0) {
                            validationMessage =
                                "Enter a valid positive quantity for ${form.medicine.medicineName}."
                            return@Button
                        }

                        if (quantity > form.remainingQuantity + 0.000001) {
                            validationMessage =
                                "Quantity for ${form.medicine.medicineName} exceeds the remaining quantity."
                            return@Button
                        }

                        if (priceCentavos == null || priceCentavos < 0L) {
                            validationMessage =
                                "Enter a valid price with no more than two decimal places for ${form.medicine.medicineName}."
                            return@Button
                        }

                        drafts.add(
                            PurchaseItemDraft(
                                prescriptionMedicineId = form.medicine.id,
                                purchasedQuantity = quantity,
                                unitPriceCentavos = priceCentavos
                            )
                        )
                    }

                    validationMessage = null
                    onSave(purchaseDate, notes, drafts)
                },
                enabled = !isSaving,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isSaving) "Saving..." else "Save Purchase")
            }
        }
    }
}

private fun parseMoneyToCentavos(value: String): Long? {
    return try {
        val amount = BigDecimal(value.trim())
        if (amount.scale() > 2) {
            null
        } else {
            amount
                .setScale(2, RoundingMode.UNNECESSARY)
                .movePointRight(2)
                .longValueExact()
        }
    } catch (_: Exception) {
        null
    }
}

private fun formatCentavos(centavos: Long): String {
    return NumberFormat.getCurrencyInstance(Locale("en", "PH"))
        .format(centavos / 100.0)
}