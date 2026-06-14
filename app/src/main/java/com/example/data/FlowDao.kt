package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FlowDao {
    @Query("SELECT * FROM institutional_flows ORDER BY date DESC")
    fun getAllFlows(): Flow<List<InstitutionalFlow>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlow(flow: InstitutionalFlow)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlows(flows: List<InstitutionalFlow>)

    @Query("SELECT COUNT(*) FROM institutional_flows")
    suspend fun getCount(): Int

    @Delete
    suspend fun deleteFlow(flow: InstitutionalFlow)

    @Query("DELETE FROM institutional_flows")
    suspend fun deleteAll()
}
