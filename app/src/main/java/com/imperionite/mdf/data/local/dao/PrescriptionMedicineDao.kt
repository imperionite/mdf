
package com.imperionite.mdf.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.imperionite.mdf.data.local.entity.PrescriptionMedicineEntity

@Dao
interface PrescriptionMedicineDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(
        medicines: List<PrescriptionMedicineEntity>
    ): List<Long>

    @Query(
        """
        SELECT * FROM prescription_medicines
        WHERE prescriptionId = :prescriptionId
        ORDER BY id ASC
        """
    )
    suspend fun getByPrescriptionId(
        prescriptionId: Long
    ): List<PrescriptionMedicineEntity>
}
