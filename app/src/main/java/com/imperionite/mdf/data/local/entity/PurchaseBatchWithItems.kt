package com.imperionite.mdf.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class PurchaseBatchWithItems(
    @Embedded
    val batch: PurchaseBatchEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "purchaseBatchId"
    )
    val items: List<PurchaseBatchItemEntity>
)