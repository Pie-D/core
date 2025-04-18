package co.nilin.opex.api.ports.proxy.impl

import co.nilin.opex.api.core.inout.PriceChange
import co.nilin.opex.api.core.inout.PriceStat
import co.nilin.opex.api.core.inout.TradeVolumeStat
import co.nilin.opex.api.core.spi.MarketStatProxy
import co.nilin.opex.api.ports.proxy.config.ProxyDispatchers
import co.nilin.opex.common.utils.Interval
import kotlinx.coroutines.reactive.awaitFirstOrElse
import kotlinx.coroutines.withContext
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import reactor.core.publisher.Mono

@Component
@ConditionalOnProperty(name = ["fake.data"], havingValue = "true")
class BinanceMarketStatProxyImpl(
) : MarketStatProxy {
    private val baseUrl: String = "https://api.binance.com"
    private val webClient = WebClient.builder().baseUrl(baseUrl).build()
    override suspend fun getMostIncreasedInPricePairs(interval: Interval, limit: Int): List<PriceStat> {
        return fetchTicker24h()
            .sortedByDescending { it.priceChangePercent }
            .take(limit)
            .map {
                PriceStat(
                    symbol = it.symbol,
                    lastPrice = it.lastPrice,
                    priceChangePercent = it.priceChangePercent.toDouble()
                )
            }
    }

    override suspend fun getMostDecreasedInPricePairs(interval: Interval, limit: Int): List<PriceStat> {
        return fetchTicker24h()
            .sortedBy { it.priceChangePercent }
            .take(limit)
            .map {
                PriceStat(
                    symbol = it.symbol,
                    lastPrice = it.lastPrice,
                    priceChangePercent = it.priceChangePercent.toDouble()
                )
            }
    }

    override suspend fun getHighestVolumePair(interval: Interval): TradeVolumeStat? {
        return fetchTicker24h()
            .maxByOrNull { it.volume }
            ?.let {
                TradeVolumeStat(
                    symbol = it.symbol,
                    volume = it.volume,
                    tradeCount = it.count.toBigDecimal(),
                    change = it.priceChangePercent.toDouble()
                )
            }
    }

    override suspend fun getTradeCountPair(interval: Interval): TradeVolumeStat? {
        return fetchTicker24h()
            .maxByOrNull { it.count }
            ?.let {
                TradeVolumeStat(
                    symbol = it.symbol,
                    volume = it.volume,
                    tradeCount = it.count.toBigDecimal(),
                    change = it.priceChangePercent.toDouble()
                )
            }
    }


    private suspend fun fetchTicker24h(): List<PriceChange> =
        withContext(ProxyDispatchers.market) {
            webClient.get()
                .uri("$baseUrl/api/v3/ticker/24hr") {
                    it.queryParam("symbols", "[\"BTCUSDT\",\"ETHUSDT\",\"TONUSDT\",\"SOLUSDT\",\"DOGEUSDT\"]").build()
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
        }
}