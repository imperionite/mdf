package com.imperionite.mdf

import android.content.Context
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.imperionite.mdf.data.local.AppDatabase
import com.imperionite.mdf.data.local.entity.PrescriptionEntity
import com.imperionite.mdf.data.local.entity.PrescriptionMedicineEntity
import com.imperionite.mdf.data.model.PrescriptionMedicineDraft
import com.imperionite.mdf.data.repository.PrescriptionRepository
import com.imperionite.mdf.data.seeding.DemoDataSeeder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProjectRegressionTest {

    private lateinit var database: AppDatabase

    @Before
    fun createDatabase() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun demoDataSeedingIsIdempotentAndCreatesExpectedScenarios() = runBlocking {
        val seeder = DemoDataSeeder(database)
        seeder.seedIfNeeded()
        seeder.seedIfNeeded()

        val prescriptions = database.prescriptionDao().observeAll().first()
        assertEquals(2, prescriptions.size)
        assertEquals(7, countRows("prescription_medicines"))
        assertEquals(6, countRows("purchase_batches"))
        assertEquals(12, countRows("purchase_batch_items"))
        assertEquals(2, database.demoSeedMetadataDao().count())

        val partial = prescriptions.first { it.prescription.physicianName == "Dr. Maria Santos" }
        val completed = prescriptions.first { it.prescription.physicianName == "Dr. Antonio Reyes" }

        assertEquals(47000.0, spendingInCentavos(partial.prescription.id), 0.001)
        assertEquals(80500.0, spendingInCentavos(completed.prescription.id), 0.001)

        val partialQuantities = partial.medicines.associate { medicine ->
            medicine.medicineName to database.prescriptionMedicineDao()
                .getPurchasedQuantity(medicine.id)
        }
        assertEquals(50.0, partialQuantities["Amlodipine 5 mg"]!!, 0.001)
        assertEquals(20.0, partialQuantities["Metformin 500 mg"]!!, 0.001)
        assertEquals(10.0, partialQuantities["Losartan 50 mg"]!!, 0.001)
        assertEquals(45.0, partialQuantities["Atorvastatin 20 mg"]!!, 0.001)

        completed.medicines.forEach { medicine ->
            assertEquals(
                medicine.prescribedQuantity,
                database.prescriptionMedicineDao().getPurchasedQuantity(medicine.id),
                0.001
            )
        }
    }


    @Test
    fun existingUnseededUserDatabaseIsNotPopulatedWithDemoRecords() = runBlocking {
        database.prescriptionDao().insert(
            PrescriptionEntity(
                physicianName = "Existing user record",
                prescriptionDate = "2026-10-18",
                notes = "Must be preserved."
            )
        )

        DemoDataSeeder(database).seedIfNeeded()

        val prescriptions = database.prescriptionDao().observeAll().first()
        assertEquals(1, prescriptions.size)
        assertEquals("Existing user record", prescriptions.single().prescription.physicianName)
        assertEquals(0, database.demoSeedMetadataDao().count())
    }

    @Test
    fun prescriptionEditCannotRemoveMedicineWithPurchaseHistory() = runBlocking {
        DemoDataSeeder(database).seedIfNeeded()
        val partial = database.prescriptionDao().observeAll().first()
            .first { it.prescription.physicianName == "Dr. Maria Santos" }
        val retainedMedicines = partial.medicines
            .filterNot { it.medicineName == "Amlodipine 5 mg" }
            .map { it.toDraft() }

        val repository = PrescriptionRepository(
            database = database,
            prescriptionDao = database.prescriptionDao(),
            medicineDao = database.prescriptionMedicineDao()
        )

        var failure: IllegalArgumentException? = null
        try {
            repository.updatePrescriptionWithMedicines(
                prescriptionId = partial.prescription.id,
                physicianName = partial.prescription.physicianName,
                prescriptionDate = partial.prescription.prescriptionDate,
                notes = partial.prescription.notes,
                attachmentUri = partial.prescription.attachmentUri,
                medicines = retainedMedicines
            )
        } catch (exception: IllegalArgumentException) {
            failure = exception
        }

        assertNotNull("Expected an edit with historical purchases to be rejected.", failure)
        assertEquals(12, countRows("purchase_batch_items"))
        assertEquals(
            50.0, database.prescriptionMedicineDao().getPurchasedQuantity(
                partial.medicines.first { it.medicineName == "Amlodipine 5 mg" }.id
            ), 0.001
        )
    }


    @Test
    fun prescriptionEditCannotReduceQuantityBelowAlreadyPurchasedAmount() = runBlocking {
        DemoDataSeeder(database).seedIfNeeded()
        val partial = database.prescriptionDao().observeAll().first()
            .first { it.prescription.physicianName == "Dr. Maria Santos" }
        val drafts = partial.medicines.map { medicine ->
            medicine.toDraft().let { draft ->
                if (medicine.medicineName == "Amlodipine 5 mg") {
                    draft.copy(prescribedQuantity = 40.0)
                } else {
                    draft
                }
            }
        }
        val repository = PrescriptionRepository(
            database = database,
            prescriptionDao = database.prescriptionDao(),
            medicineDao = database.prescriptionMedicineDao()
        )

        var failure: IllegalArgumentException? = null
        try {
            repository.updatePrescriptionWithMedicines(
                prescriptionId = partial.prescription.id,
                physicianName = partial.prescription.physicianName,
                prescriptionDate = partial.prescription.prescriptionDate,
                notes = partial.prescription.notes,
                attachmentUri = partial.prescription.attachmentUri,
                medicines = drafts
            )
        } catch (exception: IllegalArgumentException) {
            failure = exception
        }

        assertNotNull("Expected the invalid quantity reduction to be rejected.", failure)
        assertEquals(
            100.0, database.prescriptionMedicineDao()
                .getByPrescriptionId(partial.prescription.id)
                .first { it.medicineName == "Amlodipine 5 mg" }
                .prescribedQuantity, 0.001)
        assertEquals(12, countRows("purchase_batch_items"))
    }

    @Test
    fun changingCurrentPriceDoesNotChangeHistoricalPurchasePrices() = runBlocking {
        DemoDataSeeder(database).seedIfNeeded()
        val completed = database.prescriptionDao().observeAll().first()
            .first { it.prescription.physicianName == "Dr. Antonio Reyes" }
        val losartan = completed.medicines.first { it.medicineName == "Losartan 50 mg" }

        val before = pricesForMedicine(
            medicineName = "Losartan 50 mg",
            prescriptionId = completed.prescription.id
        )
        assertEquals(listOf(1200L, 1350L), before)

        database.prescriptionMedicineDao().update(
            losartan.copy(unitPriceCentavos = 1500L)
        )

        val after = pricesForMedicine(
            medicineName = "Losartan 50 mg",
            prescriptionId = completed.prescription.id
        )
        assertEquals(before, after)
        assertEquals(
            1500L, database.prescriptionMedicineDao()
                .getByPrescriptionId(completed.prescription.id)
                .first { it.medicineName == "Losartan 50 mg" }
                .unitPriceCentavos)
    }

    private fun countRows(tableName: String): Int {
        require(tableName.matches(Regex("[a-z_]+")))
        database.openHelper.readableDatabase
            .query("SELECT COUNT(*) FROM $tableName")
            .use { cursor ->
                cursor.moveToFirst()
                return cursor.getInt(0)
            }
    }

    private fun spendingInCentavos(prescriptionId: Long): Double {
        database.openHelper.readableDatabase.query(
            """
            SELECT COALESCE(SUM(pbi.purchasedQuantity * pbi.unitPriceCentavos), 0.0)
            FROM purchase_batch_items AS pbi
            INNER JOIN purchase_batches AS pb ON pb.id = pbi.purchaseBatchId
            WHERE pb.prescriptionId = ?
            """.trimIndent(),
            arrayOf(prescriptionId.toString())
        ).use { cursor ->
            cursor.moveToFirst()
            return cursor.getDouble(0)
        }
    }

    private fun pricesForMedicine(
        medicineName: String,
        prescriptionId: Long
    ): List<Long> {
        val prices = mutableListOf<Long>()

        database.openHelper.readableDatabase.query(
            """
        SELECT pbi.unitPriceCentavos
        FROM purchase_batch_items AS pbi
        INNER JOIN purchase_batches AS pb
            ON pb.id = pbi.purchaseBatchId
        INNER JOIN prescription_medicines AS pm
            ON pm.id = pbi.prescriptionMedicineId
        WHERE pm.medicineName = ?
          AND pb.prescriptionId = ?
        ORDER BY pb.purchaseDate ASC
        """.trimIndent(),
            arrayOf(medicineName, prescriptionId.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                prices += cursor.getLong(0)
            }
        }

        return prices
    }

    private fun PrescriptionMedicineEntity.toDraft() = PrescriptionMedicineDraft(
        id = id,
        medicineName = medicineName,
        prescribedQuantity = prescribedQuantity,
        unit = unit,
        dosage = dosage,
        dosageRegimen = dosageRegimen,
        unitPriceCentavos = unitPriceCentavos
    )
}