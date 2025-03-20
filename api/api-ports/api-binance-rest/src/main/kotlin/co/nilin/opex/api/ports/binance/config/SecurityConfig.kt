package co.nilin.opex.api.ports.binance.config

import co.nilin.opex.api.core.spi.APIKeyFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.server.WebFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

@EnableWebFluxSecurity
@Configuration
class SecurityConfig(
    private val webClient: WebClient,
    private val apiKeyFilter: APIKeyFilter,
    @Value("\${app.auth.cert-url}")
    private val jwkUrl: String
) {

    @Bean
    fun corsConfigurationSource(): UrlBasedCorsConfigurationSource {
        val source = UrlBasedCorsConfigurationSource()
        val config = CorsConfiguration().apply {
            allowedOrigins = listOf("*")
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = false
        }
        source.registerCorsConfiguration("/**", config)
        return source
    }

    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http.csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .authorizeExchange {
                it.pathMatchers("/actuator/**").permitAll()
                    .pathMatchers("/swagger-ui/**").permitAll()
                    .pathMatchers("/swagger-resources/**").permitAll()
                    .pathMatchers("/v2/api-docs").permitAll()
                    .pathMatchers("/v3/api-docs/**").permitAll()
                    .pathMatchers("/swagger-ui.html").permitAll()
                    .pathMatchers("/v3/depth").permitAll()
                    .pathMatchers("/v3/trades").permitAll()
                    .pathMatchers("/v3/ticker/**").permitAll()
                    .pathMatchers("/v3/exchangeInfo").permitAll()
                    .pathMatchers("/v3/currencyInfo/**").permitAll()
                    .pathMatchers("/v3/klines").permitAll()
                    .pathMatchers("/socket").permitAll()
                    .pathMatchers("/v1/landing/**").permitAll()
                    .pathMatchers("/**").hasAuthority("SCOPE_trust")
                    .anyExchange().authenticated()
            }
            .addFilterBefore(apiKeyFilter as WebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .oauth2ResourceServer { it.jwt(Customizer.withDefaults()) }
            .build()
    }

    @Bean
    @Throws(Exception::class)
    fun reactiveJwtDecoder(): ReactiveJwtDecoder? {
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwkUrl)
            .webClient(webClient)
            .build()
    }
}
