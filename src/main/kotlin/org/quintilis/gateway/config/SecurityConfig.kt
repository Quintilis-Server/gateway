package org.quintilis.gateway.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationEntryPoint
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
import reactor.core.publisher.Mono

// ... [outras importacoes omitidas para focar na mudança]

@Configuration
@EnableWebFluxSecurity
class SecurityConfig {

    @Value("\${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private lateinit var jwkSetUri: String

    @Value("\${quintilis.auth.frontend}") private lateinit var authServerUrl: String

    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        http
                .csrf { it.disable() }
                .cors(Customizer.withDefaults())
                .authorizeExchange { exchanges ->
                    exchanges
                            .pathMatchers(HttpMethod.OPTIONS, "/**")
                            .permitAll()
                            .pathMatchers(
                                    HttpMethod.GET,
                                    "/api/auth/roles/**",
                                    "/api/auth/permissions/**",
                                    "/api/auth/users/**"
                            )
                            .hasAuthority("ROLE_ADMIN")
                            .pathMatchers("/api/auth/**")
                            .permitAll()
                            .pathMatchers(HttpMethod.GET, "/api/forum/**")
                            .permitAll()
                            .pathMatchers(HttpMethod.POST, "/api/forum/category/**")
                            .hasAuthority("ROLE_ADMIN")
                            .pathMatchers(HttpMethod.PUT, "/api/forum/category/**")
                            .hasAuthority("ROLE_ADMIN")
                            .pathMatchers(HttpMethod.DELETE, "/api/forum/category/**")
                            .hasAuthority("ROLE_ADMIN")
                            .pathMatchers(HttpMethod.POST, "/api/forum/**")
                            .authenticated()
                            .pathMatchers(HttpMethod.PUT, "/api/forum/**")
                            .authenticated()
                            .pathMatchers(HttpMethod.DELETE, "/api/forum/**")
                            .authenticated()
                            .pathMatchers(HttpMethod.PUT, "/api/auth/roles/**")
                            .hasAuthority("ROLE_ADMIN")
                            .pathMatchers(HttpMethod.PUT, "/api/auth/users/**")
                            .hasAuthority("ROLE_ADMIN")
                            .anyExchange()
                            .authenticated()
                }
                .exceptionHandling { exceptions ->
                    exceptions.authenticationEntryPoint(
                            RedirectServerAuthenticationEntryPoint(authServerUrl)
                    )
                }
                .oauth2ResourceServer { oauth2 ->
                    oauth2.jwt { jwt ->
                        jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
                    }
                }

        return http.build()
    }

    @Bean
    fun jwtDecoder(): ReactiveJwtDecoder {
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build()
    }

    private fun jwtAuthenticationConverter(): Converter<Jwt, Mono<AbstractAuthenticationToken>> {
        val grantedAuthoritiesConverter = JwtGrantedAuthoritiesConverter()
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles")
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_")

        val jwtAuthenticationConverter = JwtAuthenticationConverter()
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter)

        return ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter)
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = listOf("*")
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
