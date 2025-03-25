package co.nilin.opex.wallet.app.dto.mypreferences

data class Scanner(val url: String, val maxBlockRange: Int, val delayOnRateLimit: Int, val maxParallelCall: Int)
