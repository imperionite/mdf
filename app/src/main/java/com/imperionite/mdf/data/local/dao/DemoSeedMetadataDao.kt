package com.imperionite.mdf.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.imperionite.mdf.data.local.entity.DemoSeedMetadata

@Dao
interface DemoSeedMetadataDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(metadata: DemoSeedMetadata)

    @Query("SELECT EXISTS(SELECT 1 FROM demo_seed_metadata WHERE seedKey = :seedKey)")
    suspend fun isSeeded(seedKey: String): Boolean

    @Query("SELECT COUNT(*) FROM demo_seed_metadata")
    suspend fun count(): Int
}
