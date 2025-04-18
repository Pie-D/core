package co.nilin.opex.api.ports.proxy.impl

import co.nilin.opex.api.core.inout.*
import co.nilin.opex.api.core.spi.MarketDataProxy
import co.nilin.opex.api.ports.proxy.config.ProxyDispatchers
import co.nilin.opex.common.utils.Interval
import co.nilin.opex.common.utils.LoggerDelegate
import kotlinx.coroutines.reactive.awaitFirstOrElse
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.util.*

@Component
class MarketDataProxyImpl(private val webClient: WebClient) : MarketDataProxy {

    private val logger by LoggerDelegate()

    @Value("\${app.market.url}")
    private lateinit var baseUrl: String

    @Value("\${fake.data}")
    private val fakeData: Boolean? = null

    override suspend fun getTradeTickerData(interval: Interval): List<PriceChange> {
        fakeData?.let {
            if (fakeData) {
                return withContext(ProxyDispatchers.market) {
                    WebClient.create("https://api.binance.com").get().uri("/api/v3/ticker/24hr") {
                        it.queryParam(
                            "symbols",
                            "[\"BTCUSDT\",\"ETHUSDT\",\"TONUSDT\",\"SOLUSDT\",\"DOGEUSDT\"]"
                        ).build()
                    }
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .onStatus({ it.isError }) { response ->
                            response.bodyToMono(String::class.java).flatMap { body ->
                                println("❌ Error ${response.statusCode()} - Body: $body")
                                Mono.error(RuntimeException("Error calling Binance API: $body"))
                            }
                        }
                        .bodyToFlux<PriceChange>()
                        .collectList()
                        .awaitFirstOrElse { emptyList() }
                        .also {
                            val knownQuotes = listOf("USDT", "BUSD", "BTC", "ETH", "BNB", "DOGE", "SOL", "TON")
                            it.forEach { pc ->
                                val quote = knownQuotes.find { q -> pc.symbol.endsWith(q) }
                                val base = quote?.let { q -> pc.symbol.removeSuffix(q) }
                                pc.base = base
                                pc.quote = quote
                            }
                        }
                }
            }
        }
        return withContext(ProxyDispatchers.market) {
            webClient.get()
                .uri("$baseUrl/v1/market/ticker") {
                    it.queryParam("interval", interval)
                    it.build()
                }.accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .onStatus({ t -> t.isError }, { it.createException() })
                .bodyToFlux<PriceChange>()
                .collectList()
                .awaitFirstOrElse { emptyList() }
        }
    }

    override suspend fun getTradeTickerDataBySymbol(symbol: String, interval: Interval): PriceChange {
        return withContext(ProxyDispatchers.market) {
            webClient.get()
                .uri("$baseUrl/v1/market/$symbol/ticker") {
                    it.queryParam("interval", interval)
                    it.build()
                }.accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .onStatus({ t -> t.isError }, { it.createException() })
                .bodyToMono<PriceChange>()
                .awaitSingleOrNull()
                ?: PriceChange(symbol, openTime = Date().time, closeTime = interval.getTime())
        }
    }

    override suspend fun openBidOrders(symbol: String, limit: Int): List<OrderBook> {
        return withContext(ProxyDispatchers.market) {
            webClient.get()
                .uri("$baseUrl/v1/market/$symbol/order-book") {
                    it.queryParam("limit", limit)
                    it.queryParam("direction", OrderDirection.BID)
                    it.build()
                }.accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .onStatus({ t -> t.isError }, { it.createException() })
                .bodyToFlux<OrderBook>()
                .collectList()
                .awaitFirstOrElse { emptyList() }
        }
    }

    override suspend fun openAskOrders(symbol: String, limit: Int): List<OrderBook> {
        return withContext(ProxyDispatchers.market) {
            webClient.get()
                .uri("$baseUrl/v1/market/$symbol/order-book") {
                    it.queryParam("limit", limit)
                    it.queryParam("direction", OrderDirection.ASK)
                    it.build()
                }.accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .onStatus({ t -> t.isError }, { it.createException() })
                .bodyToFlux<OrderBook>()
                .collectList()
                .awaitFirstOrElse { emptyList() }
        }
    }

    override suspend fun lastOrder(symbol: String): Order? {
        return withContext(ProxyDispatchers.market) {
            webClient.get()
                .uri("$baseUrl/v1/market/$symbol/last-order")
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .onStatus({ t -> t.isError }, { it.createException() })
                .bodyToMono<Order>()
                .awaitSingleOrNull()
        }
    }

    override suspend fun recentTrades(symbol: String, limit: Int): List<MarketTrade> {
        return withContext(ProxyDispatchers.market) {
            webClient.get()
                .uri("$baseUrl/v1/market/$symbol/recent-trades") {
                    it.queryParam("limit", limit)
                    it.build()
                }.accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .onStatus({ t -> t.isError }, { it.createException() })
                .bodyToFlux<MarketTrade>()
                .collectList()
                .awaitFirstOrElse { emptyList() }
        }
    }

    override suspend fun lastPrice(symbol: String?): List<PriceTicker> {
        return withContext(ProxyDispatchers.market) {
            webClient.get()
                .uri("$baseUrl/v1/market/prices") {
                    it.queryParam("symbol", symbol)
                    it.build()
                }.accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .onStatus({ t -> t.isError }, { it.createException() })
                .bodyToFlux<PriceTicker>()
                .collectList()
                .awaitFirstOrElse { emptyList() }
        }
    }

    override suspend fun getBestPriceForSymbols(symbols: List<String>): List<BestPrice> {
        return withContext(ProxyDispatchers.market) {
            webClient.get()
                .uri("$baseUrl/v1/market/best-prices") {
                    it.queryParam("symbols", symbols)
                    it.build()
                }.accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .onStatus({ t -> t.isError }, { it.createException() })
                .bodyToFlux<BestPrice>()
                .collectList()
                .awaitFirstOrElse { emptyList() }
        }
    }

    override suspend fun getCandleInfo(
        symbol: String,
        interval: String,
        startTime: Long?,
        endTime: Long?,
        limit: Int
    ): List<CandleData> {
        return withContext(ProxyDispatchers.market) {
            webClient.get()
                .uri("$baseUrl/v1/chart/$symbol/candle") {
                    it.queryParam("interval", interval)
                    it.queryParam("since", startTime)
                    it.queryParam("until", endTime)
                    it.queryParam("limit", limit)
                    it.build()
                }.accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .onStatus({ t -> t.isError }, { it.createException() })
                .bodyToFlux<CandleData>()
                .collectList()
                .awaitFirstOrElse { emptyList() }
        }
    }

    override suspend fun getMarketCurrencyRates(quote: String, base: String?): List<CurrencyRate> {
        return withContext(ProxyDispatchers.market) {
            webClient.get()
                .uri(if (base.isNullOrEmpty()) "$baseUrl/v1/rate/EXTERNAL" else "$baseUrl/v1/rate/$base/EXTERNAL") {
                    it.queryParam("quote", quote)
                    it.build()
                }.accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .onStatus({ t -> t.isError }, { it.createException() })
                .bodyToFlux<CurrencyRate>()
                .collectList()
                .awaitFirstOrElse { emptyList() }
        }
    }

    override suspend fun getExternalCurrencyRates(quote: String, base: String?): List<CurrencyRate> {
        return withContext(ProxyDispatchers.market) {
            webClient.get()
                .uri(if (base.isNullOrEmpty()) "$baseUrl/v1/rate/EXTERNAL" else "$baseUrl/v1/rate/$base/EXTERNAL") {
                    it.queryParam("quote", quote)
                    it.build()
                }.accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .onStatus({ t -> t.isError }, { it.createException() })
                .bodyToFlux<CurrencyRate>()
                .collectList()
                .awaitFirstOrElse { emptyList() }
        }
    }

    override suspend fun countActiveUsers(interval: Interval): Long {
        return withContext(ProxyDispatchers.market) {
            webClient.get()
                .uri("$baseUrl/v1/market/active-users") {
                    it.queryParam("interval", interval)
                    it.build()
                }.accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .onStatus({ t -> t.isError }, { it.createException() })
                .bodyToMono<CountResponse>()
                .awaitSingleOrNull()
                ?.value ?: 0
        }
    }

    override suspend fun countTotalOrders(interval: Interval): Long {
        return withContext(ProxyDispatchers.market) {
            webClient.get()
                .uri("$baseUrl/v1/market/orders-count") {
                    it.queryParam("interval", interval)
                    it.build()
                }.accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .onStatus({ t -> t.isError }, { it.createException() })
                .bodyToMono<CountResponse>()
                .awaitSingleOrNull()
                ?.value ?: 0
        }
    }

    override suspend fun countTotalTrades(interval: Interval): Long {
        return withContext(ProxyDispatchers.market) {
            webClient.get()
                .uri("$baseUrl/v1/market/trades-count") {
                    it.queryParam("interval", interval)
                    it.build()
                }.accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .onStatus({ t -> t.isError }, { it.createException() })
                .bodyToMono<CountResponse>()
                .awaitSingleOrNull()
                ?.value ?: 0
        }
    }
}