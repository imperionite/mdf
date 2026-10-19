package com.imperionite.mdf.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Records which bundled evaluation scenarios have been inserted. */
@Entity(tableName = "demo_seed_metadata")
data class DemoSeedMetadata(
    @PrimaryKey val seedKey: String,
    val seededAtEpochMillis: Long
)
