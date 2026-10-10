package com.imperionite.mdf.data.repository

data class PurchaseItemDraft(
    val prescriptionMedicineId: Long,
    val purchasedQuantity: Double,
    val unitPriceCentavos: Long
)