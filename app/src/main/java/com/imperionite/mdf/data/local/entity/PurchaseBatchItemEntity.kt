package com.imperionite.mdf.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "purchase_batch_items",
    foreignKeys = [
        ForeignKey(
            entity = PurchaseBatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["purchaseBatchId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PrescriptionMedicineEntity::class,
            parentColumns = ["id"],
            childColumns = ["prescriptionMedicineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("purchaseBatchId"),
        Index("prescriptionMedicineId")
    ]
)
data class PurchaseBatchItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val purchaseBatchId: Long,
    val prescriptionMedicineId: Long,
    val purchasedQuantity: Double,
    // Snapshot of the unit price in centavos at purchase time.
    val unitPriceCentavos: Long
)