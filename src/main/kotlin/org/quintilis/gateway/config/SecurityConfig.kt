package org.quintilis.gateway.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
import reactor.core.publisher.Mono

// ... [outras importacoes omitidas para focar na mudança]

@Configuration
@EnableWebFluxSecurity
class SecurityConfig(private val properties: GatewayConfigProperties) {

    @Value("\${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private lateinit var jwkSetUri: String

    @Value("\${quintilis.auth.frontend}") private lateinit var authServerUrl: String

    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        http
                .csrf { it.disable() }
                .cors(Customizer.withDefaults())
                .authorizeExchange { exchanges ->
                    exchanges.pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                    // Apply dynamic rules from YAML
                    properties.rules.forEach { rule ->
                        val authorities = mutableListOf<String>()
                        rule.roles.forEach { authorities.add("ROLE_$it") }
                        rule.permissions.forEach {
                            authorities.add(it)
                        } // Permissions don't get the ROLE_ prefix

                        val matcher =
                                if (rule.method != null) {
                                    exchanges.pathMatchers(rule.method, rule.path)
                                } else {
                                    exchanges.pathMatchers(rule.path)
                                }

                        if (authorities.isNotEmpty()) {
                            matcher.hasAnyAuthority(*authorities.toTypedArray())
                        } else {
                            matcher.authenticated()
                        }
                    }

                    // Default Fallbacks
                    exchanges
                            .pathMatchers("/api/auth/**")
                            .permitAll() // Login/Register routes are public
                    exchanges
                            .pathMatchers(HttpMethod.GET, "/api/forum/**")
                            .permitAll() // Reading the forum is public
                    exchanges.anyExchange().authenticated()
                }
                .exceptionHandling { exceptions ->
                    exceptions.authenticationEntryPoint(
                            HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED)
                    )
                    exceptions.accessDeniedHandler(
                            org.springframework.security.web.server.authorization
                                    .HttpStatusServerAccessDeniedHandler(HttpStatus.FORBIDDEN)
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
        val grantedAuthoritiesConverter =
                Converter<Jwt, Collection<org.springframework.security.core.GrantedAuthority>> { jwt
                    ->
                    val authorities =
                            mutableListOf<org.springframework.security.core.GrantedAuthority>()

                    // Extract roles
                    val roles = jwt.getClaimAsStringList("roles") ?: emptyList()
                    roles.forEach { role ->
                        authorities.add(
                                org.springframework.security.core.authority.SimpleGrantedAuthority(
                                        "ROLE_$role"
                                )
                        )
                    }

                    // Extract permissions
                    val permissions = jwt.getClaimAsStringList("permissions") ?: emptyList()
                    permissions.forEach { permission ->
                        authorities.add(
                                org.springframework.security.core.authority.SimpleGrantedAuthority(
                                        permission
                                )
                        )
                    }

                    authorities
                }

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
