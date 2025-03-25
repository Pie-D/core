package co.nilin.opex.api.app.data.mypreferences

data class Scanner(val url: String, val maxBlockRange: Int, val delayOnRateLimit: Int, val maxParallelCall: Int)
