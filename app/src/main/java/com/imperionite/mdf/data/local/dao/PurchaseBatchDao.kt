package com.imperionite.mdf.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.imperionite.mdf.data.local.entity.PurchaseBatchEntity
import com.imperionite.mdf.data.local.entity.PurchaseBatchWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseBatchDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(batch: PurchaseBatchEntity): Long

    @Transaction
    @Query(
        """
        SELECT * FROM purchase_batches
        WHERE prescriptionId = :prescriptionId
        ORDER BY purchaseDate DESC, id DESC
        """
    )
    fun observeByPrescription(
        prescriptionId: Long
    ): Flow<List<PurchaseBatchWithItems>>

    @Transaction
    @Query("SELECT * FROM purchase_batches WHERE id = :batchId")
    suspend fun getWithItems(batchId: Long): PurchaseBatchWithItems?
}