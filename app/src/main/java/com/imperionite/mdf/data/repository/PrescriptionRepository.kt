
package com.imperionite.mdf.data.repository

import androidx.room.withTransaction
import com.imperionite.mdf.data.local.AppDatabase
import com.imperionite.mdf.data.local.dao.PrescriptionDao
import com.imperionite.mdf.data.local.dao.PrescriptionMedicineDao
import com.imperionite.mdf.data.local.entity.PrescriptionEntity
import com.imperionite.mdf.data.local.entity.PrescriptionMedicineEntity
import com.imperionite.mdf.data.local.entity.PrescriptionWithMedicines
import com.imperionite.mdf.data.model.PrescriptionMedicineDraft
import kotlinx.coroutines.flow.Flow

class PrescriptionRepository(
    private val database: AppDatabase,
    private val prescriptionDao: PrescriptionDao,
    private val medicineDao: PrescriptionMedicineDao
) {

    fun observePrescriptions(): Flow<List<PrescriptionWithMedicines>> =
        prescriptionDao.observeAll()

    suspend fun addPrescriptionWithMedicines(
        physicianName: String,
        prescriptionDate: String,
        notes: String = "",
        attachmentUri: String? = null,
        medicines: List<PrescriptionMedicineDraft>
    ): Long {
        require(physicianName.isNotBlank()) {
            "Physician name is required."
        }

        require(medicines.isNotEmpty()) {
            "Add at least one medicine."
        }

        require(medicines.all {
            it.medicineName.isNotBlank() &&
                    it.prescribedQuantity > 0 &&
                    it.unit.isNotBlank() &&
                    it.dosage.isNotBlank() &&
                    it.frequency.isNotBlank() &&
                    it.unitPriceCentavos >= 0
        }) {
            "Check the medicine details and try again."
        }

        return database.withTransaction {
            val prescriptionId = prescriptionDao.insert(
                PrescriptionEntity(
                    physicianName = physicianName.trim(),
                    prescriptionDate = prescriptionDate,
                    notes = notes.trim(),
                    attachmentUri = attachmentUri
                )
            )

            val medicineEntities = medicines.map { medicine ->
                PrescriptionMedicineEntity(
                    prescriptionId = prescriptionId,
                    medicineName = medicine.medicineName.trim(),
                    prescribedQuantity = medicine.prescribedQuantity,
                    unit = medicine.unit.trim(),
                    dosage = medicine.dosage.trim(),
                    frequency = medicine.frequency.trim(),
                    unitPriceCentavos = medicine.unitPriceCentavos
                )
            }

            medicineDao.insertAll(medicineEntities)

            prescriptionId
        }
    }

    suspend fun deletePrescription(prescription: PrescriptionEntity) {
        prescriptionDao.delete(prescription)
    }
}
