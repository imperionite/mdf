
package com.imperionite.mdf.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.imperionite.mdf.data.local.dao.PrescriptionDao
import com.imperionite.mdf.data.local.dao.PrescriptionMedicineDao
import com.imperionite.mdf.data.local.entity.PrescriptionEntity
import com.imperionite.mdf.data.local.entity.PrescriptionMedicineEntity

@Database(
    entities = [
        PrescriptionEntity::class,
        PrescriptionMedicineEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun prescriptionDao(): PrescriptionDao

    abstract fun prescriptionMedicineDao(): PrescriptionMedicineDao

    companion object {
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
                            ON UPDATE NO ACTION
                            ON DELETE CASCADE
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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE prescription_medicines
                    RENAME COLUMN frequency TO dosageRegimen
                    """.trimIndent()
                )
            }
        }
    }
}
