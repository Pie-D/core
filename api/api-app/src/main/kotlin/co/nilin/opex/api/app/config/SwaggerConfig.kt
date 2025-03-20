//package co.nilin.opex.api.app.config
//
//import io.swagger.v3.oas.models.Components
//import io.swagger.v3.oas.models.OpenAPI
//import io.swagger.v3.oas.models.info.Info
//import io.swagger.v3.oas.models.security.SecurityRequirement
//import io.swagger.v3.oas.models.security.SecurityScheme
//import org.springframework.beans.factory.annotation.Value
//import org.springframework.context.annotation.Bean
//import org.springframework.context.annotation.Configuration
//
//@Configuration
//class SwaggerConfig {
//
//    @Value("\${swagger.authUrl}")
//    private lateinit var authUrl: String
//
//    @Bean
//    fun openAPI(): OpenAPI {
//        return OpenAPI()
//            .info(
//                Info()
//                    .title("OPEX API")
//                    .description("Backend for OPEX exchange.")
//                    .version("0.1")
//                    .license(
//                        io.swagger.v3.oas.models.info.License()
//                            .name("MIT License")
//                            .url("https://github.com/opexdev/Back-end/blob/feature/1-MVP/LICENSE")
//                    )
//            )
//            .addSecurityItem(SecurityRequirement().addList("oauth2"))
//            .components(
//                Components().addSecuritySchemes(
//                    "oauth2",
//                    SecurityScheme()
//                        .type(SecurityScheme.Type.OAUTH2)
//                        .flows(
//                            io.swagger.v3.oas.models.security.OAuthFlows()
//                                .password(
//                                    io.swagger.v3.oas.models.security.OAuthFlow()
//                                        .tokenUrl(authUrl)
//                                        .scopes(io.swagger.v3.oas.models.security.Scopes().addString("openid", "OpenId"))
//                                )
//                        )
//                )
//            )
//    }
//}
