package com.imperionite.mdf.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class PrescriptionWithMedicines(
    @Embedded
    val prescription: PrescriptionEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "prescriptionId"
    )
    val medicines: List<PrescriptionMedicineEntity>
)
