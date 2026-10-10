
package com.imperionite.mdf.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.imperionite.mdf.data.local.dao.PrescriptionDao
import com.imperionite.mdf.data.local.entity.PrescriptionEntity

@Database(
    entities = [PrescriptionEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun prescriptionDao(): PrescriptionDao
}