package co.nilin.opex.api.app.data.mypreferences

data class Chain(
    val name: String,
    val addressType: String,
    val scanners: List<Scanner>,
    val schedules: List<ChainSyncSchedule>
)
