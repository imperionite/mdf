package com.imperionite.mdf.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.imperionite.mdf.data.local.entity.PrescriptionEntity
import com.imperionite.mdf.data.local.entity.PrescriptionWithMedicines
import kotlinx.coroutines.flow.Flow

@Dao
interface PrescriptionDao {

    @Transaction
    @Query(
        """
        SELECT * FROM prescriptions
        ORDER BY prescriptionDate DESC, id DESC
        """
    )
    fun observeAll(): Flow<List<PrescriptionWithMedicines>>

    @Query("SELECT * FROM prescriptions WHERE id = :id")
    suspend fun getById(id: Long): PrescriptionEntity?

    @Query("SELECT COUNT(*) FROM prescriptions")
    suspend fun countAll(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(prescription: PrescriptionEntity): Long

    @Update
    suspend fun update(prescription: PrescriptionEntity)

    @Delete
    suspend fun delete(prescription: PrescriptionEntity)

    @Transaction
    @Query("SELECT * FROM prescriptions WHERE id = :id")
    fun observeById(id: Long): Flow<PrescriptionWithMedicines?>
}
