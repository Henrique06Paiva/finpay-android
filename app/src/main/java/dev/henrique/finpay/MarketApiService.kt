package dev.henrique.finpay

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MarketRates(
    val selic: Double = 13.75,
    val cdi: Double = 13.65,
    val ipca: Double = 4.58,
    val usdBid: Double = 5.04,
    val usdPctChange: Double = 0.38,
    val lastUpdated: String = "Hoje, 11:30"
)

/**
 * Serviço de integração com APIs públicas financeiras no Android.
 * Executa as chamadas em background thread via Dispatchers.IO para evitar NetworkOnMainThreadException.
 */
object MarketApiService {

    private const val BRASIL_API_RATES_URL = "https://brasilapi.com.br/api/taxas/v1"
    private const val AWESOME_API_USD_URL = "https://economia.awesomeapi.com.br/last/USD-BRL"
    private const val TIMEOUT_MS = 6000

    suspend fun fetchLiveMarketRates(currentFallback: MarketRates): Result<MarketRates> {
        return withContext(Dispatchers.IO) {
            try {
                var selicVal = currentFallback.selic
                var cdiVal = currentFallback.cdi
                var ipcaVal = currentFallback.ipca

                // 1. Consulta BrasilAPI para Selic, CDI e IPCA
                val brasilApiRaw = httpGet(BRASIL_API_RATES_URL)
                if (brasilApiRaw != null) {
                    val arr = JSONArray(brasilApiRaw)
                    for (i in 0 until arr.length()) {
                        val item = arr.getJSONObject(i)
                        val name = item.optString("nome", "").uppercase(Locale.ROOT)
                        val valor = item.optDouble("valor", 0.0)
                        if (name == "SELIC" && valor > 0.0) selicVal = valor
                        if (name == "CDI" && valor > 0.0) cdiVal = valor
                        if (name == "IPCA" && valor > 0.0) ipcaVal = valor
                    }
                }

                // 2. Consulta AwesomeAPI para cotação do Dólar Comercial
                var usdBidVal = currentFallback.usdBid
                var usdPctVal = currentFallback.usdPctChange
                val awesomeApiRaw = httpGet(AWESOME_API_USD_URL)
                if (awesomeApiRaw != null) {
                    val root = JSONObject(awesomeApiRaw)
                    val usdObj = root.optJSONObject("USDBRL")
                    if (usdObj != null) {
                        usdBidVal = usdObj.optDouble("bid", usdBidVal)
                        usdPctVal = usdObj.optDouble("pctChange", usdPctVal)
                    }
                }

                val nowStr = SimpleDateFormat("dd/MM 'às' HH:mm", Locale.forLanguageTag("pt-BR")).format(Date())

                val updatedRates = MarketRates(
                    selic = selicVal,
                    cdi = cdiVal,
                    ipca = ipcaVal,
                    usdBid = usdBidVal,
                    usdPctChange = usdPctVal,
                    lastUpdated = "Atualizado em $nowStr"
                )

                Result.success(updatedRates)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun httpGet(urlString: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("User-Agent", "FinPay-Android/1.0")
                setRequestProperty("Accept", "application/json")
            }

            if (conn.responseCode in 200..299) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val sb = java.lang.StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                reader.close()
                sb.toString()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }
}
