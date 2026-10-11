package com.imperionite.mdf.ui.prescriptions

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.imperionite.mdf.data.local.entity.PrescriptionWithMedicines
import com.imperionite.mdf.data.model.PrescriptionMedicineDraft
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val storageDateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

private val displayDateFormatter =
    DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault())

private data class MedicineFormState(
    val id: Long? = null,
    val medicineName: String = "",
    val prescribedQuantity: String = "",
    val unit: String = "",
    val dosage: String = "",
    val dosageRegimen: String = "",
    val unitPrice: String = ""
)

@Composable
@Suppress("FunctionName")
fun CreatePrescriptionScreen(
    initialPrescription: PrescriptionWithMedicines?,
    attachmentUri: String?,
    onChooseAttachment: () -> Unit,
    onRemoveAttachment: () -> Unit,
    onSave: (
        physicianName: String,
        prescriptionDate: String,
        notes: String,
        medicines: List<PrescriptionMedicineDraft>
    ) -> Unit,
    onCancel: () -> Unit,
    isSaving: Boolean,
    errorMessage: String?
) {

    var physicianName by remember(initialPrescription?.prescription?.id) {
        mutableStateOf(initialPrescription?.prescription?.physicianName.orEmpty())
    }

    var notes by remember(initialPrescription?.prescription?.id) {
        mutableStateOf(initialPrescription?.prescription?.notes.orEmpty())
    }

    var selectedDate by remember(initialPrescription?.prescription?.id) {
        mutableStateOf(
            runCatching {
                LocalDate.parse(
                    initialPrescription?.prescription?.prescriptionDate
                        ?: LocalDate.now().format(storageDateFormatter),
                    storageDateFormatter
                )
            }.getOrDefault(LocalDate.now())
        )
    }

    var physicianError by remember(initialPrescription?.prescription?.id) {
        mutableStateOf(false)
    }

    var medicineForms by remember(initialPrescription?.prescription?.id) {
        mutableStateOf(
            initialPrescription?.medicines?.map { medicine ->
                MedicineFormState(
                    id = medicine.id,
                    medicineName = medicine.medicineName,
                    prescribedQuantity = medicine.prescribedQuantity.toString(),
                    unit = medicine.unit,
                    dosage = medicine.dosage,
                    dosageRegimen = medicine.dosageRegimen,
                    unitPrice = BigDecimal.valueOf(
                        medicine.unitPriceCentavos,
                        2
                    ).toPlainString()
                )
            } ?: listOf(MedicineFormState())
        )
    }

    var formError by remember(initialPrescription?.prescription?.id) {
        mutableStateOf<String?>(null)
    }

    val context = LocalContext.current

    fun buildMedicineDrafts(): List<PrescriptionMedicineDraft>? {
        if (medicineForms.isEmpty()) {
            formError = "Add at least one medicine."
            return null
        }

        val drafts = mutableListOf<PrescriptionMedicineDraft>()

        for ((index, medicine) in medicineForms.withIndex()) {
            val row = index + 1

            if (medicine.medicineName.isBlank()) {
                formError = "Enter the name of medicine #$row."
                return null
            }

            val quantity = medicine.prescribedQuantity.toDoubleOrNull()
            if (quantity == null || !quantity.isFinite() || quantity <= 0.0) {
                formError = "Enter a valid quantity greater than zero for medicine #$row."
                return null
            }

            if (medicine.unit.isBlank()) {
                formError = "Enter the unit for medicine #$row."
                return null
            }

            if (medicine.dosage.isBlank()) {
                formError = "Enter the dosage or strength for medicine #$row."
                return null
            }

            if (medicine.dosageRegimen.isBlank()) {
                formError = "Enter the dosage regimen for medicine #$row."
                return null
            }

            val priceCentavos = try {
                BigDecimal(medicine.unitPrice.trim())
                    .setScale(2, RoundingMode.UNNECESSARY)
                    .movePointRight(2)
                    .longValueExact()
            } catch (_: Exception) {
                formError = "Enter a valid unit price with up to two decimal places for medicine #$row."
                return null
            }

            if (priceCentavos < 0) {
                formError = "Unit price cannot be negative for medicine #$row."
                return null
            }

            drafts.add(
                PrescriptionMedicineDraft(
                    id = medicine.id,
                    medicineName = medicine.medicineName.trim(),
                    prescribedQuantity = quantity,
                    unit = medicine.unit.trim(),
                    dosage = medicine.dosage.trim(),
                    dosageRegimen = medicine.dosageRegimen.trim(),
                    unitPriceCentavos = priceCentavos
                )
            )
        }

        formError = null
        return drafts
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (initialPrescription == null) "New Prescription" else "Edit Prescription",
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = physicianName,
            onValueChange = {
                physicianName = it
                physicianError = false
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Physician name *") },
            singleLine = true,
            isError = physicianError,
            supportingText = {
                if (physicianError) Text("Enter the physician's name.")
            },
            enabled = !isSaving
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Prescription date *")

            OutlinedButton(
                onClick = {
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            selectedDate = LocalDate.of(year, month + 1, day)
                        },
                        selectedDate.year,
                        selectedDate.monthValue - 1,
                        selectedDate.dayOfMonth
                    ).show()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            ) {
                Text(selectedDate.format(displayDateFormatter))
            }
        }

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Notes (optional)") },
            minLines = 3,
            enabled = !isSaving
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Prescription attachment (optional)",
                style = MaterialTheme.typography.titleSmall
            )

            OutlinedButton(
                onClick = onChooseAttachment,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            ) {
                Text(
                    if (attachmentUri == null) {
                        "Choose document"
                    } else {
                        "Choose a different document"
                    }
                )
            }

            if (attachmentUri != null) {
                Text(
                    text = "A document is selected.",
                    style = MaterialTheme.typography.bodySmall
                )

                TextButton(
                    onClick = onRemoveAttachment,
                    enabled = !isSaving
                ) {
                    Text("Remove attachment")
                }
            }
        }

        androidx.compose.material3.HorizontalDivider()

        Text(
            text = "Prescribed Medicines",
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = "Keep the regimen as written on the prescription, such as 1-0-1. Do not infer duration from this notation.",
            style = MaterialTheme.typography.bodyMedium
        )

        medicineForms.forEachIndexed { index, medicine ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Medicine ${index + 1}",
                            style = MaterialTheme.typography.titleMedium
                        )

                        if (medicineForms.size > 1) {
                            TextButton(
                                onClick = {
                                    medicineForms = medicineForms.filterIndexed { rowIndex, _ ->
                                        rowIndex != index
                                    }
                                    formError = null
                                },
                                enabled = !isSaving
                            ) {
                                Text("Remove")
                            }
                        }
                    }

                    OutlinedTextField(
                        value = medicine.medicineName,
                        onValueChange = { value ->
                            medicineForms = medicineForms.toMutableList().also {
                                it[index] = medicine.copy(medicineName = value)
                            }
                            formError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Medicine name *") },
                        singleLine = true,
                        enabled = !isSaving
                    )

                    OutlinedTextField(
                        value = medicine.prescribedQuantity,
                        onValueChange = { value ->
                            medicineForms = medicineForms.toMutableList().also {
                                it[index] = medicine.copy(prescribedQuantity = value)
                            }
                            formError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Prescribed quantity *") },
                        placeholder = { Text("e.g. 10 or 5.5") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        singleLine = true,
                        enabled = !isSaving
                    )

                    OutlinedTextField(
                        value = medicine.unit,
                        onValueChange = { value ->
                            medicineForms = medicineForms.toMutableList().also {
                                it[index] = medicine.copy(unit = value)
                            }
                            formError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Unit *") },
                        placeholder = { Text("tablet, capsule, mL") },
                        singleLine = true,
                        enabled = !isSaving
                    )

                    OutlinedTextField(
                        value = medicine.dosage,
                        onValueChange = { value ->
                            medicineForms = medicineForms.toMutableList().also {
                                it[index] = medicine.copy(dosage = value)
                            }
                            formError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Dosage / strength *") },
                        placeholder = { Text("e.g. 500 mg") },
                        singleLine = true,
                        enabled = !isSaving
                    )

                    OutlinedTextField(
                        value = medicine.dosageRegimen,
                        onValueChange = { value ->
                            medicineForms = medicineForms.toMutableList().also {
                                it[index] = medicine.copy(dosageRegimen = value)
                            }
                            formError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Dosage regimen (schedule) *") },
                        placeholder = { Text("e.g. 1-0-1 or 0-0-1") },
                        singleLine = true,
                        enabled = !isSaving
                    )

                    OutlinedTextField(
                        value = medicine.unitPrice,
                        onValueChange = { value ->
                            medicineForms = medicineForms.toMutableList().also {
                                it[index] = medicine.copy(unitPrice = value)
                            }
                            formError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Current unit price (₱) *") },
                        placeholder = { Text("e.g. 12.50") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        singleLine = true,
                        enabled = !isSaving
                    )
                }
            }
        }

        OutlinedButton(
            onClick = {
                medicineForms = medicineForms + MedicineFormState()
                formError = null
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving
        ) {
            Text("Add another medicine")
        }

        if (!formError.isNullOrBlank()) {
            Text(
                text = formError.orEmpty(),
                color = MaterialTheme.colorScheme.error
            )
        }

        if (!errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                enabled = !isSaving,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }

            Button(
                onClick = {
                    if (physicianName.isBlank()) {
                        physicianError = true
                    } else {
                        val drafts = buildMedicineDrafts()
                        if (drafts != null) {
                            onSave(
                                physicianName.trim(),
                                selectedDate.format(storageDateFormatter),
                                notes.trim(),
                                drafts
                            )
                        }
                    }
                },
                enabled = !isSaving,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isSaving) "Saving…" else "Save")
            }
        }
    }
}
