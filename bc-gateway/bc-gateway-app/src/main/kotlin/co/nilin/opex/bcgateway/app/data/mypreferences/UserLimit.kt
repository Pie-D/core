package co.nilin.opex.bcgateway.app.data.mypreferences

data class UserLimit(
    val level: String?,
    val owner: Long,
    val action: String,
    val walletType: String,
    val withdrawFee: java.math.BigDecimal,
    val dailyTotal: java.math.BigDecimal,
    val dailyCount: Int,
    val monthlyTotal: java.math.BigDecimal,
    val monthlyCount: Int,
)
