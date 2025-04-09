package co.nilin.opex.bcgateway.app.data.mypreferences

data class Currency(
    val symbol: String,
    val name: String,
    val precision: java.math.BigDecimal,
    val mainBalance: java.math.BigDecimal,
    val dailyTotal: java.math.BigDecimal,
    val dailyCount: Int,
    val monthlyTotal: java.math.BigDecimal,
    val monthlyCount: java.math.BigDecimal,
    val implementations: List<CurrencyImplementation>,
    val gift: java.math.BigDecimal,
)
