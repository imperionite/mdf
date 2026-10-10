
package com.imperionite.mdf.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.imperionite.mdf.data.local.entity.PrescriptionEntity
import com.imperionite.mdf.data.local.entity.PrescriptionWithMedicines
import com.imperionite.mdf.data.model.PrescriptionMedicineDraft
import com.imperionite.mdf.data.repository.PrescriptionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PrescriptionListViewModel(
    private val repository: PrescriptionRepository
) : ViewModel() {

    val prescriptions = repository.observePrescriptions().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun addPrescription(
        physicianName: String,
        prescriptionDate: String,
        notes: String,
        attachmentUri: String?,
        medicines: List<PrescriptionMedicineDraft>,
        onSaved: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.addPrescriptionWithMedicines(
                        physicianName = physicianName,
                        prescriptionDate = prescriptionDate,
                        notes = notes,
                        attachmentUri = attachmentUri,
                        medicines = medicines
                    )
                }
                onSaved()
            } catch (exception: Exception) {
                onError(
                    exception.localizedMessage
                        ?: "Unable to save the prescription."
                )
            }
        }
    }

    fun updatePrescription(
        prescriptionId: Long,
        physicianName: String,
        prescriptionDate: String,
        notes: String,
        attachmentUri: String?,
        medicines: List<PrescriptionMedicineDraft>,
        onSaved: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.updatePrescriptionWithMedicines(
                        prescriptionId = prescriptionId,
                        physicianName = physicianName,
                        prescriptionDate = prescriptionDate,
                        notes = notes,
                        attachmentUri = attachmentUri,
                        medicines = medicines
                    )
                }
                onSaved()
            } catch (exception: Exception) {
                onError(
                    exception.localizedMessage
                        ?: "Unable to update the prescription."
                )
            }
        }
    }

    fun deletePrescription(prescription: PrescriptionEntity) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.deletePrescription(prescription)
                }
            } catch (_: Exception) {
                // Delete error feedback can be added with confirmation UI.
            }
        }
    }
}

class PrescriptionListViewModelFactory(
    private val repository: PrescriptionRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(
                PrescriptionListViewModel::class.java
            )
        ) {
            return PrescriptionListViewModel(repository) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}
