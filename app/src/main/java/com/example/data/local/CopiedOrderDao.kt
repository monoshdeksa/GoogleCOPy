package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CopiedOrderDao {
    @Query("SELECT * FROM copied_orders ORDER BY copiedAtMillis DESC LIMIT 50")
    fun observeRecentOrders(): Flow<List<CopiedOrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(order: CopiedOrderEntity)

    @Query("DELETE FROM copied_orders WHERE orderNumber = :orderNumber")
    suspend fun deleteByOrderNumber(orderNumber: String)

    @Query("DELETE FROM copied_orders WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM copied_orders")
    suspend fun clearAll()
}
