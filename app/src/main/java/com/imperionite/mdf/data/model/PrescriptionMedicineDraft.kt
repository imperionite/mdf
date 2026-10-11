package com.imperionite.mdf.data.model

data class PrescriptionMedicineDraft(
    val id: Long? = null,
    val medicineName: String,
    val prescribedQuantity: Double,
    val unit: String,
    val dosage: String,
    val dosageRegimen: String,
    val unitPriceCentavos: Long
)
