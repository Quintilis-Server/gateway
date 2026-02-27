package org.quintilis.gateway.filter

import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class AuthenticationHeaderFilter : GlobalFilter, Ordered {

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        return exchange.getPrincipal<java.security.Principal>()
                .flatMap { principal ->
                    if (principal is JwtAuthenticationToken) {
                        val jwt = principal.token
                        val userId = jwt.claims["user_id"]?.toString() ?: ""

                        val rolesClaim = jwt.claims["roles"] as? List<*> ?: emptyList<Any>()
                        val roles = rolesClaim.map { it.toString() }.joinToString(",")

                        val permissionsClaim =
                                jwt.claims["permissions"] as? List<*> ?: emptyList<Any>()
                        val permissions = permissionsClaim.map { it.toString() }.joinToString(",")

                        val mutatedRequest =
                                exchange.request
                                        .mutate()
                                        .headers { h ->
                                            h.add("X-User-Id", userId)
                                            h.add("X-User-Roles", roles)
                                            h.add("X-User-Permissions", permissions)
                                        }
                                        .build()

                        val mutatedExchange = exchange.mutate().request(mutatedRequest).build()
                        chain.filter(mutatedExchange)
                    } else {
                        chain.filter(exchange)
                    }
                }
                .switchIfEmpty(chain.filter(exchange))
    }

    override fun getOrder(): Int {
        // Run after the security filter chain validates the token
        return -1
    }
}
