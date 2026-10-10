package com.imperionite.mdf.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "purchase_batches",
    foreignKeys = [
        ForeignKey(
            entity = PrescriptionEntity::class,
            parentColumns = ["id"],
            childColumns = ["prescriptionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("prescriptionId")]
)
data class PurchaseBatchEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val prescriptionId: Long,
    // ISO date, for example 2026-10-10.
    val purchaseDate: String,
    val notes: String = ""
)