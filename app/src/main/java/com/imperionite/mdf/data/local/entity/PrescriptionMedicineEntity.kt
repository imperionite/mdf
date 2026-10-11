package com.imperionite.mdf.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "prescription_medicines",
    foreignKeys = [
        ForeignKey(
            entity = PrescriptionEntity::class,
            parentColumns = ["id"],
            childColumns = ["prescriptionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["prescriptionId"])]
)
data class PrescriptionMedicineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val prescriptionId: Long,

    val medicineName: String,

    val prescribedQuantity: Double,

    val unit: String,

    val dosage: String,

    val dosageRegimen: String,

    // Store currency in integer centavos.
    val unitPriceCentavos: Long
)
