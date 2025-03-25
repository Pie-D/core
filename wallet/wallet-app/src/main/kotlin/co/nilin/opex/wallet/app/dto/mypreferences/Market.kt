package co.nilin.opex.wallet.app.dto.mypreferences

import co.nilin.opex.wallet.app.dto.mypreferences.Alias
import co.nilin.opex.wallet.app.dto.mypreferences.FeeConfig

data class Market(
    val leftSide: String,
    val rightSide: String,
    val pair: String?,
    val feeConfigs: List<FeeConfig>,
    val aliases: List<Alias>,
    val leftSideFraction: java.math.BigDecimal?,
    val rightSideFraction: java.math.BigDecimal?,
)
