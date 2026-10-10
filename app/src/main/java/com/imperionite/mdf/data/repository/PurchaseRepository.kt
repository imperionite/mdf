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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

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
        validatePurchaseInput(purchaseDate, items)

        return database.withTransaction {
            val prescription = prescriptionDao.getById(prescriptionId)
                ?: throw IllegalArgumentException("Prescription not found.")

            validateQuantities(
                prescriptionId = prescription.id,
                items = items,
                excludedBatch = null
            )

            purchaseBatchDao.insert(
                PurchaseBatchEntity(
                    prescriptionId = prescriptionId,
                    purchaseDate = purchaseDate,
                    notes = notes.trim()
                )
            ).also { batchId ->
                purchaseBatchItemDao.insertAll(
                    items.map { item ->
                        PurchaseBatchItemEntity(
                            purchaseBatchId = batchId,
                            prescriptionMedicineId =
                                item.prescriptionMedicineId,
                            purchasedQuantity = item.purchasedQuantity,
                            unitPriceCentavos = item.unitPriceCentavos
                        )
                    }
                )
            }
        }
    }

    suspend fun updatePurchaseBatch(
        batchId: Long,
        purchaseDate: String,
        notes: String,
        items: List<PurchaseItemDraft>
    ) {
        validatePurchaseInput(purchaseDate, items)

        database.withTransaction {
            val existing = purchaseBatchDao.getWithItems(batchId)
                ?: throw IllegalArgumentException("Purchase batch not found.")

            requireLatestBatch(existing.batch.prescriptionId, batchId)

            validateQuantities(
                prescriptionId = existing.batch.prescriptionId,
                items = items,
                excludedBatch = existing
            )

            purchaseBatchDao.update(
                existing.batch.copy(
                    purchaseDate = purchaseDate,
                    notes = notes.trim()
                )
            )

            purchaseBatchItemDao.deleteByBatchId(batchId)

            purchaseBatchItemDao.insertAll(
                items.map { item ->
                    PurchaseBatchItemEntity(
                        purchaseBatchId = batchId,
                        prescriptionMedicineId =
                            item.prescriptionMedicineId,
                        purchasedQuantity = item.purchasedQuantity,
                        unitPriceCentavos = item.unitPriceCentavos
                    )
                }
            )
        }
    }

    suspend fun deletePurchaseBatch(batchId: Long) {
        database.withTransaction {
            val existing = purchaseBatchDao.getWithItems(batchId)
                ?: throw IllegalArgumentException("Purchase batch not found.")

            requireLatestBatch(existing.batch.prescriptionId, batchId)

            // Explicitly remove items first to respect foreign-key constraints.
            purchaseBatchItemDao.deleteByBatchId(batchId)
            purchaseBatchDao.deleteById(batchId)
        }
    }

    suspend fun isLatestBatch(batchId: Long): Boolean {
        val batch = purchaseBatchDao.getWithItems(batchId) ?: return false

        return purchaseBatchDao.getLatestBatchId(
            batch.batch.prescriptionId
        ) == batchId
    }

    private suspend fun requireLatestBatch(
        prescriptionId: Long,
        batchId: Long
    ) {
        val latestId = purchaseBatchDao.getLatestBatchId(prescriptionId)

        require(latestId == batchId) {
            "Only the latest saved purchase batch can be edited or deleted."
        }
    }

    private fun validatePurchaseInput(
        purchaseDate: String,
        items: List<PurchaseItemDraft>
    ) {
        require(runCatching { LocalDate.parse(purchaseDate) }.isSuccess) {
            "Enter a valid purchase date."
        }

        require(items.isNotEmpty()) {
            "Select at least one medicine to purchase."
        }

        require(items.all {
            it.purchasedQuantity.isFinite() &&
                    it.purchasedQuantity > 0.0
        }) {
            "Every purchase quantity must be a valid number greater than zero."
        }

        require(items.all { it.unitPriceCentavos >= 0L }) {
            "Purchase prices cannot be negative."
        }

        require(
            items.map { it.prescriptionMedicineId }.distinct().size == items.size
        ) {
            "A medicine can only appear once in a purchase batch."
        }
    }

    private suspend fun validateQuantities(
        prescriptionId: Long,
        items: List<PurchaseItemDraft>,
        excludedBatch: PurchaseBatchWithItems?
    ) {
        val prescribedMedicines =
            prescriptionMedicineDao.getByPrescriptionId(prescriptionId)

        val medicinesById = prescribedMedicines.associateBy { it.id }

        val excludedQuantities = excludedBatch
            ?.items
            ?.groupBy { it.prescriptionMedicineId }
            ?.mapValues { (_, batchItems) ->
                batchItems.sumOf { it.purchasedQuantity }
            }
            ?: emptyMap()

        items.forEach { item ->
            val medicine = medicinesById[item.prescriptionMedicineId]
                ?: throw IllegalArgumentException(
                    "A selected medicine does not belong to this prescription."
                )

            val totalPurchased =
                purchaseBatchItemDao.getPurchasedQuantity(medicine.id)

            // When editing, do not count this batch's old quantity against
            // its replacement quantity.
            val purchasedOutsideBatch =
                totalPurchased -
                        (excludedQuantities[medicine.id] ?: 0.0)

            val remaining =
                medicine.prescribedQuantity - purchasedOutsideBatch

            require(
                item.purchasedQuantity <= remaining + 0.000001
            ) {
                "Purchase quantity for ${medicine.medicineName} exceeds " +
                        "the remaining quantity of ${remaining.coerceAtLeast(0.0)}."
            }
        }
    }
}
