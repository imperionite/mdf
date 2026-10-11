package com.imperionite.mdf

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.imperionite.mdf.data.local.DatabaseProvider
import com.imperionite.mdf.data.local.entity.PrescriptionWithMedicines
import com.imperionite.mdf.data.repository.PrescriptionRepository
import com.imperionite.mdf.data.repository.PurchaseRepository
import com.imperionite.mdf.ui.prescriptions.BatchPurchasingScreen
import com.imperionite.mdf.ui.prescriptions.CreatePrescriptionScreen
import com.imperionite.mdf.ui.prescriptions.PrescriptionDetailsScreen
import com.imperionite.mdf.ui.prescriptions.PrescriptionInsightsScreen
import com.imperionite.mdf.ui.prescriptions.PrescriptionListScreen
import com.imperionite.mdf.ui.theme.MDFTheme
import com.imperionite.mdf.viewmodel.PrescriptionDetailsViewModel
import com.imperionite.mdf.viewmodel.PrescriptionListViewModel
import com.imperionite.mdf.viewmodel.PrescriptionListViewModelFactory
import kotlinx.coroutines.launch

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
            MDFTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                val coroutineScope = rememberCoroutineScope()

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

                var showCreateForm by remember {
                    mutableStateOf(false)
                }

                var showBatchPurchasing by remember {
                    mutableStateOf(false)
                }

                var showInsightsReport by remember {
                    mutableStateOf(false)
                }

                var editingPrescription by remember {
                    mutableStateOf<PrescriptionWithMedicines?>(null)
                }

                var selectedPrescriptionId by remember {
                    mutableStateOf<Long?>(null)
                }

                var editingPurchaseBatchId by remember {
                    mutableStateOf<Long?>(null)
                }

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

                LaunchedEffect(
                    purchaseState.savedBatchId,
                    purchaseState.deletedBatchId
                ) {
                    when {
                        purchaseState.savedBatchId != null -> {
                            val message =
                                if (editingPurchaseBatchId == null) {
                                    "Purchase batch saved successfully."
                                } else {
                                    "Purchase batch updated successfully."
                                }

                            showBatchPurchasing = false
                            editingPurchaseBatchId = null

                            snackbarHostState.showSnackbar(message)
                            detailsViewModel.clearPurchaseMessage()
                        }

                        purchaseState.deletedBatchId != null -> {
                            snackbarHostState.showSnackbar(
                                "Purchase batch deleted successfully."
                            )
                            detailsViewModel.clearPurchaseMessage()
                        }
                    }
                }

                LaunchedEffect(
                    purchaseState.errorMessage,
                    showBatchPurchasing
                ) {
                    val message = purchaseState.errorMessage

                    if (message != null && !showBatchPurchasing) {
                        snackbarHostState.showSnackbar(message)
                        detailsViewModel.clearPurchaseMessage()
                    }
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
                    showInsightsReport = false
                    editingPurchaseBatchId = null

                    detailsViewModel.loadPrescription(prescriptionId)
                    detailsViewModel.clearPurchaseMessage()
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        // CREATE OR EDIT PRESCRIPTION
                        showCreateForm -> {
                            CreatePrescriptionScreen(
                                initialPrescription = editingPrescription,
                                attachmentUri = selectedAttachment?.toString(),
                                onChooseAttachment = {
                                    attachmentPicker.launch(
                                        arrayOf(
                                            "application/pdf",
                                            "image/*"
                                        )
                                    )
                                },
                                onRemoveAttachment = {
                                    selectedAttachment = null
                                },
                                onSave = { physicianName,
                                           prescriptionDate,
                                           notes,
                                           medicines ->

                                    if (!isSavingPrescription) {
                                        isSavingPrescription = true
                                        saveError = null

                                        val successMessage =
                                            if (editingPrescription == null) {
                                                "Prescription saved successfully."
                                            } else {
                                                "Prescription updated successfully."
                                            }

                                        val onSaved: () -> Unit = {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(
                                                    successMessage
                                                )
                                            }

                                            isSavingPrescription = false
                                            showCreateForm = false
                                            editingPrescription = null
                                            selectedAttachment = null
                                            saveError = null
                                            returnToDetailsAfterEdit = false
                                        }

                                        val onError: (String) -> Unit = { message ->
                                            isSavingPrescription = false
                                            saveError = message
                                        }

                                        val attachment =
                                            selectedAttachment?.toString()

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

                        // CREATE OR EDIT PURCHASE BATCH
                        showBatchPurchasing &&
                                selectedPrescriptionId != null -> {

                            val currentDetails = details
                            val batchId = editingPurchaseBatchId

                            val editingBatch = if (batchId != null) {
                                currentDetails?.purchaseBatches?.firstOrNull {
                                    it.batch.id == batchId
                                }
                            } else {
                                null
                            }

                            if (
                                currentDetails == null ||
                                (batchId != null && editingBatch == null)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            } else {
                                BatchPurchasingScreen(
                                    prescriptionId = selectedPrescriptionId!!,
                                    physicianName = currentDetails.prescription
                                        .prescription.physicianName,
                                    medicines = currentDetails.prescription.medicines,
                                    purchasedQuantities =
                                        currentDetails.purchasedQuantities,
                                    isSaving = purchaseState.isSaving,
                                    errorMessage = purchaseState.errorMessage,
                                    editingBatch = editingBatch,
                                    onBack = {
                                        showBatchPurchasing = false
                                        editingPurchaseBatchId = null
                                        detailsViewModel.clearPurchaseMessage()
                                    },
                                    onSave = { date, notes, items ->
                                        detailsViewModel.savePurchase(
                                            prescriptionId = selectedPrescriptionId!!,
                                            purchaseDate = date,
                                            notes = notes,
                                            items = items
                                        )
                                    },
                                    onUpdate = { id, date, notes, items ->
                                        detailsViewModel.updatePurchase(
                                            batchId = id,
                                            purchaseDate = date,
                                            notes = notes,
                                            items = items
                                        )
                                    }
                                )
                            }
                        }

                        // INSIGHT REPORT FOR THE SELECTED PRESCRIPTION
                        showInsightsReport &&
                                selectedPrescriptionId != null -> {

                            val currentDetails = details

                            if (currentDetails == null) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            } else {
                                PrescriptionInsightsScreen(
                                    details = currentDetails,
                                    onBack = {
                                        showInsightsReport = false
                                        selectedPrescriptionId = null
                                        editingPurchaseBatchId = null
                                    }
                                )
                            }
                        }

                        // PRESCRIPTION DETAILS
                        selectedPrescriptionId != null -> {
                            val currentDetails = details

                            if (currentDetails == null) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            } else {
                                PrescriptionDetailsScreen(
                                    details = currentDetails,
                                    onBack = {
                                        selectedPrescriptionId = null
                                        showBatchPurchasing = false
                                        showInsightsReport = false
                                        editingPurchaseBatchId = null

                                        detailsViewModel.clearPurchaseMessage()
                                    },
                                    onEdit = {
                                        editingPrescription = currentDetails.prescription

                                        selectedAttachment =
                                            currentDetails.prescription.prescription
                                                .attachmentUri?.let(Uri::parse)

                                        saveError = null
                                        returnToDetailsAfterEdit = true
                                        showCreateForm = true
                                    },
                                    onNewPurchase = {
                                        editingPurchaseBatchId = null
                                        detailsViewModel.clearPurchaseMessage()
                                        showBatchPurchasing = true
                                    },
                                    onEditBatch = { batchId ->
                                        editingPurchaseBatchId = batchId
                                        detailsViewModel.clearPurchaseMessage()
                                        showBatchPurchasing = true
                                    },
                                    onDeleteBatch = { batchId ->
                                        detailsViewModel.deletePurchase(batchId)
                                    }
                                )
                            }
                        }

                        // PRESCRIPTION LIST
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
                                    prescriptionViewModel.deletePrescription(
                                        prescription = prescription,
                                        onDeleted = {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(
                                                    "Prescription deleted successfully."
                                                )
                                            }
                                        },
                                        onError = { message ->
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(message)
                                            }
                                        }
                                    )
                                },
                                onOpenDetails = { item ->
                                    openPrescription(item.prescription.id)
                                },
                                onOpenInsights = { item ->
                                    openPrescription(item.prescription.id)
                                    showInsightsReport = true
                                }
                            )
                        }
                    }

                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}
