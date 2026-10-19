package com.imperionite.mdf

import org.junit.Assert.assertEquals
import org.junit.Test

class AppRoutesTest {
    @Test
    fun routeBuildersIncludeExplicitIds() {
        assertEquals("create-prescription", AppRoutes.CREATE_PRESCRIPTION)
        assertEquals("prescriptions/12", AppRoutes.details(12L))
        assertEquals("prescriptions/12/edit", AppRoutes.editPrescription(12L))
        assertEquals("prescriptions/12/insights", AppRoutes.insights(12L))
        assertEquals("prescriptions/12/purchases/new", AppRoutes.newPurchase(12L))
        assertEquals("prescriptions/12/purchases/34/edit", AppRoutes.editPurchase(12L, 34L))
    }
}
