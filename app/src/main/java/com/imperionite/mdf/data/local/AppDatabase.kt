package com.imperionite.mdf.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.imperionite.mdf.data.local.dao.PrescriptionDao
import com.imperionite.mdf.data.local.dao.PrescriptionMedicineDao
import com.imperionite.mdf.data.local.dao.PurchaseBatchDao
import com.imperionite.mdf.data.local.dao.PurchaseBatchItemDao
import com.imperionite.mdf.data.local.entity.PrescriptionEntity
import com.imperionite.mdf.data.local.entity.PrescriptionMedicineEntity
import com.imperionite.mdf.data.local.entity.PurchaseBatchEntity
import com.imperionite.mdf.data.local.entity.PurchaseBatchItemEntity

@Database(
    entities = [
        PrescriptionEntity::class,
        PrescriptionMedicineEntity::class,
        PurchaseBatchEntity::class,
        PurchaseBatchItemEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun prescriptionDao(): PrescriptionDao

    abstract fun prescriptionMedicineDao(): PrescriptionMedicineDao

    abstract fun purchaseBatchDao(): PurchaseBatchDao

    abstract fun purchaseBatchItemDao(): PurchaseBatchItemDao

    companion object {

        // Retain your existing migration from database version 1 to 2.
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS prescription_medicines (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        prescriptionId INTEGER NOT NULL,
                        medicineName TEXT NOT NULL,
                        prescribedQuantity REAL NOT NULL,
                        unit TEXT NOT NULL,
                        dosage TEXT NOT NULL,
                        frequency TEXT NOT NULL,
                        unitPriceCentavos INTEGER NOT NULL,
                        FOREIGN KEY (prescriptionId)
                            REFERENCES prescriptions(id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    index_prescription_medicines_prescriptionId
                    ON prescription_medicines(prescriptionId)
                    """.trimIndent()
                )
            }
        }

        // Rebuild the table instead of relying on newer SQLite column-rename support.
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS prescription_medicines_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        prescriptionId INTEGER NOT NULL,
                        medicineName TEXT NOT NULL,
                        prescribedQuantity REAL NOT NULL,
                        unit TEXT NOT NULL,
                        dosage TEXT NOT NULL,
                        dosageRegimen TEXT NOT NULL,
                        unitPriceCentavos INTEGER NOT NULL,
                        FOREIGN KEY (prescriptionId)
                            REFERENCES prescriptions(id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO prescription_medicines_new (
                        id,
                        prescriptionId,
                        medicineName,
                        prescribedQuantity,
                        unit,
                        dosage,
                        dosageRegimen,
                        unitPriceCentavos
                    )
                    SELECT
                        id,
                        prescriptionId,
                        medicineName,
                        prescribedQuantity,
                        unit,
                        dosage,
                        frequency,
                        unitPriceCentavos
                    FROM prescription_medicines
                    """.trimIndent()
                )

                db.execSQL("DROP TABLE prescription_medicines")

                db.execSQL(
                    """
                    ALTER TABLE prescription_medicines_new
                    RENAME TO prescription_medicines
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    index_prescription_medicines_prescriptionId
                    ON prescription_medicines(prescriptionId)
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS purchase_batches (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        prescriptionId INTEGER NOT NULL,
                        purchaseDate TEXT NOT NULL,
                        notes TEXT NOT NULL,
                        FOREIGN KEY (prescriptionId)
                            REFERENCES prescriptions(id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    index_purchase_batches_prescriptionId
                    ON purchase_batches(prescriptionId)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS purchase_batch_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        purchaseBatchId INTEGER NOT NULL,
                        prescriptionMedicineId INTEGER NOT NULL,
                        purchasedQuantity REAL NOT NULL,
                        unitPriceCentavos INTEGER NOT NULL,
                        FOREIGN KEY (purchaseBatchId)
                            REFERENCES purchase_batches(id)
                            ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY (prescriptionMedicineId)
                            REFERENCES prescription_medicines(id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    index_purchase_batch_items_purchaseBatchId
                    ON purchase_batch_items(purchaseBatchId)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    index_purchase_batch_items_prescriptionMedicineId
                    ON purchase_batch_items(prescriptionMedicineId)
                    """.trimIndent()
                )
            }
        }
    }
}