package com.example.data

import kotlinx.coroutines.flow.Flow

class FlowRepository(private val flowDao: FlowDao) {
    val allFlows: Flow<List<InstitutionalFlow>> = flowDao.getAllFlows()

    suspend fun insert(flow: InstitutionalFlow) {
        flowDao.insertFlow(flow)
    }

    suspend fun insertAll(flows: List<InstitutionalFlow>) {
        flowDao.insertFlows(flows)
    }

    suspend fun getCount(): Int {
        return flowDao.getCount()
    }

    suspend fun deleteAll() {
        flowDao.deleteAll()
    }
}
