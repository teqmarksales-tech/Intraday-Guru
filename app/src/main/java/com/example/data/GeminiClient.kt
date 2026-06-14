package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateMarketCommentary(
        pcr: Float,
        longShortRatio: Float,
        fiiNetCalls: Long,
        fiiNetPuts: Long,
        netCashFlow: Double,
        indexSentiment: String,
        recentHistory: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "🔑 **Gemini API Key Missing**: Please set your `GEMINI_API_KEY` in the AI Studio Secrets panel.\n\n" +
                    "**Fallback AI Strategist Report:**\n" +
                    "Based on an Index Option PCR of *${"%.2f".format(pcr)}* and an Institutional Long/Short ratio of *${"%.2f".format(longShortRatio)}*:\n" +
                    "- **Derivative Trend:** ${if (pcr > 1.2) "PCR indicates overbought/strongly bullish territory. Heavy call accumulation." else if (pcr < 0.7) "PCR suggests oversold/highly bearish sentiment. Put options are extremely active." else "Standard market balancing with balanced call-put flow."}\n" +
                    "- **Institutional Bias:** FII call-to-put positioning is highly biased towards ${if (fiiNetCalls > fiiNetPuts) "CALL BUYING/PUT WRITING" else "PUT BUYING/CALL WRITING"}. FII are positioning for a ${if (netCashFlow > 0) "bullish continuation" else "reversion/hedging breakout"}.\n" +
                    "- **Option Action Strategy:** Recommended to implement a ${if (pcr > 1.0) "Bull Call Spread" else "Bear Put Spread"} to cap risks while joining the major institutional flow."
        }

        val prompt = """
            You are a senior NSE (National Stock Exchange of India) derivatives strategist specializing in FII (Foreign Institutional Investors) and DII (Domestic Institutional Investors) data analysis.
            Provide a professional, clear, and highly actionable 2-3 paragraph option trading recommendation.
            
            Current Option Data:
            - Index Option PCR (Put-Call Ratio): $pcr
            - FII Index Futures Long-Short Ratio: $longShortRatio
            - FII Net Calls Active (Long): $fiiNetCalls
            - FII Net Puts Active (Long): $fiiNetPuts
            - Institutional Net Cash Buy/Sell (FII+DII): $netCashFlow Crores
            - Current Estimated Sentiment: $indexSentiment
            
            Historical 5-Day Trend Summary:
            $recentHistory
            
            Structure your advice as:
            1. **Derivative Landscape**: Interpret current PCR and Long-Short ratios in simple, clear trading terms.
            2. **FII Option Bias & Action**: Is FII writing calls/buying puts or buying calls/writing puts? Detail the sentiment.
            3. **Recommended Option Strategy**: Suggest specific strategies (e.g., Bull Call Spread, Bear Put Spread, Iron Condor, Straddle/Strangle hedging) including Strike selection tips relative to Spot, Stop loss logic, and safety precautions. Keep it direct and professional. Avoid lengthy disclaimers or legal headers, just focus on technical execution.
        """.trimIndent()

        val escapedPrompt = escapeString(prompt)
        val requestJson = """
            {
              "contents": [
                {
                  "parts": [
                    {
                      "text": "$escapedPrompt"
                    }
                  ]
                }
              ],
              "generationConfig": {
                "temperature": 0.4
              }
            }
        """.trimIndent()

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = requestJson.toRequestBody(mediaType)
        val request = Request.Builder()
            .url("$BASE_URL?key=$apiKey")
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val code = response.code
                    return@withContext "Market analysis unavailable (API Error $code). Ensure your API Key is valid."
                }
                val respBody = response.body?.string() ?: return@withContext "Empty response from AI engine."
                extractTextFromResponse(respBody)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Gemini Call", e)
            "Network connection failed: ${e.message ?: "Unable to contact market strategist AI."}"
        }
    }

    private fun escapeString(text: String): String {
        return text.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun extractTextFromResponse(responseBody: String): String {
        val key = "\"text\":"
        var index = responseBody.indexOf(key)
        if (index == -1) return "Analysis unavailable. Raw response pattern unrecognized."
        index = responseBody.indexOf("\"", index + key.length)
        if (index == -1) return "Analysis parsing failed."
        val startPos = index + 1
        var endPos = startPos
        while (endPos < responseBody.length) {
            if (responseBody[endPos] == '"' && responseBody[endPos - 1] != '\\') {
                break
            }
            endPos++
        }
        if (endPos >= responseBody.length) return "Analysis parsing cutoff reached."
        val rawText = responseBody.substring(startPos, endPos)
        return rawText
            .replace("\\n", "\n")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
    }
}
