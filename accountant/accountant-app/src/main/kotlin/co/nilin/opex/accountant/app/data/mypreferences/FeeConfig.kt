package co.nilin.opex.accountant.app.data.mypreferences

data class FeeConfig(
    val userLevel: String,
    val direction: String,
    val makerFee: java.math.BigDecimal,
    val takerFee: java.math.BigDecimal
)
