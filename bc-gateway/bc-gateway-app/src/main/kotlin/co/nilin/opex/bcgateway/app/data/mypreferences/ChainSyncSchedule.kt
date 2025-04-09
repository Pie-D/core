package co.nilin.opex.bcgateway.app.data.mypreferences

data class ChainSyncSchedule(
    val workerType: String,
    val delay: Long,
    val timeout: Int,
    val maxRetries: Int,
    val confirmations: Int,
    val maxBlockCount: Int,
    val enabled: Boolean
)
