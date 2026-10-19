package com.imperionite.mdf

/** Central route definitions for the app's Navigation Compose back stack. */
object AppRoutes {
    const val PRESCRIPTIONS = "prescriptions"
    const val CREATE_PRESCRIPTION = "create-prescription"
    const val EDIT_PRESCRIPTION = "prescriptions/{prescriptionId}/edit"
    const val PRESCRIPTION_DETAILS = "prescriptions/{prescriptionId}"
    const val PRESCRIPTION_INSIGHTS = "prescriptions/{prescriptionId}/insights"
    const val NEW_PURCHASE = "prescriptions/{prescriptionId}/purchases/new"
    const val EDIT_PURCHASE = "prescriptions/{prescriptionId}/purchases/{batchId}/edit"

    const val ARG_PRESCRIPTION_ID = "prescriptionId"
    const val ARG_BATCH_ID = "batchId"

    fun details(prescriptionId: Long) = "prescriptions/$prescriptionId"
    fun editPrescription(prescriptionId: Long) = "prescriptions/$prescriptionId/edit"
    fun insights(prescriptionId: Long) = "prescriptions/$prescriptionId/insights"
    fun newPurchase(prescriptionId: Long) = "prescriptions/$prescriptionId/purchases/new"
    fun editPurchase(prescriptionId: Long, batchId: Long) =
        "prescriptions/$prescriptionId/purchases/$batchId/edit"
}
