package org.quintilis.gateway.config

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Mono
import java.security.Principal

@Configuration
class RateLimiterConfig {

    @Bean
    fun userKeyResolver(): KeyResolver {
        return KeyResolver { exchange ->
            exchange.getPrincipal<Principal>()
                .map { it.name }
                .defaultIfEmpty(exchange.request.remoteAddress?.address?.hostAddress ?: "anonymous")
        }
    }
}
