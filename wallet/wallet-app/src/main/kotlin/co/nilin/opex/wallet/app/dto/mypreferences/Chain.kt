package co.nilin.opex.wallet.app.dto.mypreferences

data class Chain(
    val name: String,
    val addressType: String,
    val scanners: List<Scanner>,
    val schedules: List<ChainSyncSchedule>
)
