package com.imperionite.mdf.data.repository

import androidx.room.withTransaction
import com.imperionite.mdf.data.local.AppDatabase
import com.imperionite.mdf.data.local.dao.PrescriptionDao
import com.imperionite.mdf.data.local.dao.PrescriptionMedicineDao
import com.imperionite.mdf.data.local.dao.PurchaseBatchDao
import com.imperionite.mdf.data.local.dao.PurchaseBatchItemDao
import com.imperionite.mdf.data.local.entity.PurchaseBatchEntity
import com.imperionite.mdf.data.local.entity.PurchaseBatchItemEntity
import com.imperionite.mdf.data.local.entity.PurchaseBatchWithItems
import com.imperionite.mdf.data.local.entity.PrescriptionWithMedicines
import com.imperionite.mdf.data.local.entity.PurchasedQuantitySummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

data class MedicinePurchaseStatus(
    val medicineId: Long,
    val purchasedQuantity: Double
)

data class PrescriptionDetails(
    val prescription: PrescriptionWithMedicines,
    val purchasedQuantities: Map<Long, Double>,
    val purchaseBatches: List<PurchaseBatchWithItems>
)

class PurchaseRepository(
    private val database: AppDatabase,
    private val prescriptionDao: PrescriptionDao,
    private val prescriptionMedicineDao: PrescriptionMedicineDao,
    private val purchaseBatchDao: PurchaseBatchDao,
    private val purchaseBatchItemDao: PurchaseBatchItemDao
) {

    fun observePrescriptionDetails(
        prescriptionId: Long
    ): Flow<PrescriptionDetails?> {
        return combine(
            prescriptionDao.observeById(prescriptionId),
            purchaseBatchItemDao.observePurchasedQuantities(prescriptionId),
            purchaseBatchDao.observeByPrescription(prescriptionId)
        ) { prescription, quantities, batches ->
            if (prescription == null) {
                null
            } else {
                PrescriptionDetails(
                    prescription = prescription,
                    purchasedQuantities = quantities.associate {
                        it.medicineId to it.purchasedQuantity
                    },
                    purchaseBatches = batches
                )
            }
        }
    }

    suspend fun savePurchaseBatch(
        prescriptionId: Long,
        purchaseDate: String,
        notes: String,
        items: List<PurchaseItemDraft>
    ): Long {
        require(items.isNotEmpty()) {
            "Select at least one medicine to purchase."
        }

        require(runCatching { LocalDate.parse(purchaseDate) }.isSuccess) {
            "Enter a valid purchase date."
        }

        require(items.all { it.purchasedQuantity > 0.0 }) {
            "Every purchase quantity must be greater than zero."
        }

        require(items.all { it.unitPriceCentavos >= 0L }) {
            "Purchase prices cannot be negative."
        }

        require(items.map { it.prescriptionMedicineId }.distinct().size == items.size) {
            "A medicine can only appear once in a purchase batch."
        }

        return database.withTransaction {
            val prescription = prescriptionDao.getById(prescriptionId)
                ?: throw IllegalArgumentException("Prescription not found.")

            val prescribedMedicines =
                prescriptionMedicineDao.getByPrescriptionId(prescription.id)

            val medicinesById = prescribedMedicines.associateBy { it.id }

            items.forEach { item ->
                val medicine = medicinesById[item.prescriptionMedicineId]
                    ?: throw IllegalArgumentException(
                        "A selected medicine does not belong to this prescription."
                    )

                val alreadyPurchased =
                    purchaseBatchItemDao.getPurchasedQuantity(medicine.id)

                val remaining = medicine.prescribedQuantity - alreadyPurchased

                require(item.purchasedQuantity <= remaining + 0.000001) {
                    "Purchase quantity for ${medicine.medicineName} exceeds " +
                            "the remaining quantity of ${remaining.coerceAtLeast(0.0)}."
                }
            }

            val batchId = purchaseBatchDao.insert(
                PurchaseBatchEntity(
                    prescriptionId = prescriptionId,
                    purchaseDate = purchaseDate,
                    notes = notes.trim()
                )
            )

            purchaseBatchItemDao.insertAll(
                items.map { item ->
                    PurchaseBatchItemEntity(
                        purchaseBatchId = batchId,
                        prescriptionMedicineId = item.prescriptionMedicineId,
                        purchasedQuantity = item.purchasedQuantity,
                        unitPriceCentavos = item.unitPriceCentavos
                    )
                }
            )

            batchId
        }
    }
}