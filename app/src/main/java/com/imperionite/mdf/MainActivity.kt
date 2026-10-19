package com.imperionite.mdf

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.imperionite.mdf.data.local.DatabaseProvider
import com.imperionite.mdf.data.local.entity.PrescriptionWithMedicines
import com.imperionite.mdf.data.repository.PrescriptionRepository
import com.imperionite.mdf.data.repository.PurchaseRepository
import com.imperionite.mdf.data.repository.PurchaseItemDraft
import com.imperionite.mdf.data.seeding.DemoDataSeeder
import com.imperionite.mdf.ui.prescriptions.BatchPurchasingScreen
import com.imperionite.mdf.ui.prescriptions.CreatePrescriptionScreen
import com.imperionite.mdf.ui.prescriptions.PrescriptionDetailsScreen
import com.imperionite.mdf.ui.prescriptions.PrescriptionInsightsScreen
import com.imperionite.mdf.ui.prescriptions.PrescriptionListScreen
import com.imperionite.mdf.ui.theme.MDFTheme
import com.imperionite.mdf.viewmodel.PrescriptionDetailsViewModel
import com.imperionite.mdf.viewmodel.PrescriptionListViewModel
import com.imperionite.mdf.viewmodel.PrescriptionListViewModelFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = DatabaseProvider.getDatabase(applicationContext)
        val prescriptionRepository = PrescriptionRepository(
            database = database,
            prescriptionDao = database.prescriptionDao(),
            medicineDao = database.prescriptionMedicineDao()
        )
        val prescriptionFactory = PrescriptionListViewModelFactory(prescriptionRepository)
        val purchaseRepository = PurchaseRepository(
            database = database,
            prescriptionDao = database.prescriptionDao(),
            prescriptionMedicineDao = database.prescriptionMedicineDao(),
            purchaseBatchDao = database.purchaseBatchDao(),
            purchaseBatchItemDao = database.purchaseBatchItemDao()
        )
        val detailsFactory = PrescriptionDetailsViewModel.Factory(purchaseRepository)

        setContent {
            MDFTheme {
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }
                val coroutineScope = rememberCoroutineScope()
                val prescriptionViewModel: PrescriptionListViewModel = viewModel(
                    factory = prescriptionFactory
                )
                val detailsViewModel: PrescriptionDetailsViewModel = viewModel(
                    factory = detailsFactory
                )
                val prescriptions by prescriptionViewModel.prescriptions.collectAsState()
                val details by detailsViewModel.details.collectAsState(initial = null)
                val purchaseState by detailsViewModel.purchaseState.collectAsState()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route
                val selectedPrescriptionId = backStackEntry?.arguments
                    ?.getLong(AppRoutes.ARG_PRESCRIPTION_ID)
                    ?.takeIf { it > 0L }

                var selectedAttachmentUri by rememberSaveable {
                    mutableStateOf<String?>(null)
                }
                var isSavingPrescription by rememberSaveable {
                    mutableStateOf(false)
                }
                var saveError by rememberSaveable {
                    mutableStateOf<String?>(null)
                }

                LaunchedEffect(selectedPrescriptionId) {
                    if (selectedPrescriptionId != null) {
                        detailsViewModel.loadPrescription(selectedPrescriptionId)
                    }
                }

                // Seed demo records only for a fresh database. Existing user data is never reset.
                LaunchedEffect(database) {
                    try {
                        withContext(Dispatchers.IO) {
                            DemoDataSeeder(database).seedIfNeeded()
                        }
                    } catch (exception: Exception) {
                        if (exception is CancellationException) {
                            ensureActive()
                        }
                        snackbarHostState.showSnackbar(
                            "Demo data could not be prepared: " +
                                    (exception.localizedMessage ?: "unknown error")
                        )
                    }
                }

                LaunchedEffect(purchaseState.deletedBatchId) {
                    if (purchaseState.deletedBatchId != null) {
                        snackbarHostState.showSnackbar("Purchase batch deleted successfully.")
                        detailsViewModel.clearPurchaseMessage()
                    }
                }

                LaunchedEffect(purchaseState.errorMessage, currentRoute) {
                    val message = purchaseState.errorMessage
                    val isPurchaseScreen = currentRoute == AppRoutes.NEW_PURCHASE ||
                            currentRoute == AppRoutes.EDIT_PURCHASE
                    if (message != null && !isPurchaseScreen) {
                        snackbarHostState.showSnackbar(message)
                        detailsViewModel.clearPurchaseMessage()
                    }
                }

                // Do not let system Back leave a screen while a database write is in progress.
                BackHandler(enabled = isSavingPrescription || purchaseState.isSaving) { }

                fun finishPurchaseSave(message: String) {
                    navController.popBackStack()
                    detailsViewModel.clearPurchaseMessage()
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(message)
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
                            // Some document providers do not offer persistable permissions.
                        }
                        selectedAttachmentUri = uri.toString()
                        saveError = null
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = AppRoutes.PRESCRIPTIONS,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable(AppRoutes.PRESCRIPTIONS) {
                            PrescriptionListScreen(
                                prescriptions = prescriptions,
                                onCreatePrescription = {
                                    if (!isSavingPrescription) {
                                        selectedAttachmentUri = null
                                        saveError = null
                                        navController.navigate(AppRoutes.CREATE_PRESCRIPTION) {
                                            launchSingleTop = true
                                        }
                                    }
                                },
                                onEditPrescription = { item ->
                                    if (!isSavingPrescription) {
                                        selectedAttachmentUri = item.prescription.attachmentUri
                                        saveError = null
                                        navController.navigate(
                                            AppRoutes.editPrescription(item.prescription.id)
                                        ) { launchSingleTop = true }
                                    }
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
                                    navController.navigate(
                                        AppRoutes.details(item.prescription.id)
                                    ) { launchSingleTop = true }
                                },
                                onOpenInsights = { item ->
                                    navController.navigate(
                                        AppRoutes.insights(item.prescription.id)
                                    ) { launchSingleTop = true }
                                }
                            )
                        }

                        composable(AppRoutes.CREATE_PRESCRIPTION) {
                            PrescriptionEditorDestination(
                                initialPrescription = null,
                                selectedAttachmentUri = selectedAttachmentUri,
                                isSaving = isSavingPrescription,
                                errorMessage = saveError,
                                onChooseAttachment = {
                                    attachmentPicker.launch(arrayOf("application/pdf", "image/*"))
                                },
                                onRemoveAttachment = { selectedAttachmentUri = null },
                                onSave = { physicianName, prescriptionDate, notes, medicines ->
                                    if (!isSavingPrescription) {
                                        isSavingPrescription = true
                                        saveError = null
                                        prescriptionViewModel.addPrescription(
                                            physicianName = physicianName,
                                            prescriptionDate = prescriptionDate,
                                            notes = notes,
                                            attachmentUri = selectedAttachmentUri,
                                            medicines = medicines,
                                            onSaved = {
                                                isSavingPrescription = false
                                                selectedAttachmentUri = null
                                                saveError = null
                                                navController.popBackStack()
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        "Prescription saved successfully."
                                                    )
                                                }
                                            },
                                            onError = { message ->
                                                isSavingPrescription = false
                                                saveError = message
                                            }
                                        )
                                    }
                                },
                                onCancel = {
                                    if (!isSavingPrescription) {
                                        selectedAttachmentUri = null
                                        saveError = null
                                        navController.popBackStack()
                                    }
                                }
                            )
                        }

                        composable(
                            route = AppRoutes.EDIT_PRESCRIPTION,
                            arguments = listOf(
                                navArgument(AppRoutes.ARG_PRESCRIPTION_ID) {
                                    type = NavType.LongType
                                }
                            )
                        ) { entry ->
                            val prescriptionId = entry.arguments
                                ?.getLong(AppRoutes.ARG_PRESCRIPTION_ID)
                            val initialPrescription = prescriptions.firstOrNull {
                                it.prescription.id == prescriptionId
                            } ?: details?.prescription?.takeIf {
                                it.prescription.id == prescriptionId
                            }

                            if (initialPrescription == null) {
                                LoadingContent()
                            } else {
                                PrescriptionEditorDestination(
                                    initialPrescription = initialPrescription,
                                    selectedAttachmentUri = selectedAttachmentUri,
                                    isSaving = isSavingPrescription,
                                    errorMessage = saveError,
                                    onChooseAttachment = {
                                        attachmentPicker.launch(
                                            arrayOf("application/pdf", "image/*")
                                        )
                                    },
                                    onRemoveAttachment = { selectedAttachmentUri = null },
                                    onSave = { physicianName, prescriptionDate, notes, medicines ->
                                        if (!isSavingPrescription) {
                                            isSavingPrescription = true
                                            saveError = null
                                            prescriptionViewModel.updatePrescription(
                                                prescriptionId = initialPrescription.prescription.id,
                                                physicianName = physicianName,
                                                prescriptionDate = prescriptionDate,
                                                notes = notes,
                                                attachmentUri = selectedAttachmentUri,
                                                medicines = medicines,
                                                onSaved = {
                                                    isSavingPrescription = false
                                                    selectedAttachmentUri = null
                                                    saveError = null
                                                    navController.popBackStack()
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            "Prescription updated successfully."
                                                        )
                                                    }
                                                },
                                                onError = { message ->
                                                    isSavingPrescription = false
                                                    saveError = message
                                                }
                                            )
                                        }
                                    },
                                    onCancel = {
                                        if (!isSavingPrescription) {
                                            selectedAttachmentUri = null
                                            saveError = null
                                            navController.popBackStack()
                                        }
                                    }
                                )
                            }
                        }

                        composable(
                            route = AppRoutes.PRESCRIPTION_DETAILS,
                            arguments = listOf(
                                navArgument(AppRoutes.ARG_PRESCRIPTION_ID) {
                                    type = NavType.LongType
                                }
                            )
                        ) { entry ->
                            val prescriptionId = entry.arguments
                                ?.getLong(AppRoutes.ARG_PRESCRIPTION_ID) ?: return@composable
                            val currentDetails = details?.takeIf {
                                it.prescription.prescription.id == prescriptionId
                            }
                            if (currentDetails == null) {
                                LoadingContent()
                            } else {
                                PrescriptionDetailsScreen(
                                    details = currentDetails,
                                    onBack = {
                                        if (!purchaseState.isSaving) navController.popBackStack()
                                    },
                                    onEdit = {
                                        if (!purchaseState.isSaving && !isSavingPrescription) {
                                            selectedAttachmentUri = currentDetails.prescription
                                                .prescription.attachmentUri
                                            saveError = null
                                            navController.navigate(
                                                AppRoutes.editPrescription(prescriptionId)
                                            ) { launchSingleTop = true }
                                        }
                                    },
                                    onNewPurchase = {
                                        if (!purchaseState.isSaving) {
                                            detailsViewModel.clearPurchaseMessage()
                                            navController.navigate(
                                                AppRoutes.newPurchase(prescriptionId)
                                            ) { launchSingleTop = true }
                                        }
                                    },
                                    onEditBatch = { batchId ->
                                        if (!purchaseState.isSaving) {
                                            detailsViewModel.clearPurchaseMessage()
                                            navController.navigate(
                                                AppRoutes.editPurchase(prescriptionId, batchId)
                                            ) { launchSingleTop = true }
                                        }
                                    },
                                    onDeleteBatch = { batchId ->
                                        if (!purchaseState.isSaving) {
                                            detailsViewModel.deletePurchase(batchId)
                                        }
                                    }
                                )
                            }
                        }

                        composable(
                            route = AppRoutes.PRESCRIPTION_INSIGHTS,
                            arguments = listOf(
                                navArgument(AppRoutes.ARG_PRESCRIPTION_ID) {
                                    type = NavType.LongType
                                }
                            )
                        ) { entry ->
                            val prescriptionId = entry.arguments
                                ?.getLong(AppRoutes.ARG_PRESCRIPTION_ID) ?: return@composable
                            val currentDetails = details?.takeIf {
                                it.prescription.prescription.id == prescriptionId
                            }
                            if (currentDetails == null) {
                                LoadingContent()
                            } else {
                                PrescriptionInsightsScreen(
                                    details = currentDetails,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }

                        composable(
                            route = AppRoutes.NEW_PURCHASE,
                            arguments = listOf(
                                navArgument(AppRoutes.ARG_PRESCRIPTION_ID) {
                                    type = NavType.LongType
                                }
                            )
                        ) { entry ->
                            val prescriptionId = entry.arguments
                                ?.getLong(AppRoutes.ARG_PRESCRIPTION_ID) ?: return@composable
                            PurchaseBatchDestination(
                                prescriptionId = prescriptionId,
                                details = details?.takeIf {
                                    it.prescription.prescription.id == prescriptionId
                                },
                                batchId = null,
                                isSaving = purchaseState.isSaving,
                                errorMessage = purchaseState.errorMessage,
                                onBack = {
                                    if (!purchaseState.isSaving) {
                                        detailsViewModel.clearPurchaseMessage()
                                        navController.popBackStack()
                                    }
                                },
                                onSave = { date, notes, items ->
                                    detailsViewModel.savePurchase(
                                        prescriptionId = prescriptionId,
                                        purchaseDate = date,
                                        notes = notes,
                                        items = items,
                                        onSaved = {
                                            finishPurchaseSave("Purchase batch saved successfully.")
                                        }
                                    )
                                },
                                onUpdate = { _, _, _, _ -> }
                            )
                        }

                        composable(
                            route = AppRoutes.EDIT_PURCHASE,
                            arguments = listOf(
                                navArgument(AppRoutes.ARG_PRESCRIPTION_ID) {
                                    type = NavType.LongType
                                },
                                navArgument(AppRoutes.ARG_BATCH_ID) {
                                    type = NavType.LongType
                                }
                            )
                        ) { entry ->
                            val prescriptionId = entry.arguments
                                ?.getLong(AppRoutes.ARG_PRESCRIPTION_ID) ?: return@composable
                            val batchId = entry.arguments
                                ?.getLong(AppRoutes.ARG_BATCH_ID) ?: return@composable
                            PurchaseBatchDestination(
                                prescriptionId = prescriptionId,
                                details = details?.takeIf {
                                    it.prescription.prescription.id == prescriptionId
                                },
                                batchId = batchId,
                                isSaving = purchaseState.isSaving,
                                errorMessage = purchaseState.errorMessage,
                                onBack = {
                                    if (!purchaseState.isSaving) {
                                        detailsViewModel.clearPurchaseMessage()
                                        navController.popBackStack()
                                    }
                                },
                                onSave = { _, _, _ -> },
                                onUpdate = { id, date, notes, items ->
                                    detailsViewModel.updatePurchase(
                                        batchId = id,
                                        purchaseDate = date,
                                        notes = notes,
                                        items = items,
                                        onSaved = {
                                            finishPurchaseSave("Purchase batch updated successfully.")
                                        }
                                    )
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

@Suppress("FunctionName")
@Composable
private fun PrescriptionEditorDestination(
    initialPrescription: PrescriptionWithMedicines?,
    selectedAttachmentUri: String?,
    isSaving: Boolean,
    errorMessage: String?,
    onChooseAttachment: () -> Unit,
    onRemoveAttachment: () -> Unit,
    onSave: (
        physicianName: String,
        prescriptionDate: String,
        notes: String,
        medicines: List<com.imperionite.mdf.data.model.PrescriptionMedicineDraft>
    ) -> Unit,
    onCancel: () -> Unit
) {
    CreatePrescriptionScreen(
        initialPrescription = initialPrescription,
        attachmentUri = selectedAttachmentUri,
        onChooseAttachment = onChooseAttachment,
        onRemoveAttachment = onRemoveAttachment,
        onSave = onSave,
        onCancel = onCancel,
        isSaving = isSaving,
        errorMessage = errorMessage
    )
}

@Suppress("FunctionName")
@Composable
private fun PurchaseBatchDestination(
    prescriptionId: Long,
    details: com.imperionite.mdf.data.repository.PrescriptionDetails?,
    batchId: Long?,
    isSaving: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onSave: (String, String, List<PurchaseItemDraft>) -> Unit,
    onUpdate: (Long, String, String, List<PurchaseItemDraft>) -> Unit
) {
    val editingBatch = if (batchId == null) null else {
        details?.purchaseBatches?.firstOrNull { it.batch.id == batchId }
    }

    if (details == null) {
        LoadingContent()
    } else if (batchId != null && editingBatch == null) {
        UnavailableContent(
            message = "This purchase batch no longer exists.",
            onBack = onBack
        )
    } else {
        BatchPurchasingScreen(
            prescriptionId = prescriptionId,
            physicianName = details.prescription.prescription.physicianName,
            medicines = details.prescription.medicines,
            purchasedQuantities = details.purchasedQuantities,
            isSaving = isSaving,
            errorMessage = errorMessage,
            editingBatch = editingBatch,
            onBack = onBack,
            onSave = onSave,
            onUpdate = onUpdate
        )
    }
}

@Suppress("FunctionName")
@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Suppress("FunctionName")
@Composable
private fun UnavailableContent(
    message: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
        TextButton(onClick = onBack) {
            Text("Go back")
        }
    }
}
