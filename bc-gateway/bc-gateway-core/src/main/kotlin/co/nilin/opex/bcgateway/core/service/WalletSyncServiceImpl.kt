package co.nilin.opex.bcgateway.core.service

import co.nilin.opex.bcgateway.core.api.WalletSyncService
import co.nilin.opex.bcgateway.core.model.CurrencyImplementation
import co.nilin.opex.bcgateway.core.model.Deposit
import co.nilin.opex.bcgateway.core.model.Transfer
import co.nilin.opex.bcgateway.core.spi.AssignedAddressHandler
import co.nilin.opex.bcgateway.core.spi.CurrencyHandler
import co.nilin.opex.bcgateway.core.spi.DepositHandler
import co.nilin.opex.bcgateway.core.spi.WalletProxy
import co.nilin.opex.bcgateway.core.utils.LoggerDelegate
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.Logger
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClient
import java.math.BigDecimal

@Service
class WalletSyncServiceImpl(
    private val walletProxy: WalletProxy,
    private val assignedAddressHandler: AssignedAddressHandler,
    private val currencyHandler: CurrencyHandler,
    private val depositHandler: DepositHandler,
) : WalletSyncService {

    private val logger: Logger by LoggerDelegate()
    private val webClient = WebClient.builder().build()
    @Transactional
    override suspend fun syncTransfers(transfers: List<Transfer>) = coroutineScope {
        val groupedByChain = currencyHandler.fetchAllImplementations().groupBy { it.chain.name }
        val deposits = transfers.mapNotNull {
            coroutineScope {
                val currencyImpl = groupedByChain[it.chain]?.find { c -> c.tokenAddress == it.tokenAddress }
                    ?: throw IllegalStateException("Currency implementation not found")
                assignedAddressHandler.findUuid(it.receiver.address, it.receiver.memo)?.let { it to currencyImpl }
            }?.let { (uuid, currencyImpl) ->
                sendDeposit(uuid, currencyImpl, it)
                logger.info("Deposit synced for $uuid on ${currencyImpl.currency.symbol} - to ${it.receiver.address}")
                it
            }
        }.map {
            Deposit(
                null,
                it.txHash,
                it.receiver.address,
                it.receiver.memo,
                it.amount,
                it.chain,
                it.isTokenTransfer,
                it.tokenAddress
            )
        }.toList()
        depositHandler.saveAll(deposits)
    }
    @Transactional
    override suspend fun batchTransfers(transfers: List<Transfer>) = coroutineScope {
        val groupedByChain = currencyHandler.fetchAllImplementations().groupBy { it.chain.name }
        val deposits = transfers.mapNotNull {
            coroutineScope {
                val currencyImpl = groupedByChain[it.chain]?.find { c ->
                    if (it.isTokenTransfer) c.tokenAddress == it.tokenAddress
                    else c.tokenAddress == null
                } ?: throw IllegalStateException("CurrencyImpl not found for ${it.chain} ${it.tokenAddress}")
                assignedAddressHandler.findUuid(it.receiver.address, it.receiver.memo)?.let { it to currencyImpl }
//
            }?.let { (uuid, currencyImpl) ->
                if (it.chain == "ethereum") {
                    val receipt = getReceiptsFromBatch(it)
                    if (receipt != null) {
                        sendDeposit(uuid, currencyImpl, it)
                        logger.info("Deposit  synced for $uuid on ${currencyImpl.currency.symbol} - to ${it.receiver.address} from chain Ethereum")
                        it
                    } else {
                        logger.info("Transaction ${it.txHash} on ${it.chain} is not done yet. Skipping.")
                        null // Chưa done, bỏ qua
                    }
                }else{
                    sendDeposit(uuid, currencyImpl, it)
                    logger.info("Deposit synced for $uuid on ${currencyImpl.currency.symbol} - to ${it.receiver.address} from chain ${it.chain}")
                    it
                }
            }
        }.map {
                Deposit(
                    null,
                    it.txHash,
                    it.receiver.address,
                    it.receiver.memo,
                    it.amount,
                    it.chain,
                    it.isTokenTransfer,
                    it.tokenAddress
                )
            }.toList()
        depositHandler.saveAll(deposits)
    }

    private suspend fun sendDeposit(uuid: String, currencyImpl: CurrencyImplementation, transfer: Transfer) {
        val amount = transfer.amount.divide(BigDecimal.TEN.pow(currencyImpl.decimal))
        val symbol = currencyImpl.currency.symbol
        logger.info("Sending deposit to $uuid - $amount $symbol")
        walletProxy.transfer(uuid, symbol, amount, transfer.txHash,transfer.chain)
    }
    private suspend fun getReceiptsFromBatch(transfer: Transfer): Transfer? {
        val validatedTransfers: List<Transfer>? = webClient.post()
            .uri("http://core-ethereum-scanner-1:8880/api/blockchain/completed")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(listOf(transfer))
            .retrieve()
            .onStatus({ t -> t.isError }, { it.createException() })
            .bodyToMono(object : ParameterizedTypeReference<List<Transfer>>() {})
            .awaitSingle()

        return validatedTransfers?.firstOrNull();
    }
}
