package co.nilin.opex.bcgateway.app.data.mypreferences

data class CurrencyImplementation(
    val symbol: String?,
    val chain: String,
    val withdrawEnabled: Boolean,
    val token: Boolean,
    val tokenAddress: String?,
    val tokenName: String?,
    val withdrawFee: java.math.BigDecimal,
    val withdrawMin: java.math.BigDecimal,
    val decimal: Int
)
