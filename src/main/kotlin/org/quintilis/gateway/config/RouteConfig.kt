package org.quintilis.gateway.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RouteConfig {

    @Value("\${AUTH_SERVICE_URI:http://localhost:9000}")
    private lateinit var authUri: String

    @Value("\${FORUM_SERVICE_URI:http://localhost:8081}")
    private lateinit var forumUri: String

    @Bean
    fun customRouteLocator(builder: RouteLocatorBuilder): RouteLocator {
        return builder.routes()
            .route("auth-route") { r ->
                r.path("/api/auth/**")
                    .filters { f -> f.stripPrefix(1) }
                    .uri(authUri)
            }
            .route("forum-route") { r ->
                r.path("/api/forum/**")
                    .filters { f -> f.stripPrefix(2) }
                    .uri(forumUri)
            }
            .build()
    }
}
