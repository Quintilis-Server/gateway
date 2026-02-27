package org.quintilis.gateway.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RouteConfig(private val userKeyResolver: KeyResolver) {

    @Value("\${AUTH_SERVICE_URI:http://localhost:9000}") private lateinit var authUri: String

    @Value("\${FORUM_SERVICE_URI:http://localhost:8081}") private lateinit var forumUri: String

    @Bean
    fun customRouteLocator(builder: RouteLocatorBuilder): RouteLocator {
        return builder.routes()
                .route("auth-route") { r ->
                    r.path("/api/auth/**")
                            .filters { f ->
                                f.stripPrefix(1)
                                f.requestRateLimiter { c ->
                                    c.setRateLimiter(redisRateLimiter())
                                    c.setKeyResolver(userKeyResolver)
                                }
                            }
                            .uri(authUri)
                }
                .route("forum-route") { r ->
                    r.path("/api/forum/**")
                            .filters { f ->
                                f.stripPrefix(2)
                                f.requestRateLimiter { c ->
                                    c.setRateLimiter(redisRateLimiter())
                                    c.setKeyResolver(userKeyResolver)
                                }
                            }
                            .uri(forumUri)
                }
                .build()
    }

    @Bean
    fun redisRateLimiter(): RedisRateLimiter {
        // 10 requisições por segundo, com rajada de 20
        val limiter = RedisRateLimiter(10, 20)
        limiter.isIncludeHeaders = false

        return limiter
    }
}
