package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "institutional_flows")
data class InstitutionalFlow(
    @PrimaryKey val date: String, // Format: YYYY-MM-DD
    val fiiCashNet: Double, // in Crores
    val diiCashNet: Double, // in Crores
    val fiiIndexFutureNet: Double, // in Crores
    val fiiIndexOptionNet: Double, // in Crores
    val fiiStockFutureNet: Double, // in Crores
    val fiiStockOptionNet: Double, // in Crores
    val fiiIndexCallContracts: Long,
    val fiiIndexPutContracts: Long,
    val fiiIndexOptionPcr: Float,
    val fiiIndexFutureLongShortRatio: Float,
    val marketSentiment: String // "BULLISH", "BEARISH", "NEUTRAL", "STRONG_BULLISH"
)
