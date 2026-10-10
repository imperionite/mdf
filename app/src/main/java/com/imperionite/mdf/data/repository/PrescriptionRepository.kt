
package com.imperionite.mdf.data.repository

import com.imperionite.mdf.data.local.dao.PrescriptionDao
import com.imperionite.mdf.data.local.entity.PrescriptionEntity
import kotlinx.coroutines.flow.Flow

class PrescriptionRepository(
    private val prescriptionDao: PrescriptionDao
) {
    fun observePrescriptions(): Flow<List<PrescriptionEntity>> {
        return prescriptionDao.observeAll()
    }

    suspend fun addPrescription(
        physicianName: String,
        prescriptionDate: String,
        notes: String = "",
        attachmentUri: String? = null
    ): Long {
        val prescription = PrescriptionEntity(
            physicianName = physicianName.trim(),
            prescriptionDate = prescriptionDate,
            notes = notes.trim(),
            attachmentUri = attachmentUri
        )

        return prescriptionDao.insert(prescription)
    }

    suspend fun deletePrescription(
        prescription: PrescriptionEntity
    ) {
        prescriptionDao.delete(prescription)
    }
}