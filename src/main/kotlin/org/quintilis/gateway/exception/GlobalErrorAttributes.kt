package org.quintilis.gateway.exception

import org.springframework.boot.web.error.ErrorAttributeOptions
import org.springframework.boot.webflux.error.DefaultErrorAttributes
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class GlobalErrorAttributes : DefaultErrorAttributes() {

    override fun getErrorAttributes(request: ServerRequest, options: ErrorAttributeOptions): MutableMap<String, Any> {
        val map = super.getErrorAttributes(request, options)
        
        val status = map["status"] as? Int ?: 500
        val error = map["error"] as? String ?: "Error"
        val message = map["message"] as? String ?: "Unexpected error"
        val path = map["path"] as? String ?: ""

        val responseMap = mutableMapOf<String, Any>(
            "success" to false,
            "message" to "$error: $message",
            // "data" removido pois não pode ser null em MutableMap<String, Any>
            "errorCode" to status.toString(),
            "timestamp" to LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
            "path" to path
        )
        
        return responseMap
    }
}
