package co.nilin.opex.api.app.config

import co.nilin.opex.api.ports.postgres.dao.SymbolMapRepository
import co.nilin.opex.api.ports.postgres.model.SymbolMapModel
import co.nilin.opex.utility.preferences.Preferences
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.DependsOn
import org.springframework.stereotype.Component
import co.nilin.opex.api.app.data.mypreferences.MyPreferences

@Component
@DependsOn("postgresConfig")
class InitializeService(private val symbolMapRepository: SymbolMapRepository) {

//    @Autowired
//    private lateinit var preferences: Preferences

    @Autowired
    private lateinit var preferences: MyPreferences

    private val logger = LoggerFactory.getLogger(InitializeService::class.java)

    @PostConstruct
    fun init() = runBlocking {
        logger.info("Initializing symbol map...")
//        logger.info("My Preferences = $myPreferences")
        logger.info("Preferences = $preferences")
        if (preferences.currencies.isEmpty()) {
            logger.warn("No currencies found in preferences")
        } else {
            preferences.currencies.forEach {
                logger.info("Log currencies from preferences")
                logger.info("Currency $it")
            }
        }
        preferences.markets.map {
            val pair = it.pair ?: "${it.leftSide}_${it.rightSide}"
            val items = it.aliases.map { a -> SymbolMapModel(null, pair, a.key, a.alias) }
            runCatching { symbolMapRepository.saveAll(items).collectList().awaitSingleOrNull() }
        }
    }
}
