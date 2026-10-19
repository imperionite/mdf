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
        validatePrescription(physicianName, medicines)

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
                medicine.toEntity(prescriptionId)
            }

            medicineDao.insertAll(medicineEntities)
            prescriptionId
        }
    }

    suspend fun updatePrescriptionWithMedicines(
        prescriptionId: Long,
        physicianName: String,
        prescriptionDate: String,
        notes: String,
        attachmentUri: String?,
        medicines: List<PrescriptionMedicineDraft>
    ) {
        validatePrescription(physicianName, medicines)

        database.withTransaction {
            val existingPrescription = prescriptionDao.getById(prescriptionId)
                ?: throw IllegalArgumentException("Prescription not found.")

            val existingMedicines =
                medicineDao.getByPrescriptionId(prescriptionId)

            val existingById = existingMedicines.associateBy { it.id }
            val retainedIds = mutableSetOf<Long>()

            prescriptionDao.update(
                existingPrescription.copy(
                    physicianName = physicianName.trim(),
                    prescriptionDate = prescriptionDate,
                    notes = notes.trim(),
                    attachmentUri = attachmentUri
                )
            )

            val submittedExistingIds = medicines.mapNotNull { it.id }
            require(submittedExistingIds.distinct().size == submittedExistingIds.size) {
                "A medicine cannot be included more than once in a prescription."
            }

            medicines.forEach { draft ->
                val draftId = draft.id

                if (draftId != null) {
                    val existingMedicine = existingById[draftId]
                        ?: throw IllegalArgumentException(
                            "A medicine does not belong to this prescription."
                        )

                    val hasPurchaseHistory = medicineDao.hasPurchaseHistory(draftId)
                    if (hasPurchaseHistory) {
                        require(
                            draft.medicineName.trim() == existingMedicine.medicineName &&
                                    draft.unit.trim() == existingMedicine.unit
                        ) {
                            "A medicine with purchase history cannot be renamed or have its unit changed. " +
                                    "Keep the original medicine entry to preserve historical records."
                        }

                        val purchasedQuantity = medicineDao.getPurchasedQuantity(draftId)
                        require(
                            draft.prescribedQuantity + QUANTITY_TOLERANCE >= purchasedQuantity
                        ) {
                            "The prescribed quantity for ${existingMedicine.medicineName} cannot be " +
                                    "less than the $purchasedQuantity already purchased."
                        }
                    }

                    retainedIds.add(draftId)
                    medicineDao.update(
                        draft.toEntity(prescriptionId).copy(id = existingMedicine.id)
                    )
                } else {
                    medicineDao.insert(draft.toEntity(prescriptionId))
                }
            }

            existingMedicines
                .filterNot { it.id in retainedIds }
                .forEach { medicine ->
                    require(!medicineDao.hasPurchaseHistory(medicine.id)) {
                        "${medicine.medicineName} has purchase history and cannot be removed. " +
                                "Keep the medicine entry to preserve past purchases."
                    }
                    medicineDao.delete(medicine)
                }
        }
    }

    suspend fun deletePrescription(prescription: PrescriptionEntity) {
        prescriptionDao.delete(prescription)
    }

    private companion object {
        const val QUANTITY_TOLERANCE = 0.000001
    }

    private fun validatePrescription(
        physicianName: String,
        medicines: List<PrescriptionMedicineDraft>
    ) {
        require(physicianName.isNotBlank()) {
            "Physician name is required."
        }

        require(medicines.isNotEmpty()) {
            "Add at least one medicine."
        }

        require(medicines.all { medicine ->
            medicine.medicineName.isNotBlank() &&
                    medicine.prescribedQuantity > 0.0 &&
                    medicine.prescribedQuantity.isFinite() &&
                    medicine.unit.isNotBlank() &&
                    medicine.dosage.isNotBlank() &&
                    medicine.dosageRegimen.isNotBlank() &&
                    medicine.unitPriceCentavos >= 0
        }) {
            "Check the medicine details and try again."
        }
    }

    private fun PrescriptionMedicineDraft.toEntity(
        prescriptionId: Long
    ): PrescriptionMedicineEntity {
        return PrescriptionMedicineEntity(
            prescriptionId = prescriptionId,
            medicineName = medicineName.trim(),
            prescribedQuantity = prescribedQuantity,
            unit = unit.trim(),
            dosage = dosage.trim(),
            dosageRegimen = dosageRegimen.trim(),
            unitPriceCentavos = unitPriceCentavos
        )
    }
}
