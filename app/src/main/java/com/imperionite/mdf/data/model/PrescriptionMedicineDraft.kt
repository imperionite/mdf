
package com.imperionite.mdf.data.model

data class PrescriptionMedicineDraft(
    val medicineName: String,
    val prescribedQuantity: Double,
    val unit: String,
    val dosage: String,
    val frequency: String,
    val unitPriceCentavos: Long
)
