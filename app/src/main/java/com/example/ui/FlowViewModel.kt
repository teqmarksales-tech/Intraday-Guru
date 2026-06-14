package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

// Represents a Live Order Flow entry
data class LiveOrderFlow(
    val timestamp: String,
    val segment: String, // "NIFTY", "BANKNIFTY", "FINNIFTY"
    val strike: Int,
    val type: String, // "CE" or "PE"
    val institution: String, // "FII" or "DII"
    val action: String, // "BUY", "SELL", "SHORT COVERING", "UNWINDING"
    val qty: Int, // in Lots
    val premium: Double, // in Rupees
    val sentiment: String // "BULLISH", "BEARISH", "HEDGING", "NEUTRAL"
)

// Represents a Strike in our Option Chain
data class OptionChainStrike(
    val strikePrice: Int,
    val callPrice: Double,
    val callOiChange: Float,
    val callLabel: String,
    val hasFiiCallActivity: Boolean,
    val putPrice: Double,
    val putOiChange: Float,
    val putLabel: String,
    val hasFiiPutActivity: Boolean
)

data class UiState(
    val historicalFlows: List<InstitutionalFlow> = emptyList(),
    val isDbGenerating: Boolean = false,
    val selectedFlow: InstitutionalFlow? = null,
    val showAddDialog: Boolean = false,
    
    // Live Monitoring Stream States
    val isLiveMonitoring: Boolean = true,
    val niftySpot: Double = 23450.0,
    val bankNiftySpot: Double = 50120.0,
    val livePcr: Float = 1.05f,
    val liveSentiment: String = "MILDLY BULLISH",
    val liveOrders: List<LiveOrderFlow> = emptyList(),
    val optionChain: List<OptionChainStrike> = emptyList(),
    val liveCallPremiumVol: Double = 4120.5, // in Crores
    val livePutPremiumVol: Double = 3940.8,  // in Crores
    
    // AI Strategist States
    val aiResponse: String = "",
    val isAnalyzing: Boolean = false,
    val activeTab: Int = 0 // 0: Live Scanner, 1: Historical Flows, 2: AI Advisor
)

class FlowViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = FlowRepository(database.flowDao())

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var liveMonitorJob: Job? = null
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    init {
        viewModelScope.launch {
            // Load historical flows
            repository.allFlows.collect { flows ->
                _uiState.update { it.copy(historicalFlows = flows) }
                if (flows.isEmpty()) {
                    prepopulateDatabase()
                } else if (_uiState.value.selectedFlow == null && flows.isNotEmpty()) {
                    _uiState.update { it.copy(selectedFlow = flows.first()) }
                }
            }
        }
        startLiveMonitoring()
    }

    private suspend fun prepopulateDatabase() {
        _uiState.update { it.copy(isDbGenerating = true) }
        val sampleFlows = listOf(
            InstitutionalFlow(
                date = "2026-06-12",
                fiiCashNet = 1450.20,
                diiCashNet = 820.50,
                fiiIndexFutureNet = 340.10,
                fiiIndexOptionNet = 3120.40, // Huge call buying
                fiiStockFutureNet = -150.40,
                fiiStockOptionNet = 45.20,
                fiiIndexCallContracts = 425200,
                fiiIndexPutContracts = 311000,
                fiiIndexOptionPcr = 1.37f,
                fiiIndexFutureLongShortRatio = 1.15f,
                marketSentiment = "STRONG_BULLISH"
            ),
            InstitutionalFlow(
                date = "2026-06-11",
                fiiCashNet = 920.40,
                diiCashNet = 1100.80,
                fiiIndexFutureNet = -120.30,
                fiiIndexOptionNet = 1250.60,
                fiiStockFutureNet = 420.10,
                fiiStockOptionNet = -12.40,
                fiiIndexCallContracts = 398000,
                fiiIndexPutContracts = 334000,
                fiiIndexOptionPcr = 1.19f,
                fiiIndexFutureLongShortRatio = 1.08f,
                marketSentiment = "BULLISH"
            ),
            InstitutionalFlow(
                date = "2026-06-10",
                fiiCashNet = -840.50,
                diiCashNet = 1250.30,
                fiiIndexFutureNet = -450.80,
                fiiIndexOptionNet = -890.40, // FII Put Buying
                fiiStockFutureNet = -610.20,
                fiiStockOptionNet = -230.10,
                fiiIndexCallContracts = 320000,
                fiiIndexPutContracts = 412000,
                fiiIndexOptionPcr = 0.77f,
                fiiIndexFutureLongShortRatio = 0.85f,
                marketSentiment = "BEARISH"
            ),
            InstitutionalFlow(
                date = "2026-06-09",
                fiiCashNet = 110.30,
                diiCashNet = 450.20,
                fiiIndexFutureNet = 80.50,
                fiiIndexOptionNet = 740.20,
                fiiStockFutureNet = 120.60,
                fiiStockOptionNet = 80.40,
                fiiIndexCallContracts = 350000,
                fiiIndexPutContracts = 340000,
                fiiIndexOptionPcr = 1.03f,
                fiiIndexFutureLongShortRatio = 0.98f,
                marketSentiment = "NEUTRAL"
            ),
            InstitutionalFlow(
                date = "2026-06-08",
                fiiCashNet = -1250.40,
                diiCashNet = 1840.50,
                fiiIndexFutureNet = -890.30,
                fiiIndexOptionNet = -2150.80, // Heavy capitulation
                fiiStockFutureNet = -1100.20,
                fiiStockOptionNet = -410.50,
                fiiIndexCallContracts = 280000,
                fiiIndexPutContracts = 485000,
                fiiIndexOptionPcr = 0.58f,
                fiiIndexFutureLongShortRatio = 0.65f,
                marketSentiment = "STRONG_BEARISH"
            ),
            InstitutionalFlow(
                date = "2026-06-05",
                fiiCashNet = -410.20,
                diiCashNet = 950.40,
                fiiIndexFutureNet = 150.40,
                fiiIndexOptionNet = -150.20,
                fiiStockFutureNet = 340.20,
                fiiStockOptionNet = 10.40,
                fiiIndexCallContracts = 330000,
                fiiIndexPutContracts = 360000,
                fiiIndexOptionPcr = 0.91f,
                fiiIndexFutureLongShortRatio = 0.88f,
                marketSentiment = "NEUTRAL"
            ),
            InstitutionalFlow(
                date = "2026-06-04",
                fiiCashNet = 680.90,
                diiCashNet = 230.10,
                fiiIndexFutureNet = 410.10,
                fiiIndexOptionNet = 1100.40,
                fiiStockFutureNet = 780.10,
                fiiStockOptionNet = 350.20,
                fiiIndexCallContracts = 380000,
                fiiIndexPutContracts = 310000,
                fiiIndexOptionPcr = 1.22f,
                fiiIndexFutureLongShortRatio = 1.12f,
                marketSentiment = "BULLISH"
            ),
            InstitutionalFlow(
                date = "2026-06-03",
                fiiCashNet = 1520.10,
                diiCashNet = -210.40,
                fiiIndexFutureNet = 890.40,
                fiiIndexOptionNet = 3540.20, // Breakout day
                fiiStockFutureNet = 1240.20,
                fiiStockOptionNet = 180.12,
                fiiIndexCallContracts = 445000,
                fiiIndexPutContracts = 290000,
                fiiIndexOptionPcr = 1.53f,
                fiiIndexFutureLongShortRatio = 1.35f,
                marketSentiment = "STRONG_BULLISH"
            ),
            InstitutionalFlow(
                date = "2026-06-02",
                fiiCashNet = 240.50,
                diiCashNet = 810.20,
                fiiIndexFutureNet = -50.20,
                fiiIndexOptionNet = 510.60,
                fiiStockFutureNet = -110.10,
                fiiStockOptionNet = -30.40,
                fiiIndexCallContracts = 340000,
                fiiIndexPutContracts = 320000,
                fiiIndexOptionPcr = 1.06f,
                fiiIndexFutureLongShortRatio = 0.99f,
                marketSentiment = "NEUTRAL"
            ),
            InstitutionalFlow(
                date = "2026-06-01",
                fiiCashNet = -110.40,
                diiCashNet = 510.80,
                fiiIndexFutureNet = 120.30,
                fiiIndexOptionNet = -110.80,
                fiiStockFutureNet = 230.40,
                fiiStockOptionNet = -5.10,
                fiiIndexCallContracts = 335000,
                fiiIndexPutContracts = 355000,
                fiiIndexOptionPcr = 0.94f,
                fiiIndexFutureLongShortRatio = 0.92f,
                marketSentiment = "NEUTRAL"
            )
        )
        repository.insertAll(sampleFlows)
        _uiState.update { it.copy(isDbGenerating = false, selectedFlow = sampleFlows.first()) }
    }

    fun startLiveMonitoring() {
        _uiState.update { it.copy(isLiveMonitoring = true) }
        liveMonitorJob?.cancel()
        
        // Initialize base live order list and option chain strikes
        val initialOrders = mutableListOf<LiveOrderFlow>()
        val initialChain = generateBaseOptionChain(23450)
        
        _uiState.update { 
            it.copy(
                optionChain = initialChain,
                liveOrders = initialOrders
            )
        }

        liveMonitorJob = viewModelScope.launch {
            while (true) {
                delay(3000) // update every 3 seconds
                if (!_uiState.value.isLiveMonitoring) continue
                
                // 1. Gently fluctuate Nifty and BankNifty Spot
                val niftyMove = Random.nextDouble(-8.0, 9.0)
                val bankNiftyMove = Random.nextDouble(-20.0, 25.0)
                val newNifty = _uiState.value.niftySpot + niftyMove
                val newBankNifty = _uiState.value.bankNiftySpot + bankNiftyMove

                // 2. Generate a new Live Order Flow
                val newOrder = createRandomOrderFlow(newNifty)
                val updatedOrders = listOf(newOrder) + _uiState.value.liveOrders
                val cappedOrders = updatedOrders.take(40) // Keep the last 40 entries

                // 3. Update Volume values based on order
                var callVolAdd = 0.0
                var putVolAdd = 0.0
                if (newOrder.type == "CE") {
                    callVolAdd = (newOrder.qty * newOrder.premium * 50) / 10_000_000.0 // Nifty lot size is 50, convert to Crores roughly
                } else {
                    putVolAdd = (newOrder.qty * newOrder.premium * 50) / 10_000_000.0
                }

                val newCallVol = _uiState.value.liveCallPremiumVol + callVolAdd
                val newPutVol = _uiState.value.livePutPremiumVol + putVolAdd
                val calcPcr = (newPutVol / newCallVol).toFloat()

                // Determine instant sentiment
                val instantSentiment = when {
                    calcPcr > 1.25f -> "STRONGLY BULLISH FLOW"
                    calcPcr > 1.05 -> "BULLISH SUPPORTED"
                    calcPcr < 0.75f -> "HEAVY BEARISH BIAS"
                    calcPcr < 0.95f -> "BEARISH PRESSURE"
                    else -> "BOUNDED - BALANCED FLOW"
                }

                // 4. Update Option Chain premiums
                val rawSpotStrike = (newNifty / 50).toInt() * 50
                val updatedChain = _uiState.value.optionChain.map { strike ->
                    val distance = Math.abs(strike.strikePrice - newNifty)
                    val baseCall = Math.max(1.0, 320.0 - (strike.strikePrice - newNifty) * 1.1)
                    val basePut = Math.max(1.0, 320.0 - (newNifty - strike.strikePrice) * 1.1)
                    
                    // Add variance
                    val callPriceDev = baseCall + Random.nextDouble(-1.5, 1.5)
                    val putPriceDev = basePut + Random.nextDouble(-1.5, 1.5)
                    
                    val callChange = strike.callOiChange + Random.nextFloat() * 1.8f - 0.8f
                    val putChange = strike.putOiChange + Random.nextFloat() * 1.8f - 0.8f

                    strike.copy(
                        callPrice = Math.round(callPriceDev * 100.0) / 100.0,
                        putPrice = Math.round(putPriceDev * 100.0) / 100.0,
                        callOiChange = callChange,
                        putOiChange = putChange
                    )
                }

                _uiState.update {
                    it.copy(
                        niftySpot = newNifty,
                        bankNiftySpot = newBankNifty,
                        liveOrders = cappedOrders,
                        liveCallPremiumVol = newCallVol,
                        livePutPremiumVol = newPutVol,
                        livePcr = calcPcr,
                        liveSentiment = instantSentiment,
                        optionChain = updatedChain
                    )
                }
            }
        }
    }

    fun stopLiveMonitoring() {
        _uiState.update { it.copy(isLiveMonitoring = false) }
        liveMonitorJob?.cancel()
    }

    fun selectFlow(flow: InstitutionalFlow) {
        _uiState.update { it.copy(selectedFlow = flow) }
    }

    fun selectTab(tab: Int) {
        _uiState.update { it.copy(activeTab = tab) }
        if (tab == 2 && _uiState.value.aiResponse.isEmpty()) {
            analyzeWithAI()
        }
    }

    fun showAddDialog(show: Boolean) {
        _uiState.update { it.copy(showAddDialog = show) }
    }

    fun insertCustomFlow(
        date: String,
        fiiCash: Double,
        diiCash: Double,
        fiiFuture: Double,
        fiiOption: Double,
        callContracts: Long,
        putContracts: Long
    ) {
        viewModelScope.launch {
            val pcr = if (callContracts > 0) putContracts.toFloat() / callContracts.toFloat() else 1.0f
            val ratio = if (putContracts > 0) callContracts.toFloat() / putContracts.toFloat() else 1.0f // futures proxy
            val sentiment = when {
                fiiCash > 500.0 && fiiOption > 1000.0 -> "STRONG_BULLISH"
                fiiCash > 0 && fiiOption > 0 -> "BULLISH"
                fiiCash < -500.0 && fiiOption < -1000.0 -> "STRONG_BEARISH"
                fiiCash < 0 && fiiOption < 0 -> "BEARISH"
                else -> "NEUTRAL"
            }

            val flow = InstitutionalFlow(
                date = date,
                fiiCashNet = fiiCash,
                diiCashNet = diiCash,
                fiiIndexFutureNet = fiiFuture,
                fiiIndexOptionNet = fiiOption,
                fiiStockFutureNet = fiiFuture * 0.4,
                fiiStockOptionNet = fiiOption * 0.05,
                fiiIndexCallContracts = callContracts,
                fiiIndexPutContracts = putContracts,
                fiiIndexOptionPcr = pcr,
                fiiIndexFutureLongShortRatio = ratio,
                marketSentiment = sentiment
            )
            repository.insert(flow)
            _uiState.update { it.copy(showAddDialog = false, selectedFlow = flow) }
        }
    }

    fun analyzeWithAI() {
        val current = _uiState.value
        val statePcr = current.livePcr
        val stateSentiment = current.liveSentiment
        val orders = current.liveOrders.take(3)
        val historySnippet = current.historicalFlows.take(5).joinToString("\n") {
            "- Date: ${it.date} | FII Cash: ${it.fiiCashNet}Cr | FII Options: ${it.fiiIndexOptionNet}Cr | PCR: ${it.fiiIndexOptionPcr}"
        }

        _uiState.update { it.copy(isAnalyzing = true, aiResponse = "Consulting AI trading strategist...") }

        viewModelScope.launch {
            val response = GeminiClient.generateMarketCommentary(
                pcr = statePcr,
                longShortRatio = if (current.historicalFlows.isNotEmpty()) current.historicalFlows.first().fiiIndexFutureLongShortRatio else 1.0f,
                fiiNetCalls = if (current.historicalFlows.isNotEmpty()) current.historicalFlows.first().fiiIndexCallContracts else 350000L,
                fiiNetPuts = if (current.historicalFlows.isNotEmpty()) current.historicalFlows.first().fiiIndexPutContracts else 320000L,
                netCashFlow = if (current.historicalFlows.isNotEmpty()) (current.historicalFlows.first().fiiCashNet + current.historicalFlows.first().diiCashNet) else 2270.0,
                indexSentiment = stateSentiment,
                recentHistory = historySnippet
            )
            _uiState.update { it.copy(aiResponse = response, isAnalyzing = false) }
        }
    }

    private fun generateBaseOptionChain(spot: Int): List<OptionChainStrike> {
        val list = mutableListOf<OptionChainStrike>()
        val strikeCenter = (spot / 50) * 50
        for (i in -4..4) {
            val strike = strikeCenter + (i * 50)
            val isItmCall = strike <= spot
            val isItmPut = strike >= spot
            
            val callPrice = Math.max(1.5, 230.0 - (strike - spot) * 1.1)
            val putPrice = Math.max(1.5, 230.0 - (spot - strike) * 1.1)

            val callLabel = when {
                i == -2 -> "FII LONG BUYING"
                i == 0 -> "HEAVY FII CALL WRITING"
                i == 3 -> "RETAIL BUYERS"
                else -> "NORMAL VOLUME"
            }
            val putLabel = when {
                i == -3 -> "RETAIL SPAN"
                i == -1 -> "DII PUT COVERAGE"
                i == 2 -> "FII PUT HEDGING"
                else -> "NORMAL VOLUME"
            }

            list.add(
                OptionChainStrike(
                    strikePrice = strike,
                    callPrice = Math.round(callPrice * 100.0) / 100.0,
                    callOiChange = Random.nextFloat() * 8.0f + 2.0f,
                    callLabel = callLabel,
                    hasFiiCallActivity = (i == -2 || i == 0),
                    putPrice = Math.round(putPrice * 100.0) / 100.0,
                    putOiChange = Random.nextFloat() * 8.0f + 2.0f,
                    putLabel = putLabel,
                    hasFiiPutActivity = (i == -1 || i == 2)
                )
            )
        }
        return list
    }

    private fun createRandomOrderFlow(currentNifty: Double): LiveOrderFlow {
        val segment = listOf("NIFTY", "BANKNIFTY").random()
        val baseSpot = if (segment == "NIFTY") currentNifty else _uiState.value.bankNiftySpot
        val strikeInterval = if (segment == "NIFTY") 50 else 100
        val centerStrike = (baseSpot / strikeInterval).toInt() * strikeInterval
        
        val offset = listOf(-2, -1, 0, 1, 2).random()
        val strike = centerStrike + (offset * strikeInterval)
        val type = listOf("CE", "PE").random()
        val institution = listOf("FII", "FII", "DII", "FII").random() // lean heavy FII
        
        val action = if (type == "CE") {
            listOf("BUY", "SELL", "BUY", "SHORT COVERING").random()
        } else {
            listOf("BUY", "SELL", "BUY", "UNWINDING").random()
        }

        val qty = Random.nextInt(25, 280) * 50 // Lots of 50
        val premium = if (type == "CE") {
            Math.max(15.0, 240.0 - (strike - baseSpot) * 0.8)
        } else {
            Math.max(15.0, 240.0 - (baseSpot - strike) * 0.8)
        }

        val sentiment = when {
            type == "CE" && (action == "BUY" || action == "SHORT COVERING") -> "BULLISH"
            type == "CE" && action == "SELL" -> "BEARISH (WRITING)"
            type == "PE" && (action == "BUY" || action == "SHORT COVERING") -> "BEARISH (HEDGE)"
            type == "PE" && action == "SELL" -> "BULLISH (WRITING)"
            else -> "NEUTRAL"
        }

        val timestamp = timeFormat.format(Date())

        return LiveOrderFlow(
            timestamp = timestamp,
            segment = segment,
            strike = strike,
            type = type,
            institution = institution,
            action = action,
            qty = qty,
            premium = Math.round(premium * 10.0) / 10.0,
            sentiment = sentiment
        )
    }
}
