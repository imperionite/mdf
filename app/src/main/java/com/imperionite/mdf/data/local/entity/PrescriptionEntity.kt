package com.imperionite.mdf.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prescriptions")
data class PrescriptionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val physicianName: String,

    // Store dates in ISO format, for example 2026-10-09.
    val prescriptionDate: String,

    val notes: String = "",

    // Optional URI from Android's document picker.
    val attachmentUri: String? = null
)