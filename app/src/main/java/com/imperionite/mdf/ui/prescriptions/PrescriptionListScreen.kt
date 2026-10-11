package com.imperionite.mdf.ui.prescriptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.imperionite.mdf.data.local.entity.PrescriptionEntity
import com.imperionite.mdf.data.local.entity.PrescriptionWithMedicines
import java.text.NumberFormat
import java.util.Locale

private val DetailsButtonColor = Color(0xFF527D8B)
private val ReportButtonColor = Color(0xFF86B0BD)
private val EditButtonColor = Color(0xFFFFBB5C)
private val DeleteButtonColor = Color(0xFFC63D2F)

private val DarkButtonText = Color(0xFF24323A)
private val LightButtonText = Color.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("FunctionName")
fun PrescriptionListScreen(
    prescriptions: List<PrescriptionWithMedicines>,
    onCreatePrescription: () -> Unit,
    onEditPrescription: (PrescriptionWithMedicines) -> Unit,
    onDeletePrescription: (PrescriptionEntity) -> Unit,
    onOpenDetails: (PrescriptionWithMedicines) -> Unit,
    onOpenInsights: (PrescriptionWithMedicines) -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingDeletePrescription by remember {
        mutableStateOf<PrescriptionEntity?>(null)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "✚",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(
                                    horizontal = 10.dp,
                                    vertical = 4.dp
                                )
                            )
                        }

                        Column {
                            Text(
                                text = "MDF",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Prescription Manager",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            Button(onClick = onCreatePrescription) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null
                )
                Text(
                    text = " Add prescription",
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    ) { innerPadding ->
        if (prescriptions.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No prescriptions yet",
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    text = "Your saved prescriptions will appear here.",
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium
                )

                Button(
                    onClick = onCreatePrescription,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Create your first prescription")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = prescriptions,
                    key = { it.prescription.id }
                ) { item ->
                    val prescription = item.prescription

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 2.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = prescription.physicianName,
                                style = MaterialTheme.typography.titleMedium
                            )

                            Text(
                                text = "Prescription date: ${prescription.prescriptionDate}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            Text(
                                text = "Medicines: ${item.medicines.size}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 8.dp)
                            )

                            item.medicines.forEach { medicine ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                ) {
                                    Text(
                                        text = medicine.medicineName,
                                        style = MaterialTheme.typography.titleSmall
                                    )

                                    Text(
                                        text = "Prescribed: ${medicine.prescribedQuantity} ${medicine.unit}",
                                        style = MaterialTheme.typography.bodySmall
                                    )

                                    Text(
                                        text = "Dosage: ${medicine.dosage}",
                                        style = MaterialTheme.typography.bodySmall
                                    )

                                    Text(
                                        text = "Dosage regimen: ${medicine.dosageRegimen}",
                                        style = MaterialTheme.typography.bodySmall
                                    )

                                    Text(
                                        text = "Unit price: ${formatPrice(medicine.unitPriceCentavos)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }

                            if (prescription.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Notes: ${prescription.notes}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            if (prescription.attachmentUri != null) {
                                Text(
                                    text = "Attachment added",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp)
                            )

                            // Row 1: Details and this prescription's report.
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onOpenDetails(item) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DetailsButtonColor,
                                        contentColor = LightButtonText
                                    )
                                ) {
                                    Text("View Details")
                                }

                                Button(
                                    onClick = { onOpenInsights(item) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ReportButtonColor,
                                        contentColor = DarkButtonText
                                    )
                                ) {
                                    Text("Insight Report")
                                }
                            }

                            // Row 2: Edit and Delete.
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onEditPrescription(item) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EditButtonColor,
                                        contentColor = DarkButtonText
                                    )
                                ) {
                                    Text("Edit")
                                }

                                Button(
                                    onClick = {
                                        pendingDeletePrescription = prescription
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DeleteButtonColor,
                                        contentColor = LightButtonText
                                    )
                                ) {
                                    Text("Delete")
                                }
                            }
                        }
                    }
                }
            }
        }

        pendingDeletePrescription?.let { prescriptionToDelete ->
            AlertDialog(
                onDismissRequest = {
                    pendingDeletePrescription = null
                },
                title = {
                    Text("Delete prescription?")
                },
                text = {
                    Text(
                        "Delete the prescription for ${prescriptionToDelete.physicianName}? " +
                                "Its associated medicines and purchase history will also be removed. " +
                                "This action cannot be undone."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            pendingDeletePrescription = null
                            onDeletePrescription(prescriptionToDelete)
                        }
                    ) {
                        Text(
                            "Delete",
                            color = DeleteButtonColor
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            pendingDeletePrescription = null
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

private fun formatPrice(centavos: Long): String {
    val locale = Locale.Builder()
        .setLanguage("en")
        .setRegion("PH")
        .build()

    return NumberFormat
        .getCurrencyInstance(locale)
        .format(centavos / 100.0)
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
@Suppress("FunctionName")
private fun PrescriptionListScreenPreview() {
    com.imperionite.mdf.ui.theme.MDFTheme {
        PrescriptionListScreen(
            prescriptions = emptyList(),
            onCreatePrescription = {},
            onEditPrescription = {},
            onDeletePrescription = {},
            onOpenDetails = {},
            onOpenInsights = {}
        )
    }
}
