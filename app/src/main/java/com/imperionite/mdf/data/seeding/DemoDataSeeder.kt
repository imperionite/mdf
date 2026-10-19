
package com.imperionite.mdf.data.seeding

import androidx.room.withTransaction
import com.imperionite.mdf.data.local.AppDatabase
import com.imperionite.mdf.data.local.entity.DemoSeedMetadata
import com.imperionite.mdf.data.local.entity.PrescriptionEntity
import com.imperionite.mdf.data.local.entity.PrescriptionMedicineEntity
import com.imperionite.mdf.data.local.entity.PurchaseBatchEntity
import com.imperionite.mdf.data.local.entity.PurchaseBatchItemEntity

/**
 * Inserts two deterministic evaluation scenarios on a fresh installation.
 *
 * The datasets are fictional and intended only for software evaluation.
 * Dosage regimens use the project's posology format:
 * morning-breakfast / midday-lunch / evening-dinner.
 *
 * Examples:
 * 1-0-0   = one unit in the morning
 * 1-0-1   = one unit in the morning and one in the evening
 * 0-0-1   = one unit in the evening
 *
 * Existing user-created prescriptions are never deleted or overwritten.
 */
class DemoDataSeeder(private val database: AppDatabase) {

    private val prescriptionDao = database.prescriptionDao()
    private val medicineDao = database.prescriptionMedicineDao()
    private val batchDao = database.purchaseBatchDao()
    private val batchItemDao = database.purchaseBatchItemDao()
    private val metadataDao = database.demoSeedMetadataDao()

    suspend fun seedIfNeeded() {
        database.withTransaction {
            val hasSeedMetadata = metadataDao.count() > 0
            val existingPrescriptionCount = prescriptionDao.countAll()

            // Do not mix demo data into an existing, unmarked user database.
            if (!hasSeedMetadata && existingPrescriptionCount > 0) {
                return@withTransaction
            }

            seedPartialPurchaseScenario()
            seedCompletedPurchaseScenario()
        }
    }

    /**
     * Scenario A:
     * Multiple medicines have been partially purchased.
     *
     * Expected batch totals:
     * Batch 1 = ₱140.00
     * Batch 2 = ₱235.00
     * Batch 3 = ₱95.00
     * Overall = ₱470.00
     */
    private suspend fun seedPartialPurchaseScenario() {
        val seedKey = SEED_PARTIAL_PURCHASES
        if (metadataDao.isSeeded(seedKey)) return

        val prescriptionId = prescriptionDao.insert(
            PrescriptionEntity(
                physicianName = "Dr. Maria Santos",
                prescriptionDate = "2026-10-01",
                notes = "Fictional evaluation scenario: multiple partial purchases.",
                attachmentUri = null
            )
        )

        val medicines = listOf(
            PrescriptionMedicineEntity(
                prescriptionId = prescriptionId,
                medicineName = "Amlodipine 5 mg",
                prescribedQuantity = 100.0,
                unit = "tablets",
                dosage = "5 mg",
                dosageRegimen = "1-0-0",
                unitPriceCentavos = 250L
            ),
            PrescriptionMedicineEntity(
                prescriptionId = prescriptionId,
                medicineName = "Metformin 500 mg",
                prescribedQuantity = 60.0,
                unit = "tablets",
                dosage = "500 mg",
                dosageRegimen = "1-0-1",
                unitPriceCentavos = 800L
            ),
            PrescriptionMedicineEntity(
                prescriptionId = prescriptionId,
                medicineName = "Losartan 50 mg",
                prescribedQuantity = 30.0,
                unit = "tablets",
                dosage = "50 mg",
                dosageRegimen = "1-0-0",
                unitPriceCentavos = 500L
            ),
            PrescriptionMedicineEntity(
                prescriptionId = prescriptionId,
                medicineName = "Atorvastatin 20 mg",
                prescribedQuantity = 90.0,
                unit = "tablets",
                dosage = "20 mg",
                dosageRegimen = "0-0-1",
                unitPriceCentavos = 300L
            )
        )

        val ids = medicineDao.insertAll(medicines)

        // Keys must exactly match SeedItem.medicineName values below.
        val medicineIds = medicines.mapIndexed { index, medicine ->
            medicine.medicineName to ids[index]
        }.toMap()

        insertBatch(
            prescriptionId = prescriptionId,
            date = "2026-10-03",
            notes = "Batch 1: initial partial purchase.",
            medicineIds = medicineIds,
            items = listOf(
                SeedItem("Amlodipine 5 mg", 20.0, 250L),
                SeedItem("Atorvastatin 20 mg", 30.0, 300L)
            )
        )

        insertBatch(
            prescriptionId = prescriptionId,
            date = "2026-10-10",
            notes = "Batch 2: additional medicines purchased.",
            medicineIds = medicineIds,
            items = listOf(
                SeedItem("Amlodipine 5 mg", 30.0, 250L),
                SeedItem("Metformin 500 mg", 20.0, 800L)
            )
        )

        insertBatch(
            prescriptionId = prescriptionId,
            date = "2026-10-15",
            notes = "Batch 3: another partial purchase.",
            medicineIds = medicineIds,
            items = listOf(
                SeedItem("Losartan 50 mg", 10.0, 500L),
                SeedItem("Atorvastatin 20 mg", 15.0, 300L)
            )
        )

        metadataDao.insert(
            DemoSeedMetadata(seedKey, System.currentTimeMillis())
        )
    }

    /**
     * Scenario B:
     * All prescribed quantities have been purchased across three batches.
     *
     * Expected batch totals:
     * Batch 1 = ₱200.00
     * Batch 2 = ₱345.00
     * Batch 3 = ₱260.00
     * Overall = ₱805.00
     *
     * Historical batch prices intentionally differ from current prices
     * to test purchase price snapshots.
     */
    private suspend fun seedCompletedPurchaseScenario() {
        val seedKey = SEED_COMPLETED_PURCHASES
        if (metadataDao.isSeeded(seedKey)) return

        val prescriptionId = prescriptionDao.insert(
            PrescriptionEntity(
                physicianName = "Dr. Antonio Reyes",
                prescriptionDate = "2026-10-05",
                notes = "Fictional evaluation scenario: prescription fully purchased in batches.",
                attachmentUri = null
            )
        )

        val medicines = listOf(
            PrescriptionMedicineEntity(
                prescriptionId = prescriptionId,
                medicineName = "Losartan 50 mg",
                prescribedQuantity = 30.0,
                unit = "tablets",
                dosage = "50 mg",
                dosageRegimen = "1-0-0",
                unitPriceCentavos = 1350L
            ),
            PrescriptionMedicineEntity(
                prescriptionId = prescriptionId,
                medicineName = "Metformin 500 mg",
                prescribedQuantity = 60.0,
                unit = "tablets",
                dosage = "500 mg",
                dosageRegimen = "1-0-1",
                unitPriceCentavos = 450L
            ),
            PrescriptionMedicineEntity(
                prescriptionId = prescriptionId,
                medicineName = "Atorvastatin 20 mg",
                prescribedQuantity = 20.0,
                unit = "tablets",
                dosage = "20 mg",
                dosageRegimen = "0-0-1",
                unitPriceCentavos = 800L
            )
        )

        val ids = medicineDao.insertAll(medicines)

        val medicineIds = medicines.mapIndexed { index, medicine ->
            medicine.medicineName to ids[index]
        }.toMap()

        insertBatch(
            prescriptionId = prescriptionId,
            date = "2026-10-06",
            notes = "Batch 1: first purchase.",
            medicineIds = medicineIds,
            items = listOf(
                SeedItem("Losartan 50 mg", 10.0, 1200L),
                SeedItem("Metformin 500 mg", 20.0, 400L)
            )
        )

        insertBatch(
            prescriptionId = prescriptionId,
            date = "2026-10-12",
            notes = "Batch 2: Losartan price changed; first Atorvastatin purchase.",
            medicineIds = medicineIds,
            items = listOf(
                SeedItem("Losartan 50 mg", 20.0, 1350L),
                SeedItem("Atorvastatin 20 mg", 10.0, 750L)
            )
        )

        insertBatch(
            prescriptionId = prescriptionId,
            date = "2026-10-16",
            notes = "Batch 3: remaining quantities purchased at updated prices.",
            medicineIds = medicineIds,
            items = listOf(
                SeedItem("Metformin 500 mg", 40.0, 450L),
                SeedItem("Atorvastatin 20 mg", 10.0, 800L)
            )
        )

        metadataDao.insert(
            DemoSeedMetadata(seedKey, System.currentTimeMillis())
        )
    }

    private suspend fun insertBatch(
        prescriptionId: Long,
        date: String,
        notes: String,
        medicineIds: Map<String, Long>,
        items: List<SeedItem>
    ) {
        val batchId = batchDao.insert(
            PurchaseBatchEntity(
                prescriptionId = prescriptionId,
                purchaseDate = date,
                notes = notes
            )
        )

        batchItemDao.insertAll(
            items.map { item ->
                val medicineId = medicineIds[item.medicineName]
                    ?: error("Missing seeded medicine: ${item.medicineName}")

                PurchaseBatchItemEntity(
                    purchaseBatchId = batchId,
                    prescriptionMedicineId = medicineId,
                    purchasedQuantity = item.quantity,
                    unitPriceCentavos = item.unitPriceCentavos
                )
            }
        )
    }

    private data class SeedItem(
        val medicineName: String,
        val quantity: Double,
        val unitPriceCentavos: Long
    )

    companion object {
        // Keep the existing keys so existing installations are not seeded twice.
        const val SEED_PARTIAL_PURCHASES = "demo_prescription_a_v1"
        const val SEED_COMPLETED_PURCHASES = "demo_prescription_b_v1"
    }
}
