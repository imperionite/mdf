package com.imperionite.mdf.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.imperionite.mdf.data.local.entity.PurchaseBatchItemEntity
import com.imperionite.mdf.data.local.entity.PurchasedQuantitySummary
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseBatchItemDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(
        items: List<PurchaseBatchItemEntity>
    )

    @Query(
        "DELETE FROM purchase_batch_items WHERE purchaseBatchId = :batchId"
    )
    suspend fun deleteByBatchId(batchId: Long)

    @Query(
        """
        SELECT
            pm.id AS medicineId,
            COALESCE(SUM(pbi.purchasedQuantity), 0.0)
                AS purchasedQuantity
        FROM prescription_medicines AS pm
        LEFT JOIN purchase_batch_items AS pbi
            ON pbi.prescriptionMedicineId = pm.id
        WHERE pm.prescriptionId = :prescriptionId
        GROUP BY pm.id
        """
    )
    fun observePurchasedQuantities(
        prescriptionId: Long
    ): Flow<List<PurchasedQuantitySummary>>

    @Query(
        """
        SELECT COALESCE(SUM(purchasedQuantity), 0.0)
        FROM purchase_batch_items
        WHERE prescriptionMedicineId = :medicineId
        """
    )
    suspend fun getPurchasedQuantity(medicineId: Long): Double
}
