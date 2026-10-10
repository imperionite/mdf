package com.imperionite.mdf



import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.imperionite.mdf.data.local.DatabaseProvider
import com.imperionite.mdf.data.local.entity.PrescriptionWithMedicines
import com.imperionite.mdf.data.repository.PrescriptionRepository
import com.imperionite.mdf.data.repository.PurchaseRepository
import com.imperionite.mdf.ui.prescriptions.BatchPurchasingScreen
import com.imperionite.mdf.ui.prescriptions.CreatePrescriptionScreen
import com.imperionite.mdf.ui.prescriptions.PrescriptionDetailsScreen
import com.imperionite.mdf.ui.prescriptions.PrescriptionListScreen
import com.imperionite.mdf.viewmodel.PrescriptionListViewModel
import com.imperionite.mdf.viewmodel.PrescriptionListViewModelFactory
import com.imperionite.mdf.viewmodel.PrescriptionDetailsViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = DatabaseProvider.getDatabase(applicationContext)

        val prescriptionRepository = PrescriptionRepository(
            database = database,
            prescriptionDao = database.prescriptionDao(),
            medicineDao = database.prescriptionMedicineDao()
        )

        val prescriptionFactory =
            PrescriptionListViewModelFactory(prescriptionRepository)

        val purchaseRepository = PurchaseRepository(
            database = database,
            prescriptionDao = database.prescriptionDao(),
            prescriptionMedicineDao = database.prescriptionMedicineDao(),
            purchaseBatchDao = database.purchaseBatchDao(),
            purchaseBatchItemDao = database.purchaseBatchItemDao()
        )

        val detailsFactory =
            PrescriptionDetailsViewModel.Factory(purchaseRepository)

        setContent {
            val prescriptionViewModel: PrescriptionListViewModel = viewModel(
                factory = prescriptionFactory
            )

            val detailsViewModel: PrescriptionDetailsViewModel = viewModel(
                factory = detailsFactory
            )

            val prescriptions by prescriptionViewModel.prescriptions
                .collectAsState()

            val details by detailsViewModel.details.collectAsState(
                initial = null
            )

            val purchaseState by detailsViewModel.purchaseState.collectAsState()

            var showCreateForm by remember { mutableStateOf(false) }
            var showBatchPurchasing by remember { mutableStateOf(false) }

            var editingPrescription by remember {
                mutableStateOf<PrescriptionWithMedicines?>(null)
            }

            var selectedPrescriptionId by remember {
                mutableStateOf<Long?>(null)
            }

            // Used to return to Prescription Details after editing from there.
            var returnToDetailsAfterEdit by remember {
                mutableStateOf(false)
            }

            var selectedAttachment by remember {
                mutableStateOf<Uri?>(null)
            }

            var isSavingPrescription by remember {
                mutableStateOf(false)
            }

            var saveError by remember {
                mutableStateOf<String?>(null)
            }

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

            fun openPrescription(prescriptionId: Long) {
                selectedPrescriptionId = prescriptionId
                showBatchPurchasing = false
                detailsViewModel.loadPrescription(prescriptionId)
                detailsViewModel.clearPurchaseMessage()
            }

            when {
                showCreateForm -> {
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
                        onSave = {
                                physicianName,
                                prescriptionDate,
                                notes,
                                medicines ->

                            if (!isSavingPrescription) {
                                isSavingPrescription = true
                                saveError = null

                                val onSaved: () -> Unit = {
                                    isSavingPrescription = false
                                    showCreateForm = false
                                    editingPrescription = null
                                    selectedAttachment = null
                                    saveError = null
                                    // selectedPrescriptionId is intentionally
                                    // retained when editing from Details.
                                    returnToDetailsAfterEdit = false
                                }

                                val onError: (String) -> Unit = { message ->
                                    isSavingPrescription = false
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
                            if (!isSavingPrescription) {
                                showCreateForm = false
                                editingPrescription = null
                                selectedAttachment = null
                                saveError = null

                                if (!returnToDetailsAfterEdit) {
                                    selectedPrescriptionId = null
                                }

                                returnToDetailsAfterEdit = false
                            }
                        },
                        isSaving = isSavingPrescription,
                        errorMessage = saveError
                    )
                }

                showBatchPurchasing && selectedPrescriptionId != null -> {
                    val currentDetails = details

                    if (currentDetails == null) {
                        CircularProgressIndicator()
                    } else {
                        BatchPurchasingScreen(
                            prescriptionId = selectedPrescriptionId!!,
                            physicianName = currentDetails.prescription
                                .prescription.physicianName,
                            medicines = currentDetails.prescription.medicines,
                            purchasedQuantities = currentDetails.purchasedQuantities,
                            isSaving = purchaseState.isSaving,
                            errorMessage = purchaseState.errorMessage,
                            onBack = {
                                showBatchPurchasing = false
                                detailsViewModel.clearPurchaseMessage()
                            },
                            onSave = { date, notes, items ->
                                detailsViewModel.savePurchase(
                                    prescriptionId = selectedPrescriptionId!!,
                                    purchaseDate = date,
                                    notes = notes,
                                    items = items
                                )
                            }
                        )
                    }

                    LaunchedEffect(purchaseState.savedBatchId) {
                        if (purchaseState.savedBatchId != null) {
                            showBatchPurchasing = false
                            detailsViewModel.clearPurchaseMessage()
                        }
                    }
                }

                selectedPrescriptionId != null -> {
                    val currentDetails = details

                    if (currentDetails == null) {
                        CircularProgressIndicator()
                    } else {
                        PrescriptionDetailsScreen(
                            details = currentDetails,
                            onBack = {
                                selectedPrescriptionId = null
                                showBatchPurchasing = false
                                detailsViewModel.clearPurchaseMessage()
                            },
                            onEdit = {
                                editingPrescription =
                                    currentDetails.prescription

                                selectedAttachment =
                                    currentDetails.prescription.prescription
                                        .attachmentUri?.let(Uri::parse)

                                saveError = null
                                returnToDetailsAfterEdit = true
                                showCreateForm = true
                            },
                            onNewPurchase = {
                                detailsViewModel.clearPurchaseMessage()
                                showBatchPurchasing = true
                            }
                        )
                    }
                }

                else -> {
                    PrescriptionListScreen(
                        prescriptions = prescriptions,
                        onCreatePrescription = {
                            editingPrescription = null
                            selectedAttachment = null
                            saveError = null
                            returnToDetailsAfterEdit = false
                            showCreateForm = true
                        },
                        onEditPrescription = { item ->
                            editingPrescription = item
                            selectedAttachment =
                                item.prescription.attachmentUri?.let(Uri::parse)

                            saveError = null
                            returnToDetailsAfterEdit = false
                            showCreateForm = true
                        },
                        onDeletePrescription = { prescription ->
                            prescriptionViewModel.deletePrescription(prescription)
                        },
                        onOpenDetails = { item ->
                            openPrescription(item.prescription.id)
                        }
                    )
                }
            }
        }
    }
}
