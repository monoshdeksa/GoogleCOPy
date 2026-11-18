package com.example.data

import com.example.data.local.CopiedOrderDao
import com.example.data.local.CopiedOrderEntity
import kotlinx.coroutines.flow.Flow

class OrderHistoryRepository(private val dao: CopiedOrderDao) {

    fun observeRecentOrders(): Flow<List<CopiedOrderEntity>> = dao.observeRecentOrders()

    suspend fun recordCopy(orderNumber: String, copiedText: String, matchedLabel: String?) {
        val cleaned = orderNumber.trim()
        if (cleaned.isEmpty()) return
        dao.deleteByOrderNumber(cleaned)
        dao.insert(
            CopiedOrderEntity(
                orderNumber = cleaned,
                copiedText = copiedText,
                matchedLabel = matchedLabel,
                copiedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteItem(id: Long) {
        dao.deleteById(id)
    }

    suspend fun clearAll() {
        dao.clearAll()
    }
}
