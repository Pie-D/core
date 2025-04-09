package co.nilin.opex.accountant.app.data.mypreferences

data class Market(
    val leftSide: String,
    val rightSide: String,
    val pair: String?,
    val feeConfigs: List<FeeConfig>,
    val aliases: List<Alias>,
    val leftSideFraction: java.math.BigDecimal?,
    val rightSideFraction: java.math.BigDecimal?,
)
