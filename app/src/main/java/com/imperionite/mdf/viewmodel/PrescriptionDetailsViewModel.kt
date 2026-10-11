@file:OptIn(ExperimentalCoroutinesApi::class)

package com.imperionite.mdf.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.imperionite.mdf.data.repository.PurchaseItemDraft
import com.imperionite.mdf.data.repository.PurchaseRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PurchaseUiState(
    val isSaving: Boolean = false,
    val savedBatchId: Long? = null,
    val deletedBatchId: Long? = null,
    val errorMessage: String? = null
)

class PrescriptionDetailsViewModel(
    private val repository: PurchaseRepository
) : ViewModel() {

    private val selectedPrescriptionId = MutableStateFlow<Long?>(null)

    val details = selectedPrescriptionId
        .flatMapLatest { id ->
            if (id == null) {
                flowOf(null)
            } else {
                repository.observePrescriptionDetails(id)
            }
        }

    private val _purchaseState = MutableStateFlow(PurchaseUiState())
    val purchaseState: StateFlow<PurchaseUiState> =
        _purchaseState.asStateFlow()

    fun loadPrescription(prescriptionId: Long) {
        selectedPrescriptionId.value = prescriptionId
    }

    fun savePurchase(
        prescriptionId: Long,
        purchaseDate: String,
        notes: String,
        items: List<PurchaseItemDraft>
    ) {
        if (_purchaseState.value.isSaving) return

        viewModelScope.launch {
            _purchaseState.value = PurchaseUiState(isSaving = true)

            try {
                val batchId = repository.savePurchaseBatch(
                    prescriptionId = prescriptionId,
                    purchaseDate = purchaseDate,
                    notes = notes,
                    items = items
                )

                _purchaseState.value = PurchaseUiState(
                    savedBatchId = batchId
                )
            } catch (exception: Exception) {
                if (exception is CancellationException) {
                    ensureActive()
                }
                _purchaseState.value = PurchaseUiState(
                    errorMessage = exception.message
                        ?: "Unable to save this purchase."
                )
            }
        }
    }

    fun updatePurchase(
        batchId: Long,
        purchaseDate: String,
        notes: String,
        items: List<PurchaseItemDraft>
    ) {
        if (_purchaseState.value.isSaving) return

        viewModelScope.launch {
            _purchaseState.value = PurchaseUiState(isSaving = true)

            try {
                repository.updatePurchaseBatch(
                    batchId = batchId,
                    purchaseDate = purchaseDate,
                    notes = notes,
                    items = items
                )

                _purchaseState.value = PurchaseUiState(
                    savedBatchId = batchId
                )
            } catch (exception: Exception) {
                if (exception is CancellationException) {
                    ensureActive()
                }
                _purchaseState.value = PurchaseUiState(
                    errorMessage = exception.message
                        ?: "Unable to update this purchase."
                )
            }
        }
    }

    fun deletePurchase(batchId: Long) {
        if (_purchaseState.value.isSaving) return

        viewModelScope.launch {
            _purchaseState.value = PurchaseUiState(isSaving = true)

            try {
                repository.deletePurchaseBatch(batchId)

                _purchaseState.value = PurchaseUiState(
                    deletedBatchId = batchId
                )
            } catch (exception: Exception) {
                if (exception is CancellationException) {
                    ensureActive()
                }
                _purchaseState.value = PurchaseUiState(
                    errorMessage = exception.message
                        ?: "Unable to delete this purchase."
                )
            }
        }
    }

    fun clearPurchaseMessage() {
        _purchaseState.update { PurchaseUiState() }
    }

    class Factory(
        private val repository: PurchaseRepository
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            modelClass: Class<T>
        ): T {
            require(
                modelClass.isAssignableFrom(
                    PrescriptionDetailsViewModel::class.java
                )
            )

            return PrescriptionDetailsViewModel(repository) as T
        }
    }
}
