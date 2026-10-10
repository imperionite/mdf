
package com.imperionite.mdf.ui.prescriptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.imperionite.mdf.data.local.entity.PrescriptionEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrescriptionListScreen(
    prescriptions: List<PrescriptionEntity>,
    onCreatePrescription: () -> Unit,
    onDeletePrescription: (PrescriptionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text("Prescriptions")
                }
            )
        },
        floatingActionButton = {
            Button(onClick = onCreatePrescription) {
                androidx.compose.material3.Icon(
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
                    key = { prescription -> prescription.id }
                ) { prescription ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
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

                            if (prescription.notes.isNotBlank()) {
                                Text(
                                    text = prescription.notes,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 8.dp)
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

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        onDeletePrescription(prescription)
                                    }
                                ) {
                                    Text("Delete")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/*
 * Preview-only state.
 * An empty list means no fake prescription is displayed.
 * This function does not connect to Room or save anything.
 */
@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun PrescriptionListScreenPreview() {
    MaterialTheme {
        PrescriptionListScreen(
            prescriptions = emptyList(),
            onCreatePrescription = {},
            onDeletePrescription = {}
        )
    }
}
