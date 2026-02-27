package org.quintilis.gateway.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod

@Configuration
@ConfigurationProperties(prefix = "quintilis.security")
class GatewayConfigProperties {
    var rules: List<SecurityRule> = emptyList()
}

data class SecurityRule(
        var path: String = "",
        var method: HttpMethod? = null,
        var roles: List<String> = emptyList(),
        var permissions: List<String> = emptyList()
)
