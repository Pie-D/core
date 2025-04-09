package co.nilin.opex.accountant.app.config

import co.nilin.opex.accountant.app.data.mypreferences.MyPreferences
import co.nilin.opex.accountant.ports.postgres.dao.PairConfigRepository
import co.nilin.opex.accountant.ports.postgres.dao.PairFeeConfigRepository
import co.nilin.opex.accountant.ports.postgres.dao.UserLevelRepository
import co.nilin.opex.accountant.ports.postgres.model.PairFeeConfigModel
import co.nilin.opex.utility.preferences.Preferences
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.DependsOn
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
@DependsOn("postgresConfig")
class InitializeService(
    private val pairConfigRepository: PairConfigRepository,
    private val pairFeeConfigRepository: PairFeeConfigRepository,
    private val userLevelRepository: UserLevelRepository,
) {

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
        preferences.userLevels.forEach {
            userLevelRepository.insert(it).awaitSingleOrNull()
        }

        preferences.markets.map {
            val pair = it.pair ?: "${it.leftSide}_${it.rightSide}"
            val leftSideCurrency = preferences.currencies.first { c -> it.leftSide == c.symbol }
            val rightSideCurrency = preferences.currencies.first { c -> it.rightSide == c.symbol }
            val leftSideFraction = (it.leftSideFraction ?: leftSideCurrency.precision)
            val rightSideFraction = (it.rightSideFraction ?: rightSideCurrency.precision)
            pairConfigRepository.insert(
                pair,
                it.leftSide,
                it.rightSide,
                leftSideFraction,
                rightSideFraction
            ).awaitSingleOrNull()
            it.feeConfigs.forEach { f ->
                runCatching {
                    pairFeeConfigRepository.save(
                        PairFeeConfigModel(
                            null,
                            pair,
                            f.direction,
                            f.userLevel,
                            f.makerFee,
                            f.takerFee
                        )
                    ).awaitSingleOrNull()
                }
            }
        }
    }
}
