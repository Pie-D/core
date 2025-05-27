package co.nilin.opex.wallet.core.inout


import java.math.BigInteger

data class SendTxRequestData (
    val toAddress: String?,
    val amountInWei: BigInteger
)
