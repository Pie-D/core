package co.nilin.opex.bcgateway.app.data.mypreferences

data class MyPreferences(
    val addressTypes: List<AddressType>,
    val chains: List<Chain>,
    val currencies: List<Currency>,
    val markets: List<Market>,
    val userLimits: List<UserLimit>,
    val userLevels: List<String>,
    val system: System,
    val admin: Admin,
    val auth: Auth,
)
