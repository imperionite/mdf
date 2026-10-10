package com.imperionite.mdf.ui.prescriptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.imperionite.mdf.data.local.entity.PrescriptionMedicineEntity
import com.imperionite.mdf.data.local.entity.PurchaseBatchWithItems
import com.imperionite.mdf.data.repository.PurchaseItemDraft
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

private data class PurchaseMedicineForm(
    val medicine: PrescriptionMedicineEntity,
    val remainingQuantity: Double,
    val selected: Boolean = false,
    val quantityText: String = "",
    val unitPriceText: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchPurchasingScreen(
    prescriptionId: Long,
    physicianName: String,
    medicines: List<PrescriptionMedicineEntity>,
    purchasedQuantities: Map<Long, Double>,
    isSaving: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onSave: (
        purchaseDate: String,
        notes: String,
        items: List<PurchaseItemDraft>
    ) -> Unit,
    editingBatch: PurchaseBatchWithItems? = null,
    onUpdate: (
        batchId: Long,
        purchaseDate: String,
        notes: String,
        items: List<PurchaseItemDraft>
    ) -> Unit = { _, _, _, _ -> }
) {
    val isEditing = editingBatch != null

    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "PH"))
    }

    // Quantities already recorded in the batch being edited.
    // These are added back to the available quantity for this form.
    val quantitiesInEditingBatch = remember(editingBatch?.batch?.id) {
        editingBatch?.items
            ?.groupBy { it.prescriptionMedicineId }
            ?.mapValues { (_, items) ->
                items.sumOf { it.purchasedQuantity }
            }
            ?: emptyMap()
    }

    val existingItems = remember(editingBatch?.batch?.id) {
        editingBatch?.items?.associateBy {
            it.prescriptionMedicineId
        } ?: emptyMap()
    }

    val forms = remember(
        medicines,
        purchasedQuantities,
        editingBatch?.batch?.id
    ) {
        mutableStateListOf<PurchaseMedicineForm>().apply {
            medicines.forEach { medicine ->
                val totalPurchased =
                    purchasedQuantities[medicine.id] ?: 0.0

                val quantityInThisBatch =
                    existingItems[medicine.id]?.purchasedQuantity ?: 0.0

                val remainingForThisForm = (
                        medicine.prescribedQuantity -
                                totalPurchased +
                                quantityInThisBatch
                        ).coerceAtLeast(0.0)

                val existingItem = existingItems[medicine.id]

                add(
                    PurchaseMedicineForm(
                        medicine = medicine,
                        remainingQuantity = remainingForThisForm,
                        selected = existingItem != null,
                        quantityText = existingItem
                            ?.purchasedQuantity
                            ?.let { formatQuantity(it) }
                            ?: "",
                        unitPriceText = centsToMoneyText(
                            existingItem?.unitPriceCentavos
                                ?: medicine.unitPriceCentavos
                        )
                    )
                )
            }
        }
    }

    var purchaseDate by remember(editingBatch?.batch?.id) {
        mutableStateOf(
            editingBatch?.batch?.purchaseDate
                ?: LocalDate.now().toString()
        )
    }

    var notes by remember(editingBatch?.batch?.id) {
        mutableStateOf(editingBatch?.batch?.notes ?: "")
    }

    var dateError by remember { mutableStateOf<String?>(null) }

    val availableForms = forms.filter {
        it.remainingQuantity > 0.000001
    }

    val selectedForms = forms.filter { it.selected }

    val drafts = selectedForms.mapNotNull { form ->
        val quantity = form.quantityText.toDoubleOrNull()
        val priceCentavos = parseMoneyToCentavos(form.unitPriceText)

        if (
            quantity != null &&
            quantity.isFinite() &&
            quantity > 0.0 &&
            priceCentavos != null &&
            priceCentavos >= 0L
        ) {
            PurchaseItemDraft(
                prescriptionMedicineId = form.medicine.id,
                purchasedQuantity = quantity,
                unitPriceCentavos = priceCentavos
            )
        } else {
            null
        }
    }

    val estimatedTotalCentavos = drafts.fold(0L) { total, draft ->
        val itemTotal = BigDecimal.valueOf(draft.purchasedQuantity)
            .multiply(BigDecimal.valueOf(draft.unitPriceCentavos))
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()

        Math.addExact(total, itemTotal)
    }

    val quantityOrPriceInvalid = selectedForms.any { form ->
        val quantity = form.quantityText.toDoubleOrNull()
        val price = parseMoneyToCentavos(form.unitPriceText)

        quantity == null ||
                !quantity.isFinite() ||
                quantity <= 0.0 ||
                quantity > form.remainingQuantity + 0.000001 ||
                price == null ||
                price < 0L
    }

    val noMedicineSelected = selectedForms.isEmpty()

    val canSave =
        !isSaving &&
                !noMedicineSelected &&
                !quantityOrPriceInvalid &&
                dateError == null &&
                validateIsoDate(purchaseDate) == null &&
                drafts.size == selectedForms.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditing) {
                            "Edit Purchase Batch #${editingBatch!!.batch.id}"
                        } else {
                            "Batch Purchasing"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        enabled = !isSaving
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Physician: $physicianName",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "Prescription ID: $prescriptionId",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isEditing) {
                    Text(
                        text = "Editing a saved transaction. Changes will replace this batch rather than create a new one.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = purchaseDate,
                    onValueChange = {
                        purchaseDate = it
                        dateError = validateIsoDate(it)
                    },
                    label = { Text("Purchase date (YYYY-MM-DD)") },
                    supportingText = {
                        Text("Enter the actual date of this purchase.")
                    },
                    isError = dateError != null,
                    enabled = !isSaving,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (dateError != null) {
                    Text(
                        text = dateError.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isEditing) {
                        "Update the medicines, quantities, and actual prices in this batch."
                    } else {
                        "Select the medicines purchased in this batch."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (availableForms.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = "There are no remaining prescribed quantities available to purchase.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                itemsIndexed(
                    items = forms,
                    key = { _, form -> form.medicine.id }
                ) { index, form ->
                    if (form.remainingQuantity > 0.000001) {
                        val medicine = form.medicine
                        val quantity = form.quantityText.toDoubleOrNull()
                        val priceCentavos =
                            parseMoneyToCentavos(form.unitPriceText)

                        val quantityError = when {
                            form.selected && form.quantityText.isBlank() ->
                                "Enter the quantity to purchase."

                            form.selected && quantity == null ->
                                "Enter a valid numeric quantity."

                            form.selected && quantity != null &&
                                    (!quantity.isFinite() || quantity <= 0.0) ->
                                "Quantity must be greater than zero."

                            form.selected && quantity != null &&
                                    quantity > form.remainingQuantity + 0.000001 ->
                                "Cannot exceed the available quantity."

                            else -> null
                        }

                        val priceError = when {
                            form.selected &&
                                    priceCentavos == null ->
                                "Enter a valid price with up to 2 decimal places."

                            form.selected &&
                                    priceCentavos != null &&
                                    priceCentavos < 0L ->
                                "Price cannot be negative."

                            else -> null
                        }

                        val subtotalCentavos =
                            if (
                                form.selected &&
                                quantity != null &&
                                quantity.isFinite() &&
                                quantity > 0.0 &&
                                priceCentavos != null &&
                                priceCentavos >= 0L
                            ) {
                                BigDecimal.valueOf(quantity)
                                    .multiply(
                                        BigDecimal.valueOf(priceCentavos)
                                    )
                                    .setScale(0, RoundingMode.HALF_UP)
                                    .longValueExact()
                            } else {
                                0L
                            }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement =
                                    Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = form.selected,
                                        onCheckedChange = { checked ->
                                            forms[index] = form.copy(
                                                selected = checked
                                            )
                                        },
                                        enabled = !isSaving
                                    )

                                    Spacer(modifier = Modifier.width(4.dp))

                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = medicine.medicineName,
                                            style = MaterialTheme.typography.titleMedium
                                        )

                                        Text(
                                            text = "Available for this ${if (isEditing) "edit" else "purchase"}: ${
                                                formatQuantity(
                                                    form.remainingQuantity
                                                )
                                            } ${medicine.unit}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )

                                        Text(
                                            text = "Prescribed: ${formatQuantity(medicine.prescribedQuantity)} ${medicine.unit}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (medicine.dosageRegimen.isNotBlank()) {
                                    Text(
                                        text = "Dosage regimen: ${medicine.dosageRegimen}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                Text(
                                    text = "Original unit price: ${
                                        currencyFormatter.format(
                                            medicine.unitPriceCentavos / 100.0
                                        )
                                    }",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                OutlinedTextField(
                                    value = form.quantityText,
                                    onValueChange = { value ->
                                        forms[index] = form.copy(
                                            quantityText = value.filter {
                                                it.isDigit() || it == '.'
                                            }
                                        )
                                    },
                                    label = { Text("Quantity to purchase") },
                                    placeholder = { Text("e.g. 10") },
                                    supportingText = {
                                        Text(
                                            "Maximum: ${formatQuantity(form.remainingQuantity)} ${medicine.unit}"
                                        )
                                    },
                                    isError = quantityError != null,
                                    enabled = !isSaving && form.selected,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Decimal
                                    ),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (quantityError != null) {
                                    Text(
                                        text = quantityError,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                OutlinedTextField(
                                    value = form.unitPriceText,
                                    onValueChange = { value ->
                                        forms[index] = form.copy(
                                            unitPriceText = value.filter {
                                                it.isDigit() || it == '.'
                                            }
                                        )
                                    },
                                    label = {
                                        Text("Actual unit price (₱)")
                                    },
                                    placeholder = { Text("e.g. 12.50") },
                                    supportingText = {
                                        Text(
                                            "Enter the actual price paid per ${medicine.unit}."
                                        )
                                    },
                                    isError = priceError != null,
                                    enabled = !isSaving && form.selected,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Decimal
                                    ),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (priceError != null) {
                                    Text(
                                        text = priceError,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                HorizontalDivider()

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement =
                                        Arrangement.SpaceBetween,
                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Subtotal",
                                        style = MaterialTheme.typography.titleSmall
                                    )

                                    Text(
                                        text = currencyFormatter.format(
                                            subtotalCentavos / 100.0
                                        ),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Purchase notes (optional)") },
                    placeholder = {
                        Text("e.g. Partial purchase, receipt details")
                    },
                    enabled = !isSaving,
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Purchase Summary",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text("Selected medicines: ${selectedForms.size}")

                        Text(
                            text = "Estimated total: ${
                                currencyFormatter.format(
                                    estimatedTotalCentavos / 100.0
                                )
                            }",
                            style = MaterialTheme.typography.headlineSmall
                        )

                        Text(
                            text = if (isEditing) {
                                "Saving will replace the contents of this batch."
                            } else {
                                "Actual unit prices will be saved with this batch."
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (noMedicineSelected) {
                    Text(
                        text = "Select at least one medicine and enter its purchase quantity.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val validDate = validateIsoDate(purchaseDate)
                        dateError = validDate

                        if (
                            validDate == null &&
                            !noMedicineSelected &&
                            !quantityOrPriceInvalid &&
                            drafts.size == selectedForms.size
                        ) {
                            if (editingBatch != null) {
                                onUpdate(
                                    editingBatch.batch.id,
                                    purchaseDate.trim(),
                                    notes.trim(),
                                    drafts
                                )
                            } else {
                                onSave(
                                    purchaseDate.trim(),
                                    notes.trim(),
                                    drafts
                                )
                            }
                        }
                    },
                    enabled = canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .height(20.dp)
                                .width(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isEditing) "Updating purchase..."
                            else "Saving purchase..."
                        )
                    } else {
                        Text(
                            if (isEditing) "Save Changes"
                            else "Save Purchase Batch"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

private fun parseMoneyToCentavos(value: String): Long? {
    val trimmed = value.trim()

    if (trimmed.isEmpty()) return null
    if (!trimmed.matches(Regex("""\d+(\.\d{0,2})?"""))) return null

    return try {
        BigDecimal(trimmed)
            .setScale(2, RoundingMode.UNNECESSARY)
            .movePointRight(2)
            .longValueExact()
    } catch (_: Exception) {
        null
    }
}

private fun centsToMoneyText(centavos: Long): String {
    return BigDecimal.valueOf(centavos, 2).toPlainString()
}

private fun formatQuantity(quantity: Double): String {
    return BigDecimal.valueOf(quantity)
        .stripTrailingZeros()
        .toPlainString()
}

private fun validateIsoDate(value: String): String? {
    return try {
        val parsed = LocalDate.parse(value.trim())
        if (parsed.toString() != value.trim()) {
            "Use the date format YYYY-MM-DD."
        } else {
            null
        }
    } catch (_: Exception) {
        "Enter a valid date in YYYY-MM-DD format."
    }
}
