
package com.imperionite.mdf.ui.prescriptions

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val storageDateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
private val displayDateFormatter =
    DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault())

@Composable
fun CreatePrescriptionScreen(
    attachmentUri: String?,
    onChooseAttachment: () -> Unit,
    onRemoveAttachment: () -> Unit,
    onSave: (
        physicianName: String,
        prescriptionDate: String,
        notes: String
    ) -> Unit,
    onCancel: () -> Unit,
    isSaving: Boolean,
    errorMessage: String?
) {
    var physicianName by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var physicianError by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "New Prescription",
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
                if (physicianError) {
                    Text("Enter the physician's name.")
                }
            }
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Prescription date *")

            OutlinedButton(
                onClick = {
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            selectedDate = LocalDate.of(
                                year,
                                month + 1,
                                day
                            )
                        },
                        selectedDate.year,
                        selectedDate.monthValue - 1,
                        selectedDate.dayOfMonth
                    ).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(selectedDate.format(displayDateFormatter))
            }
        }

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Notes (optional)") },
            minLines = 3
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Prescription attachment (optional)",
                style = MaterialTheme.typography.titleSmall
            )

            OutlinedButton(
                onClick = onChooseAttachment,
                modifier = Modifier.fillMaxWidth()
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

                TextButton(onClick = onRemoveAttachment) {
                    Text("Remove attachment")
                }
            }
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
                    val trimmedName = physicianName.trim()
                    if (trimmedName.isBlank()) {
                        physicianError = true
                    } else {
                        onSave(
                            trimmedName,
                            selectedDate.format(storageDateFormatter),
                            notes.trim()
                        )
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