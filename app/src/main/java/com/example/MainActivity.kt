package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.InstitutionalFlow
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("main_scaffold"),
                    containerColor = Color(0xFF0F1319) // Elegant deep dark background as core styling vibe
                ) { innerPadding ->
                    FlowDashboardScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun FlowDashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: FlowViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0F14))
    ) {
        // --- 1. Top Live Trading Bar Header ---
        HeaderBar(
            niftySpot = state.niftySpot,
            bankNiftySpot = state.bankNiftySpot,
            isMonitoring = state.isLiveMonitoring,
            onToggleMonitor = {
                if (state.isLiveMonitoring) {
                    viewModel.stopLiveMonitoring()
                } else {
                    viewModel.startLiveMonitoring()
                }
            }
        )

        // --- 2. Live Market Pulse Stats Block ---
        MarketPulseSummary(
            livePcr = state.livePcr,
            liveSentiment = state.liveSentiment,
            callVol = state.liveCallPremiumVol,
            putVol = state.livePutPremiumVol
        )

        // --- 3. Material 3 Main Navigation Tabs ---
        TabRow(
            selectedTabIndex = state.activeTab,
            containerColor = Color(0xFF131822),
            contentColor = Color.White
        ) {
            Tab(
                selected = state.activeTab == 0,
                onClick = { viewModel.selectTab(0) },
                icon = { Icon(Icons.Filled.DynamicFeed, contentDescription = "Terminal") },
                text = { Text("Live Terminal", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
            Tab(
                selected = state.activeTab == 1,
                onClick = { viewModel.selectTab(1) },
                icon = { Icon(Icons.Filled.History, contentDescription = "History") },
                text = { Text("Historical Flow", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
            Tab(
                selected = state.activeTab == 2,
                onClick = { viewModel.selectTab(2) },
                icon = { Icon(Icons.Filled.Psychology, contentDescription = "AI Strategist") },
                text = { Text("AI Strategist", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
        }

        // --- 4. Tab Content Area with AnimatedTransitions ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (state.activeTab) {
                0 -> LiveTerminalView(
                    optionChain = state.optionChain,
                    liveOrders = state.liveOrders,
                    niftyPrice = state.niftySpot
                )
                1 -> HistoricalView(
                    flows = state.historicalFlows,
                    selectedFlow = state.selectedFlow,
                    isGenerating = state.isDbGenerating,
                    onSelectFlow = { viewModel.selectFlow(it) },
                    onAddClick = { viewModel.showAddDialog(true) }
                )
                2 -> AiStrategistView(
                    aiResponse = state.aiResponse,
                    isAnalyzing = state.isAnalyzing,
                    onAnalyzeLive = { viewModel.analyzeWithAI() }
                )
            }
        }
    }

    // --- 5. Add Custom End of Day Flow Record Dialog ---
    if (state.showAddDialog) {
        AddFlowDialog(
            onDismiss = { viewModel.showAddDialog(false) },
            onSave = { date, fiiCash, diiCash, fiiFut, fiiOpt, callC, putC ->
                viewModel.insertCustomFlow(date, fiiCash, diiCash, fiiFut, fiiOpt, callC, putC)
            }
        )
    }
}

@Composable
fun HeaderBar(
    niftySpot: Double,
    bankNiftySpot: Double,
    isMonitoring: Boolean,
    onToggleMonitor: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131822)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // App Identity & Ticker Status
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Pulse Animation to represent "Live" action
                val transition = rememberInfiniteTransition(label = "pulse_state")
                val opacity by transition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "opacity"
                )

                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            if (isMonitoring) Color(0xFF00E676).copy(alpha = opacity)
                            else Color(0xFFFF5252)
                        )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = "NSE FII / DII LIVE",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (isMonitoring) "Live Option Tracking Active" else "Stream Paused",
                        color = if (isMonitoring) Color(0xFF00E676) else Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Real-Time Spot Indexes
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IndexTag(name = "NIFTY", value = niftySpot, deviation = 1.15)
                IndexTag(name = "BANKNIFTY", value = bankNiftySpot, deviation = -2.40)

                // Play / Pause stream action button
                IconButton(
                    onClick = onToggleMonitor,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF202938))
                ) {
                    Icon(
                        imageVector = if (isMonitoring) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Toggle Stream",
                        tint = if (isMonitoring) Color(0xFFFFCC00) else Color(0xFF00E676),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun IndexTag(name: String, value: Double, deviation: Double) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF1E2633))
            .border(0.5.dp, Color(0xFF2C394E), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Text(name, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(
                "%,.2f".format(value),
                color = Color.White,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MarketPulseSummary(
    livePcr: Float,
    liveSentiment: String,
    callVol: Double,
    putVol: Double
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Sentiment indicator
        Card(
            modifier = Modifier.weight(1.2f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131822)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val icon = when {
                    "STRONG_BULLISH" in liveSentiment || "STRONGLY BULLISH" in liveSentiment -> Icons.Filled.TrendingUp
                    "BEARISH" in liveSentiment -> Icons.Filled.TrendingDown
                    else -> Icons.Filled.TrendingFlat
                }
                val tint = when {
                    "BULLISH" in liveSentiment -> Color(0xFF00E676)
                    "BEARISH" in liveSentiment -> Color(0xFFFF5252)
                    else -> Color(0xFFFFCC00)
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(tint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = "Sentiment Indicator", tint = tint, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("INTRADAY PULSE", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(
                        liveSentiment,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Live PCR
        Card(
            modifier = Modifier.weight(0.8f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131822)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "FII OPTION PCR",
                    color = Color.Gray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "%.2f".format(livePcr),
                    color = if (livePcr >= 1.0f) Color(0xFF00E676) else Color(0xFFFF5252),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// ================= TAB 0: LIVE OPTION RUNNING TERMINAL =================

@Composable
fun LiveTerminalView(
    optionChain: List<OptionChainStrike>,
    liveOrders: List<LiveOrderFlow>,
    niftyPrice: Double
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // Core view divider: Option Chain at top, Live flowing prints at bottom
        Text(
            text = "LIVE NIFTY OPTION CHAIN SCANNER",
            color = Color.Gray,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp, top = 4.dp)
        )

        // Option Chain Headers
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF192231)),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("CALL PREMIUM", color = Color(0xFF00E676), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Left)
                Text("OI CHG%", color = Color(0xFF00E676), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
                Text("STRIKE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
                Text("OI CHG%", color = Color(0xFFFF5252), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
                Text("PUT PREMIUM", color = Color(0xFFFF5252), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Right)
            }
        }

        // Option Chain strikes rows
        Box(modifier = Modifier.weight(1.1f)) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(optionChain) { strike ->
                    val isAtm = Math.abs(strike.strikePrice - niftyPrice) < 25.0
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isAtm) Color(0xFF232D3F) else Color(0xFF111622))
                            .border(
                                1.dp,
                                if (isAtm) Color(0xFF00E676).copy(alpha = 0.4f) else Color.Transparent,
                                RoundedCornerShape(6.dp)
                            )
                            .padding(vertical = 8.dp, horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Call Premium Details
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                            Text(
                                "₹${"%.1f".format(strike.callPrice)}",
                                color = if (strike.hasFiiCallActivity) Color(0xFF00E676) else Color.White,
                                fontWeight = if (strike.hasFiiCallActivity) FontWeight.Black else FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            if (strike.hasFiiCallActivity) {
                                Text(
                                    strike.callLabel,
                                    color = Color(0xFF00E676),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        // Call OI change %
                        Text(
                            "+${"%.1f".format(strike.callOiChange)}%",
                            color = Color(0xFF81C784),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(0.7f),
                            textAlign = TextAlign.Center
                        )

                        // Strike Center Text
                        Text(
                            "${strike.strikePrice}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(0.8f)
                        )

                        // Put OI change %
                        Text(
                            "+${"%.1f".format(strike.putOiChange)}%",
                            color = Color(0xFFE57373),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(0.7f),
                            textAlign = TextAlign.Center
                        )

                        // Put Premium Details
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text(
                                "₹${"%.1f".format(strike.putPrice)}",
                                color = if (strike.hasFiiPutActivity) Color(0xFFFF5252) else Color.White,
                                fontWeight = if (strike.hasFiiPutActivity) FontWeight.Black else FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            if (strike.hasFiiPutActivity) {
                                Text(
                                    strike.putLabel,
                                    color = Color(0xFFFF5252),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Live flowing Prints Ticker at bottom
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "INSTITUTIONAL DETECTED BLOCK FLOWS (LIVE)",
                color = Color.LightGray,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
            Text(
                "Ticker Active",
                color = Color(0xFF00E676),
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                modifier = Modifier.padding(end = 4.dp)
            )
        }

        Box(
            modifier = Modifier
                .weight(0.9f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF090C11))
                .border(0.5.dp, Color(0xFF1B2331), RoundedCornerShape(8.dp))
        ) {
            if (liveOrders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Scanning order book logs for block trades...", color = Color.Gray, fontSize = 11.sp)
                }
            } else {
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(liveOrders) { order ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF131822))
                                .padding(vertical = 5.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${order.timestamp} ",
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (order.institution == "FII") Color(0xFF2D8CFF).copy(0.15f) else Color(0xFF9C27B0).copy(0.15f))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        order.institution,
                                        color = if (order.institution == "FII") Color(0xFF2D8CFF) else Color(0xFFD500F9),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "${order.segment} ${order.strike} ${order.type}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${order.action} Qty: ${order.qty}",
                                    color = when {
                                        "BULLISH" in order.sentiment -> Color(0xFF00E676)
                                        "BEARISH" in order.sentiment -> Color(0xFFFF5252)
                                        else -> Color.LightGray
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "₹${order.premium}",
                                    color = Color(0xFFFFCC00),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ================= TAB 1: HISTORICAL DATA PANEL =================

@Composable
fun HistoricalView(
    flows: List<InstitutionalFlow>,
    selectedFlow: InstitutionalFlow?,
    isGenerating: Boolean,
    onSelectFlow: (InstitutionalFlow) -> Unit,
    onAddClick: () -> Unit
) {
    if (isGenerating || flows.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF00E676))
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Visualize flows on custom canvas
            Text(
                "FII INDEX OPTIONS INFLOW (10-DAY TREND)",
                color = Color.Gray,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )

            // Dynamic Chart Box
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131822)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(bottom = 10.dp)
            ) {
                FlowBarChart(flows = flows.take(10).reversed())
            }

            // Quick Actions Block
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "INSTITUTIONAL RECORD LOGS",
                    color = Color.LightGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )

                Button(
                    onClick = onAddClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E8DFF)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp).testTag("add_custom_record_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add EOD Flow", tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Manual entry", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Highlight of Selected Flow details
            selectedFlow?.let { flow ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2633)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Breakdown for Date: ${flow.date}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        when {
                                            "BULLISH" in flow.marketSentiment -> Color(0xFF00E676).copy(0.15f)
                                            "BEARISH" in flow.marketSentiment -> Color(0xFFFF5252).copy(0.15f)
                                            else -> Color.Gray.copy(0.15f)
                                        }
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    flow.marketSentiment,
                                    color = when {
                                        "BULLISH" in flow.marketSentiment -> Color(0xFF00E676)
                                        "BEARISH" in flow.marketSentiment -> Color(0xFFFF5252)
                                        else -> Color.LightGray
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Grid values
                        Row(modifier = Modifier.fillMaxWidth()) {
                            FlowMetricColumn(title = "FII Cash Market", value = flow.fiiCashNet, modifier = Modifier.weight(1f))
                            FlowMetricColumn(title = "DII Cash Market", value = flow.diiCashNet, modifier = Modifier.weight(1f))
                            FlowMetricColumn(title = "FII Derivatives", value = flow.fiiIndexOptionNet, modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            FlowMetricColumn(title = "Call Contracts", value = flow.fiiIndexCallContracts.toDouble(), isCrores = false, modifier = Modifier.weight(1f))
                            FlowMetricColumn(title = "Put Contracts", value = flow.fiiIndexPutContracts.toDouble(), isCrores = false, modifier = Modifier.weight(1f))
                            FlowMetricColumn(title = "Option PCR", value = flow.fiiIndexOptionPcr.toDouble(), isCrores = false, isRaw = true, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Historical List Rows
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 80.dp) // padding for FAB/Scroll clearance
            ) {
                flows.forEach { item ->
                    val isSelected = item.date == selectedFlow?.date
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFF242E3D) else Color(0xFF131822))
                            .border(
                                1.dp,
                                if (isSelected) Color(0xFF00E676).copy(0.5f) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelectFlow(item) }
                            .padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                item.date,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("FII Cash: ", color = Color.Gray, fontSize = 10.sp)
                                Text(
                                    "${if (item.fiiCashNet >= 0) "+" else ""}${"%,.1f".format(item.fiiCashNet)} Cr",
                                    color = if (item.fiiCashNet >= 0) Color(0xFF00E676) else Color(0xFFFF5252),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "${if (item.fiiIndexOptionNet >= 0) "+" else ""}${"%,.1f".format(item.fiiIndexOptionNet)} Cr",
                                color = if (item.fiiIndexOptionNet >= 0) Color(0xFF00E676) else Color(0xFFFF5252),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                "PCR: ${"%.2f".format(item.fiiIndexOptionPcr)}",
                                color = Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FlowMetricColumn(title: String, value: Double, isCrores: Boolean = true, isRaw: Boolean = false, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        Text(title, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
        Text(
            text = when {
                isRaw -> "%.2f".format(value)
                isCrores -> "${if (value >= 0) "+" else ""}${"%,.1f".format(value)} Cr"
                else -> "${if (value >= 0) "+" else ""}${"%,.0f".format(value)}"
            },
            color = if (isRaw) Color.White else if (value >= 0) Color(0xFF00E676) else Color(0xFFFF5252),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun FlowBarChart(flows: List<InstitutionalFlow>) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        val count = flows.size
        if (count == 0) return@Canvas

        val maxVal = flows.maxOf { Math.abs(it.fiiIndexOptionNet) }.coerceAtLeast(100.0)
        val w = size.width
        val h = size.height
        val halfH = h / 2f

        // Draw horizontal baseline
        drawLine(
            color = Color(0xFF2C353F),
            start = Offset(0f, halfH),
            end = Offset(w, halfH),
            strokeWidth = 1.dp.toPx()
        )

        // Draw dotted max lines
        drawLine(
            color = Color(0xFF1F2633),
            start = Offset(0f, 10.dp.toPx()),
            end = Offset(w, 10.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = Color(0xFF1F2633),
            start = Offset(0f, h - 10.dp.toPx()),
            end = Offset(w, h - 10.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )

        val barSpacing = w / count
        val barWidth = barSpacing * 0.6f

        flows.forEachIndexed { idx, flow ->
            val net = flow.fiiIndexOptionNet
            val ratio = (net / maxVal).toFloat()
            val barH = halfH * ratio

            val startX = (idx * barSpacing) + (barSpacing - barWidth) / 2f
            val startY = if (ratio >= 0) halfH - barH else halfH
            val rectH = Math.abs(barH).coerceAtLeast(4f) // make sure it's visible

            // Brush for stylish fading bars
            val brush = if (net >= 0) {
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF00E676), Color(0xFF1B5E20)),
                    startY = startY,
                    endY = startY + rectH
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFB71C1C), Color(0xFFFF5252)),
                    startY = startY,
                    endY = startY + rectH
                )
            }

            drawRect(
                brush = brush,
                topLeft = Offset(startX, startY),
                size = Size(barWidth, rectH)
            )

            // Very subtle dates marker at bottom
            val dateLabel = flow.date.takeLast(2)
            // simple visual separation
        }
    }
}

// ================= TAB 2: AI STRATEGIST ADVICE =================

@Composable
fun AiStrategistView(
    aiResponse: String,
    isAnalyzing: Boolean,
    onAnalyzeLive: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131822)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E676).copy(0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Psychology,
                            contentDescription = "AI Expert",
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            "AI DERIVATIVES STRATEGIST",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                        Text(
                            "Real-time institutional flow analyst",
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "This engine examines active FII option accumulation, put-call ratios, cash volumes, and intraday blocks to define structured derivative strategies (such as call ratio spread, bull put hedges) with strike target parameters.",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onAnalyzeLive,
                    enabled = !isAnalyzing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00E676),
                        disabledContainerColor = Color(0xFF2C394E)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("ai_reposition_button")
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reading derivative contracts...", color = Color.White, fontSize = 12.sp)
                    } else {
                        Icon(Icons.Filled.QueryStats, contentDescription = "Run AI", tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Request Live AI Repositioning Plans", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Response box
        Text(
            "STRATAGEM ASSESSMENT",
            color = Color.Gray,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F2B)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 80.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                if (aiResponse.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 30.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Analytics,
                            contentDescription = "Analysis Idle",
                            tint = Color.Gray,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Ready to calculate derivative positioning.",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    SelectionContainer {
                        Text(
                            text = aiResponse,
                            color = Color.White,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            }
        }
    }
}

// ================= DIALOG: ADD CUSTOM FLOW =================

@Composable
fun AddFlowDialog(
    onDismiss: () -> Unit,
    onSave: (String, Double, Double, Double, Double, Long, Long) -> Unit
) {
    var date by remember { mutableStateOf("") }
    var fiiCash by remember { mutableStateOf("") }
    var diiCash by remember { mutableStateOf("") }
    var fiiFutures by remember { mutableStateOf("") }
    var fiiOptions by remember { mutableStateOf("") }
    var callContracts by remember { mutableStateOf("") }
    var putContracts by remember { mutableStateOf("") }

    val context = LocalContext.current

    // Automatically set default date to today to save time
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        date = sdf.format(Date())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Manual EOD Flow Entry",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp
            )
        },
        containerColor = Color(0xFF131822),
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00E676),
                        unfocusedBorderColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_flow_date_input")
                )

                OutlinedTextField(
                    value = fiiCash,
                    onValueChange = { fiiCash = it },
                    label = { Text("FII Cash Net (Crores)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00E676)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_flow_fii_cash_input")
                )

                OutlinedTextField(
                    value = diiCash,
                    onValueChange = { diiCash = it },
                    label = { Text("DII Cash Net (Crores)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00E676)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = fiiFutures,
                    onValueChange = { fiiFutures = it },
                    label = { Text("FII Index Futures Net (Crores)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00E676)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = fiiOptions,
                    onValueChange = { fiiOptions = it },
                    label = { Text("FII Index Options Net (Crores)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00E676)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = callContracts,
                    onValueChange = { callContracts = it },
                    label = { Text("FII Call Long Contracts") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00E676)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = putContracts,
                    onValueChange = { putContracts = it },
                    label = { Text("FII Put Long Contracts") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00E676)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val fc = fiiCash.toDoubleOrNull() ?: 0.0
                    val dc = diiCash.toDoubleOrNull() ?: 0.0
                    val ff = fiiFutures.toDoubleOrNull() ?: 0.0
                    val fo = fiiOptions.toDoubleOrNull() ?: 0.0
                    val cc = callContracts.toLongOrNull() ?: 350000L
                    val pc = putContracts.toLongOrNull() ?: 310000L

                    if (date.isEmpty()) return@Button
                    onSave(date, fc, dc, ff, fo, cc, pc)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                modifier = Modifier.testTag("save_flow_entry_button")
            ) {
                Text("Insert Logs", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.LightGray)
            }
        }
    )
}
