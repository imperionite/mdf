
package com.imperionite.mdf

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.imperionite.mdf.data.local.DatabaseProvider
import com.imperionite.mdf.data.local.entity.PrescriptionWithMedicines
import com.imperionite.mdf.data.model.PrescriptionMedicineDraft
import com.imperionite.mdf.data.repository.PrescriptionRepository
import com.imperionite.mdf.ui.prescriptions.CreatePrescriptionScreen
import com.imperionite.mdf.ui.prescriptions.PrescriptionListScreen
import com.imperionite.mdf.viewmodel.PrescriptionListViewModel
import com.imperionite.mdf.viewmodel.PrescriptionListViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = DatabaseProvider.getDatabase(applicationContext)

        val repository = PrescriptionRepository(
            database = database,
            prescriptionDao = database.prescriptionDao(),
            medicineDao = database.prescriptionMedicineDao()
        )

        val factory = PrescriptionListViewModelFactory(repository)

        setContent {
            val prescriptionViewModel: PrescriptionListViewModel = viewModel(
                factory = factory
            )

            val prescriptions by prescriptionViewModel.prescriptions
                .collectAsState()

            var showCreateForm by remember { mutableStateOf(false) }
            var editingPrescription by remember {
                mutableStateOf<PrescriptionWithMedicines?>(null)
            }
            var selectedAttachment by remember { mutableStateOf<Uri?>(null) }
            var isSaving by remember { mutableStateOf(false) }
            var saveError by remember { mutableStateOf<String?>(null) }

            val attachmentPicker = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    try {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (_: SecurityException) {
                        // The URI may still work during this session.
                    }

                    selectedAttachment = uri
                    saveError = null
                }
            }

            if (showCreateForm) {
                CreatePrescriptionScreen(
                    initialPrescription = editingPrescription,
                    attachmentUri = selectedAttachment?.toString(),
                    onChooseAttachment = {
                        attachmentPicker.launch(
                            arrayOf("application/pdf", "image/*")
                        )
                    },
                    onRemoveAttachment = {
                        selectedAttachment = null
                    },
                    onSave = { physicianName, prescriptionDate, notes, medicines ->
                        if (!isSaving) {
                            isSaving = true
                            saveError = null

                            val onSaved: () -> Unit = {
                                isSaving = false
                                showCreateForm = false
                                editingPrescription = null
                                selectedAttachment = null
                                saveError = null
                            }

                            val onError: (String) -> Unit = { message ->
                                isSaving = false
                                saveError = message
                            }

                            val attachment = selectedAttachment?.toString()

                            if (editingPrescription == null) {
                                prescriptionViewModel.addPrescription(
                                    physicianName = physicianName,
                                    prescriptionDate = prescriptionDate,
                                    notes = notes,
                                    attachmentUri = attachment,
                                    medicines = medicines,
                                    onSaved = onSaved,
                                    onError = onError
                                )
                            } else {
                                prescriptionViewModel.updatePrescription(
                                    prescriptionId = editingPrescription!!
                                        .prescription.id,
                                    physicianName = physicianName,
                                    prescriptionDate = prescriptionDate,
                                    notes = notes,
                                    attachmentUri = attachment,
                                    medicines = medicines,
                                    onSaved = onSaved,
                                    onError = onError
                                )
                            }
                        }
                    },
                    onCancel = {
                        if (!isSaving) {
                            showCreateForm = false
                            editingPrescription = null
                            selectedAttachment = null
                            saveError = null
                        }
                    },
                    isSaving = isSaving,
                    errorMessage = saveError
                )
            } else {
                PrescriptionListScreen(
                    prescriptions = prescriptions,
                    onCreatePrescription = {
                        editingPrescription = null
                        selectedAttachment = null
                        saveError = null
                        showCreateForm = true
                    },
                    onEditPrescription = { item ->
                        editingPrescription = item
                        selectedAttachment = item.prescription.attachmentUri
                            ?.let(Uri::parse)
                        saveError = null
                        showCreateForm = true
                    },
                    onDeletePrescription = { prescription ->
                        prescriptionViewModel.deletePrescription(prescription)
                    }
                )
            }
        }
    }
}
