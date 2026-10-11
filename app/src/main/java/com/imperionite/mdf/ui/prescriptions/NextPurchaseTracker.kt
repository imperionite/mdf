package com.imperionite.mdf.ui.prescriptions

import com.imperionite.mdf.data.local.entity.PrescriptionMedicineEntity
import com.imperionite.mdf.data.local.entity.PurchaseBatchWithItems
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

data class NextPurchaseTrackingResult(
    val dailyConsumption: Double? = null,
    val estimatedCurrentStock: Double? = null,
    val estimatedRunOutDate: LocalDate? = null,
    val suggestedPurchaseDate: LocalDate? = null,
    val status: String,
    val detail: String
)

private data class DatedPurchase(
    val date: LocalDate,
    val batchId: Long,
    val quantity: Double
)

/**
 * Estimates medicine coverage from the three-part posology notation:
 * breakfast-lunch-dinner, for example 1-1-1 or 1-0-0.
 *
 * This is a tracking estimate, not medical advice or a guarantee
 * of actual stock on hand.
 */
fun calculateNextPurchaseTracking(
    medicine: PrescriptionMedicineEntity,
    batches: List<PurchaseBatchWithItems>,
    today: LocalDate = LocalDate.now(),
    advancePurchaseDays: Long = 3L
): NextPurchaseTrackingResult {

    val dailyConsumption = parseDailyConsumption(medicine.dosageRegimen)
        ?: return NextPurchaseTrackingResult(
            status = "Manual Review Required",
            detail = "The dosage regimen '${medicine.dosageRegimen}' " +
                    "is not a supported three-part numeric schedule. " +
                    "Expected format: 1-1-1 or 1-0-0."
        )

    val medicinePurchases = batches.mapNotNull { batch ->
        val item = batch.items.firstOrNull {
            it.prescriptionMedicineId == medicine.id
        } ?: return@mapNotNull null

        val purchaseDate = runCatching {
            LocalDate.parse(batch.batch.purchaseDate)
        }.getOrNull() ?: return NextPurchaseTrackingResult(
            dailyConsumption = dailyConsumption,
            status = "Manual Review Required",
            detail = "Batch #${batch.batch.id} has an invalid purchase date."
        )

        if (!item.purchasedQuantity.isFinite() ||
            item.purchasedQuantity <= 0.0
        ) {
            return NextPurchaseTrackingResult(
                dailyConsumption = dailyConsumption,
                status = "Manual Review Required",
                detail = "Batch #${batch.batch.id} has an invalid " +
                        "purchased quantity for this medicine."
            )
        }

        DatedPurchase(
            date = purchaseDate,
            batchId = batch.batch.id,
            quantity = item.purchasedQuantity
        )
    }.filter { it.date <= today }
        .sortedWith(
            compareBy<DatedPurchase> { it.date }
                .thenBy { it.batchId }
        )

    if (medicinePurchases.isEmpty()) {
        return NextPurchaseTrackingResult(
            dailyConsumption = dailyConsumption,
            status = "No Purchase Data",
            detail = "Record a purchase batch for this medicine " +
                    "to estimate its supply and next purchase date."
        )
    }

    var estimatedStock = 0.0
    var supplyGapDetected = false
    var cursorDate = medicinePurchases.first().date

    for ((date, _, quantity) in medicinePurchases) {
        val elapsedDays = ChronoUnit.DAYS.between(
            cursorDate,
            date
        )

        val expectedConsumption = elapsedDays * dailyConsumption

        if (estimatedStock + 0.000001 < expectedConsumption) {
            supplyGapDetected = true
        }

        estimatedStock = (
                estimatedStock - expectedConsumption
                ).coerceAtLeast(0.0)

        estimatedStock += quantity
        cursorDate = date
    }

    val daysSinceLastPurchase = ChronoUnit.DAYS.between(
        cursorDate,
        today
    )

    val expectedConsumptionSinceLastPurchase =
        daysSinceLastPurchase * dailyConsumption

    if (
        estimatedStock + 0.000001 <
        expectedConsumptionSinceLastPurchase
    ) {
        supplyGapDetected = true
    }

    estimatedStock = (
            estimatedStock - expectedConsumptionSinceLastPurchase
            ).coerceAtLeast(0.0)

    val runOutDate = if (estimatedStock <= 0.000001) {
        today
    } else {
        today.plusDays(
            ceil(estimatedStock / dailyConsumption).toLong()
        )
    }

    val suggestedDate = runOutDate.minusDays(
        advancePurchaseDays.coerceAtLeast(0L)
    )

    val status = when {
        supplyGapDetected -> "Review Supply History"
        estimatedStock <= 0.000001 -> "Possible Run-Out"
        estimatedStock <= dailyConsumption * advancePurchaseDays ->
            "Purchase Soon"

        else -> "Supply Estimate Available"
    }

    val detail = buildString {
        append(
            "Estimate assumes the recorded regimen is followed " +
                    "consistently and every recorded purchase is available."
        )

        if (supplyGapDetected) {
            append(
                " The recorded timeline suggests a possible gap in " +
                        "supply, so the current stock estimate may be inaccurate."
            )
        }

        append(
            " Verify the actual quantity on hand before relying on these dates."
        )
    }

    return NextPurchaseTrackingResult(
        dailyConsumption = dailyConsumption,
        estimatedCurrentStock = estimatedStock,
        estimatedRunOutDate = runOutDate,
        suggestedPurchaseDate = suggestedDate,
        status = status,
        detail = detail
    )
}

/**
 * Parses breakfast-lunch-dinner unit counts, e.g. 1-1-1.
 * Rejects PRN, tapering schedules, and other unsupported notation.
 */
private fun parseDailyConsumption(regimen: String): Double? {
    val parts = regimen.trim().split(Regex("\\s*-\\s*"))

    if (parts.size != 3) return null

    val values = parts.map { part ->
        if (!part.matches(Regex("""\d+(?:\.\d+)?"""))) {
            return null
        }

        part.toDoubleOrNull()?.takeIf {
            it.isFinite() && it >= 0.0
        } ?: return null
    }

    val total = values.sum()

    return total.takeIf {
        it.isFinite() && it > 0.0
    }
}
